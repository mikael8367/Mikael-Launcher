package com.mikael.launcher.ui.screens

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikael.launcher.fcl.FclEngineBootstrap
import com.mikael.launcher.fcl.FclJavaRuntime
import com.mikael.launcher.fcl.FclRuntimeInstaller
import kotlinx.coroutines.launch
import com.mikael.launcher.ui.theme.OutlineSoft
import com.mikael.launcher.ui.theme.PrimaryGreen
import com.mikael.launcher.ui.theme.Surface as PanelSurface
import com.mikael.launcher.ui.theme.TextPrimary
import com.mikael.launcher.ui.theme.TextSecondary

internal const val LAUNCHER_PREFS = "mikael_launcher_preferences"
internal const val PREF_FIRST_RUN_DONE = "first_run_runtime_choice"
internal const val PREF_RUNTIME = "selected_java_runtime"
internal const val PREF_RENDERER = "selected_renderer"
internal const val PREF_RAM = "selected_ram_limit"
internal const val PREF_JVM_ARGS = "selected_jvm_arguments"
internal const val PREF_GAME_TREE_URI = "minecraft_game_tree_uri"

val JAVA_RUNTIMES = listOf("Java 8", "Java 17", "Java 21", "Java 25")
val RENDERER_BACKENDS = listOf("GL4ES", "Zink", "VGPU")

/** Parses "Java 17" into 17. Returns null instead of crashing on corrupted preferences. */
internal fun runtimeMajorFromLabel(label: String): Int? =
    label.substringAfter("Java ", "").trim().toIntOrNull()

