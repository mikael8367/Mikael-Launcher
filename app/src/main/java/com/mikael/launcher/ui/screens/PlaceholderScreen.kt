package com.mikael.launcher.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikael.launcher.ui.navigation.LauncherSection
import com.mikael.launcher.ui.theme.PrimaryGreen
import com.mikael.launcher.ui.theme.Surface as PanelSurface
import com.mikael.launcher.ui.theme.TextPrimary
import com.mikael.launcher.ui.theme.TextSecondary

@Composable
fun PlaceholderScreen(section: LauncherSection) {
    val (icon, description) = sectionContent(section)
    Box(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(color = PanelSurface, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier.padding(horizontal = 36.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(color = PrimaryGreen.copy(alpha = 0.13f), shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
                    Icon(icon, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.padding(16.dp))
                }
                Text(section.label, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(description, color = TextSecondary, fontSize = 12.sp)
                Text("Esta área será desenvolvida na próxima etapa.", color = PrimaryGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun sectionContent(section: LauncherSection): Pair<ImageVector, String> = when (section) {
    LauncherSection.HOME -> Icons.Rounded.Gamepad to "Painel principal do launcher"
    LauncherSection.PLAY -> Icons.Rounded.Gamepad to "Escolha e inicie uma experiência"
    LauncherSection.VERSIONS -> Icons.Rounded.Layers to "Gerencie versões e instalações"
    LauncherSection.MODS -> Icons.Rounded.Extension to "Organize mods e modpacks por perfil"
    LauncherSection.DOWNLOADS -> Icons.Rounded.CloudDownload to "Acompanhe instaladores e arquivos"
    LauncherSection.SETTINGS -> Icons.Rounded.Settings to "Personalize o comportamento do launcher"
}
