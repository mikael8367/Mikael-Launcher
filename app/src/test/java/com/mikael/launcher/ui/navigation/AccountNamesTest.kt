package com.mikael.launcher.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountNamesTest {
    @Test
    fun parsesRealNewlineFormat() {
        assertEquals(listOf("Steve", "Alex"), parseAccountNames("Steve\nAlex"))
    }

    @Test
    fun parsesLegacyLiteralBackslashNFormat() {
        assertEquals(listOf("Steve", "Alex"), parseAccountNames("Steve\\nAlex"))
    }

    @Test
    fun parsesMixedFormatsAndIgnoresBlankEntries() {
        assertEquals(listOf("Steve", "Alex"), parseAccountNames("Steve\\n\nAlex\n\n"))
    }

    @Test
    fun deduplicatesAndTrims() {
        assertEquals(listOf("Steve"), parseAccountNames(" Steve \n\\nSteve"))
    }

    @Test
    fun emptyRawYieldsEmptyList() {
        assertEquals(emptyList<String>(), parseAccountNames(""))
    }
}
