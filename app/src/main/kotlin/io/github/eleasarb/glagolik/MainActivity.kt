package io.github.eleasarb.glagolik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.github.eleasarb.glagolik.ui.navigation.GlagolikNavHost
import io.github.eleasarb.glagolik.ui.theme.GlagolikTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlagolikTheme {
                GlagolikNavHost()
            }
        }
    }
}
