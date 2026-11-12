package com.mikael.launcher.engine.java

import android.os.Build
import com.mikael.launcher.engine.EngineAvailability
import com.mikael.launcher.engine.EngineCapability
import com.mikael.launcher.engine.EngineId
import com.mikael.launcher.engine.GameEngine
import com.mikael.launcher.fcl.FclEngineBootstrap
import com.mikael.launcher.fcl.FclRuntimeInstaller
import com.tungsten.fclauncher.utils.FCLPath
import java.io.File

/**
 * Reports readiness from the real FCL bootstrap, validated ARM64 Java runtimes,
 * and registered renderers. UI selections alone never mark the engine ready.
 */
class JavaEngineAvailability(
    private val readinessProvider: () -> JavaEngineReadiness = ::readFclReadiness
) : GameEngine {
    override val id: EngineId = EngineId.MINECRAFT_JAVA

    override fun availability(): EngineAvailability {
        val readiness = runCatching(readinessProvider).getOrElse { failure ->
            JavaEngineReadiness(
                initialized = false,
                validRuntimeCount = 0,
                rendererCount = 0,
                failureMessage = failure.message ?: "Falha ao verificar o motor FCL."
            )
        }
        if (readiness.failureMessage != null) {
            return EngineAvailability.Unavailable(
                "bootstrap_failed",
                readiness.failureMessage.ifBlank { "Falha ao verificar o motor FCL." }
            )
        }
        if (!readiness.initialized) return EngineAvailability.Initializing
        if (readiness.validRuntimeCount == 0) {
            return EngineAvailability.Unavailable(
                "java_runtime_missing",
                "Instale um runtime Java ARM64 nas configurações antes de iniciar o Minecraft."
            )
        }
        if (readiness.rendererCount == 0) {
            return EngineAvailability.Unavailable(
                "renderer_missing",
                "Nenhum renderizador gráfico do FCL foi inicializado."
            )
        }
        return EngineAvailability.Ready(
            setOf(
                EngineCapability.VERSION_DISCOVERY,
                EngineCapability.VERSION_INSTALLATION,
                EngineCapability.RUNTIME_MANAGEMENT,
                EngineCapability.MOD_LOADER_INSTALLATION,
                EngineCapability.GAME_LAUNCH
            )
        )
    }
}

data class JavaEngineReadiness(
    val initialized: Boolean,
    val validRuntimeCount: Int,
    val rendererCount: Int,
    val failureMessage: String? = null
)

private fun readFclReadiness(): JavaEngineReadiness {
    val status = FclEngineBootstrap.status()
    if (status.stage == "failed") {
        return JavaEngineReadiness(
            initialized = false,
            validRuntimeCount = 0,
            rendererCount = 0,
            failureMessage = status.message
        )
    }
    if (!status.initialized) return JavaEngineReadiness(false, 0, 0)

    val abi = Build.SUPPORTED_ABIS.firstOrNull()
        ?: return JavaEngineReadiness(true, 0, status.rendererCount)
    val javaRoot = FCLPath.JAVA_PATH?.takeIf { it.isNotBlank() }?.let(::File)
    val runtimeCheck = runCatching {
        if (abi == "arm64-v8a" && javaRoot != null) {
            FclRuntimeInstaller.installedRuntimes().count { runtimeName ->
                JavaRuntimeInspector.inspect(File(javaRoot, runtimeName), abi) is JavaRuntimeInspection.Valid
            }
        } else {
            0
        }
    }
    val validRuntimeCount = runtimeCheck.getOrElse { failure ->
        return JavaEngineReadiness(
            initialized = true,
            validRuntimeCount = 0,
            rendererCount = status.rendererCount,
            failureMessage = failure.message ?: "Não foi possível verificar os runtimes Java."
        )
    }
    return JavaEngineReadiness(
        initialized = true,
        validRuntimeCount = validRuntimeCount,
        rendererCount = status.rendererCount
    )
}
