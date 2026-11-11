package com.mikael.launcher.fcl

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

/** Resolves a user-selected local shared-storage SAF tree to a real File path.
 * FCL and the game process require filesystem paths; arbitrary cloud/document providers
 * cannot be used as a Minecraft game directory.
 */
object FclGameDirectoryResolver {
    const val PREFERENCES_NAME = "mikael_launcher_preferences"
    const val TREE_URI_KEY = "minecraft_game_tree_uri"

    fun resolve(context: Context): File {
        val uriText = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(TREE_URI_KEY, null)
        if (uriText.isNullOrBlank()) {
            return File(context.filesDir, "minecraft").apply {
                if (!exists() && !mkdirs()) error("Não foi possível criar a pasta padrão do Minecraft.")
            }
        }
        if (Build.VERSION.SDK_INT >= 30) {
            check(Environment.isExternalStorageManager()) {
                "Conceda a permissão 'Acesso a todos os arquivos' nas configurações para usar essa pasta com o FCL."
            }
        } else {
            check(
                context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            ) {
                "Conceda as permissões de leitura e gravação dos arquivos para usar essa pasta com o FCL."
            }
        }
        val uri = Uri.parse(uriText)
        require(uri.authority == "com.android.externalstorage.documents") {
            "O FCL precisa de uma pasta do armazenamento local. Pastas de nuvem ou provedores externos não são compatíveis."
        }
        val documentId = DocumentsContract.getTreeDocumentId(uri)
        val separator = documentId.indexOf(':')
        require(separator > 0) { "Identificador da pasta SAF inválido." }
        val volume = documentId.substring(0, separator)
        val relative = documentId.substring(separator + 1)
        val volumeRoot = if (volume.equals("primary", ignoreCase = true)) {
            Environment.getExternalStorageDirectory()
        } else {
            File("/storage", volume)
        }.canonicalFile
        val target = if (relative.isBlank()) volumeRoot else File(volumeRoot, relative).canonicalFile
        val prefix = volumeRoot.path + File.separator
        require(target == volumeRoot || target.path.startsWith(prefix)) {
            "A pasta selecionada aponta para fora do volume de armazenamento."
        }
        if (!target.exists()) require(target.mkdirs()) { "Não foi possível criar a pasta selecionada." }
        require(target.isDirectory && target.canRead() && target.canWrite()) {
            "A pasta selecionada não permite leitura e gravação pelo aplicativo."
        }
        return target
    }
}
