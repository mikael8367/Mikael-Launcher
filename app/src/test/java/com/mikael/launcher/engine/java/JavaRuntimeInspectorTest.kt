package com.mikael.launcher.engine.java

import java.io.File
import java.nio.file.Files
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
        val root = createRuntime("8.0.442", elfClass = 2, machine = 183)
        try {
            File(root, "lib/aarch64/server/libjvm.so").writeBytes(elfHeader(2, 62))
            val result = JavaRuntimeInspector.inspect(root, "arm64-v8a")
            assertTrue(result is JavaRuntimeInspection.Invalid)
            assertEquals("runtime_abi_mismatch", (result as JavaRuntimeInspection.Invalid).reasonCode)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun nonExecutableJavaBinaryIsRejected() {
        val root = createRuntime("17.0.12", elfClass = 2, machine = 183)
        try {
            File(root, "bin/java").setExecutable(false)
            val result = JavaRuntimeInspector.inspect(root, "arm64-v8a")
            assertTrue(result is JavaRuntimeInspection.Invalid)
            assertEquals("java_not_executable", (result as JavaRuntimeInspection.Invalid).reasonCode)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun javaExecutableWithWrongElfArchitectureIsRejected() {
        val root = createRuntime("17.0.12", elfClass = 2, machine = 183)
        try {
            val java = File(root, "bin/java")
            java.writeBytes(elfHeader(2, 62))
            java.setExecutable(true)
            val result = JavaRuntimeInspector.inspect(root, "arm64-v8a")
            assertTrue(result is JavaRuntimeInspection.Invalid)
            assertEquals("java_abi_mismatch", (result as JavaRuntimeInspection.Invalid).reasonCode)
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
        val root = Files.createTempDirectory("mikael-runtime-test-").toFile()
        File(root, "release").writeText("JAVA_VERSION=\"$version\"\nOS_ARCH=\"aarch64\"\n")
        File(root, "bin").mkdirs()
        val java = File(root, "bin/java")
        java.writeBytes(elfHeader(elfClass, machine))
        java.setExecutable(true)
        val jvm = File(root, "lib/aarch64/server/libjvm.so")
        jvm.parentFile?.mkdirs()
        jvm.writeBytes(elfHeader(elfClass, machine))
        return root
    }

    private fun elfHeader(elfClass: Int, machine: Int): ByteArray {
        val header = ByteArray(20)
        header[0] = 0x7f
        header[1] = 'E'.code.toByte()
        header[2] = 'L'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = elfClass.toByte()
        header[5] = 1
        header[18] = (machine and 0xff).toByte()
        header[19] = ((machine shr 8) and 0xff).toByte()
        return header
    }
}
