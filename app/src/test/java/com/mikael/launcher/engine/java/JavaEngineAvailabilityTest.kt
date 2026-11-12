package com.mikael.launcher.engine.java

import com.mikael.launcher.engine.EngineAvailability
import com.mikael.launcher.engine.EngineCapability
import com.mikael.launcher.engine.EngineId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JavaEngineAvailabilityTest {
    @Test
    fun reportsInitializingBeforeFclBootstrapCompletes() {
        val engine = JavaEngineAvailability {
            JavaEngineReadiness(false, 0, 0)
        }
        assertEquals(EngineId.MINECRAFT_JAVA, engine.id)
        assertTrue(engine.availability() is EngineAvailability.Initializing)
    }

    @Test
    fun doesNotClaimReadyWithoutValidatedRuntime() {
        val state = JavaEngineAvailability {
            JavaEngineReadiness(true, 0, 5)
        }.availability()
        assertTrue(state is EngineAvailability.Unavailable)
        assertEquals("java_runtime_missing", (state as EngineAvailability.Unavailable).reasonCode)
    }

    @Test
    fun doesNotClaimReadyWithoutRenderer() {
        val state = JavaEngineAvailability {
            JavaEngineReadiness(true, 1, 0)
        }.availability()
        assertTrue(state is EngineAvailability.Unavailable)
        assertEquals("renderer_missing", (state as EngineAvailability.Unavailable).reasonCode)
    }

    @Test
    fun advertisesOnlyCapabilitiesBackedByTheIntegratedFclEngine() {
        val state = JavaEngineAvailability {
            JavaEngineReadiness(true, 1, 5)
        }.availability()
        assertTrue(state is EngineAvailability.Ready)
        assertEquals(
            setOf(
                EngineCapability.VERSION_DISCOVERY,
                EngineCapability.VERSION_INSTALLATION,
                EngineCapability.RUNTIME_MANAGEMENT,
                EngineCapability.MOD_LOADER_INSTALLATION,
                EngineCapability.GAME_LAUNCH
            ),
            (state as EngineAvailability.Ready).capabilities
        )
    }

    @Test
    fun convertsReadinessInspectionExceptionsIntoUnavailableState() {
        val state = JavaEngineAvailability {
            throw IllegalStateException("Runtime metadata is corrupted")
        }.availability()

        assertTrue(state is EngineAvailability.Unavailable)
        assertEquals("bootstrap_failed", (state as EngineAvailability.Unavailable).reasonCode)
    }

    @Test
    fun reportsBootstrapFailure() {
        val state = JavaEngineAvailability {
            JavaEngineReadiness(false, 0, 0, "Falha no FCL")
        }.availability()
        assertTrue(state is EngineAvailability.Unavailable)
        assertEquals("bootstrap_failed", (state as EngineAvailability.Unavailable).reasonCode)
    }
}
