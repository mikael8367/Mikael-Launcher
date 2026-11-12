package com.mikael.launcher.engine.java

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class ModLoaderProfileInstallerTest {
    @Test
    fun mergesLoaderProfileAndRetainsVanillaLibraries() {
        val base = JSONObject(
            """{"id":"1.20.1","mainClass":"net.minecraft.client.main.Main","libraries":[{"name":"org.example:base:1.0"}],"arguments":{"game":["--demo"],"jvm":["-Xmx2G"]}}"""
        )
        val loader = JSONObject(
            """{"id":"fabric-loader-0.15.0-1.20.1","inheritsFrom":"1.20.1","mainClass":"net.fabricmc.loader.impl.launch.knot.KnotClient","libraries":[{"name":"net.fabricmc:fabric-loader:0.15.0"}]}"""
        )

        val merged = mergeLoaderProfile(base, loader, "fabric-loader-0.15.0-1.20.1", "1.20.1")

        assertEquals("fabric-loader-0.15.0-1.20.1", merged.getString("id"))
        assertEquals(false, merged.has("inheritsFrom"))
        assertEquals("net.fabricmc.loader.impl.launch.knot.KnotClient", merged.getString("mainClass"))
        assertEquals(2, merged.getJSONArray("libraries").length())
        assertEquals("org.example:base:1.0", merged.getJSONArray("libraries").getJSONObject(0).getString("name"))
    }

    @Test
    fun mergedProfileKeepsBaseClientDownloadMetadata() {
        // FCL integrity checks and any fallback re-download rely on
        // downloads.client/assetIndex still pointing at official vanilla artifacts.
        val base = JSONObject(
            """{"id":"1.20.1","downloads":{"client":{"url":"https://piston-data.mojang.com/client.jar","sha1":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","size":123456}},"assetIndex":{"id":"5","url":"https://piston-meta.mojang.com/v1/packages/aa/5.json","sha1":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}}"""
        )
        val loader = JSONObject("""{"id":"fabric-loader-1.20.1","inheritsFrom":"1.20.1"}""")

        val merged = mergeLoaderProfile(base, loader, "fabric-loader-1.20.1", "1.20.1")

        assertEquals(
            "https://piston-data.mojang.com/client.jar",
            merged.getJSONObject("downloads").getJSONObject("client").getString("url")
        )
        assertEquals(123456L, merged.getJSONObject("downloads").getJSONObject("client").getLong("size"))
        assertEquals("5", merged.getJSONObject("assetIndex").getString("id"))
    }

    @Test
    fun quiltLoaderDeclaresItsOwnMavenRepository() {
        // Quilt-only artifacts (org.quiltmc:*) are not published on
        // maven.fabricmc.net; the fallback must point at the Quilt repository.
        assertEquals("https://maven.fabricmc.net/", SupportedProfileLoader.FABRIC.defaultRepository)
        assertEquals(
            "https://maven.quiltmc.org/repository/release/",
            SupportedProfileLoader.QUILT.defaultRepository
        )
    }

    @Test
    fun resolvesLoaderLibrariesAgainstTheLoaderDefaultRepository() {
        val library = JSONObject("""{"name":"org.quiltmc:quilt-loader:0.21.0"}""")

        val artifact = loaderLibraryArtifact(library, SupportedProfileLoader.QUILT.defaultRepository)!!

        assertEquals(
            "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-loader/0.21.0/quilt-loader-0.21.0.jar",
            artifact.getString("url")
        )
        assertEquals(
            "org/quiltmc/quilt-loader/0.21.0/quilt-loader-0.21.0.jar",
            artifact.getString("path")
        )
    }

    @Test
    fun resolvesLoaderLibrariesAgainstFabricRepositoryByDefault() {
        val library = JSONObject("""{"name":"net.fabricmc:fabric-loader:0.15.0"}""")

        val artifact = loaderLibraryArtifact(library, SupportedProfileLoader.FABRIC.defaultRepository)!!

        assertEquals(
            "https://maven.fabricmc.net/net/fabricmc/fabric-loader/0.15.0/fabric-loader-0.15.0.jar",
            artifact.getString("url")
        )
    }

    @Test
    fun loaderLibraryHonorsExplicitRepositoryAndArtifactMetadata() {
        val library = JSONObject(
            """{"name":"org.example:custom:2.0","url":"https://repo.example/maven/","downloads":{"artifact":{"path":"custom/path.jar","url":"https://cdn.example/custom.jar","sha1":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","size":99}}}"""
        )

        val artifact = loaderLibraryArtifact(library, SupportedProfileLoader.FABRIC.defaultRepository)!!

        assertEquals("https://cdn.example/custom.jar", artifact.getString("url"))
        assertEquals("custom/path.jar", artifact.getString("path"))
        assertEquals(99L, artifact.getLong("size"))
    }

    @Test
    fun rejectsInsecureLoaderRepositories() {
        val library = JSONObject(
            """{"name":"org.example:custom:2.0","url":"http://repo.example/maven/"}"""
        )
        assertThrows(IllegalArgumentException::class.java) {
            loaderLibraryArtifact(library, SupportedProfileLoader.FABRIC.defaultRepository)
        }
    }

    @Test
    fun ignoresBlankLoaderLibraryEntries() {
        assertEquals(null, loaderLibraryArtifact(JSONObject("""{"name":"  "}"""), SupportedProfileLoader.FABRIC.defaultRepository))
    }

    @Test
    fun rejectsUnsafeVersionIdentifiers() {
        val installer = ModLoaderProfileInstaller(java.io.File("build/test-minecraft"))
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { installer.availableVersions("../outside", SupportedProfileLoader.FABRIC) }
        }
    }

    @Test
    fun profileArgumentObjectsDoNotMutateTheBaseMetadata() {
        val base = JSONObject("""{"id":"1.20.1","arguments":{"game":["--demo"]}}""")
        val loader = JSONObject("""{"arguments":{"game":["--fabric"]}}""")

        mergeLoaderProfile(base, loader, "fabric-1.20.1", "1.20.1")

        assertEquals(JSONArray(listOf("--demo")).toString(), base.getJSONObject("arguments").getJSONArray("game").toString())
    }
}
