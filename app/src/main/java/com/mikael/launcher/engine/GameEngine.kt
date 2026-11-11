package com.mikael.launcher.engine

/**
 * Small engine boundary consumed by launcher orchestration.
 *
 * Implementations must not retain an Activity or Compose object. Android UI,
 * rendering surfaces and process lifecycle belong in platform adapters.
 */
interface GameEngine {
    val id: EngineId

    /** Returns a truthful snapshot; never infer readiness from UI selections. */
    fun availability(): EngineAvailability
}
