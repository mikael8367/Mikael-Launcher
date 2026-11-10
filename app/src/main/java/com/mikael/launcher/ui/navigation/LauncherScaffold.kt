package com.mikael.launcher.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikael.launcher.ui.components.LauncherSidebar
import com.mikael.launcher.ui.screens.HomeScreen
import com.mikael.launcher.ui.screens.PlaceholderScreen
import com.mikael.launcher.ui.theme.Background

@Composable
fun LauncherScaffold(modifier: Modifier = Modifier) {
    var selected by rememberSaveable { mutableStateOf(LauncherSection.HOME.name) }
    var sidebarExpanded by rememberSaveable { mutableStateOf(true) }
    val currentSection = LauncherSection.valueOf(selected)

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LauncherSidebar(
            selected = currentSection,
            expanded = sidebarExpanded,
            onExpandedChange = { sidebarExpanded = it },
            onSectionSelected = { selected = it.name }
        )
        AnimatedContent(
            targetState = currentSection,
            modifier = Modifier.weight(1f).fillMaxSize(),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "launcher-section"
        ) { section ->
            if (section == LauncherSection.HOME) {
                HomeScreen(onNavigate = { selected = it.name })
            } else {
                PlaceholderScreen(section = section)
            }
        }
    }
}
