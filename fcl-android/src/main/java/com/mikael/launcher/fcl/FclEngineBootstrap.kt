package com.mikael.launcher.fcl

import android.content.Context
import com.mio.JavaManager
import com.mio.manager.RendererManager
import com.tungsten.fcl.setting.ConfigHolder
import com.tungsten.fcl.setting.Controllers
import com.tungsten.fcl.setting.Profiles
import com.tungsten.fclauncher.utils.FCLPath
import com.tungsten.fclcore.util.Logging
import java.io.File
import java.nio.file.Paths
import java.util.logging.Level
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class FclBootstrapStatus(
    val initialized: Boolean,
    val stage: String,
    val message: String,
    val installedJavaRuntimes: List<String> = emptyList(),
    val rendererCount: Int = 0,
    val usingAppPrivateGameDirectory: Boolean = false
) {
    val hasAnyJavaRuntime: Boolean
        get() = installedJavaRuntimes.isNotEmpty()
}

/**
 * Initializes FCL's process-wide services without starting its original launcher UI.
 *
 * The order mirrors the pinned upstream initialization flow: paths/logging,
 * renderers, Java discovery, ConfigHolder (accounts and profiles), then controller
 * layouts. All blocking disk work runs on Dispatchers.IO.
 */
object FclEngineBootstrap {
    private val mutex = Mutex()
    private val processScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initializeInBackground(context: Context): Job =
        processScope.launch { initialize(context.applicationContext) }

    @Volatile
    private var currentStatus = FclBootstrapStatus(
        initialized = false,
        stage = "not_started",
        message = "O backend FCL ainda não foi inicializado."
    )

    fun status(): FclBootstrapStatus = currentStatus

    /**
     * Applies a changed storage selection without restarting the process-wide FCL services.
     * The selected profile repository is switched to the same real File path used by installers.
     */
    suspend fun updateGameDirectory(context: Context): Boolean = mutex.withLock {
        if (!currentStatus.initialized) return@withLock false
        withContext(Dispatchers.IO) {
            val target = FclGameDirectoryResolver.resolve(context.applicationContext).canonicalFile
            if (!target.exists() && !target.mkdirs()) {
                throw IllegalStateException("Não foi possível criar a nova pasta do Minecraft.")
            }
            FCLPath.PRIVATE_COMMON_DIR = target.absolutePath
            val profile = Profiles.getSelectedProfile()
            if (runCatching { profile.gameDir.canonicalFile != target }.getOrDefault(true)) {
                profile.gameDir = target
                Profiles.setSelectedProfile(profile)
            }
            profile.repository.refreshVersions()
            true
        }
    }

    suspend fun installedVersionIds(context: Context): List<String> {
        if (!currentStatus.initialized) initialize(context.applicationContext)
        return mutex.withLock {
            check(currentStatus.initialized) { currentStatus.message }
            withContext(Dispatchers.IO) {
                val target = FclGameDirectoryResolver.resolve(context.applicationContext).canonicalFile
                val profile = Profiles.getSelectedProfile()
                if (profile.gameDir.canonicalFile != target) {
                    profile.gameDir = target
                    Profiles.setSelectedProfile(profile)
                }
                profile.repository.refreshVersions()
                profile.repository.versions.map { it.id }.distinct().sorted()
            }
        }
    }

    suspend fun refreshAndSelectGameVersion(context: Context, versionId: String): Boolean {
        if (!currentStatus.initialized) {
            initialize(context.applicationContext)
        }
        return mutex.withLock {
            if (!currentStatus.initialized) return@withLock false
            withContext(Dispatchers.IO) {
                val target = FclGameDirectoryResolver.resolve(context.applicationContext).canonicalFile
                val profile = Profiles.getSelectedProfile()
                if (profile.gameDir.canonicalFile != target) {
                    profile.gameDir = target
                }
                profile.repository.refreshVersions()
                check(profile.repository.hasVersion(versionId)) {
                    "O FCL não encontrou o perfil instalado: $versionId"
                }
                profile.selectedVersion = versionId
                Profiles.setSelectedProfile(profile)
                true
            }
        }
    }

