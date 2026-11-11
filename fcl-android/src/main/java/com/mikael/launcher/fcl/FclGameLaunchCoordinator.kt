package com.mikael.launcher.fcl

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import com.mio.JavaManager
import com.mio.manager.RendererManager
import com.tungsten.fcl.activity.JVMActivity
import com.tungsten.fcl.control.MenuType
import com.tungsten.fcl.game.FCLGameLauncher
import com.tungsten.fcl.setting.Accounts
import com.tungsten.fcl.setting.Profiles
import com.tungsten.fcl.util.JnaNatives
import com.tungsten.fclauncher.utils.FCLPath
import com.tungsten.fclcore.auth.offline.OfflineAccountFactory
import com.tungsten.fclcore.download.MaintainTask
import com.tungsten.fclcore.download.game.GameVerificationFixTask
import com.tungsten.fclcore.util.LibFilter
import com.tungsten.fclcore.task.Task
import com.tungsten.fclcore.task.TaskExecutor
import com.tungsten.fclcore.task.TaskListener
import com.tungsten.fclcore.util.versioning.VersionNumber
import com.tungsten.fclcore.util.versioning.GameVersionNumber
import org.lwjgl.glfw.CallbackBridge
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Adapta a execução do FCL ao host Compose do Mikael Launcher.
 *
 * Não usa LauncherHelper porque o fluxo upstream tenta acessar a MainActivity
 * visual do FCL depois de abrir o jogo. O Mikael Launcher mantém sua própria
 * Activity principal e remove a tela original do FCL do manifesto.
 */
object FclGameLaunchCoordinator {
    private val launching = AtomicBoolean(false)

    fun isValidOfflineUsername(username: String): Boolean =
        username.matches(Regex("[A-Za-z0-9_]{1,16}"))

    suspend fun installedVersionIds(context: Context): List<String> {
        val appContext = context.applicationContext
        val bootstrap = FclEngineBootstrap.initialize(appContext)
        check(bootstrap.initialized) { bootstrap.message }
        val profile = withContext(Dispatchers.IO) { Profiles.getSelectedProfile() }
        return withContext(Dispatchers.IO) {
            profile.repository.refreshVersions()
            profile.repository.versions.map { it.id }.distinct().sorted()
        }
    }

