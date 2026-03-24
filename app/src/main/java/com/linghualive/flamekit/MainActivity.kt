package com.linghualive.flamekit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.linghualive.flamekit.core.datastore.ReadingPreferences
import com.linghualive.flamekit.core.datastore.ReadingPrefsDataStore
import com.linghualive.flamekit.core.navigation.AppNavHost
import com.linghualive.flamekit.core.theme.FlameKitTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var readingPrefsDataStore: ReadingPrefsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs by readingPrefsDataStore.preferencesFlow
                .collectAsState(initial = ReadingPreferences())
            FlameKitTheme(
                themeMode = prefs.themeMode,
                dynamicColor = prefs.dynamicColor,
            ) {
                AppNavHost()
            }
        }
    }
}
