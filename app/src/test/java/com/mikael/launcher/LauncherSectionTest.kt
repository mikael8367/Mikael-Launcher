package com.mikael.launcher

import com.mikael.launcher.ui.navigation.LauncherSection
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherSectionTest {
    @Test
    fun navigationContainsAllThirteenExpectedSectionsInOrder() {
        assertEquals(
            listOf(
                "Início",
                "Jogar",
                "Horizon Rust",
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
                "HORIZON_RUST",
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
