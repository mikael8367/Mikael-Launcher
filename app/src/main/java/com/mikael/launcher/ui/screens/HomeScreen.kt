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
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikael.launcher.ui.components.LauncherCard
import com.mikael.launcher.ui.navigation.LauncherSection
import com.mikael.launcher.ui.theme.AccentBlue
import com.mikael.launcher.ui.theme.AccentPurple
import com.mikael.launcher.ui.theme.AccentYellow
import com.mikael.launcher.ui.theme.OutlineSoft
import com.mikael.launcher.ui.theme.PrimaryGreen
import com.mikael.launcher.ui.theme.Surface
import com.mikael.launcher.ui.theme.SurfaceRaised
import com.mikael.launcher.ui.theme.TextPrimary
import com.mikael.launcher.ui.theme.TextSecondary

@Composable
fun HomeScreen(onNavigate: (LauncherSection) -> Unit) {
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
            Surface(color = Surface, shape = RoundedCornerShape(30.dp), border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSoft)) {
                Row(modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(AccentYellow))
                    Spacer(Modifier.width(8.dp))
                    Text("PRÉ-ALPHA", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HeroPanel(onExplore = { onNavigate(LauncherSection.VERSIONS) })

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
}

@Composable
private fun HeroPanel(onExplore: () -> Unit) {
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
            Button(
                onClick = onExplore,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color(0xFF062116)),
                shape = RoundedCornerShape(11.dp)
            ) {
                Text("Explorar versões", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
        color = Surface,
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
        modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(SurfaceRaised),
        color = SurfaceRaised,
        shape = RoundedCornerShape(9.dp),
        onClick = onClick
    ) {
        Text(label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}
