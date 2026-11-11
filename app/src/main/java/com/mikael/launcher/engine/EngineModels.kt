package com.mikael.launcher.engine

/**
 * Stable identifiers for the independent game engines hosted by Mikael Launcher.
 *
 * These identifiers are persisted/diagnostic values. Do not rename them without
 * a migration strategy.
 */
enum class EngineId(val value: String) {
    MINECRAFT_JAVA("minecraft-java"),
    HORIZON_RUST("horizon-rust")
}

/** Capabilities are explicit and must only be advertised after backend verification. */
enum class EngineCapability {
    VERSION_DISCOVERY,
    VERSION_INSTALLATION,
    RUNTIME_MANAGEMENT,
    MOD_LOADER_INSTALLATION,
    MOD_MANAGEMENT,
    RESOURCE_PACK_MANAGEMENT,
    SHADER_MANAGEMENT,
    WORLD_MANAGEMENT,
    GAME_LAUNCH,
    LOG_STREAMING
}

/** Current readiness of an engine implementation. */
sealed interface EngineAvailability {
    data class Unavailable(
        val reasonCode: String,
        val message: String
    ) : EngineAvailability {
        init {
            require(reasonCode.isNotBlank()) { "reasonCode must not be blank" }
            require(message.isNotBlank()) { "message must not be blank" }
        }
    }

    data object Initializing : EngineAvailability

    data class Ready(
        val capabilities: Set<EngineCapability>
    ) : EngineAvailability
}

/** Lifecycle-neutral progress and result state for long-running engine operations. */
sealed interface EngineOperationState {
    data object Idle : EngineOperationState

    data class Running(
        val stage: String,
        val progress: Float? = null
    ) : EngineOperationState {
        init {
            require(stage.isNotBlank()) { "stage must not be blank" }
            require(progress == null || progress in 0f..1f) {
                "progress must be between 0.0 and 1.0"
            }
        }
    }

    data object Cancelled : EngineOperationState

    data object Succeeded : EngineOperationState

    data class Failed(
        val code: String,
        val message: String
    ) : EngineOperationState {
        init {
            require(code.isNotBlank()) { "code must not be blank" }
            require(message.isNotBlank()) { "message must not be blank" }
        }
    }
}
