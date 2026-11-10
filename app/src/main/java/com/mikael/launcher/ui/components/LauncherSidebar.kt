package com.mikael.launcher.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikael.launcher.ui.navigation.LauncherSection
import com.mikael.launcher.ui.theme.OutlineSoft
import com.mikael.launcher.ui.theme.PrimaryGreen
import com.mikael.launcher.ui.theme.Surface
import com.mikael.launcher.ui.theme.SurfaceRaised
import com.mikael.launcher.ui.theme.TextSecondary

@Composable
fun LauncherSidebar(
    selected: LauncherSection,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSectionSelected: (LauncherSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(if (expanded) 218.dp else 76.dp)
            .animateContentSize(),
        color = Surface,
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = if (expanded) Alignment.Start else Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (expanded) 4.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    color = PrimaryGreen.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Text("M", color = PrimaryGreen, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    }
                }
                if (expanded) {
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("MIKAEL", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 1.2.sp)
                        Text("LAUNCHER", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = if (expanded) "MENU PRINCIPAL" else "MENU",
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                modifier = Modifier.padding(start = if (expanded) 10.dp else 0.dp, bottom = 10.dp)
            )

            val items = listOf(
                LauncherSection.HOME to Icons.Rounded.Home,
                LauncherSection.PLAY to Icons.Rounded.PlayArrow,
                LauncherSection.VERSIONS to Icons.Rounded.Layers,
                LauncherSection.MODS to Icons.Rounded.Extension,
                LauncherSection.DOWNLOADS to Icons.Rounded.Download,
                LauncherSection.SETTINGS to Icons.Rounded.Settings
            )
            items.forEach { (section, icon) ->
                SidebarItem(
                    section = section,
                    icon = icon,
                    selected = selected == section,
                    expanded = expanded,
                    onClick = { onSectionSelected(section) }
                )
                Spacer(Modifier.height(5.dp))
            }

            Spacer(Modifier.weight(1f))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onExpandedChange(!expanded) },
                color = SurfaceRaised,
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 11.dp, horizontal = if (expanded) 12.dp else 0.dp),
                    horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ChevronLeft else Icons.Rounded.ChevronRight,
                        contentDescription = if (expanded) "Recolher menu" else "Expandir menu",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    if (expanded) {
                        Spacer(Modifier.width(9.dp))
                        Text("Recolher menu", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (expanded) {
                Text("VERSÃO 0.1.0 • ALPHA", color = TextSecondary.copy(alpha = 0.65f), fontSize = 9.sp, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun SidebarItem(
    section: LauncherSection,
    icon: ImageVector,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) PrimaryGreen.copy(alpha = 0.13f) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = if (expanded) 11.dp else 0.dp, vertical = 12.dp),
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = section.label,
            tint = if (selected) PrimaryGreen else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        if (expanded) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = section.label,
                color = if (selected) MaterialTheme.colorScheme.onSurface else TextSecondary,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            if (selected) {
                Surface(
                    modifier = Modifier.size(5.dp),
                    color = PrimaryGreen,
                    shape = CircleShape
                ) {}
            }
        }
    }
}
