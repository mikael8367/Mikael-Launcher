package com.mikael.launcher.fcl

import com.mio.data.Renderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes das funções puras de preferências de lançamento. A aplicação sobre
 * [com.tungsten.fclcore.setting.VersionSetting] depende do runtime Android e
 * é validada pela compilação e pelo comportamento no aparelho.
 */
class FclLaunchPreferencesTest {

    @Test
    fun `renderer known labels map to FCL ids`() {
        assertEquals(Renderer.ID_NGGL4ES, rendererIdForLabel("GL4ES"))
        assertEquals(Renderer.ID_ZINK, rendererIdForLabel("Zink"))
        assertEquals(Renderer.ID_VIRGL, rendererIdForLabel("VirGL"))
    }

    @Test
    fun `renderer unknown or blank label keeps default`() {
        assertNull(rendererIdForLabel(null))
        assertNull(rendererIdForLabel(""))
        assertNull(rendererIdForLabel("   "))
        assertNull(rendererIdForLabel("ANGLE"))
        assertNull(rendererIdForLabel("Vulkan experimental"))
        assertNull(rendererIdForLabel("Software"))
        assertNull(rendererIdForLabel("gl4es"))
    }

    @Test
    fun `runtime folders map to FCL java directories`() {
        assertEquals("jre8", runtimeFolderForLabel("Java 8"))
        assertEquals("jre17", runtimeFolderForLabel("Java 17"))
        assertEquals("jre21", runtimeFolderForLabel("Java 21"))
        assertEquals("jre25", runtimeFolderForLabel("Java 25"))
    }

    @Test
    fun `runtime unknown labels keep auto`() {
        assertNull(runtimeFolderForLabel(null))
        assertNull(runtimeFolderForLabel(""))
        assertNull(runtimeFolderForLabel("Auto"))
        assertNull(runtimeFolderForLabel("Java 11"))
    }

    @Test
    fun `memory parsing accepts valid range`() {
        assertEquals(1024, parseMemoryMB("1024"))
        assertEquals(256, parseMemoryMB("256"))
        assertEquals(8192, parseMemoryMB("8192"))
        assertEquals(2048, parseMemoryMB(" 2048 "))
    }

    @Test
    fun `memory parsing rejects invalid input`() {
        assertNull(parseMemoryMB(null))
        assertNull(parseMemoryMB(""))
        assertNull(parseMemoryMB("abc"))
        assertNull(parseMemoryMB("255"))
        assertNull(parseMemoryMB("8193"))
        assertNull(parseMemoryMB("-1024"))
    }

    @Test
    fun `strip memory jvm args removes xms and xmx`() {
        assertEquals(
            listOf("-XX:+UseG1GC", "-Dfml.ignoreInvalidMinecraftCertificates=true"),
            stripMemoryJvmArgs(listOf("-XX:+UseG1GC", "-Xms512M", "-Xmx2048M", "-Dfml.ignoreInvalidMinecraftCertificates=true"))
        )
    }

    @Test
    fun `strip memory jvm args keeps other flags`() {
        assertEquals(
            listOf("-Xincgc", "-XX:MaxMetaspaceSize=256m"),
            stripMemoryJvmArgs(listOf("-Xincgc", "-XX:MaxMetaspaceSize=256m"))
        )
        assertTrue(stripMemoryJvmArgs(emptyList()).isEmpty())
    }

    @Test
    fun `strip memory jvm args is case insensitive`() {
        assertEquals(
            emptyList<String>(),
            stripMemoryJvmArgs(listOf("-xms256m", "-XMX1024M"))
        )
    }
}
