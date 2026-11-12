package com.mikael.launcher.engine.java

import com.mikael.launcher.engine.EngineAvailability
import com.mikael.launcher.engine.EngineId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FclJavaGameEngineTest {
    @Test
    fun exposesTheMinecraftJavaEngineId() {
        val engine = FclJavaGameEngine(
            JavaEngineAvailability { JavaEngineReadiness(false, 0, 0) }
        )
        assertEquals(EngineId.MINECRAFT_JAVA, engine.id)
    }

    @Test
    fun delegatesReadinessToTheFclRuntimeAndRendererChecks() {
        val engine = FclJavaGameEngine(
            JavaEngineAvailability { JavaEngineReadiness(true, 1, 4) }
        )
        assertTrue(engine.availability() is EngineAvailability.Ready)
    }
}
