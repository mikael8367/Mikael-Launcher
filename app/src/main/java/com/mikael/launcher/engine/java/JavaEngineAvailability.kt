package com.mikael.launcher.engine.java

import com.mikael.launcher.engine.EngineAvailability
import com.mikael.launcher.engine.EngineId
import com.mikael.launcher.engine.GameEngine

/**
 * Honest placeholder for the Java backend until a verified FCL adapter exists.
 *
 * UI configuration is not evidence that a runtime, renderer or game process
 * can actually be started.
 */
class JavaEngineAvailability : GameEngine {
    override val id: EngineId = EngineId.MINECRAFT_JAVA

    override fun availability(): EngineAvailability.Unavailable =
        EngineAvailability.Unavailable(
            reasonCode = "backend_not_integrated",
            message = "O motor Minecraft Java ainda está em integração."
        )
}
