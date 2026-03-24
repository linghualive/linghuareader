package com.linghualive.flamekit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.linghualive.flamekit.core.navigation.AppNavHost
import com.linghualive.flamekit.core.theme.FlameKitTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlameKitTheme {
                AppNavHost()
            }
        }
    }
}
