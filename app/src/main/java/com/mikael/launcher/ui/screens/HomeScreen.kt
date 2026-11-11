package com.mikael.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.mikael.launcher.fcl.FclGameLaunchCoordinator
import com.mikael.launcher.fcl.FclLaunchPreferences
import com.mikael.launcher.fcl.buildLaunchDiagnostic
import com.mikael.launcher.fcl.parseMemoryMB
import com.mikael.launcher.fcl.runtimeFolderForLabel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikael.launcher.ui.components.LauncherCard
import com.mikael.launcher.ui.components.AnimatedLauncherButton
import com.mikael.launcher.ui.navigation.LauncherSection
import com.mikael.launcher.ui.theme.AccentBlue
import com.mikael.launcher.ui.theme.AccentPurple
import com.mikael.launcher.ui.theme.AccentYellow
import com.mikael.launcher.ui.theme.OutlineSoft
import com.mikael.launcher.ui.theme.PrimaryGreen
import com.mikael.launcher.ui.theme.Surface as PanelSurface
import com.mikael.launcher.ui.theme.SurfaceRaised
import com.mikael.launcher.ui.theme.TextPrimary
import com.mikael.launcher.ui.theme.TextSecondary

private const val VERSION_PREFS = "mikael_launcher_preferences"
private const val INSTALLED_VERSION_PROFILES = "launcher_installed_version_profiles"
private const val SELECTED_VERSION_PROFILE = "launcher_selected_version_profile"

@Composable
fun HomeScreen(onNavigate: (LauncherSection) -> Unit, selectedAccount: String) {
    val context = LocalContext.current
    val hostActivity = remember(context) { findHostActivity(context) as? ComponentActivity }
    val prefs = remember { context.getSharedPreferences(VERSION_PREFS, Context.MODE_PRIVATE) }
    var versionProfiles by remember {
        mutableStateOf(prefs.getString(INSTALLED_VERSION_PROFILES, "").orEmpty().split("\n").filter { it.isNotBlank() })
    }
    var selectedVersion by remember {
        mutableStateOf(
            prefs.getString(SELECTED_VERSION_PROFILE, "").orEmpty()
                .takeIf { it in versionProfiles }
                .orEmpty()
        )
    }
    var showVersionPicker by remember { mutableStateOf(false) }
    var isLaunching by remember { mutableStateOf(false) }
    var launchDialogMessage by remember { mutableStateOf<String?>(null) }
    var launchDiagnostic by remember { mutableStateOf<String?>(null) }
    var diagnosticCopied by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(end = 4.dp, top = 3.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("PAINEL PRINCIPAL", color = PrimaryGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Bem-vindo ao seu launcher", color = TextPrimary, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
            }
            Surface(color = PanelSurface, shape = RoundedCornerShape(30.dp), border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSoft)) {
                Row(modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(AccentYellow))
                    Spacer(Modifier.width(8.dp))
                    Text("PRÉ-ALPHA", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HeroPanel(
            selectedVersion = selectedVersion,
            onChooseVersion = { showVersionPicker = true },
            onStart = {
                when {
                    selectedVersion.isBlank() -> {
                        launchDialogMessage = "Selecione uma versão instalada antes de iniciar."
                        launchDiagnostic = null
                    }
                    selectedAccount.isBlank() -> {
                        launchDialogMessage = "Selecione ou adicione uma conta offline na aba Contas."
                        launchDiagnostic = null
                    }
                    hostActivity == null -> {
                        launchDialogMessage = "Não foi possível acessar a tela principal do Android."
                        launchDiagnostic = null
                    }
                    else -> {
                        isLaunching = true
                        launchDialogMessage = "Preparando a versão, verificando bibliotecas e iniciando o motor Java do FCL..."
                        launchDiagnostic = null
                        diagnosticCopied = false
                        // Preferências escolhidas em Configurações (runtime Java,
                        // renderizador, RAM e argumentos JVM) passam a valer na
                        // inicialização; chaves desconhecidas ficam nulas e o FCL
                        // mantém seus próprios valores.
                        val appPrefs = context.getSharedPreferences(VERSION_PREFS, Context.MODE_PRIVATE)
                        val launchPreferences = FclLaunchPreferences(
                            runtimeFolder = runtimeFolderForLabel(appPrefs.getString(PREF_RUNTIME, null)),
                            rendererLabel = appPrefs.getString(PREF_RENDERER, null),
                            memoryMB = parseMemoryMB(appPrefs.getString(PREF_RAM, null)),
                            autoMemory = appPrefs.getBoolean("perf_auto_ram", true),
                            javaArgs = appPrefs.getString(PREF_JVM_ARGS, null)
                        )
                        hostActivity.lifecycleScope.launch {
                            try {
                                FclGameLaunchCoordinator.launch(hostActivity, selectedVersion, selectedAccount, launchPreferences)
                                launchDialogMessage = null
                            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                throw cancelled
                            } catch (error: Throwable) {
                                launchDialogMessage = error.message ?: "Falha desconhecida ao iniciar Minecraft."
                                launchDiagnostic = buildLaunchDiagnostic(
                                    error = error,
                                    stage = FclGameLaunchCoordinator.currentStage,
                                    logDir = FclGameLaunchCoordinator.currentLogDirectory()
                                )
                            } finally {
                                isLaunching = false
                            }
                        }
                    }
                }
            },
            onExplore = { onNavigate(LauncherSection.VERSIONS) }
        )

        Spacer(Modifier.height(14.dp))
        Text("SEU AMBIENTE", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            QuickInfoCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.SportsEsports,
                iconColor = PrimaryGreen,
                title = "Minecraft Java",
                subtitle = "Motor independente"
            )
            QuickInfoCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Bolt,
                iconColor = AccentPurple,
                title = "Horizon Rust",
                subtitle = "Motor experimental"
            )
            QuickInfoCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Memory,
                iconColor = AccentBlue,
                title = "Desempenho",
                subtitle = "Ajustes em breve"
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LauncherCard(
                modifier = Modifier.weight(1f),
                accent = PrimaryGreen,
                title = "Comece por aqui",
                subtitle = "Acesse as áreas do launcher"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallAction("Versões", onClick = { onNavigate(LauncherSection.VERSIONS) })
                    SmallAction("Mods", onClick = { onNavigate(LauncherSection.MODS) })
                }
            }
            LauncherCard(
                modifier = Modifier.weight(1f),
                accent = AccentBlue,
                title = "Central de downloads",
                subtitle = "Instaladores e arquivos em um só lugar"
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Gerenciador será integrado na próxima etapa", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    if (showVersionPicker) {
        AlertDialog(
            onDismissRequest = { showVersionPicker = false },
            title = { Text("Selecionar versão") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (versionProfiles.isEmpty()) {
                        Text("Nenhuma versão foi adicionada ainda. Acesse Versões para criar um perfil de instalação.", color = TextSecondary)
                    } else {
                        versionProfiles.forEach { profile ->
                            TextButton(onClick = {
                                selectedVersion = profile
                                prefs.edit().putString(SELECTED_VERSION_PROFILE, profile).apply()
                                showVersionPicker = false
                            }) {
                                Text(if (profile == selectedVersion) "✓  $profile" else profile)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showVersionPicker = false }) { Text("Fechar") } },
            dismissButton = { TextButton(onClick = { showVersionPicker = false; onNavigate(LauncherSection.VERSIONS) }) { Text("Gerenciar versões") } }
        )
    }
    if (isLaunching || launchDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { if (!isLaunching) launchDialogMessage = null },
            title = { Text(if (isLaunching) "Preparando Minecraft" else "Iniciar Minecraft") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isLaunching) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                    Text(launchDialogMessage.orEmpty())
                    if (!isLaunching && selectedAccount.isNotBlank()) {
                        Text("Conta selecionada: $selectedAccount", color = TextSecondary)
                    }
                    if (!isLaunching && launchDiagnostic != null) {
                        Surface(
                            color = PanelSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSoft)
                        ) {
                            Text(
                                launchDiagnostic.orEmpty(),
                                color = TextSecondary,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (!isLaunching) {
                    TextButton(onClick = { launchDialogMessage = null }) { Text("Fechar") }
                }
            },
            dismissButton = {
                if (!isLaunching && launchDiagnostic != null) {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("mikael-launch-diagnostic", launchDiagnostic))
                        diagnosticCopied = true
                    }) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (diagnosticCopied) "Copiado!" else "Copiar diagnóstico")
                    }
                } else if (!isLaunching) {
                    TextButton(onClick = {
                        launchDialogMessage = null
                        onNavigate(LauncherSection.VERSIONS)
                    }) { Text("Versões") }
                }
            }
        )
    }
}

