package com.mikael.launcher.engine.java

import java.io.File
import java.io.RandomAccessFile

/** Result of validating a Java runtime directory before the launcher attempts to use it. */
sealed interface JavaRuntimeInspection {
    data class Valid(
        val directory: File,
        val javaVersion: String,
        val androidAbi: String,
        val javaExecutable: File,
        val jvmLibrary: File
    ) : JavaRuntimeInspection

    data class Invalid(
        val reasonCode: String,
        val message: String
    ) : JavaRuntimeInspection
}

/**
 * Performs structural and ELF-ABI checks only. This does not execute Java and does not
 * prove that the runtime's Android linker dependencies or Minecraft native libraries work.
 *
 * Expected FCL-style runtime layout:
 *   release
 *   bin/java
 *   lib/server/libjvm.so
 */
object JavaRuntimeInspector {
    private data class ElfTarget(val elfClass: Int, val machine: Int)

    private val targets = mapOf(
        "arm64-v8a" to ElfTarget(2, 183),
        "armeabi-v7a" to ElfTarget(1, 40),
        "x86" to ElfTarget(1, 3),
        "x86_64" to ElfTarget(2, 62)
    )

    fun inspect(directory: File, expectedAbi: String): JavaRuntimeInspection {
        val target = targets[expectedAbi]
            ?: return invalid("unsupported_abi", "A arquitetura $expectedAbi não é suportada pelo verificador.")

        val root = runCatching { directory.canonicalFile }.getOrElse {
            return invalid("invalid_runtime_path", "Não foi possível resolver a pasta do runtime.")
        }
        if (!root.isDirectory) {
            return invalid("runtime_directory_missing", "A pasta do runtime Java não existe.")
        }

        val releaseFile = File(root, "release")
        if (!releaseFile.isFile) {
            return invalid("runtime_release_missing", "O arquivo release do Java não foi encontrado.")
        }
        val releaseText = runCatching { releaseFile.readText() }.getOrElse {
            return invalid("runtime_release_unreadable", "Não foi possível ler a versão do runtime.")
        }
        val version = Regex("(?m)^JAVA_VERSION=\"?([^\r\n\"]+)\"?\\s*$")
            .find(releaseText)?.groupValues?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return invalid("runtime_version_unknown", "O runtime não informa uma versão Java válida.")

        val java = File(root, "bin/java")
        if (!java.isFile) {
            return invalid("java_executable_missing", "O executável bin/java não foi encontrado.")
        }
        if (!java.canExecute()) {
            return invalid("java_not_executable", "O arquivo bin/java não possui permissão de execução.")
        }
        val javaElf = readElfTarget(java) ?: return invalid(
            "invalid_java_elf",
            "O executável bin/java não possui um cabeçalho ELF válido."
        )
        if (javaElf.elfClass != target.elfClass || javaElf.machine != target.machine) {
            return invalid(
                "java_abi_mismatch",
                "O executável bin/java não corresponde à arquitetura $expectedAbi do dispositivo."
            )
        }

        val jvm = File(root, "lib/server/libjvm.so")
        if (!jvm.isFile) {
            return invalid("jvm_library_missing", "A biblioteca lib/server/libjvm.so não foi encontrada.")
        }

        val elf = readElfTarget(jvm) ?: return invalid(
            "invalid_jvm_elf",
            "A biblioteca libjvm.so não possui um cabeçalho ELF válido."
        )
        if (elf.elfClass != target.elfClass || elf.machine != target.machine) {
            return invalid(
                "runtime_abi_mismatch",
                "A biblioteca libjvm.so não corresponde à arquitetura $expectedAbi do dispositivo."
            )
        }

        return JavaRuntimeInspection.Valid(
            directory = root,
            javaVersion = version,
            androidAbi = expectedAbi,
            javaExecutable = java,
            jvmLibrary = jvm
        )
    }

    private fun readElfTarget(file: File): ElfTarget? = runCatching {
        RandomAccessFile(file, "r").use { input ->
            if (input.length() < 20L) return null
            val ident = ByteArray(6)
            input.readFully(ident)
            if (ident[0] != 0x7f.toByte() || ident[1] != 'E'.code.toByte() ||
                ident[2] != 'L'.code.toByte() || ident[3] != 'F'.code.toByte()
            ) return null
            val elfClass = ident[4].toInt() and 0xff
            val dataEncoding = ident[5].toInt() and 0xff
            if (dataEncoding != 1 || elfClass !in 1..2) return null
            input.seek(18L)
            val low = input.read()
            val high = input.read()
            if (low < 0 || high < 0) return null
            ElfTarget(elfClass, low or (high shl 8))
        }
    }.getOrNull()

    private fun invalid(code: String, message: String) =
        JavaRuntimeInspection.Invalid(code, message)
}
