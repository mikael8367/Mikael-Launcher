package com.mikael.launcher.fcl

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import com.mio.JavaManager
import com.mio.manager.RendererManager
import com.mio.util.DisplayUtil
import com.tungsten.fcl.activity.JVMActivity
import com.tungsten.fcl.control.MenuType
import com.tungsten.fcl.game.FCLGameLauncher
import com.tungsten.fcl.setting.Accounts
import com.tungsten.fcl.setting.Profiles
import com.tungsten.fcl.util.JnaNatives
import com.tungsten.fcllibrary.component.theme.ThemeEngine
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
import java.util.jar.JarFile
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

    /** Etapa atual da inicialização, para diagnóstico quando algo falha. */
    @Volatile
    var currentStage: String = "aguardando solicitação"
        private set

    fun isValidOfflineUsername(username: String): Boolean =
        username.matches(Regex("[A-Za-z0-9_]{1,16}"))

    /** Pasta onde o FCL grava launcher.log e minecraft.log (para diagnóstico). */
    fun currentLogDirectory(): String? = runCatching { FCLPath.LOG_DIR }.getOrNull()

    suspend fun installedVersionIds(context: Context): List<String> =
        FclEngineBootstrap.installedVersionIds(context.applicationContext)

    suspend fun launch(
        context: Context,
        versionId: String,
        username: String,
        preferences: FclLaunchPreferences = FclLaunchPreferences()
    ) {
        stage("validando solicitação de inicialização")
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
            stage("preparando serviços do FCL")
            val appContext = activity.applicationContext
            val bootstrap = FclEngineBootstrap.initialize(appContext)
            check(bootstrap.initialized) { bootstrap.message }

            stage("selecionando a versão instalada")
            check(FclEngineBootstrap.refreshAndSelectGameVersion(appContext, versionId)) {
                "Não foi possível selecionar a versão instalada no perfil FCL."
            }

            stage("resolvendo a versão e as dependências")
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
                stage("verificando arquivos essenciais do jogo")
                runTask(GameVerificationFixTask(dependencyManager, gameVersion, version))
            }

            stage("preparando bibliotecas internas do FCL")
            // These two FCL-owned patchers are extracted by the upstream launch
            // flow before preparing the JVM. They are required by some legacy
            // and modern launch paths, so materialize them before building classpath.
            unpackBuiltinJar(appContext, "/assets/game/MioLibPatcher.jar", FCLPath.LIB_PATCHER_PATH)
            unpackBuiltinJar(appContext, "/assets/game/MioLaunchWrapper.jar", FCLPath.MIO_LAUNCH_WRAPPER)

            stage("aplicando preferências de runtime, renderizador e memória")
            val setting = profile.getVersionSetting(versionId)
            // Só aponta setting.java para o runtime escolhido se ele realmente
            // estiver instalado; caso contrário mantém "Auto" e deixa o FCL
            // escolher o runtime compatível disponível.
            val requestedRuntime = preferences.runtimeFolder
                ?.takeIf { folder ->
                    val javaRoot = FCLPath.JAVA_PATH?.takeIf { it.isNotBlank() }
                    javaRoot != null && File(javaRoot, folder).resolve("release").isFile
                }
            if (preferences.runtimeFolder != null && requestedRuntime == null) {
                setting.java = "Auto"
            }
            applyFclLaunchPreferences(setting, preferences.copy(runtimeFolder = requestedRuntime))

            stage("selecionando o runtime Java compatível")
            val javaVersion = withContext(Dispatchers.IO) {
                val minimumCompatible = JavaManager.getSuitableJavaVersion(version)
                check(minimumCompatible.getVersion() > 0) {
                    "Não existe um runtime Java compatível instalado para esta versão."
                }
                val requested = if (setting.java == "Auto") {
                    minimumCompatible
                } else {
                    JavaManager.getJavaFromVersionName(setting.java)
                }
                if (requested.getVersion() < minimumCompatible.getVersion()) {
                    minimumCompatible
                } else {
                    requested
                }
            }
            setting.java = javaVersion.name
            check(javaVersion.getVersion() > 0) {
                "Não existe um runtime Java compatível instalado para esta versão."
            }
            val javaHome = File(javaVersion.getJavaPath(version))
            check(
                javaHome.resolve("release").isFile &&
                    javaHome.resolve("bin/java").isFile &&
                    javaHome.resolve("bin/java").canExecute()
            ) {
                "O runtime " + javaVersion.name + " está incompleto. Reinstale esse runtime nas configurações."
            }

            stage("preparando a conta offline")
            // A tela de contas atual mantém nomes offline locais; autenticação Microsoft
            // precisa ser integrada separadamente antes de oferecer esse tipo de conta.
            val account = Accounts.FACTORY_OFFLINE.create(
                username,
                OfflineAccountFactory.getUUIDFromUserName(username)
            )
            val authInfo = withContext(Dispatchers.IO) { account.logIn() }

            stage("validando o renderizador gráfico")
            val selectedRendererId = rendererIdForLabel(preferences.rendererLabel)
            if (selectedRendererId != null) {
                profile.setRendererOverride(versionId, selectedRendererId)
            }
            val selectedRenderer = RendererManager.findRenderer(
                profile.getEffectiveRendererOverride(versionId) ?: setting.renderer
            ) ?: throw IllegalStateException(
                "O renderizador selecionado não está registrado no FCL. Escolha GL4ES nas configurações."
            )
            val detectedGameVersion = GameVersionNumber.asGameVersion(gameVersion?.takeIf { it.isNotBlank() } ?: versionId.substringBefore('-'))
            if (selectedRenderer.minMCver.isNotBlank()) {
                check(detectedGameVersion.compareTo(selectedRenderer.minMCver) >= 0) {
                    "${selectedRenderer.name} não suporta Minecraft $versionId; versão mínima: ${selectedRenderer.minMCver}."
                }
            }
            if (selectedRenderer.maxMCver.isNotBlank()) {
                check(detectedGameVersion.compareTo(selectedRenderer.maxMCver) <= 0) {
                    "${selectedRenderer.name} não suporta Minecraft $versionId; versão máxima: ${selectedRenderer.maxMCver}."
                }
            }

            stage("inicializando métricas da tela do FCL")
            // The upstream FCLActivity normally initializes ThemeEngine and calls
            // DisplayUtil.updateWindowSize() in onCreate. This launcher intentionally
            // hosts its own Compose Activity, so that FCLActivity lifecycle callback
            // never runs. FCLGameRepository.getLaunchOptions() reads screen dimensions
            // through AndroidUtilKt, which dereferences currentDisplayMetrics when
            // screenWidth/screenHeight are unset. Initialize it on the UI thread before
            // building launch arguments, matching the upstream FCLActivity contract.
            withContext(Dispatchers.Main.immediate) {
                ThemeEngine.getInstance().setupThemeEngine(activity)
                DisplayUtil.updateWindowSize(activity)
            }

            stage("montando os argumentos de inicialização")
            val launchOptions = withContext(Dispatchers.IO) {
                repository.getLaunchOptions(versionId, javaVersion, profile.gameDir, 1.0, 0.0)
            }
            check(launchOptions.renderer != null) {
                "Nenhum renderizador compatível está selecionado para esta versão."
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
                stage("preparando natives do JNA")
                withContext(Dispatchers.IO) {
                    launcher.setJnaVersion(versionName)
                    JnaNatives.ensureNativeLibrary(activity, versionName)
                }
            }
            lwjglVersion?.let(launcher::setLwjglVersion)

            stage("iniciando a JVM do Minecraft")
            val bridge = withContext(Dispatchers.IO) { launcher.launch() }

            stage("abrindo a tela do jogo")
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
            stage("jogo em execução")
        } finally {
            launching.set(false)
        }
    }

    private fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    private fun stage(description: String) {
        currentStage = description
    }

    /**
     * Extrai um JAR embutido (asset do APK do FCL) para o caminho esperado pelo
     * motor. Em Android, ClassLoader.getResourceAsStream NÃO lê do diretório
     * assets/ do APK; os patchers do FCL são assets, então a leitura correta é
     * via AssetManager. Mantemos o fallback para recurso de classe para cobrir
     * empacotamentos em que o JAR venha como recurso Java.
     */
    private fun unpackBuiltinJar(context: Context, resourcePath: String, targetPath: String) {
        val target = File(targetPath)
        val parent = target.parentFile
            ?: throw IllegalStateException("Caminho inválido para biblioteca interna do FCL: $targetPath")
        if (target.isFile && target.length() > 0L && runCatching {
                JarFile(target).use { it.entries().hasMoreElements() }
            }.getOrDefault(false)
        ) return
        if (!parent.exists() && !parent.mkdirs()) {
            throw IllegalStateException("Não foi possível criar a pasta de bibliotecas internas do FCL.")
        }
        val temp = File(parent, target.name + ".tmp")
        try {
            val assetPath = resourcePath.removePrefix("/assets/").removePrefix("/")
            val stream = try {
                context.assets.open(assetPath)
            } catch (_: Exception) {
                // Fallback: alguns empacotamentos incluem o JAR como recurso Java.
                FclGameLaunchCoordinator::class.java.getResourceAsStream(resourcePath)
                    ?: throw IllegalStateException("Biblioteca interna do FCL não encontrada: $resourcePath")
            }
            stream.use { input ->
                Files.copy(input, temp.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            check(temp.length() > 0L) { "Biblioteca interna do FCL vazia: ${target.name}" }
            JarFile(temp).use { jar ->
                check(jar.entries().hasMoreElements()) {
                    "Biblioteca interna do FCL inválida: ${target.name}"
                }
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

/**
 * Monta o texto de diagnóstico exibido ao usuário quando a inicialização falha.
 * Puro (sem Android), para poder ser testado na JVM.
 */
fun buildLaunchDiagnostic(error: Throwable, stage: String, logDir: String?): String {
    val causes = buildList {
        val visited = HashSet<Throwable>()
        var cause: Throwable? = error
        while (cause != null && size < 5 && visited.add(cause)) {
            add("${cause.javaClass.simpleName}: ${cause.message.orEmpty().ifBlank { "(sem mensagem)" }}")
            cause = cause.cause
        }
    }
    return buildString {
        appendLine("A inicialização falhou na etapa: $stage")
        appendLine()
        appendLine("Erros:")
        causes.forEachIndexed { index, line ->
            appendLine("${index + 1}. $line")
        }
        if (!logDir.isNullOrBlank()) {
            appendLine()
            appendLine("Logs completos do FCL (arquivos launcher.log e minecraft.log):")
            appendLine(logDir)
        }
    }
}
