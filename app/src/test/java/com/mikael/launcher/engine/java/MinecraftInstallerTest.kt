package com.mikael.launcher.engine.java

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.json.JSONObject

class MinecraftInstallerTest {
    @Test
    fun readsClientArtifactFromModernMetadata() {
        val metadata = JSONObject(
            """{"downloads":{"client":{"url":"https://example.invalid/client.jar","sha1":"abc"}}}"""
        )
        assertEquals(
            "https://example.invalid/client.jar",
            minecraftClientDownload(metadata)?.getString("url")
        )
    }

    @Test
    fun readsClientArtifactFromLegacyMainJarMetadata() {
        val metadata = JSONObject(
            """{"mainJar":{"downloads":{"artifact":{"url":"https://example.invalid/legacy-client.jar","sha1":"def"}}}}"""
        )
        assertEquals(
            "https://example.invalid/legacy-client.jar",
            minecraftClientDownload(metadata)?.getString("url")
        )
    }

    @Test
    fun returnsNoClientArtifactWhenMetadataHasNeitherFormat() {
        assertNull(minecraftClientDownload(JSONObject("""{"id":"test"}""")))
    }

    @Test
    fun skipsOnlyDesktopWindowIconsOnAndroid() {
        assertEquals(false, minecraftAssetRequiredOnAndroid("icons/icon_16x16.png"))
        assertEquals(false, minecraftAssetRequiredOnAndroid("icons/icon_32x32.png"))
        assertEquals(true, minecraftAssetRequiredOnAndroid("minecraft/lang/en_us.json"))
        assertEquals(true, minecraftAssetRequiredOnAndroid("textures/blocks/stone.png"))
    }

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

    @Test
    fun evaluatesOrderedOsRulesAndLastMatchingRuleWins() {
        val library = JSONObject(
            """{"rules":[
                {"action":"allow","os":{"name":"linux","version":"^5\\."}},
                {"action":"disallow","os":{"name":"linux","arch":"x86_64"}},
                {"action":"allow","os":{"name":"linux","arch":"aarch64","version":"^5\\."}}
            ]}"""
        )
        assertEquals(true, minecraftLibraryApplies(library, "linux", "aarch64", "5.15.0"))
        assertEquals(false, minecraftLibraryApplies(library, "linux", "x86_64", "5.15.0"))
        assertEquals(false, minecraftLibraryApplies(library, "linux", "aarch64", "6.1.0"))
    }

    @Test
    fun matchesFeatureRulesAgainstProvidedFeatureContext() {
        val library = JSONObject(
            """{"rules":[{"action":"allow","features":{"is_demo_user":true}}]}"""
        )
        assertEquals(false, minecraftLibraryApplies(library, "linux", "aarch64", "5.15"))
        assertEquals(
            true,
            minecraftLibraryApplies(
                library, "linux", "aarch64", "5.15",
                mapOf("is_demo_user" to true)
            )
        )
    }

    @Test
    fun defaultsLibrariesWithoutRulesToAllowed() {
        assertEquals(true, minecraftLibraryApplies(JSONObject("""{"name":"org.example:demo:1.0"}"""), "linux", "aarch64", "5.15"))
    }

    @Test
    fun resolvesLibraryFromOfficialDefaultMavenRepositoryWhenArtifactIsMissing() {
        val library = JSONObject("""{"name":"com.example:sample:1.2.3"}""")
        val artifact = minecraftLibraryArtifact(library)!!
        assertEquals(
            "https://libraries.minecraft.net/com/example/sample/1.2.3/sample-1.2.3.jar",
            artifact.getString("url")
        )
        assertEquals(
            "com/example/sample/1.2.3/sample-1.2.3.jar",
            artifact.getString("path")
        )
    }

    @Test
    fun resolvesLibraryFromItsDeclaredMavenRepository() {
        val library = JSONObject(
            """{"name":"org.example:custom:2.0@zip","url":"https://repo.example/maven/"}"""
        )
        val artifact = minecraftLibraryArtifact(library)!!
        assertEquals(
            "https://repo.example/maven/org/example/custom/2.0/custom-2.0.zip",
            artifact.getString("url")
        )
    }

