package com.mikael.launcher.fcl

import android.content.Context
import android.os.Build
import com.mio.JavaManager
import com.tungsten.fcl.util.RuntimeUtils
import com.tungsten.fclauncher.FCLauncher
import com.tungsten.fclauncher.utils.FCLPath
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class FclJavaRuntime(
    val majorVersion: Int,
    val assetFolder: String,
    val directoryName: String
) {
    JAVA_8(8, "jre8", "jre8"),
    JAVA_17(17, "jre17", "jre17"),
    JAVA_21(21, "jre21", "jre21"),
    JAVA_25(25, "jre25", "jre25");

    fun targetDirectory(): File = File(FCLPath.JAVA_PATH, directoryName)

    fun assetDirectory(): String = "app_runtime/java/$assetFolder"
}

data class FclRuntimeInstallProgress(
    val runtime: FclJavaRuntime,
    val detail: String,
    val stageResourceId: Int? = null
)

data class FclRuntimeInstallResult(
    val runtime: FclJavaRuntime,
    val installed: Boolean,
    val javaVersion: String?,
    val path: String,
    val message: String
)

/**
 * Installs FCL's bundled ARM64 JRE archives into app-private storage.
 *
 * The upstream RuntimeUtils installer is reused for its Android-specific
 * Pack200, freetype, AWT and Java Sound patches. Installation happens in a
 * staging directory; an existing runtime is kept as a backup until the new
 * tree has passed structural validation and has been registered with JavaManager.
 */
object FclRuntimeInstaller {
    private val locks = ConcurrentHashMap<FclJavaRuntime, Mutex>()

    suspend fun install(
        context: Context,
        runtime: FclJavaRuntime,
        onProgress: (FclRuntimeInstallProgress) -> Unit = {}
    ): FclRuntimeInstallResult {
        val bootstrap = FclEngineBootstrap.initialize(context.applicationContext)
        check(bootstrap.initialized) {
            "FCL services must be initialized before installing a Java runtime."
        }
        check(Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }) {
            "This launcher build currently provides Java runtimes for ARM64 only."
        }