@Composable
fun RuntimeSettingsPanel() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(LAUNCHER_PREFS, Context.MODE_PRIVATE) }
    var runtime by remember { mutableStateOf(prefs.getString(PREF_RUNTIME, "Java 8") ?: "Java 8") }
    var renderer by remember { mutableStateOf(prefs.getString(PREF_RENDERER, "GL4ES") ?: "GL4ES") }
    var javaArgs by remember { mutableStateOf(prefs.getString(PREF_JVM_ARGS, "") ?: "") }
    var ramLimit by remember { mutableStateOf(prefs.getString(PREF_RAM, "2048 MB") ?: "2048 MB") }
    var installNotice by remember { mutableStateOf(false) }
    var installing by remember { mutableStateOf(false) }
    var installDetail by remember { mutableStateOf("Verificando backend FCL...") }
    var installedRuntimes by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    LaunchedEffect(context) {
        val bootstrap = FclEngineBootstrap.initialize(context.applicationContext)
        installDetail = bootstrap.message
        if (bootstrap.initialized) {
            installedRuntimes = FclRuntimeInstaller.installedRuntimes().toSet()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        GameStorageLocationPanel()
        Text("RUNTIME JAVA", color = PrimaryGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("Runtime selecionada: $runtime", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            JAVA_RUNTIMES.forEach { option ->
                FilterChip(selected = runtime == option, onClick = {
                    runtime = option
                    prefs.edit().putString(PREF_RUNTIME, option).apply()
                }, label = { Text(option, fontSize = 10.sp) })
            }
        }
        Text("RENDERIZADOR GRÁFICO", color = PrimaryGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("Renderizador: $renderer", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        RENDERER_BACKENDS.chunked(3).forEach { group ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                group.forEach { option ->
                    FilterChip(selected = renderer == option, onClick = {
                        renderer = option
                        prefs.edit().putString(PREF_RENDERER, option).apply()
                    }, label = { Text(option, fontSize = 9.sp) })
                }
            }
        }
        Text("MEMÓRIA E PARÂMETROS", color = PrimaryGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("1024 MB", "1536 MB", "2048 MB", "3072 MB").forEach { option ->
                FilterChip(selected = ramLimit == option, onClick = { ramLimit = option; prefs.edit().putString(PREF_RAM, option).apply() }, label = { Text(option, fontSize = 9.sp) })
            }
        }
        androidx.compose.material3.OutlinedTextField(
            value = javaArgs,
            onValueChange = { javaArgs = it; prefs.edit().putString(PREF_JVM_ARGS, it).apply() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Argumentos JVM opcionais") },
            minLines = 2
        )
        val runtimeFolder = when (runtime) {
            "Java 8" -> "jre8"
            "Java 17" -> "jre17"
            "Java 21" -> "jre21"
            else -> "jre25"
        }
        Text(
            if (runtimeFolder in installedRuntimes) "$runtime instalado" else "$runtime ainda não instalado",
            color = if (runtimeFolder in installedRuntimes) PrimaryGreen else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Button(
            enabled = !installing,
            onClick = {
                installing = true
                installDetail = "Iniciando instalação de $runtime..."
                scope.launch {
                    try {
                        val major = runtimeMajorFromLabel(runtime)
                            ?: throw IllegalStateException("Runtime Java inválido: $runtime")
                        val target = FclJavaRuntime.values().firstOrNull { it.majorVersion == major }
                            ?: throw IllegalStateException("Runtime Java não suportado: $runtime")
                        val result = FclRuntimeInstaller.install(context.applicationContext, target) { progress ->
                            mainHandler.post { installDetail = progress.detail }
                        }
                        installedRuntimes = FclRuntimeInstaller.installedRuntimes().toSet()
                        installDetail = result.message
                    } catch (error: Throwable) {
                        installDetail = "Falha: ${error.message ?: error.javaClass.simpleName}"
                    } finally {
                        installing = false
                    }
                }
            }
        ) { Text(if (installing) "Instalando..." else "Instalar $runtime") }
        Text(installDetail, color = TextSecondary, fontSize = 10.sp)
        Text("Os runtimes são instalados no armazenamento privado do aplicativo e validados quanto à versão e arquitetura ARM64.", color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
fun FirstRunRuntimeDialog(onFinished: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(LAUNCHER_PREFS, Context.MODE_PRIVATE) }
    var runtime by remember { mutableStateOf(prefs.getString(PREF_RUNTIME, "Java 8") ?: "Java 8") }
    var renderer by remember { mutableStateOf(prefs.getString(PREF_RENDERER, "GL4ES") ?: "GL4ES") }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("Configuração inicial") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Escolha o runtime Java que deseja preparar para instalar.", color = TextSecondary, fontSize = 12.sp)
                JAVA_RUNTIMES.forEach { option ->
                    SelectableRuntimeRow(option, runtime == option) { runtime = option }
                }
                Text("Renderizador gráfico inicial", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("GL4ES", "Zink", "VirGL").forEach { option ->
                        FilterChip(selected = renderer == option, onClick = { renderer = option }, label = { Text(option, fontSize = 10.sp) })
                    }
                }
                Text("A compatibilidade depende do Android, processador gráfico e versão do Minecraft.", color = TextSecondary, fontSize = 10.sp)
            }
        },
        confirmButton = {
            Button(onClick = {
                prefs.edit()
                    .putString(PREF_RUNTIME, runtime)
                    .putString(PREF_RENDERER, renderer)
                    .putBoolean(PREF_FIRST_RUN_DONE, true)
                    .apply()
                onFinished()
            }) { Text("Salvar e continuar") }
        }
    )
}

@Composable
private fun SelectableRuntimeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (selected) PrimaryGreen.copy(alpha = 0.10f) else PanelSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) PrimaryGreen else OutlineSoft)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            if (selected) Text("Selecionado", color = PrimaryGreen, fontSize = 10.sp)
        }
    }
}


@Composable
fun PersistentPreferenceToggle(key: String, title: String, description: String, defaultValue: Boolean = false) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(LAUNCHER_PREFS, Context.MODE_PRIVATE) }
    var checked by remember(key, prefs.getBoolean(key, defaultValue)) { mutableStateOf(prefs.getBoolean(key, defaultValue)) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            Text(description, color = TextSecondary, fontSize = 10.sp)
        }
        Switch(checked = checked, onCheckedChange = {
            checked = it
            prefs.edit().putBoolean(key, it).apply()
        })
    }
}
