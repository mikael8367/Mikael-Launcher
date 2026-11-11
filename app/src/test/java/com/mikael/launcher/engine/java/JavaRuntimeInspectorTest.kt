package com.mikael.launcher.engine.java

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JavaRuntimeInspectorTest {
    @Test
    fun validArm64RuntimeIsRecognized() {
        val root = createRuntime("17.0.12", elfClass = 2, machine = 183)
        try {
            val result = JavaRuntimeInspector.inspect(root, "arm64-v8a")
            assertTrue(result is JavaRuntimeInspection.Valid)
            assertEquals("17.0.12", (result as JavaRuntimeInspection.Valid).javaVersion)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun mismatchedElfArchitectureIsRejected() {
        val root = createRuntime("8.0.442", elfClass = 2, machine = 62)
        try {
            val result = JavaRuntimeInspector.inspect(root, "arm64-v8a")
            assertTrue(result is JavaRuntimeInspection.Invalid)
            assertEquals("runtime_abi_mismatch", (result as JavaRuntimeInspection.Invalid).reasonCode)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun missingJvmLibraryIsRejected() {
        val root = createRuntime("21.0.4", elfClass = 2, machine = 183)
        try {
            File(root, "lib/server/libjvm.so").delete()
            val result = JavaRuntimeInspector.inspect(root, "arm64-v8a")
            assertTrue(result is JavaRuntimeInspection.Invalid)
            assertEquals("jvm_library_missing", (result as JavaRuntimeInspection.Invalid).reasonCode)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun unknownAbiIsRejected() {
        val root = createRuntime("17.0.12", elfClass = 2, machine = 183)
        try {
            val result = JavaRuntimeInspector.inspect(root, "riscv64")
            assertTrue(result is JavaRuntimeInspection.Invalid)
            assertEquals("unsupported_abi", (result as JavaRuntimeInspection.Invalid).reasonCode)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun createRuntime(version: String, elfClass: Int, machine: Int): File {
        val root = createTempDir(prefix = "mikael-runtime-test-")
        File(root, "release").writeText("JAVA_VERSION=\"$version\"\n")
        File(root, "bin").mkdirs()
        File(root, "bin/java").writeBytes(byteArrayOf(0x01, 0x02, 0x03))
        val jvm = File(root, "lib/server/libjvm.so")
        jvm.parentFile?.mkdirs()
        val header = ByteArray(20)
        header[0] = 0x7f
        header[1] = 'E'.code.toByte()
        header[2] = 'L'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = elfClass.toByte()
        header[5] = 1
        header[18] = (machine and 0xff).toByte()
        header[19] = ((machine shr 8) and 0xff).toByte()
        jvm.writeBytes(header)
        return root
    }
}
