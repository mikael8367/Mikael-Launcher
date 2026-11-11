package com.mikael.launcher.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuntimeSettingsLabelTest {
    @Test
    fun parsesKnownRuntimeLabels() {
        assertEquals(8, runtimeMajorFromLabel("Java 8"))
        assertEquals(11, runtimeMajorFromLabel("Java 11"))
        assertEquals(17, runtimeMajorFromLabel("Java 17"))
        assertEquals(21, runtimeMajorFromLabel("Java 21"))
    }

    @Test
    fun returnsNullForMalformedLabels() {
        assertNull(runtimeMajorFromLabel("Java"))
        assertNull(runtimeMajorFromLabel("Java "))
        assertNull(runtimeMajorFromLabel("lixo"))
        assertNull(runtimeMajorFromLabel(""))
    }
}
