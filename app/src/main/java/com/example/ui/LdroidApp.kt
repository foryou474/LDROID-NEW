package com.example.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
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
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PrimaryDestination
import com.example.ui.screens.ChromeBrowserScreen
import com.example.ui.screens.DeveloperLabDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LdroidSidebarDrawer
import com.example.ui.screens.MaxStudioScreen
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidViewModel
import kotlinx.coroutines.launch

@Composable
fun LdroidApp(viewModel: LdroidViewModel) {
    val context = LocalContext.current
    val tokens = LocalLdroidTokens.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val terminalSessions by viewModel.terminalSessions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    if (uiState.currentTab != PrimaryDestination.HOME && uiState.activeLabSection == null) {
        BackHandler { viewModel.selectPrimaryTab(PrimaryDestination.HOME) }
    }

    val importZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importZipProjectViaSaf(uri)
    }

    val pickApkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = resolveUriName(context, uri) ?: "artifact.apk"
            viewModel.analyzeApkUri(uri, name)
        }
    }

    val extractSdkZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.extractSdkArchiveViaSaf(uri)
    }

    val isMainBottomBarVisibleTab = uiState.currentTab != PrimaryDestination.MAX_STUDIO &&
        uiState.activeLabSection == null

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        gesturesEnabled = isMainBottomBarVisibleTab,
        drawerContent = {
            LdroidSidebarDrawer(
                uiState = uiState,
                settings = settings,
                viewModel = viewModel,
                onCloseDrawer = { coroutineScope.launch { drawerState.close() } },
                onOpenLabSection = { section -> viewModel.openDeveloperLabSection(section) },
                onOpenWorkspaceFile = { relPath ->
                    viewModel.openFileInEditor(relPath, jumpToEditorTab = true)
                },
                onExtractSdkZipClick = {
                    extractSdkZipLauncher.launch(
                        arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*")
                    )
                }
            )
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 600.dp
            val density = LocalDensity.current
            val isImeVisible = WindowInsets.ime.getBottom(density) > 0

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = tokens.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    // Fixed 3-Tab Bottom Bar rendered ONLY on Home and Google (COMPLETELY HIDDEN in Max Studio & Developer Lab)
                    if (!isWide && isMainBottomBarVisibleTab && !isImeVisible) {
                        NavigationBar(
                            containerColor = tokens.surface1,
                            windowInsets = WindowInsets.navigationBars,
                            modifier = Modifier.testTag("main_bottom_bar")
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
                                    label = {
                                        Text(
                                            text = dest.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    },
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
                        .consumeWindowInsets(innerPadding)
                ) {
                    if (isWide && isMainBottomBarVisibleTab) {
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
                                activeThemeId = tokens.id,
                                reducedMotion = settings.reducedMotion,
                                onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                onNavigateTab = { viewModel.selectPrimaryTab(it) },
                                onOpenLabSection = { viewModel.openDeveloperLabSection(it) },
                                onOpenFile = { viewModel.openFileInEditor(it, jumpToEditorTab = true) },
                                onJumpToFileLine = { rel, line -> viewModel.jumpToFileLine(rel, line) },
                                onApplyTheme = { viewModel.applyTheme(it) },
                                onNewProject = { appName, pkg, tpl ->
                                    viewModel.createProject(appName, pkg, tpl, openMaxStudio = false)
                                },
                                onSwitchProject = { proj ->
                                    viewModel.switchProject(proj, openMaxStudio = false)
                                },
                                onImportZipClick = {
                                    importZipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            PrimaryDestination.MAX_STUDIO -> MaxStudioScreen(
                                uiState = uiState,
                                settings = settings,
                                terminalSessions = terminalSessions,
                                viewModel = viewModel,
                                isExpandedWidth = isWide,
                                onBackToHome = { viewModel.selectPrimaryTab(PrimaryDestination.HOME) },
                                modifier = Modifier.weight(1f)
                            )
                            PrimaryDestination.CHROME -> ChromeBrowserScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun iconForTab(dest: PrimaryDestination, selected: Boolean) = when (dest) {
    PrimaryDestination.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    PrimaryDestination.MAX_STUDIO -> if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
    PrimaryDestination.CHROME -> if (selected) Icons.Filled.Language else Icons.Outlined.Language
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
