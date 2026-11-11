package com.mikael.launcher.fcl

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.mio.JavaManager
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

    suspend fun launch(context: Context, versionId: String, username: String) {
        require(context is Activity) {
            "A inicialização precisa ser chamada a partir da tela do launcher."
        }
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
            val appContext = context.applicationContext
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

            val setting = profile.getVersionSetting(versionId)
            // JavaManager scans runtime metadata and resolves filesystem paths.
            // Keep that work off the UI thread to avoid freezing the launcher on Android.
            val javaVersion = withContext(Dispatchers.IO) {
                if (setting.java == "Auto") {
                    JavaManager.getSuitableJavaVersion(version)
                } else {
                    JavaManager.getJavaFromVersionName(setting.java)
                }
            }
            check(javaVersion.getVersion() > 0) {
                "Não existe um runtime Java compatível instalado para esta versão."
            }
            withContext(Dispatchers.IO) {
                val javaHome = File(javaVersion.getJavaPath(version))
                check(javaHome.resolve("release").isFile && javaHome.resolve("bin/java").isFile) {
                    "O runtime " + javaVersion.name + " está incompleto. Reinstale esse runtime nas configurações."
                }
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
                context,
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
                    JnaNatives.ensureNativeLibrary(context, versionName)
                }
            }
            lwjglVersion?.let(launcher::setLwjglVersion)

            val bridge = withContext(Dispatchers.IO) { launcher.launch() }

            withContext(Dispatchers.Main) {
                check(!context.isFinishing && !context.isDestroyed) {
                    "A tela do launcher foi encerrada antes de abrir o Minecraft."
                }
                check(launchOptions.renderer != null) {
                    "Nenhum renderizador compatível está selecionado para esta versão."
                }
                CallbackBridge.nativeSetUseInputStackQueue(version.arguments.isPresent)
                bridge.scaleFactor = 1.0
                bridge.controller = setting.controller
                bridge.gameDir = repository.getRunDirectory(versionId).absolutePath
                bridge.java = javaVersion.getVersion().toString()
                bridge.renderer = launchOptions.renderer.name
                JVMActivity.setFCLBridge(bridge, MenuType.GAME)

                val intent = Intent(context, JVMActivity::class.java).apply {
                    putExtra("controller", setting.controller)
                    putExtra("TERRACOTTA_PLAYER", username)
                }
                context.startActivity(intent)
            }
        } finally {
            launching.set(false)
        }
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
