package com.mikael.launcher

import com.mikael.launcher.ui.navigation.LauncherSection
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherSectionTest {
    @Test
    fun navigationContainsAllSixExpectedSectionsInOrder() {
        assertEquals(
            listOf("Início", "Jogar", "Versões", "Mods", "Downloads", "Configurações"),
            LauncherSection.entries.map { it.label }
        )
    }

    @Test
    fun sectionNamesRemainStableForSavedNavigationState() {
        assertEquals(
            listOf("HOME", "PLAY", "VERSIONS", "MODS", "DOWNLOADS", "SETTINGS"),
            LauncherSection.entries.map { it.name }
        )
    }
}
