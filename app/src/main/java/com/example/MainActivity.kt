package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.LdroidApp
import com.example.ui.theme.LdroidTheme
import com.example.viewmodel.LdroidViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: LdroidViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val tokens by viewModel.activeThemeTokens.collectAsStateWithLifecycle()

            LaunchedEffect(tokens.isDark) {
                val style = if (tokens.isDark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            LdroidTheme(tokens = tokens) {
                LdroidApp(viewModel = viewModel)
            }
        }
    }
}
