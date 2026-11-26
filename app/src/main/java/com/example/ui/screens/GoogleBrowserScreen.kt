package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

/**
 * Forwarding alias for GoogleBrowserScreen to ChromeBrowserScreen.
 */
@Deprecated("Use ChromeBrowserScreen instead", ReplaceWith("ChromeBrowserScreen(uiState, viewModel, modifier)"))
@Composable
fun GoogleBrowserScreenDeprecated(
    uiState: LdroidUiState,
    viewModel: LdroidViewModel,
    modifier: Modifier = Modifier
) {
    ChromeBrowserScreen(uiState, viewModel, modifier)
}
