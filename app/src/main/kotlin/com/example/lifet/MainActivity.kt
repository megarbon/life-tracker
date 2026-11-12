package com.example.lifet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.navigation.LifeTrackerNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LifeTrackerTheme {
                LifeTrackerNavHost()
            }
        }
    }
}
