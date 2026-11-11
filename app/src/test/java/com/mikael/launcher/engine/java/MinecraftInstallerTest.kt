package com.mikael.launcher.engine.java

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MinecraftInstallerTest {
    @Test
    fun resolvesStandardMavenCoordinates() {
        assertEquals(
            "com/google/guava/guava/21.0/guava-21.0.jar",
            minecraftMavenPath("com.google.guava:guava:21.0")
        )
    }

    @Test
    fun resolvesClassifierAndExtensionCoordinates() {
        assertEquals(
            "org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3-natives-linux.jar",
            minecraftMavenPath("org.lwjgl:lwjgl:3.3.3:natives-linux@jar")
        )
        assertEquals(
            "org/example/sample/1.0/sample-1.0.zip",
            minecraftMavenPath("org.example:sample:1.0@zip")
        )
    }

    @Test
    fun rejectsMalformedMavenCoordinates() {
        assertThrows(IllegalArgumentException::class.java) {
            minecraftMavenPath("not-a-coordinate")
        }
        assertThrows(IllegalArgumentException::class.java) {
            minecraftMavenPath("org.example:sample:1.0@zip/../../escape")
        }
    }

    @Test
    fun progressFractionIsBoundedByCompletedTasks() {
        assertEquals(0f, MinecraftInstallProgress("test", 0, 0, "").fraction)
        assertEquals(0.5f, MinecraftInstallProgress("test", 2, 4, "asset").fraction)
    }
}
