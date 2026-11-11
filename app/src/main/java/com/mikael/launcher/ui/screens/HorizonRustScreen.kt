package com.mikael.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikael.launcher.ui.components.AnimatedLauncherButton
import com.mikael.launcher.ui.theme.AccentPurple
import com.mikael.launcher.ui.theme.OutlineSoft
import com.mikael.launcher.ui.theme.PrimaryGreen
import com.mikael.launcher.ui.theme.Surface as PanelSurface
import com.mikael.launcher.ui.theme.SurfaceRaised
import com.mikael.launcher.ui.theme.TextPrimary
import com.mikael.launcher.ui.theme.TextSecondary

@Composable
fun HorizonRustScreen() {
    val uriHandler = LocalUriHandler.current
    var showInstallInfo by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(end = 4.dp, top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "MOTOR ALTERNATIVO",
            color = AccentPurple,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.8.sp
        )
        Text(
            "Horizon Rust",
            color = TextPrimary,
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            "Minecraft Java 26.3 reimplementado em Rust, com motor separado do Java/FCL.",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = PanelSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSoft)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(178.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF102D25), Color(0xFF172D3A), Color(0xFF342343))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Surface(
                        color = PrimaryGreen.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.padding(18.dp).size(48.dp)
                        )
                    }
                    Column {
                        Text(
                            "HORIZON",
                            color = Color.White,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "RUST ENGINE",
                            color = PrimaryGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Sobre o projeto",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Projeto open source que reimplementa o Minecraft Java 26.3 em Rust. O motor é independente da JVM e não executa mods Java Forge/Fabric; utiliza o próprio sistema de mods Horizon.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = AccentPurple.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(30.dp)
                    ) {
                        Text(
                            "ANDROID EM AVALIAÇÃO",
                            color = AccentPurple,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        "Sem launcher Android ARM64 publicado",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
                AnimatedLauncherButton(
                    text = "Consultar disponibilidade",
                    onClick = { showInstallInfo = true },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = { uriHandler.openUri("https://github.com/horizon-rust/horizon") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Abrir projeto oficial no GitHub")
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Compatibilidade Android: pendências",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("• Empacotar e validar o cliente Android ARM64 completo", color = TextSecondary, fontSize = 11.sp)
                Text("• Ligar o cliente real ao JNI e ao ciclo de vida Android", color = TextSecondary, fontSize = 11.sp)
                Text("• Validar Vulkan/GLES no aparelho e controles por toque", color = TextSecondary, fontSize = 11.sp)
                Text("• Preparação de recursos, instalação e inicialização", color = TextSecondary, fontSize = 11.sp)
            }
        }
    }

    if (showInstallInfo) {
        AlertDialog(
            onDismissRequest = { showInstallInfo = false },
            title = { Text("Instalação Android ainda indisponível") },
            text = {
                Text(
                    "O código upstream já contém módulos de entrada específicos para Android, mas as releases atuais do launcher Horizon continuam publicando pacotes para Windows, macOS e Linux — não um launcher Android ARM64. O protótipo JNI deste repositório ainda só cria sessões de teste: não inicializa o cliente Horizon, renderer ou mundo. Precisamos integrar os crates reais, adaptar superfície/lifecycle/armazenamento e validar GLES/Vulkan no aparelho antes de liberar instalar ou jogar. Nenhum arquivo será baixado agora."
                )
            },
            confirmButton = {
                TextButton(onClick = { showInstallInfo = false }) {
                    Text("Entendi")
                }
            }
        )
    }
}