    @Test
    fun keepsOfficialArtifactUrlAndPathWhenProvided() {
        val library = JSONObject(
            """{"name":"org.example:custom:2.0","downloads":{"artifact":{"path":"custom/path.jar","url":"https://cdn.example/custom.jar","sha1":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","size":123}}}"""
        )
        val artifact = minecraftLibraryArtifact(library)!!
        assertEquals("https://cdn.example/custom.jar", artifact.getString("url"))
        assertEquals("custom/path.jar", artifact.getString("path"))
        assertEquals(123L, artifact.getLong("size"))
    }

    @Test
    fun rejectsInsecureLibraryRepository() {
        val library = JSONObject(
            """{"name":"org.example:custom:2.0","url":"http://repo.example/maven/"}"""
        )
        assertThrows(IllegalArgumentException::class.java) {
            minecraftLibraryArtifact(library)
        }
    }

    @Test
    fun parsesAndValidatesLegacyVirtualAssetIndex() {
        val index = JSONObject(
            """{"virtual":true,"objects":{"sound/test.ogg":{"hash":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","size":42}}}"""
        )
        val entries = minecraftAssetEntries(index)
        assertEquals(1, entries.size)
        assertEquals("sound/test.ogg", entries.single().name)
        assertEquals(42L, entries.single().size)
    }

    @Test
    fun rejectsAssetIndexWithoutObjects() {
        assertThrows(IllegalArgumentException::class.java) {
            minecraftAssetEntries(JSONObject("""{"virtual":true}"""))
        }
    }

    @Test
    fun rejectsMalformedAssetHashInsteadOfSilentlySkippingIt() {
        val index = JSONObject(
            """{"objects":{"sound/bad.ogg":{"hash":"not-a-sha1","size":42}}}"""
        )
        assertThrows(IllegalArgumentException::class.java) {
            minecraftAssetEntries(index)
        }
    }

    @Test
    fun rejectsAssetPathTraversal() {
        val index = JSONObject(
            """{"objects":{"../../outside":{"hash":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","size":42}}}"""
        )
        assertThrows(IllegalArgumentException::class.java) {
            minecraftAssetEntries(index)
        }
    }

    @Test
    fun acceptsZeroByteAssetSizeAsDeclaredMetadata() {
        val index = JSONObject(
            """{"objects":{"empty.dat":{"hash":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","size":0}}}"""
        )
        assertEquals(0L, minecraftAssetEntries(index).single().size)
    }
}

class MinecraftInstallerRetryTest {
    @Test
    fun treatsTransientHttpStatusesAsRetryable() {
        for (status in listOf(408, 429, 500, 502, 503, 504)) {
            assert(minecraftIsRetryableDownloadFailure(HttpDownloadException(status, "HTTP $status")))
        }
    }

    @Test
    fun treatsDefinitiveHttpStatusesAsNotRetryable() {
        for (status in listOf(400, 403, 404, 410)) {
            assert(!minecraftIsRetryableDownloadFailure(HttpDownloadException(status, "HTTP $status")))
        }
    }

    @Test
    fun treatsIoFailuresAsRetryableAndValidationFailuresAsNot() {
        assert(minecraftIsRetryableDownloadFailure(java.io.IOException("connection reset")))
        assert(!minecraftIsRetryableDownloadFailure(IllegalArgumentException("bad metadata")))
        assert(!minecraftIsRetryableDownloadFailure(IllegalStateException("bad state")))
    }

    @Test
    fun backoffGrowsAndIsCapped() {
        assertEquals(1000L, retryBackoffMillis(1))
        assertEquals(2000L, retryBackoffMillis(2))
        assertEquals(4000L, retryBackoffMillis(3))
        assertEquals(8000L, retryBackoffMillis(9))
    }
}
