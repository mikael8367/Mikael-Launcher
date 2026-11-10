package com.mikael.launcher

import com.mikael.launcher.ui.navigation.LauncherSection
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherSectionTest {
    @Test
    fun navigationContainsAllTwelveExpectedSectionsInOrder() {
        assertEquals(
            listOf(
                "Início",
                "Jogar",
                "Versões",
                "Contas",
                "Mods",
                "Pacotes",
                "Shaders",
                "Mundos",
                "Downloads",
                "Desempenho",
                "Logs",
                "Configurações"
            ),
            LauncherSection.entries.map { it.label }
        )
    }

    @Test
    fun sectionNamesRemainStableForSavedNavigationState() {
        assertEquals(
            listOf(
                "HOME",
                "PLAY",
                "VERSIONS",
                "ACCOUNTS",
                "MODS",
                "RESOURCE_PACKS",
                "SHADERS",
                "WORLDS",
                "DOWNLOADS",
                "PERFORMANCE",
                "LOGS",
                "SETTINGS"
            ),
            LauncherSection.entries.map { it.name }
        )
    }
}
