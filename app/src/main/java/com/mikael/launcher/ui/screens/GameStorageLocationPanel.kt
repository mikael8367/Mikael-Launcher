package com.mikael.launcher.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.launch
import com.mikael.launcher.fcl.FclEngineBootstrap
import com.mikael.launcher.ui.theme.PrimaryGreen
import com.mikael.launcher.ui.theme.TextPrimary
import com.mikael.launcher.ui.theme.TextSecondary

@Composable
internal fun GameStorageLocationPanel() {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    val prefs = remember { context.getSharedPreferences(LAUNCHER_PREFS, Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf(prefs.getString(PREF_GAME_TREE_URI, null)) }
    var folderName by remember { mutableStateOf("Armazenamento interno do aplicativo") }
    var status by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<String>>(emptyList()) }
    var hasFileAccess by remember { mutableStateOf(checkFileAccess(context)) }
    var applyingFolder by remember { mutableStateOf(false) }

    fun applyFclDirectory() {
        if (selectedUri != null && !checkFileAccess(context)) {
            status = "Pasta salva. Conceda acesso aos arquivos para conectá-la ao FCL."
            return
        }
        applyingFolder = true
        scope.launch {
            try {
                val updated = FclEngineBootstrap.updateGameDirectory(context)
                status = if (updated) "Pasta aplicada ao FCL e aos próximos downloads." else "Pasta salva; será aplicada quando o FCL inicializar."
            } catch (error: Exception) {
                status = error.message ?: "Não foi possível aplicar a pasta ao FCL."
            } finally {
                applyingFolder = false
            }
        }
    }

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
        status = "Pasta local selecionada. Para instaladores e FCL usarem esse caminho, conceda também o acesso amplo aos arquivos."
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                selectedUri = uri.toString()
                prefs.edit().putString(PREF_GAME_TREE_URI, uri.toString()).apply()
                refresh(uri.toString())
                applyFclDirectory()
            } catch (error: Exception) {
                status = "Falha ao salvar permissão da pasta: ${error.message}"
            }
        }
    }
    val legacyPermissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        hasFileAccess = result[Manifest.permission.READ_EXTERNAL_STORAGE] == true && result[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
        status = if (hasFileAccess) "Permissão de arquivos concedida." else "Permissão não concedida. Você ainda pode escolher uma pasta pelo seletor."
    }

    fun requestFileAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                runCatching { context.startActivity(intent) }.onFailure {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }
        } else {
            legacyPermissionRequest.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    DisposableEffect(lifecycleOwner, selectedUri) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasFileAccess = checkFileAccess(context)
                if (hasFileAccess && selectedUri != null) applyFclDirectory()
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }
    LaunchedEffect(selectedUri) { refresh(selectedUri) }

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("PASTA DO MINECRAFT", color = PrimaryGreen, fontSize = 10.sp)
        Text("Local atual: $folderName", color = TextPrimary, fontSize = 12.sp)
        Text(if (hasFileAccess) "Acesso amplo aos arquivos: permitido" else "Acesso amplo aos arquivos: não concedido", color = if (hasFileAccess) PrimaryGreen else TextSecondary, fontSize = 10.sp)
        OutlinedButton(onClick = { requestFileAccess() }) { Text(if (hasFileAccess) "Gerenciar permissão de arquivos" else "Permitir acesso aos arquivos") }
        Text("No Android 11 ou superior, o Android abre a tela especial de acesso a todos os arquivos. No Android 8 a 10, aparece a solicitação de leitura e gravação. A permissão é opcional quando você usa apenas a pasta escolhida pelo seletor.", color = TextSecondary, fontSize = 10.sp)
        Text(if (applyingFolder) "Aplicando pasta ao FCL..." else status, color = TextSecondary, fontSize = 10.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { folderPicker.launch(null) }) { Text("Escolher pasta") }
            OutlinedButton(onClick = {
                selectedUri = null
                prefs.edit().remove(PREF_GAME_TREE_URI).apply()
                refresh(null)
                applyFclDirectory()
            }) { Text("Usar padrão") }
        }
        Text("Conteúdo reconhecido", color = TextPrimary, fontSize = 11.sp)
        entries.forEach { Text("• $it", color = TextSecondary, fontSize = 10.sp) }
        Text("A pasta selecionada agora é usada pelo instalador Vanilla, Fabric/Quilt e pelo diretório ativo do FCL. No Android 11+, conceda também Acesso a todos os arquivos. Pastas de nuvem e diretórios protegidos do Android não servem como diretório de jogo.", color = TextSecondary, fontSize = 10.sp)
    }
}

private fun checkFileAccess(context: Context): Boolean = when {
    Build.VERSION.SDK_INT >= 30 -> Environment.isExternalStorageManager()
    else -> ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
}