@Composable
private fun HeroPanel(selectedVersion: String, onChooseVersion: () -> Unit, onStart: () -> Unit, onExplore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF173B2C), Color(0xFF172B31), Color(0xFF20213A))
                )
            )
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Surface(color = PrimaryGreen.copy(alpha = 0.14f), shape = RoundedCornerShape(30.dp)) {
                Text(
                    "SUA JORNADA COMEÇA AQUI",
                    color = PrimaryGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("Seu Minecraft.\nDo seu jeito.", color = TextPrimary, fontSize = 25.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Uma central para organizar suas versões, mods e experiências.",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(13.dp))
            Text("VERSÃO SELECIONADA", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Surface(
                onClick = onChooseVersion,
                color = Color(0xFF10271E),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.55f))
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (selectedVersion.isBlank()) "Selecionar versão" else selectedVersion, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("⌄", color = PrimaryGreen, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimatedLauncherButton(text = "▶  Iniciar", onClick = onStart)
                AnimatedLauncherButton(text = "Versões", onClick = onExplore)
            }
        }
        Box(
            modifier = Modifier
                .padding(start = 12.dp)
                .size(112.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(PrimaryGreen.copy(alpha = 0.30f), Color.Transparent))),
            contentAlignment = Alignment.Center
        ) {
            Surface(color = Color(0xFF0D211A).copy(alpha = 0.7f), shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.45f))) {
                Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                    Text("M", color = PrimaryGreen, fontSize = 47.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun QuickInfoCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String
) {
    Surface(
        modifier = modifier,
        color = PanelSurface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSoft.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = iconColor.copy(alpha = 0.13f), shape = RoundedCornerShape(11.dp)) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(9.dp).size(18.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column {
                Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = TextSecondary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SmallAction(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(9.dp)),
        color = SurfaceRaised,
        shape = RoundedCornerShape(9.dp),
        onClick = onClick
    ) {
        Text(label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}


private tailrec fun findHostActivity(context: Context): Activity? = when (context) {
    is Activity -> context
    is ContextWrapper -> findHostActivity(context.baseContext)
    else -> null
}
