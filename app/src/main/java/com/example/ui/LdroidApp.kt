package com.example.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PrimaryDestination
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.DeveloperLabDetailScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidViewModel

@Composable
fun LdroidApp(viewModel: LdroidViewModel) {
    val context = LocalContext.current
    val tokens = LocalLdroidTokens.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val terminalSessions by viewModel.terminalSessions.collectAsStateWithLifecycle()
    val snapshots by viewModel.activeProjectSnapshots.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // Return to Home tab from secondary primary tabs on system Back
    if (uiState.currentTab != PrimaryDestination.HOME && uiState.activeLabSection == null) {
        BackHandler { viewModel.selectPrimaryTab(PrimaryDestination.HOME) }
    }

    // SAF Launchers (zero broad storage permissions required)
    val importFilesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.importFilesViaSaf(uris)
    }

    val importZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importZipProjectViaSaf(uri)
    }

    val exportZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) viewModel.exportActiveProjectZip(uri)
    }

    val pickApkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = resolveUriName(context, uri) ?: "artifact.apk"
            viewModel.analyzeApkUri(uri, name)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = tokens.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isWide && uiState.activeLabSection == null) {
                    NavigationBar(
                        containerColor = tokens.surface1,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        PrimaryDestination.entries.forEach { dest ->
                            val selected = uiState.currentTab == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectPrimaryTab(dest) },
                                icon = {
                                    Icon(
                                        imageVector = iconForTab(dest, selected),
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, fontSize = 11.sp, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = tokens.primaryAccent,
                                    selectedTextColor = tokens.primaryAccent,
                                    indicatorColor = tokens.primaryAccent.copy(alpha = 0.16f),
                                    unselectedIconColor = tokens.textMuted,
                                    unselectedTextColor = tokens.textMuted
                                ),
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWide && uiState.activeLabSection == null) {
                    NavigationRail(containerColor = tokens.surface1) {
                        PrimaryDestination.entries.forEach { dest ->
                            val selected = uiState.currentTab == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectPrimaryTab(dest) },
                                icon = {
                                    Icon(
                                        imageVector = iconForTab(dest, selected),
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, fontSize = 11.sp) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = tokens.primaryAccent,
                                    selectedTextColor = tokens.primaryAccent,
                                    indicatorColor = tokens.primaryAccent.copy(alpha = 0.16f),
                                    unselectedIconColor = tokens.textMuted,
                                    unselectedTextColor = tokens.textMuted
                                ),
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                val labSection = uiState.activeLabSection
                if (labSection != null) {
                    DeveloperLabDetailScreen(
                        section = labSection,
                        uiState = uiState,
                        viewModel = viewModel,
                        onPickApkOrAab = {
                            pickApkLauncher.launch(
                                arrayOf("application/vnd.android.package-archive", "application/zip", "application/octet-stream", "*/*")
                            )
                        },
                        onBack = { viewModel.closeDeveloperLabSection() },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    when (uiState.currentTab) {
                        PrimaryDestination.HOME -> HomeScreen(
                            uiState = uiState,
                            activeThemeName = tokens.name,
                            onNavigateTab = { viewModel.selectPrimaryTab(it) },
                            onOpenLabSection = { viewModel.openDeveloperLabSection(it) },
                            onOpenFile = { viewModel.openFileInEditor(it, jumpToEditorTab = true) },
                            onImportZipClick = { importZipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*")) },
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryDestination.PROJECTS -> ProjectsScreen(
                            uiState = uiState,
                            snapshots = snapshots,
                            viewModel = viewModel,
                            onImportFilesClick = { importFilesLauncher.launch(arrayOf("*/*")) },
                            onImportZipClick = { importZipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*")) },
                            onExportZipClick = { defaultName -> exportZipLauncher.launch(defaultName) },
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryDestination.EDITOR -> EditorScreen(
                            uiState = uiState,
                            settings = settings,
                            terminalSessions = terminalSessions,
                            viewModel = viewModel,
                            isExpandedWidth = isWide,
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryDestination.AI_STUDIO -> AiStudioScreen(
                            uiState = uiState,
                            settings = settings,
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryDestination.SETTINGS -> SettingsScreen(
                            uiState = uiState,
                            settings = settings,
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private fun iconForTab(dest: PrimaryDestination, selected: Boolean) = when (dest) {
    PrimaryDestination.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    PrimaryDestination.PROJECTS -> if (selected) Icons.Filled.Folder else Icons.Outlined.Folder
    PrimaryDestination.EDITOR -> if (selected) Icons.Filled.Code else Icons.Outlined.Code
    PrimaryDestination.AI_STUDIO -> if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
    PrimaryDestination.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
}

private fun resolveUriName(context: android.content.Context, uri: Uri): String? {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) return c.getString(idx)
        }
    }
    return uri.lastPathSegment
}
