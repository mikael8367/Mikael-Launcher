package com.mikael.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikael.launcher.ui.theme.OutlineSoft
import com.mikael.launcher.ui.theme.PrimaryGreen
import com.mikael.launcher.ui.theme.Surface as PanelSurface
import com.mikael.launcher.ui.theme.TextPrimary
import com.mikael.launcher.ui.theme.TextSecondary

@Composable
fun AccountHeader(
    accountName: String,
    accounts: List<String>,
    onOpenAccounts: () -> Unit,
    onSelectAccount: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Surface(
                modifier = Modifier.clickable { expanded = true },
                color = PanelSurface,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OutlineSoft)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MinecraftHead(accountName.ifBlank { "Steve" }, Modifier.size(30.dp))
                    Text(
                        accountName.ifBlank { "Selecionar conta" },
                        color = TextPrimary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Abrir contas", tint = TextSecondary)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (accounts.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Nenhuma conta adicionada") },
                        onClick = { expanded = false }
                    )
                } else {
                    accounts.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            leadingIcon = { MinecraftHead(name, Modifier.size(24.dp)) },
                            trailingIcon = {
                                if (name == accountName) Icon(Icons.Rounded.AccountCircle, contentDescription = "Conta selecionada", tint = PrimaryGreen)
                            },
                            onClick = {
                                onSelectAccount(name)
                                expanded = false
                            }
                        )
                    }
                }
                DropdownMenuItem(
                    text = { Text("Adicionar ou gerenciar contas") },
                    leadingIcon = { Icon(Icons.Rounded.PersonAdd, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onOpenAccounts()
                    }
                )
            }
        }
    }
}

@Composable
fun MinecraftHead(name: String, modifier: Modifier = Modifier) {
    val palette = remember(name) {
        val seed = name.hashCode()
        listOf(
            Color(0xFFB9784C), Color(0xFF7B4B32), Color(0xFFE0B18B),
            Color(0xFF4B3328), Color(0xFF2B211D)
        )
    }
    Canvas(modifier = modifier.background(Color(0xFF252A31), CircleShape)) {
        val unit = size.minDimension / 8f
        val skin = palette[0]
        val hair = palette[1]
        val eye = Color(0xFF263B55)
        drawRect(skin, Offset(unit, unit), androidx.compose.ui.geometry.Size(unit * 6, unit * 6))
        drawRect(hair, Offset(unit, unit), androidx.compose.ui.geometry.Size(unit * 6, unit * 1.5f))
        drawRect(hair, Offset(unit, unit), androidx.compose.ui.geometry.Size(unit * 1.2f, unit * 3.3f))
        drawRect(hair, Offset(unit * 5.8f, unit), androidx.compose.ui.geometry.Size(unit * 1.2f, unit * 3.3f))
        drawRect(eye, Offset(unit * 2, unit * 3.5f), androidx.compose.ui.geometry.Size(unit, unit * 0.8f))
        drawRect(eye, Offset(unit * 5, unit * 3.5f), androidx.compose.ui.geometry.Size(unit, unit * 0.8f))
        drawRect(Color(0xFF75412F), Offset(unit * 3, unit * 5), androidx.compose.ui.geometry.Size(unit * 2, unit * 0.7f))
    }
}

