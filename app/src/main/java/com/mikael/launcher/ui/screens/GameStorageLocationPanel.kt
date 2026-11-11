package com.mikael.launcher.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.mikael.launcher.ui.theme.PrimaryGreen
import com.mikael.launcher.ui.theme.TextPrimary
import com.mikael.launcher.ui.theme.TextSecondary

@Composable
internal fun GameStorageLocationPanel() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(LAUNCHER_PREFS, Context.MODE_PRIVATE) }
    var selectedUri by remember { mutableStateOf(prefs.getString(PREF_GAME_TREE_URI, null)) }
    var folderName by remember { mutableStateOf("Armazenamento interno do aplicativo") }
    var status by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<String>>(emptyList()) }

    fun refresh(uriString: String?) {
        val folders = listOf("versions", "libraries", "assets", "mods", "resourcepacks", "shaderpacks", "saves")
        if (uriString.isNullOrBlank()) {
            val root = context.filesDir.resolve("minecraft")
            folders.forEach { java.io.File(root, it).mkdirs() }
            folderName = "Armazenamento interno do aplicativo"
            entries = folders.map { name ->
                val folder = java.io.File(root, name)
                "$name/ • ${folder.list()?.size ?: 0} itens"
            }
            status = "Padrão ativo: ${root.absolutePath}"
            return
        }
        val tree = DocumentFile.fromTreeUri(context, Uri.parse(uriString))
        if (tree == null || !tree.isDirectory || !tree.canWrite()) {
            status = "Pasta sem permissão de escrita. Selecione novamente."
            entries = emptyList()
            return
        }
        folderName = tree.name ?: "Pasta personalizada"
        folders.forEach { name -> if (tree.findFile(name) == null) tree.createDirectory(name) }
        entries = folders.map { name ->
            val folder = tree.findFile(name)
            "$name/ • ${folder?.listFiles()?.size ?: 0} itens"
        }
        val installed = tree.findFile("versions")?.listFiles()?.filter { it.isDirectory }?.mapNotNull { it.name }.orEmpty()
        entries = entries + if (installed.isEmpty()) listOf("Nenhuma versão encontrada") else installed.map { "Versão: $it" }
        status = "Pasta selecionada e permissão persistida."
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                selectedUri = uri.toString()
                prefs.edit().putString(PREF_GAME_TREE_URI, uri.toString()).apply()
                refresh(uri.toString())
            } catch (error: Exception) {
                status = "Falha ao salvar permissão: ${error.message}"
            }
        }
    }

    LaunchedEffect(selectedUri) { refresh(selectedUri) }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("PASTA DO MINECRAFT", color = PrimaryGreen, fontSize = 10.sp)
        Text("Local atual: $folderName", color = TextPrimary, fontSize = 12.sp)
        Text(status, color = TextSecondary, fontSize = 10.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { picker.launch(null) }) { Text("Escolher pasta") }
            OutlinedButton(onClick = {
                selectedUri = null
                prefs.edit().remove(PREF_GAME_TREE_URI).apply()
                refresh(null)
            }) { Text("Usar padrão") }
        }
        Text("Conteúdo reconhecido", color = TextPrimary, fontSize = 11.sp)
        entries.forEach { Text("• $it", color = TextSecondary, fontSize = 10.sp) }
        Text(
            "A seleção e a permissão ficam salvas. A migração dos downloads e a integração com o FCL ainda precisam ser conectadas; por enquanto, as instalações continuam usando a pasta padrão privada.",
            color = TextSecondary, fontSize = 10.sp
        )
    }
}
