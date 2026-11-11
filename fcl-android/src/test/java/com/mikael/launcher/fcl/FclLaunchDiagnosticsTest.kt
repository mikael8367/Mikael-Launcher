package com.mikael.launcher.fcl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FclLaunchDiagnosticsTest {
    @Test
    fun diagnosticNamesTheFailingStage() {
        val text = buildLaunchDiagnostic(IllegalStateException("boom"), "iniciando a JVM do Minecraft", null)
        assertTrue(text.contains("iniciando a JVM do Minecraft"))
        assertTrue(text.contains("IllegalStateException: boom"))
    }

    @Test
    fun diagnosticWalksTheCauseChain() {
        val root = IllegalArgumentException("runtime incompleto")
        val mid = IllegalStateException("falha ao preparar runtime", root)
        val text = buildLaunchDiagnostic(mid, "selecionando o runtime Java compatível", null)
        assertTrue(text.contains("1. IllegalStateException: falha ao preparar runtime"))
        assertTrue(text.contains("2. IllegalArgumentException: runtime incompleto"))
        // Cadeia circular não pode travar nem repetir infinitamente.
        assertFalse(text.contains("3. IllegalArgumentException"))
    }

    @Test
    fun diagnosticHandlesBlankMessagesAndCycles() {
        val a = RuntimeException()
        val b = RuntimeException("segundo", a)
        a.initCause(b) // ciclo A -> B -> A
        val text = buildLaunchDiagnostic(a, "etapa", null)
        assertTrue(text.contains("RuntimeException: (sem mensagem)"))
        assertTrue(text.contains("RuntimeException: segundo"))
    }

    @Test
    fun diagnosticPointsToLogDirectoryWhenAvailable() {
        val text = buildLaunchDiagnostic(IllegalStateException("x"), "etapa", "/data/fcl/log")
        assertTrue(text.contains("/data/fcl/log"))
        assertEquals(1, Regex("/data/fcl/log").findAll(text).count())
    }

    @Test
    fun diagnosticWithoutLogDirectoryOmitsLogSection() {
        val text = buildLaunchDiagnostic(IllegalStateException("x"), "etapa", null)
        assertFalse(text.contains("Logs completos"))
    }
}
