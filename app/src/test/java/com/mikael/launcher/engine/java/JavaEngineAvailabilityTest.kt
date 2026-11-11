package com.mikael.launcher.engine.java

import com.mikael.launcher.engine.EngineAvailability
import com.mikael.launcher.engine.EngineId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JavaEngineAvailabilityTest {
    @Test
    fun javaEngineDoesNotClaimToBeReadyBeforeBackendIntegration() {
        val engine = JavaEngineAvailability()

        assertEquals(EngineId.MINECRAFT_JAVA, engine.id)
        val state = engine.availability()
        assertTrue(state is EngineAvailability.Unavailable)
        assertEquals("backend_not_integrated", state.reasonCode)
        assertTrue(state.message.contains("ainda está em integração"))
    }
}
