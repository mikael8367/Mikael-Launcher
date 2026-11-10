package com.mikael.launcher.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun PlaceholderScreen(section: LauncherSection) {
    when (section) {
        LauncherSection.PLAY -> PlayScreen()
        LauncherSection.VERSIONS -> VersionsScreen()
        LauncherSection.MODS -> ModsScreen()
        LauncherSection.DOWNLOADS -> DownloadsScreen()
        LauncherSection.SETTINGS -> SettingsScreen()
        LauncherSection.ACCOUNTS,
        LauncherSection.RESOURCE_PACKS,
        LauncherSection.SHADERS,
        LauncherSection.WORLDS,
        LauncherSection.PERFORMANCE,
        LauncherSection.LOGS -> AdditionalLauncherScreen(section)
        LauncherSection.HOME -> EmptySection(
            section = section,
            icon = Icons.Rounded.Gamepad,
            description = "Painel principal do launcher"
        )
    }
}

@Composable
private fun PlayScreen() {
    var engine by rememberSaveable { mutableStateOf("Minecraft Java") }
    var profile by rememberSaveable { mutableStateOf("Vanilla 1.20.1") }
    var showInfo by rememberSaveable { mutableStateOf(false) }

    ScreenColumn(
        eyebrow = "CENTRAL DE JOGO",
        title = "Jogar",
        subtitle = "Escolha o motor e o perfil que deseja preparar."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChoiceCard(
                modifier = Modifier.weight(1f),
                title = "Minecraft Java",
                description = "Forge, Fabric, OptiFine e versões tradicionais.",
                accent = PrimaryGreen,
                selected = engine == "Minecraft Java",
                onClick = {
                    engine = "Minecraft Java"
                    profile = "Vanilla 1.20.1"
                }
            )
            ChoiceCard(
                modifier = Modifier.weight(1f),
                title = "Horizon Rust",
                description = "Motor independente. Requer compatibilidade Vulkan.",
                accent = AccentPurple,
                selected = engine == "Horizon Rust",
                onClick = {
                    engine = "Horizon Rust"
                    profile = "Horizon"
                }
            )
        }
        SectionLabel("PERFIL SELECIONADO")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Vanilla 1.20.1", "Forge 1.12.2", "Fabric 1.20.1").forEach { option ->
                FilterChip(
                    selected = profile == option && engine == "Minecraft Java",
                    onClick = {
                        engine = "Minecraft Java"
                        profile = option
                    },
                    label = { Text(option, fontSize = 11.sp) }
                )
            }
        }
        InfoCard(
            title = "Resumo da seleção",
            body = "Motor: $engine\nPerfil: $profile\nEstado: aguardando integração do motor.",
            accent = AccentBlue
        )
        Button(
            onClick = { showInfo = true },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color(0xFF062116))
        ) {
            Icon(Icons.Rounded.Gamepad, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Preparar para jogar")
        }
    }
    if (showInfo) {
        InfoDialog(
            title = "Motor ainda não conectado",
            message = "A seleção de motor e perfil já funciona. A inicialização real será ligada quando os módulos Java e Horizon forem implementados."
        ) { showInfo = false }
    }
}

@Composable
private fun VersionsScreen() {
    var engine by rememberSaveable { mutableStateOf("Java") }
    var version by rememberSaveable { mutableStateOf("1.12.2") }
    var showInfo by rememberSaveable { mutableStateOf(false) }
    ScreenColumn("GERENCIAMENTO", "Versões", "Escolha o tipo de instalação que deseja organizar.") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Java", "Horizon").forEach {
                FilterChip(selected = engine == it, onClick = {
                    engine = it
                    version = if (it == "Java") "1.12.2" else "Horizon"
                }, label = { Text(it) })
            }
        }
        SectionLabel("VERSÕES DISPONÍVEIS PARA CONFIGURAÇÃO")
        val options = if (engine == "Java") listOf("1.12.2", "1.16.5", "1.20.1", "1.21.1") else listOf("Horizon")
        options.forEach { option ->
            SelectableRow(
                title = option,
                subtitle = if (engine == "Java") "Minecraft Java • instalação ainda não baixada" else "Motor Rust independente",
                selected = version == option,
                onClick = { version = option }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showInfo = true }) {
                Icon(Icons.Rounded.Layers, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Nova instalação")
            }
            OutlinedButton(onClick = { showInfo = true }) { Text("Editar") }
            OutlinedButton(onClick = { showInfo = true }) { Text("Duplicar") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showInfo = true }) { Text("Excluir") }
            OutlinedButton(onClick = { showInfo = true }) { Text("Abrir pasta") }
            OutlinedButton(onClick = { showInfo = true }) { Text("Configurar") }
        }
        SectionLabel("OPÇÕES DA INSTALAÇÃO")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Loader", "Runtime Java", "Memória RAM", "Argumentos JVM").forEach { option ->
                FilterChip(selected = false, onClick = { showInfo = true }, label = { Text(option, fontSize = 10.sp) })
            }
        }
    }
    if (showInfo) InfoDialog(
        "Instalador em desenvolvimento",
        "A versão $version foi selecionada. O download e a instalação serão conectados na próxima etapa."
    ) { showInfo = false }
}

