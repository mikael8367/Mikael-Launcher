package com.mikael.launcher.fcl

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FclRuntimeLayoutTest {
    @Test
    fun modernJavaLibraryPathUsesStandardLibServerLayout() {
        val root = Files.createTempDirectory("fcl-jre17-test-").toFile()
        try {
            val expected = File(root, "lib/server/libjvm.so")
            expected.parentFile?.mkdirs()
            expected.writeBytes(byteArrayOf(1))
            assertEquals(expected.canonicalFile, resolveFclJvmLibrary(root, "/lib", false)?.canonicalFile)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun java8LibraryPathUsesNestedJreDirectoryWhenPresent() {
        val root = Files.createTempDirectory("fcl-jre8-test-").toFile()
        try {
            val expected = File(root, "jre/lib/aarch64/server/libjvm.so")
            expected.parentFile?.mkdirs()
            expected.writeBytes(byteArrayOf(1))
            assertEquals(expected.canonicalFile, resolveFclJvmLibrary(root, "/lib/aarch64", true)?.canonicalFile)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun java8ArchiveLayoutWithoutNestedJreDirectoryIsAlsoResolved() {
        val root = Files.createTempDirectory("fcl-jre8-root-test-").toFile()
        try {
            val expected = File(root, "lib/aarch64/server/libjvm.so")
            expected.parentFile?.mkdirs()
            expected.writeBytes(byteArrayOf(1))
            assertEquals(expected.canonicalFile, resolveFclJvmLibrary(root, "/lib/aarch64", true)?.canonicalFile)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun clientJvmIsAcceptedWhenServerJvmIsAbsent() {
        val root = Files.createTempDirectory("fcl-jre-client-test-").toFile()
        try {
            val expected = File(root, "lib/aarch64/client/libjvm.so")
            expected.parentFile?.mkdirs()
            expected.writeBytes(byteArrayOf(1))
            assertEquals(expected.canonicalFile, resolveFclJvmLibrary(root, "/lib/aarch64", false)?.canonicalFile)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun missingJvmLibraryIsRejected() {
        val root = Files.createTempDirectory("fcl-jre-missing-test-").toFile()
        try {
            assertNull(resolveFclJvmLibrary(root, "/lib/aarch64", false))
        } finally {
            root.deleteRecursively()
        }
    }
}