    suspend fun launch(context: Context, versionId: String, username: String) {
        val activity = context.findActivity()
            ?: throw IllegalArgumentException("A inicialização precisa ser chamada a partir da tela do launcher.")
        require(versionId.isNotBlank()) {
            "Selecione uma versão instalada antes de iniciar."
        }
        require(isValidOfflineUsername(username)) {
            "O nome da conta offline deve ter de 1 a 16 caracteres: letras, números ou sublinhado."
        }
        check(launching.compareAndSet(false, true)) {
            "Já existe uma inicialização em andamento."
        }

        try {
            val appContext = activity.applicationContext
            val bootstrap = FclEngineBootstrap.initialize(appContext)
            check(bootstrap.initialized) { bootstrap.message }

            check(FclEngineBootstrap.refreshAndSelectGameVersion(appContext, versionId)) {
                "Não foi possível selecionar a versão instalada no perfil FCL."
            }

            val profile = withContext(Dispatchers.IO) { Profiles.getSelectedProfile() }
            val repository = profile.repository
            val resolvedVersion = withContext(Dispatchers.IO) {
                repository.getResolvedVersion(versionId)
            }

            // MaintainTask.maintain is synchronous and returns a patched Version;
            // it is not a Task and must not be passed to TaskExecutor.
            val maintainedVersion = withContext(Dispatchers.IO) {
                MaintainTask.maintain(repository, resolvedVersion)
            }
            val version = withContext(Dispatchers.IO) {
                LibFilter.filter(maintainedVersion, false)
            }
            val gameVersion = withContext(Dispatchers.IO) {
                repository.getGameVersion(maintainedVersion).orElse(null)
            }
            val verifyAbnormalLaunch = withContext(Dispatchers.IO) {
                repository.unmarkVersionLaunchedAbnormally(versionId)
            }
            val dependencyManager = profile.getDependency()
            runTask(dependencyManager.checkGameCompletionAsync(version, verifyAbnormalLaunch))

            if (gameVersion != null) {
                runTask(GameVerificationFixTask(dependencyManager, gameVersion, version))
            }

            // These two FCL-owned patchers are extracted by the upstream launch
            // flow before preparing the JVM. They are required by some legacy
            // and modern launch paths, so materialize them before building classpath.
            unpackBuiltinJar("/assets/game/MioLibPatcher.jar", FCLPath.LIB_PATCHER_PATH)
            unpackBuiltinJar("/assets/game/MioLaunchWrapper.jar", FCLPath.MIO_LAUNCH_WRAPPER)

            // Apply the launcher's real runtime, renderer and memory preferences to
            // a per-version FCL setting. Creating a local setting prevents one version
            // from silently changing the global configuration of every other profile.
            val setting = withContext(Dispatchers.IO) {
                profile.repository.createLocalVersionSetting(versionId)
                    ?: error("Não foi possível criar as configurações da versão $versionId.")
            }
            setting.isUsesGlobal = false

            val preferences = appContext.getSharedPreferences(
                "mikael_launcher_preferences",
                Context.MODE_PRIVATE
            )
            val preferredRuntimeName = when (
                preferences.getString("selected_java_runtime", "Java 8").orEmpty()
            ) {
                "Java 8" -> "jre8"
                "Java 17" -> "jre17"
                "Java 21" -> "jre21"
                "Java 25" -> "jre25"
                else -> "Auto"
            }
            val javaVersion = withContext(Dispatchers.Main) {
                val compatible = JavaManager.getSuitableJavaVersion(version)
                check(compatible.getVersion() > 0) {
                    "Não há runtime Java instalado compatível com Minecraft $versionId. Instale o Java recomendado nas configurações."
                }
                val preferred = JavaManager.getJavaFromVersionName(preferredRuntimeName)
                if (preferred.getVersion() == compatible.getVersion()) preferred else compatible
            }
            setting.java = javaVersion.name

            val rendererPreference = preferences.getString("selected_renderer", "GL4ES").orEmpty()
            val renderer = when (rendererPreference) {
                "GL4ES" -> RendererManager.RENDERER_GL4ES
                "Zink" -> RendererManager.RENDERER_ZINK
                "VGPU" -> RendererManager.RENDERER_VGPU
                else -> throw IllegalStateException(
                    "O renderizador $rendererPreference ainda não está disponível no motor FCL. Selecione GL4ES, Zink ou VGPU nas configurações."
                )
            }
            check(RendererManager.findRenderer(renderer.id) != null) {
                "O renderizador $rendererPreference não foi registrado pelo FCL."
            }
            val detectedGameVersion = GameVersionNumber.asGameVersion(gameVersion ?: versionId)
            if (renderer.minMCver.isNotBlank()) {
                check(detectedGameVersion.compareTo(renderer.minMCver) >= 0) {
                    "$rendererPreference não suporta Minecraft $versionId; versão mínima: ${renderer.minMCver}."
                }
            }
            if (renderer.maxMCver.isNotBlank()) {
                check(detectedGameVersion.compareTo(renderer.maxMCver) <= 0) {
                    "$rendererPreference não suporta Minecraft $versionId; versão máxima: ${renderer.maxMCver}."
                }
            }
            setting.renderer = renderer.id
            profile.setRendererOverride(versionId, null)

            val maxMemoryMb = preferences.getString("selected_ram_limit", "2048 MB")
                .orEmpty().substringBefore(' ').toIntOrNull()?.coerceIn(512, 4096) ?: 2048
            setting.maxMemory = maxMemoryMb
            setting.isAutoMemory = false
            setting.javaArgs = preferences.getString("selected_jvm_arguments", "").orEmpty().take(2048)

            val javaHome = File(javaVersion.getJavaPath(version))
            check(
                javaHome.resolve("release").isFile &&
                    javaHome.resolve("bin/java").isFile &&
                    javaHome.resolve("bin/java").canExecute()
            ) {
                "O runtime " + javaVersion.name + " está incompleto. Reinstale esse runtime nas configurações."
            }

            // A tela de contas atual mantém nomes offline locais; autenticação Microsoft
            // precisa ser integrada separadamente antes de oferecer esse tipo de conta.
            val account = Accounts.FACTORY_OFFLINE.create(
                username,
                OfflineAccountFactory.getUUIDFromUserName(username)
            )
            val authInfo = withContext(Dispatchers.IO) { account.logIn() }

            val launchOptions = withContext(Dispatchers.IO) {
                repository.getLaunchOptions(versionId, javaVersion, profile.gameDir, 1.0, 0.0)
            }
            val launcher = FCLGameLauncher(
                activity,
                repository,
                version,
                authInfo,
                launchOptions
            )

            var jnaVersion: String? = null
            var lwjglVersion: String? = null
            var lwjglMax: VersionNumber? = null
            for (library in version.libraries) {
                val name = library.name
                if (name.startsWith("net.java.dev.jna:jna:")) {
                    jnaVersion = library.version
                } else if (
                    name.startsWith("org.lwjgl.lwjgl:lwjgl:") ||
                    name.startsWith("org.lwjgl:lwjgl:")
                ) {
                    val candidate = VersionNumber.asVersion(library.version)
                    if (lwjglMax == null || candidate > lwjglMax) {
                        lwjglMax = candidate
                        lwjglVersion = library.version
                    }
                }
            }
            jnaVersion?.let { versionName ->
                withContext(Dispatchers.IO) {
                    launcher.setJnaVersion(versionName)
                    JnaNatives.ensureNativeLibrary(activity, versionName)
                }
            }
            lwjglVersion?.let(launcher::setLwjglVersion)

            check(launchOptions.renderer != null) {
                "Nenhum renderizador compatível está selecionado para esta versão."
            }
            val bridge = withContext(Dispatchers.IO) { launcher.launch() }

            withContext(Dispatchers.Main) {
                check(!activity.isFinishing && !activity.isDestroyed) {
                    "A tela do launcher foi encerrada antes de abrir o Minecraft."
                }
                CallbackBridge.nativeSetUseInputStackQueue(version.arguments.isPresent)
                bridge.scaleFactor = 1.0
                bridge.controller = setting.controller
                bridge.gameDir = repository.getRunDirectory(versionId).absolutePath
                bridge.java = javaVersion.getVersion().toString()
                bridge.renderer = launchOptions.renderer.name
                JVMActivity.setFCLBridge(bridge, MenuType.GAME)

                val intent = Intent(activity, JVMActivity::class.java).apply {
                    putExtra("controller", setting.controller)
                    putExtra("TERRACOTTA_PLAYER", username)
                }
                activity.startActivity(intent)
            }
        } finally {
            launching.set(false)
        }
    }