@Composable
private fun ModsScreen() {
    var selectedFilter by rememberSaveable { mutableStateOf("Todos") }
    var selectedFile by rememberSaveable { mutableStateOf<String?>(null) }
    var showInfo by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        selectedFile = uri?.lastPathSegment ?: uri?.toString()
    }
    ScreenColumn("PERSONALIZAÇÃO", "Mods e modpacks", "Selecione arquivos para preparar a futura biblioteca de mods.") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todos", "Forge", "Fabric", "Modpacks").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 11.sp) }
                )
            }
        }
        InfoCard(
            title = "Biblioteca vazia",
            body = "Os mods ainda não foram importados para nenhum perfil. A seleção de categoria acima já está ativa.",
            accent = AccentPurple
        )
        OutlinedButton(onClick = {
            picker.launch(arrayOf("application/java-archive", "application/zip", "application/octet-stream"))
        }) {
            Icon(Icons.Rounded.Extension, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Selecionar arquivo .jar ou .zip")
        }
        SectionLabel("AÇÕES DO MOD SELECIONADO")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showInfo = true }) { Text("Ativar") }
            OutlinedButton(onClick = { showInfo = true }) { Text("Desativar") }
            OutlinedButton(onClick = { showInfo = true }) { Text("Configurar") }
            OutlinedButton(onClick = { showInfo = true }) { Text("Remover") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showInfo = true }) { Text("Ver dependências") }
            OutlinedButton(onClick = { showInfo = true }) { Text("Abrir pasta") }
        }
        if (selectedFile != null) {
            SelectableRow(
                title = "Arquivo selecionado",
                subtitle = selectedFile ?: "",
                selected = true,
                onClick = { showInfo = true }
            )
            Text("O arquivo foi selecionado pelo Android, mas ainda não foi copiado para uma pasta de perfil.", color = TextSecondary, fontSize = 11.sp)
        }
    }
    if (showInfo) InfoDialog(
        "Importação ainda não concluída",
        "O seletor de arquivos está funcionando. A cópia, validação de dependências e ativação por perfil serão implementadas no módulo de mods."
    ) { showInfo = false }
}

@Composable
private fun DownloadsScreen() {
    var refreshCount by rememberSaveable { mutableIntStateOf(0) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    ScreenColumn("CENTRAL DE ARQUIVOS", "Downloads", "Acompanhe os arquivos que serão gerenciados pelo launcher.") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { refreshCount++ }) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Atualizar")
            }
            Text("Atualizações da tela: $refreshCount", color = TextSecondary, fontSize = 11.sp)
        }
        EmptyState(
            icon = Icons.Rounded.CloudDownload,
            title = "Nenhum download em andamento",
            description = "Quando os instaladores e arquivos forem integrados, o progresso aparecerá aqui."
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showDetails = true }) { Text("Pausar tudo") }
            OutlinedButton(onClick = { showDetails = true }) { Text("Retomar tudo") }
            OutlinedButton(onClick = { showDetails = true }) { Text("Cancelar fila") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showDetails = true }) { Text("Limpar concluídos") }
            OutlinedButton(onClick = { showDetails = true }) { Text("Ver informações") }
        }
    }
    if (showDetails) InfoDialog(
        "Gerenciador de downloads",
        "Esta tela ainda não consulta uma fila real. O botão Atualizar atualiza o estado local da tela; downloads reais virão com o serviço de transferência."
    ) { showDetails = false }
}

@Composable
private fun SettingsScreen() {
    var animations by rememberSaveable { mutableStateOf(true) }
    var compactCards by rememberSaveable { mutableStateOf(false) }
    var notifications by rememberSaveable { mutableStateOf(true) }
    var showReset by rememberSaveable { mutableStateOf(false) }
    ScreenColumn("PREFERÊNCIAS", "Configurações", "Ajuste as preferências visuais desta interface.") {
        SettingToggle("Animações da interface", "Controla a preferência visual nesta etapa.", animations) { animations = it }
        SettingToggle("Cards compactos", "Preferência de densidade visual.", compactCards) { compactCards = it }
        SettingToggle("Notificações", "Preferência local para avisos do launcher.", notifications) { notifications = it }
        HorizontalDivider(color = OutlineSoft)
        InfoCard(
            title = "Sobre o projeto",
            body = "Mikael Launcher • versão 0.1.0-alpha\nPreferências demonstrativas ficam nesta tela e ainda não alteram os motores.",
            accent = AccentYellow
        )
        OutlinedButton(onClick = { showReset = true }) { Text("Restaurar preferências desta tela") }
    }
    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Restaurar preferências?") },
            text = { Text("As três opções voltarão aos valores iniciais.") },
            confirmButton = {
                TextButton(onClick = {
                    animations = true
                    compactCards = false
                    notifications = true
                    showReset = false
                }) { Text("Restaurar") }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ScreenColumn(
    eyebrow: String,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(eyebrow, color = PrimaryGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
        Text(title, color = TextPrimary, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        content()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
}

@Composable
private fun ChoiceCard(
    modifier: Modifier,
    title: String,
    description: String,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (selected) accent.copy(alpha = 0.13f) else PanelSurface),
        border = BorderStroke(1.dp, if (selected) accent else OutlineSoft)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = "Selecionado", tint = accent)
            }
            Text(description, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SelectableRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = if (selected) PrimaryGreen.copy(alpha = 0.10f) else PanelSurface,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, if (selected) PrimaryGreen.copy(alpha = 0.7f) else OutlineSoft)
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, color = TextSecondary, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = "Selecionado", tint = PrimaryGreen)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String, accent: Color) {
    Surface(color = PanelSurface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OutlineSoft)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(body, color = TextSecondary, fontSize = 11.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun SettingToggle(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(PanelSurface, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = TextSecondary, fontSize = 10.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Surface(color = AccentBlue.copy(alpha = 0.12f), shape = CircleShape) {
            Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.padding(18.dp).size(28.dp))
        }
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(description, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun EmptySection(section: LauncherSection, icon: ImageVector, description: String) {
    ScreenColumn("MIKAEL LAUNCHER", section.label, description) {
        EmptyState(icon, section.label, description)
    }
}

@Composable
private fun InfoDialog(title: String, message: String, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onClose) { Text("Entendi") } }
    )
}
