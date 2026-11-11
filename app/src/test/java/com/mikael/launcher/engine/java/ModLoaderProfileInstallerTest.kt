package com.mikael.launcher.engine.java

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

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
        assertEquals("1.20.1", merged.getString("inheritsFrom"))
        assertEquals("net.fabricmc.loader.impl.launch.knot.KnotClient", merged.getString("mainClass"))
        assertEquals(2, merged.getJSONArray("libraries").length())
        assertEquals("org.example:base:1.0", merged.getJSONArray("libraries").getJSONObject(0).getString("name"))
    }

    @Test
    fun rejectsUnsafeVersionIdentifiers() {
        val installer = ModLoaderProfileInstaller(java.io.File("build/test-minecraft"))
        assertThrows(IllegalArgumentException::class.java) {
            installer.availableVersions("../outside", SupportedProfileLoader.FABRIC)
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