        val lock = locks.getOrPut(runtime) { Mutex() }
        return lock.withLock {
            withContext(Dispatchers.IO) {
                installBlocking(context.applicationContext, runtime, onProgress)
            }
        }
    }

    fun installedRuntimes(): List<String> {
        val javaRoot = File(FCLPath.JAVA_PATH)
        return javaRoot.listFiles()
            .orEmpty()
            .filter { it.isDirectory && File(it, "release").isFile }
            .map { it.name }
            .sorted()
    }

    private fun installBlocking(
        context: Context,
        runtime: FclJavaRuntime,
        onProgress: (FclRuntimeInstallProgress) -> Unit
    ): FclRuntimeInstallResult {
        val target = runtime.targetDirectory().canonicalFile
        val parent = target.parentFile ?: throw IOException("Invalid Java runtime destination.")
        if (!parent.exists() && !parent.mkdirs()) {
            throw IOException("Could not create Java runtime directory.")
        }

        val staging = File(parent, ".${runtime.directoryName}.installing")
        val backup = File(parent, ".${runtime.directoryName}.backup")
        deleteTree(staging)
        deleteTree(backup)

        val assetDirectory = runtime.assetDirectory()
        requireAsset(context, "$assetDirectory/version")
        requireAsset(context, "$assetDirectory/universal.tar.xz")
        requireAsset(context, "$assetDirectory/bin-arm64.tar.xz")

        var previousMoved = false
        var newRuntimeMoved = false
        try {
            onProgress(FclRuntimeInstallProgress(runtime, "Preparando Java ${runtime.majorVersion}"))
            RuntimeUtils.installJava(
                context,
                staging.absolutePath,
                assetDirectory,
                object : RuntimeUtils.InstallListener {
                    override fun onUpdate(detail: String) {
                        onProgress(FclRuntimeInstallProgress(runtime, detail))
                    }

                    override fun onStage(resId: Int) {
                        onProgress(FclRuntimeInstallProgress(runtime, "Aplicando correções Android", resId))
                    }
                }
            )

            val version = validateRuntime(staging, runtime)
            staging.resolve("bin/java").setExecutable(true, false)

            if (target.exists()) {
                moveDirectory(target, backup)
                previousMoved = true
            }
            moveDirectory(staging, target)
            newRuntimeMoved = true

            if (!JavaManager.addToJavaVersion(target)) {
                throw IOException("The installed runtime was not accepted by FCL JavaManager.")
            }

            deleteTree(backup)
            return FclRuntimeInstallResult(
                runtime = runtime,
                installed = true,
                javaVersion = version,
                path = target.absolutePath,
                message = "Java ${runtime.majorVersion} ARM64 instalado e registrado."
            )
        } catch (failure: Throwable) {
            // Keep the last known-good runtime. If swapping failed, restore its directory.
            if (newRuntimeMoved) {
                deleteTree(target)
            }
            if (previousMoved && backup.exists() && !target.exists()) {
                runCatching { moveDirectory(backup, target) }
            }
            deleteTree(staging)
            if (failure is IOException) throw failure
            throw IOException("Failed to install Java ${runtime.majorVersion}: ${failure.message}", failure)
        } finally {
            // A successful swap removes the backup above. On errors the backup is
            // intentionally retained if restoration itself failed, for recovery.
            if (target.exists()) deleteTree(staging)
        }
    }

    private fun validateRuntime(root: File, runtime: FclJavaRuntime): String {
        val release = File(root, "release")
        val java = File(root, "bin/java")
        if (!release.isFile || release.length() == 0L) {
            throw IOException("Java ${runtime.majorVersion}: missing or empty release metadata.")
        }
        if (!java.isFile || java.length() == 0L) {
            throw IOException("Java ${runtime.majorVersion}: bin/java is missing.")
        }

        val properties = release.readLines()
        val javaVersion = Regex("""JAVA_VERSION="([^"]+)"""")
            .find(properties.joinToString("\n"))
            ?.groupValues
            ?.getOrNull(1)
            ?: throw IOException("Java ${runtime.majorVersion}: JAVA_VERSION is missing from release.")

        val detectedMajor = javaVersion.substringBefore('.').toIntOrNull()
            ?.let { first -> if (first == 1) javaVersion.split('.').getOrNull(1)?.toIntOrNull() else first }
        if (detectedMajor != runtime.majorVersion) {
            throw IOException("Expected Java ${runtime.majorVersion}, but archive reports $javaVersion.")
        }

        val libDir = try {
            FCLauncher.getJavaLibDir(root.absolutePath)
        } catch (error: Exception) {
            throw IOException("Java ${runtime.majorVersion}: architecture library directory is invalid.", error)
        }
        val jvmRoot = if (FCLauncher.isJDK8(root.absolutePath)) {
            File(root, "jre$libDir")
        } else {
            File(root, libDir)
        }
        val serverJvm = File(jvmRoot, "server/libjvm.so")
        val clientJvm = File(jvmRoot, "client/libjvm.so")
        if (!serverJvm.isFile && !clientJvm.isFile) {
            throw IOException("Java ${runtime.majorVersion}: ARM64 libjvm.so was not found.")
        }
        return javaVersion
    }

    private fun requireAsset(context: Context, path: String) {
        try {
            context.assets.open(path).use { stream ->
                if (stream.read() < 0) throw IOException("Runtime asset is empty: $path")
            }
        } catch (error: IOException) {
            throw IOException("Required Java runtime asset is missing: $path", error)
        }
    }

    private fun moveDirectory(source: File, destination: File) {
        try {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), destination.toPath())
        }
    }

    private fun deleteTree(file: File) {
        if (!file.exists()) return
        if (file.isDirectory) {
            file.listFiles()?.forEach(::deleteTree)
        }
        if (!file.delete() && file.exists()) {
            throw IOException("Could not remove temporary runtime path: ${file.absolutePath}")
        }
    }
}