    private fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    private fun unpackBuiltinJar(resourcePath: String, targetPath: String) {
        val target = File(targetPath)
        val parent = target.parentFile
            ?: throw IllegalStateException("Caminho inválido para biblioteca interna do FCL: $targetPath")
        if (!parent.exists() && !parent.mkdirs()) {
            throw IllegalStateException("Não foi possível criar a pasta de bibliotecas internas do FCL.")
        }
        val temp = File(parent, target.name + ".tmp")
        try {
            val stream = FclGameLaunchCoordinator::class.java.getResourceAsStream(resourcePath)
                ?: throw IllegalStateException("Biblioteca interna do FCL não encontrada: $resourcePath")
            stream.use { input ->
                Files.copy(input, temp.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            try {
                Files.move(
                    temp.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (error: Throwable) {
            runCatching { Files.deleteIfExists(temp.toPath()) }
            throw IllegalStateException("Falha ao preparar biblioteca interna do FCL: ${target.name}", error)
        }
    }

    private suspend fun runTask(task: Task<*>) {
        val executor = task.executor()
        suspendCancellableCoroutine { continuation ->
            executor.addTaskListener(object : TaskListener() {
                override fun onStop(success: Boolean, executor: TaskExecutor) {
                    if (!continuation.isActive) return
                    if (success) {
                        continuation.resume(Unit)
                    } else {
                        val error = executor.exception
                        if (error != null) continuation.resumeWithException(error)
                        else continuation.cancel(CancellationException("A operação do FCL foi cancelada."))
                    }
                }
            })
            executor.start()
            continuation.invokeOnCancellation {
                runCatching { executor.cancel() }
            }
        }
    }
}
