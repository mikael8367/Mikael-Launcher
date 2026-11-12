package com.mikael.launcher.engine.java

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ModLoaderInstallRequestTest {
    @Test
    fun capturesAllSelectionsInAnImmutableRequest() {
        val request = createModLoaderInstallRequest(
            minecraftVersion = "1.12.2",
            loader = "Forge + OptiFine",
            loaderVersion = "HD_U_G5",
            forgeVersion = "14.23.5.2860"
        )

        assertEquals("1.12.2", request.minecraftVersion)
        assertEquals("Forge + OptiFine", request.loader)
        assertEquals("HD_U_G5", request.loaderVersion)
        assertEquals("14.23.5.2860", request.forgeVersion)
    }

    @Test
    fun allowsVanillaWithoutLoaderVersions() {
        val request = createModLoaderInstallRequest("1.12.2", "Vanilla", "", "")
        assertEquals("Vanilla", request.loader)
    }

    @Test
    fun rejectsUnsupportedLoadersInsteadOfStartingAnInstall() {
        assertThrows(IllegalArgumentException::class.java) {
            createModLoaderInstallRequest("1.12.2", "Legacy Fabric", "1.0", "")
        }
    }

    @Test
    fun rejectsCombinedInstallWhenForgeVersionIsMissing() {
        assertThrows(IllegalArgumentException::class.java) {
            createModLoaderInstallRequest("1.12.2", "Forge + OptiFine", "HD_U_G5", "")
        }
    }

    @Test
    fun rejectsUnsafeMinecraftVersionIdentifiers() {
        assertThrows(IllegalArgumentException::class.java) {
            createModLoaderInstallRequest("../outside", "Vanilla", "", "")
        }
    }
}
