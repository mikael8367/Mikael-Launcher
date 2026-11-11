package com.mikael.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.mikael.launcher.fcl.FclEngineBootstrap
import com.mikael.launcher.ui.navigation.LauncherScaffold
import com.mikael.launcher.ui.theme.Background
import com.mikael.launcher.ui.theme.MikaelLauncherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FclEngineBootstrap.initializeInBackground(applicationContext)
        setContent {
            MikaelLauncherTheme {
                LauncherScaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Background)
                )
            }
        }
    }
}
