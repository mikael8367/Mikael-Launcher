package com.mikael.launcher.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineContractTest {
    @Test
    fun engineIdsRemainStable() {
        assertEquals("minecraft-java", EngineId.MINECRAFT_JAVA.value)
        assertEquals("horizon-rust", EngineId.HORIZON_RUST.value)
    }

    @Test
    fun unavailableStatePreservesDiagnosticDetails() {
        val state = EngineAvailability.Unavailable(
            reasonCode = "backend_not_integrated",
            message = "O motor Java ainda não foi integrado."
        )

        assertEquals("backend_not_integrated", state.reasonCode)
        assertEquals("O motor Java ainda não foi integrado.", state.message)
    }

    @Test
    fun readyStateExposesOnlyExplicitCapabilities() {
        val state = EngineAvailability.Ready(
            setOf(EngineCapability.VERSION_INSTALLATION)
        )

        assertEquals(
            setOf(EngineCapability.VERSION_INSTALLATION),
            state.capabilities
        )
        assertTrue(EngineCapability.GAME_LAUNCH !in state.capabilities)
    }

    @Test
    fun operationStateRepresentsProgressCancellationAndFailure() {
        val running = EngineOperationState.Running("Baixando bibliotecas", 0.5f)
        val cancelled = EngineOperationState.Cancelled
        val failed = EngineOperationState.Failed("download_failed", "Falha no download.")

        assertEquals("Baixando bibliotecas", running.stage)
        assertEquals(0.5f, running.progress)
        assertTrue(cancelled is EngineOperationState)
        assertEquals("download_failed", failed.code)
        assertEquals("Falha no download.", failed.message)
    }

    @Test
    fun gameEngineReportsItsIdentityAndCurrentAvailability() {
        val engine = object : GameEngine {
            override val id = EngineId.MINECRAFT_JAVA
            override fun availability() = EngineAvailability.Unavailable(
                "adapter_missing",
                "Adaptador FCL indisponível."
            )
        }

        assertEquals(EngineId.MINECRAFT_JAVA, engine.id)
        assertEquals("adapter_missing", (engine.availability() as EngineAvailability.Unavailable).reasonCode)
    }
}