    suspend fun initialize(context: Context): FclBootstrapStatus = mutex.withLock {
        if (currentStatus.initialized) return@withLock currentStatus

        currentStatus = FclBootstrapStatus(
            initialized = false,
            stage = "initializing",
            message = "Preparando bibliotecas e serviços do FCL."
        )

        try {
            withContext(Dispatchers.IO) {
                val appContext = context.applicationContext

                // FCLPath contains static paths used by ConfigHolder and other FCL
                // classes. It must be initialized before those classes are touched.
                FCLPath.loadPaths(appContext)
                val appPrivateGameDirectory = File(appContext.filesDir, "minecraft").canonicalFile

                // The selected SAF directory is resolved to a real local filesystem
                // path because FCL's game repository and Minecraft process use java.io.File.
                val selectedGameDirectory = FclGameDirectoryResolver.resolve(appContext).canonicalFile
                if (!selectedGameDirectory.exists() && !selectedGameDirectory.mkdirs()) {
                    throw IllegalStateException("Não foi possível criar a pasta selecionada do Minecraft.")
                }
                FCLPath.PRIVATE_COMMON_DIR = selectedGameDirectory.absolutePath

                // Keep FCL logs, controllers and runtime support files app-private.
                // Only the game directory follows the user's selected local folder.
                val fclPrivateRoot = File(appContext.filesDir, "fcl").apply {
                    if (!exists() && !mkdirs()) {
                        throw IllegalStateException("Não foi possível criar o diretório privado do FCL.")
                    }
                }
                FCLPath.LOG_DIR = File(fclPrivateRoot, "log").absolutePath
                FCLPath.CONTROLLER_DIR = File(fclPrivateRoot, "control").absolutePath
                FCLPath.SHARE_DIR = File(fclPrivateRoot, "share").absolutePath
                FCLPath.SHARED_COMMON_DIR = File(
                    appContext.getExternalFilesDir(null) ?: appContext.filesDir,
                    ".minecraft"
                ).absolutePath
                listOf(
                    FCLPath.LOG_DIR,
                    FCLPath.CONTROLLER_DIR,
                    FCLPath.SHARE_DIR,
                    FCLPath.SHARED_COMMON_DIR
                ).forEach { path ->
                    val directory = File(path)
                    if (!directory.exists() && !directory.mkdirs()) {
                        throw IllegalStateException("Não foi possível criar diretório do FCL: $path")
                    }
                }
                Logging.start(Paths.get(FCLPath.LOG_DIR))

                RendererManager.init(appContext)
                JavaManager.init()

                // Match upstream ordering: Settings/Accounts/Profiles are loaded
                // before controller layouts are initialized.
                if (!ConfigHolder.isInit()) {
                    ConfigHolder.init()
                }
                Controllers.init()

                // Route the currently selected FCL profile to the same directory
                // used by the version/mod-loader installers. Profile.gameDir is mutable
                // and its setter switches the repository and persists the profile.
                val activeProfile = runCatching { Profiles.getSelectedProfile() }.getOrNull()
                if (activeProfile != null &&
                    runCatching { activeProfile.gameDir.canonicalFile != selectedGameDirectory }.getOrDefault(true)
                ) {
                    activeProfile.gameDir = selectedGameDirectory
                    Profiles.setSelectedProfile(activeProfile)
                }

                // A brand-new FCL profile defaults to shared external storage.
                // Route a newly-created profile to the selected game directory. Existing
                // profile data is not deleted when the directory changes.
                if (ConfigHolder.isNewlyCreated()) {
                    val privateDir = runCatching {
                        selectedGameDirectory
                    }.getOrNull()
                    val privateProfile = privateDir?.let { target ->
                        Profiles.profiles.firstOrNull { profile ->
                            runCatching { profile.gameDir.canonicalFile == target }.getOrDefault(false)
                        }
                    }
                    if (privateProfile != null) {
                        Profiles.setSelectedProfile(privateProfile)
                    }
                }

                val installedRuntimes = File(FCLPath.JAVA_PATH)
                    .listFiles()
                    .orEmpty()
                    .filter { it.isDirectory && File(it, "release").isFile }
                    .map { it.name }
                    .sorted()

                val selectedProfile = runCatching { Profiles.getSelectedProfile() }.getOrNull()
                val privateGameDirectory = selectedProfile?.let { profile ->
                    runCatching {
                        profile.gameDir.canonicalFile ==
                            appPrivateGameDirectory
                    }.getOrDefault(false)
                } ?: false

                FclBootstrapStatus(
                    initialized = true,
                    stage = "ready",
                    message = if (installedRuntimes.isEmpty()) {
                        "Serviços FCL preparados. Ainda falta instalar um runtime Java ARM64."
                    } else {
                        "Serviços FCL preparados; runtimes Java detectados."
                    },
                    installedJavaRuntimes = installedRuntimes,
                    rendererCount = RendererManager.rendererList.size,
                    usingAppPrivateGameDirectory = privateGameDirectory
                )
            }.also { result ->
                currentStatus = result
            }
        } catch (error: Throwable) {
            runCatching {
                Logging.LOG.log(Level.SEVERE, "Failed to initialize FCL engine services", error)
            }
            FclBootstrapStatus(
                initialized = false,
                stage = "failed",
                message = "Falha ao preparar o backend FCL (${error.javaClass.simpleName})."
            ).also { currentStatus = it }
        }
    }
}
