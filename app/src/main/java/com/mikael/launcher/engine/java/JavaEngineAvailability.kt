package com.mikael.launcher.engine.java

import com.mikael.launcher.engine.EngineAvailability
import com.mikael.launcher.engine.EngineId
import com.mikael.launcher.engine.GameEngine

/**
 * Honest placeholder for the Java backend until the FCL adapter is integrated.
 *
 * This prevents the launcher from presenting a selectable UI configuration as
 * proof that a runtime, renderer or game process can actually be started.
 */
class JavaEngineAvailability(
    private val backendReady: Boolean = false
) : GameEngine {
    override val id: EngineId = EngineId.MINECRAFT_JAVA

    override fun availability(): EngineAvailability =
        if (backendReady) {
            // A future adapter must replace this class and supply verified capabilities.
            EngineAvailability.Unavailable(
                reasonCode = "adapter_not_bound",
                message = "O adaptador Java ainda não foi conectado ao launcher."
            )
        } else {
            EngineAvailability.Unavailable(
                reasonCode = "backend_not_integrated",
                message = "O motor Minecraft Java ainda está em integração."
            )
        }
}
