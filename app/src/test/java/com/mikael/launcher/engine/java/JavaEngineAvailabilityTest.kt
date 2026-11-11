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
            JavaEngineReadiness(
                initialized = false,
                validRuntimeCount = 0,
                rendererCount = 0
            )
        }

        assertEquals(EngineId.MINECRAFT_JAVA, engine.id)
        assertTrue(engine.availability() is EngineAvailability.Initializing)
    }

    @Test
    fun doesNotClaimReadyWithoutValidatedRuntime() {
        val engine = JavaEngineAvailability {
            JavaEngineReadiness(
                initialized = true,
                validRuntimeCount = 0,
                rendererCount = 5
            )
        }

        val state = engine.availability()
        assertTrue(state is EngineAvailability.Unavailable)
        assertEquals("java_runtime_missing", (state as EngineAvailability.Unavailable).reasonCode)
    }

    @Test
    fun doesNotClaimReadyWithoutRenderer() {
        val engine = JavaEngineAvailability {
            JavaEngineReadiness(
                initialized = true,
                validRuntimeCount = 1,
                rendererCount = 0
            )
        }

        val state = engine.availability()
        assertTrue(state is EngineAvailability.Unavailable)
        assertEquals("renderer_missing", (state as EngineAvailability.Unavailable).reasonCode)
    }

    @Test
    fun advertisesOnlyCapabilitiesBackedByTheIntegratedFclEngine() {
        val engine = JavaEngineAvailability {
            JavaEngineReadiness(
                initialized = true,
                validRuntimeCount = 1,
                rendererCount = 5
            )
        }

        val state = engine.availability()
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
    fun reportsBootstrapFailure() {
        val engine = JavaEngineAvailability {
            JavaEngineReadiness(
                initialized = false,
                validRuntimeCount = 0,
                rendererCount = 0,
                failureMessage = "Falha no FCL"
            )
        }

        val state = engine.availability()
        assertTrue(state is EngineAvailability.Unavailable)
        assertEquals("bootstrap_failed", (state as EngineAvailability.Unavailable).reasonCode)
    }
}
