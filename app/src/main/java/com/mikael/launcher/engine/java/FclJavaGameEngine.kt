package com.mikael.launcher.engine.java

import android.content.Context
import com.mikael.launcher.engine.EngineAvailability
import com.mikael.launcher.engine.EngineId
import com.mikael.launcher.engine.GameEngine
import com.mikael.launcher.fcl.FclGameLaunchCoordinator
import com.mikael.launcher.fcl.FclLaunchPreferences

/**
 * Concrete Minecraft Java engine adapter used by launcher orchestration.
 *
 * The adapter owns no Activity or Compose state. Android UI passes a short-lived
 * Context to operations; FCL lifecycle and JVM startup remain in the coordinator.
 */
class FclJavaGameEngine(
    private val readiness: JavaEngineAvailability = JavaEngineAvailability()
) : GameEngine {
    override val id: EngineId = EngineId.MINECRAFT_JAVA

    override fun availability(): EngineAvailability = readiness.availability()

    suspend fun installedVersionIds(context: Context): List<String> =
        FclGameLaunchCoordinator.installedVersionIds(context.applicationContext)

    suspend fun launch(
        context: Context,
        versionId: String,
        username: String,
        preferences: FclLaunchPreferences = FclLaunchPreferences()
    ) {
        FclGameLaunchCoordinator.launch(
            context = context,
            versionId = versionId,
            username = username,
            preferences = preferences
        )
    }
}
