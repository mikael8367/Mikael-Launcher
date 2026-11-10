package com.mikael.launcher.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikael.launcher.ui.navigation.LauncherSection
import com.mikael.launcher.ui.theme.OutlineSoft
import com.mikael.launcher.ui.theme.PrimaryGreen
import com.mikael.launcher.ui.theme.Surface as PanelSurface
import com.mikael.launcher.ui.theme.TextPrimary
import com.mikael.launcher.ui.theme.TextSecondary

@Composable
fun AdditionalLauncherScreen(section: LauncherSection) {
    val config = when (section) {
        LauncherSection.ACCOUNTS -> ScreenConfig("CONTAS", "Contas", "Gerencie os perfis de autenticação do launcher.", listOf("Microsoft", "Mojang legado", "Offline", "Ely.by"), listOf("Adicionar conta", "Definir conta padrão", "Atualizar sessão", "Remover conta"))
        LauncherSection.RESOURCE_PACKS -> ScreenConfig("PERSONALIZAÇÃO", "Pacotes de recursos", "Organize texturas e resource packs por instalação.", listOf("Padrão", "Pacotes instalados", "Desativados"), listOf("Adicionar pacote", "Abrir pasta", "Ativar selecionado", "Remover selecionado"))
        LauncherSection.SHADERS -> ScreenConfig("GRÁFICOS", "Shaders", "Gerencie os pacotes gráficos compatíveis com cada perfil.", listOf("Todos", "Compatíveis", "Incompatíveis"), listOf("Adicionar shader", "Abrir pasta de shaders", "Pré-visualizar", "Remover shader"))
        LauncherSection.WORLDS -> ScreenConfig("BIBLIOTECA", "Mundos", "Organize seus mundos locais e backups.", listOf("Todos", "Sobrevivência", "Criativo", "Favoritos"), listOf("Importar mundo", "Criar backup", "Restaurar backup", "Abrir pasta", "Excluir mundo"))
        LauncherSection.PERFORMANCE -> ScreenConfig("OTIMIZAÇÃO", "Desempenho", "Configure os limites e preferências de execução por perfil.", listOf("Equilibrado", "FPS", "Baixo consumo"), listOf("Detectar dispositivo", "Configurar memória", "Configurar resolução", "Opções gráficas", "Restaurar padrão"))
        LauncherSection.LOGS -> ScreenConfig("DIAGNÓSTICO", "Logs e relatórios", "Consulte registros e prepare relatórios de falha.", listOf("Todos", "Inicialização", "Erros", "Avisos"), listOf("Atualizar logs", "Copiar relatório", "Compartilhar relatório", "Limpar registros"))
        else -> ScreenConfig("GERENCIAMENTO", section.label, "Área de gerenciamento do launcher.", listOf("Todos", "Ativos", "Arquivados"), listOf("Adicionar", "Editar", "Abrir pasta", "Remover"))
    }
    var selectedTab by rememberSaveable(section.name) { mutableStateOf(config.tabs.first()) }
    var selectedAction by rememberSaveable(section.name) { mutableStateOf<String?>(null) }
    var query by rememberSaveable(section.name) { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Text(config.eyebrow, color = PrimaryGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text(config.title, color = TextPrimary, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        Text(config.subtitle, color = TextSecondary, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            config.tabs.forEach { tab ->
                FilterChip(selected = selectedTab == tab, onClick = { selectedTab = tab }, label = { Text(tab, fontSize = 10.sp) })
            }
        }
        Surface(color = PanelSurface, shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, OutlineSoft)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Área de trabalho", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Filtro atual: $selectedTab", color = TextSecondary, fontSize = 11.sp)
                Text("Nenhum item cadastrado nesta interface ainda. Os controles abaixo representam as ações previstas para este módulo.", color = TextSecondary, fontSize = 11.sp)
            }
        }
        Text("AÇÕES", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
        config.actions.forEachIndexed { index, action ->
            val icon = when {
                action.contains("Adicionar") || action.contains("Criar") || action.contains("Importar") -> Icons.Rounded.Add
                action.contains("Remover") || action.contains("Excluir") || action.contains("Limpar") -> Icons.Rounded.Delete
                action.contains("Editar") || action.contains("Configurar") -> Icons.Rounded.Edit
                action.contains("Abrir") -> Icons.Rounded.FolderOpen
                action.contains("Atualizar") || action.contains("Detectar") -> Icons.Rounded.Refresh
                else -> Icons.Rounded.Settings
            }
            OutlinedButton(
                onClick = { selectedAction = action },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(icon, contentDescription = null)
                Spacer(Modifier.width(9.dp))
                Text(action)
            }
        }
        Surface(color = PanelSurface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, OutlineSoft)) {
            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = PrimaryGreen)
                Spacer(Modifier.width(9.dp))
                Text("Interface preparada. As operações reais serão conectadas aos serviços de cada módulo depois.", color = TextSecondary, fontSize = 11.sp)
            }
        }
    }

    selectedAction?.let { action ->
        AlertDialog(
            onDismissRequest = { selectedAction = null },
            title = { Text(action) },
            text = { Text("O botão e o fluxo visual estão definidos. A operação real será implementada na etapa de lógica deste módulo.") },
            confirmButton = { TextButton(onClick = { selectedAction = null }) { Text("Entendi") } },
            dismissButton = { TextButton(onClick = { selectedAction = null }) { Text("Fechar") } }
        )
    }
}

private data class ScreenConfig(
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val tabs: List<String>,
    val actions: List<String>
)
