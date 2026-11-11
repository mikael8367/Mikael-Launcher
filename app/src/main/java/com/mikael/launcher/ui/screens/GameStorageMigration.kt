package com.mikael.launcher.ui.screens

import android.content.Context
import com.mikael.launcher.fcl.FclGameDirectoryResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal data class GameStorageMigrationResult(val copiedFiles: Int, val skippedFiles: Int, val copiedBytes: Long)

internal object GameStorageMigration {
    suspend fun migrate(context: Context): GameStorageMigrationResult = withContext(Dispatchers.IO) {
        val target = FclGameDirectoryResolver.resolve(context).canonicalFile
        val sources = listOf(
            File(context.filesDir, "minecraft"),
            File(context.getExternalFilesDir(null) ?: context.filesDir, ".minecraft")
        ).map { it.canonicalFile }.distinct()
        var copied = 0
        var skipped = 0
        var bytes = 0L
        for (source in sources) {
            if (!source.isDirectory || source == target) continue
            val sourcePrefix = source.path + File.separator
            val targetPrefix = target.path + File.separator
            require(!target.path.startsWith(sourcePrefix) && !source.path.startsWith(targetPrefix)) {
                "A pasta de destino não pode ficar dentro da pasta antiga (nem o contrário)."
            }
            source.walkTopDown().forEach { item ->
                val relative = item.relativeTo(source).path
                if (relative.isBlank()) return@forEach
                val destination = File(target, relative).canonicalFile
                require(destination.path.startsWith(targetPrefix)) { "Caminho de migração fora da pasta de destino bloqueado." }
                if (item.isDirectory) {
                    if (!destination.exists() && !destination.mkdirs()) error("Não foi possível criar ${destination.path}")
                } else if (item.isFile) {
                    destination.parentFile?.let { if (!it.exists() && !it.mkdirs()) error("Não foi possível criar ${it.path}") }
                    if (destination.exists()) {
                        skipped++
                    } else {
                        item.inputStream().buffered().use { input ->
                            destination.outputStream().buffered().use { output -> input.copyTo(output) }
                        }
                        copied++
                        bytes += item.length()
                    }
                }
            }
        }
        GameStorageMigrationResult(copied, skipped, bytes)
    }
}
