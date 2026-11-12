package com.mikael.launcher.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.content.Context
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikael.launcher.ui.components.LauncherSidebar
import com.mikael.launcher.ui.components.AccountHeader
import com.mikael.launcher.ui.screens.AccountsScreen
import com.mikael.launcher.ui.screens.HomeScreen
import com.mikael.launcher.ui.screens.PlaceholderScreen
import com.mikael.launcher.ui.screens.FirstRunRuntimeDialog
import com.mikael.launcher.ui.screens.LAUNCHER_PREFS
import com.mikael.launcher.ui.screens.PREF_FIRST_RUN_DONE
import com.mikael.launcher.ui.theme.Background
import com.mikael.launcher.ui.theme.PrimaryGreen
import com.mikael.launcher.ui.theme.Surface as PanelSurface
import com.mikael.launcher.ui.theme.TextSecondary

@Composable
fun LauncherScaffold(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val setupPrefs = remember { context.getSharedPreferences(LAUNCHER_PREFS, Context.MODE_PRIVATE) }
    var showRuntimeSetup by remember { mutableStateOf(!setupPrefs.getBoolean(PREF_FIRST_RUN_DONE, false)) }
    var selected by rememberSaveable { mutableStateOf(LauncherSection.HOME.name) }
    var sidebarExpanded by rememberSaveable { mutableStateOf(true) }
    val accountPrefs = remember { context.getSharedPreferences("mikael_launcher_accounts", Context.MODE_PRIVATE) }
    var accountNames by rememberSaveable {
        mutableStateOf(accountPrefs.getString("account_names", "").orEmpty())
    }
    var selectedAccount by rememberSaveable {
        mutableStateOf(accountPrefs.getString("selected_account", "").orEmpty())
    }
    val accounts = parseAccountNames(accountNames)
    val currentSection = LauncherSection.values().firstOrNull { it.name == selected }
        ?: LauncherSection.HOME

    val onAddAccount: (String) -> Unit = { name ->
        accountNames = (accounts + name).distinct().joinToString("\n")
        selectedAccount = name
        accountPrefs.edit()
            .putString("account_names", accountNames)
            .putString("selected_account", selectedAccount)
            .apply()
    }
    val onSelectAccount: (String) -> Unit = { name ->
        selectedAccount = name
        accountPrefs.edit().putString("selected_account", name).apply()
    }
    val onRemoveAccount: (String) -> Unit = { name ->
        accountNames = accounts.filterNot { it == name }.joinToString("\n")
        if (selectedAccount == name) selectedAccount = ""
        accountPrefs.edit()
            .putString("account_names", accountNames)
            .putString("selected_account", selectedAccount)
            .apply()
    }

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(Background).padding(14.dp)
    ) {
        val compact = maxWidth < 600.dp
        if (compact) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LauncherPageContent(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    section = currentSection,
                    selectedAccount = selectedAccount,
                    accounts = accounts,
                    onOpenAccounts = { selected = LauncherSection.ACCOUNTS.name },
                    onSelectAccount = onSelectAccount,
                    onAddAccount = onAddAccount,
                    onRemoveAccount = onRemoveAccount,
                    onNavigate = { selected = it.name }
                )
                LauncherBottomNavigation(
                    selected = currentSection,
                    onSectionSelected = { selected = it.name }
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LauncherSidebar(
                    selected = currentSection,
                    expanded = sidebarExpanded,
                    onExpandedChange = { sidebarExpanded = it },
                    onSectionSelected = { selected = it.name }
                )
                LauncherPageContent(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    section = currentSection,
                    selectedAccount = selectedAccount,
                    accounts = accounts,
                    onOpenAccounts = { selected = LauncherSection.ACCOUNTS.name },
                    onSelectAccount = onSelectAccount,
                    onAddAccount = onAddAccount,
                    onRemoveAccount = onRemoveAccount,
                    onNavigate = { selected = it.name }
                )
            }
        }
    }

    if (showRuntimeSetup) {
        FirstRunRuntimeDialog(onFinished = { showRuntimeSetup = false })
    }
}

@Composable
private fun LauncherPageContent(
    modifier: Modifier,
    section: LauncherSection,
    selectedAccount: String,
    accounts: List<String>,
    onOpenAccounts: () -> Unit,
    onSelectAccount: (String) -> Unit,
    onAddAccount: (String) -> Unit,
    onRemoveAccount: (String) -> Unit,
    onNavigate: (LauncherSection) -> Unit
) {
    Column(modifier = modifier) {
        AccountHeader(
            accountName = selectedAccount,
            onOpenAccounts = onOpenAccounts,
            onSelectAccount = onSelectAccount,
            accounts = accounts
        )
        AnimatedContent(
            targetState = section,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "launcher-section"
        ) { target ->
            when (target) {
                LauncherSection.HOME, LauncherSection.PLAY -> HomeScreen(
                    onNavigate = onNavigate,
                    selectedAccount = selectedAccount
                )
                LauncherSection.ACCOUNTS -> AccountsScreen(
                    accounts = accounts,
                    selectedAccount = selectedAccount,
                    onAddAccount = onAddAccount,
                    onSelectAccount = onSelectAccount,
                    onRemoveAccount = onRemoveAccount
                )
                else -> PlaceholderScreen(section = target)
            }
        }
    }
}

@Composable
private fun LauncherBottomNavigation(
    selected: LauncherSection,
    onSectionSelected: (LauncherSection) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PanelSurface,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LauncherSection.values().forEach { section ->
                val active = selected == section
                val itemColor by animateColorAsState(
                    targetValue = if (active) PrimaryGreen.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent,
                    label = "bottom-nav-color"
                )
                Surface(
                    onClick = { onSectionSelected(section) },
                    modifier = Modifier.animateContentSize(),
                    color = itemColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = section.label,
                        color = if (active) PrimaryGreen else TextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Accepts both real line breaks and the literal "\\n" used by older builds.
 */
internal fun parseAccountNames(raw: String): List<String> =
    raw.split("\n", "\\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
