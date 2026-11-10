package com.mikael.launcher.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikael.launcher.ui.components.LauncherSidebar
import com.mikael.launcher.ui.screens.AccountsScreen
import com.mikael.launcher.ui.screens.HomeScreen
import com.mikael.launcher.ui.screens.PlaceholderScreen
import com.mikael.launcher.ui.screens.FirstRunRuntimeDialog
import com.mikael.launcher.ui.screens.LAUNCHER_PREFS
import com.mikael.launcher.ui.screens.PREF_FIRST_RUN_DONE
import com.mikael.launcher.ui.components.AccountHeader
import com.mikael.launcher.ui.theme.Background

@Composable
fun LauncherScaffold(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val setupPrefs = remember { context.getSharedPreferences(LAUNCHER_PREFS, Context.MODE_PRIVATE) }
    var showRuntimeSetup by remember { mutableStateOf(!setupPrefs.getBoolean(PREF_FIRST_RUN_DONE, false)) }
    var selected by rememberSaveable { mutableStateOf(LauncherSection.HOME.name) }
    var sidebarExpanded by rememberSaveable { mutableStateOf(true) }
    var accountNames by rememberSaveable { mutableStateOf("") }
    var selectedAccount by rememberSaveable { mutableStateOf("") }
    val accounts = accountNames.split("\\n").filter { it.isNotBlank() }.distinct()
    val currentSection = LauncherSection.valueOf(selected)

    Row(
        modifier = modifier.fillMaxSize().background(Background).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LauncherSidebar(
            selected = currentSection,
            expanded = sidebarExpanded,
            onExpandedChange = { sidebarExpanded = it },
            onSectionSelected = { selected = it.name }
        )
        Column(modifier = Modifier.weight(1f).fillMaxSize()) {
            AccountHeader(
                accountName = selectedAccount,
                onOpenAccounts = { selected = LauncherSection.ACCOUNTS.name },
                onSelectAccount = { selectedAccount = it },
                accounts = accounts
            )
            AnimatedContent(
                targetState = currentSection,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "launcher-section"
            ) { section ->
                when (section) {
                    LauncherSection.HOME -> HomeScreen(onNavigate = { selected = it.name })
                    LauncherSection.ACCOUNTS -> AccountsScreen(
                        accounts = accounts,
                        selectedAccount = selectedAccount,
                        onAddAccount = { name ->
                            accountNames = (accounts + name).distinct().joinToString("\\n")
                            selectedAccount = name
                        },
                        onSelectAccount = { selectedAccount = it },
                        onRemoveAccount = { name ->
                            accountNames = accounts.filterNot { it == name }.joinToString("\\n")
                            if (selectedAccount == name) selectedAccount = ""
                        }
                    )
                    else -> PlaceholderScreen(section = section)
                }
            }
        }
    }
    if (showRuntimeSetup) {
        FirstRunRuntimeDialog(onFinished = { showRuntimeSetup = false })
    }
}
