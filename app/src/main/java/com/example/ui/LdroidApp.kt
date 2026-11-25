package com.example.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DeveloperLabSection
import com.example.model.PrimaryDestination
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.DeveloperLabDetailScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LdroidApp(viewModel: LdroidViewModel) {
    val context = LocalContext.current
    val tokens = LocalLdroidTokens.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val terminalSessions by viewModel.terminalSessions.collectAsStateWithLifecycle()
    val snapshots by viewModel.activeProjectSnapshots.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var showNewFileDialog by remember { mutableStateOf<Boolean?>(null) } // true=dir, false=file
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var showAllDrawerFiles by remember { mutableStateOf(false) }

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = tokens.surface1,
                drawerContentColor = tokens.textPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = "LDROID IDE",
                        color = tokens.primaryAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = uiState.activeProject?.name ?: "No Workspace Active",
                        color = tokens.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = uiState.activeProject?.packageName ?: "com.ldroid.ide",
                        color = tokens.textMuted,
                        fontSize = 11.sp,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = tokens.divider)
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "PRIMARY NAVIGATION",
                        color = tokens.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))

                    PrimaryDestination.entries.forEach { dest ->
                        val selected = uiState.currentTab == dest && uiState.activeLabSection == null
                        NavigationDrawerItem(
                            label = { Text(dest.label, fontSize = 13.sp) },
                            selected = selected,
                            onClick = {
                                viewModel.selectPrimaryTab(dest)
                                coroutineScope.launch { drawerState.close() }
                            },
                            icon = { Icon(iconForTab(dest, selected), contentDescription = dest.label, modifier = Modifier.size(20.dp)) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = tokens.primaryAccent.copy(alpha = 0.16f),
                                selectedTextColor = tokens.primaryAccent,
                                selectedIconColor = tokens.primaryAccent,
                                unselectedTextColor = tokens.textPrimary,
                                unselectedIconColor = tokens.textMuted
                            ),
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = tokens.divider)
                    Spacer(Modifier.height(12.dp))

                    // Integrated NavigationDrawer File Explorer Component
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "WORKSPACE EXPLORER",
                            color = tokens.textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = { showNewFileDialog = false },
                                modifier = Modifier.size(40.dp).testTag("drawer_new_file")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "New File", tint = tokens.primaryAccent, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { showNewFileDialog = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.CreateNewFolder, contentDescription = "New Folder", tint = tokens.secondaryAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))

                    if (uiState.workspaceFiles.isEmpty()) {
                        Text("No files in workspace", color = tokens.textMuted, fontSize = 11.sp, modifier = Modifier.padding(4.dp))
                    } else {
                        val visibleFiles = if (showAllDrawerFiles) uiState.workspaceFiles else uiState.workspaceFiles.take(16)
                        visibleFiles.forEach { fileItem ->
                            val isActiveFile = uiState.activeTabPath == fileItem.relativePath
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable {
                                        if (!fileItem.isDirectory) {
                                            viewModel.openFileInEditor(fileItem.relativePath, jumpToEditorTab = true)
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                    },
                                color = if (isActiveFile) tokens.primaryAccent.copy(alpha = 0.16f) else tokens.surface2,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isActiveFile) tokens.primaryAccent.copy(alpha = 0.5f) else tokens.border)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (fileItem.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                                            contentDescription = null,
                                            tint = if (fileItem.isDirectory) tokens.warning else tokens.primaryAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.size(6.dp))
                                        Text(
                                            text = fileItem.relativePath,
                                            color = tokens.textPrimary,
                                            fontSize = 11.sp,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (!fileItem.isDirectory) {
                                        Row {
                                            IconButton(
                                                onClick = { renameTarget = fileItem.relativePath },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = "Rename", tint = tokens.textMuted, modifier = Modifier.size(15.dp))
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteSelectedFiles(listOf(fileItem.relativePath)) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = tokens.error, modifier = Modifier.size(15.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (uiState.workspaceFiles.size > 16) {
                            TextButton(
                                onClick = { showAllDrawerFiles = !showAllDrawerFiles },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (showAllDrawerFiles) "Show fewer files" else "Show all ${uiState.workspaceFiles.size} files",
                                    fontSize = 11.sp,
                                    color = tokens.primaryAccent
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = tokens.divider)
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "DEVELOPER LAB TOOLS",
                        color = tokens.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))

                    DeveloperLabSection.entries.forEach { section ->
                        val selected = uiState.activeLabSection == section
                        NavigationDrawerItem(
                            label = { Text(section.title, fontSize = 12.sp) },
                            selected = selected,
                            onClick = {
                                viewModel.openDeveloperLabSection(section)
                                coroutineScope.launch { drawerState.close() }
                            },
                            icon = { Icon(iconForLabSection(section), contentDescription = section.title, modifier = Modifier.size(18.dp)) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = tokens.secondaryAccent.copy(alpha = 0.16f),
                                selectedTextColor = tokens.secondaryAccent,
                                selectedIconColor = tokens.secondaryAccent,
                                unselectedTextColor = tokens.textPrimary,
                                unselectedIconColor = tokens.textMuted
                            ),
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = tokens.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    if (uiState.activeLabSection == null) {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = uiState.activeProject?.name ?: "LDROID Workspace",
                                        color = tokens.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = uiState.currentTab.label,
                                        color = tokens.textMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = { coroutineScope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("drawer_menu_btn")
                                ) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Open Navigation Menu", tint = tokens.primaryAccent)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = tokens.surface1)
                        )
                    }
                },
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

    // Dialogs for file operations inside drawer
    showNewFileDialog?.let { isDir ->
        DrawerPathDialog(
            title = if (isDir) "New Folder" else "New File",
            initial = if (isDir) "app/src/main/java/newdir" else "app/src/main/java/NewScreen.kt",
            onDismiss = { showNewFileDialog = null },
            onConfirm = { path ->
                viewModel.createFileOrFolder(path, isDir)
                showNewFileDialog = null
            }
        )
    }

    renameTarget?.let { oldPath ->
        DrawerPathDialog(
            title = "Rename ${oldPath.substringAfterLast('/')}",
            initial = oldPath.substringAfterLast('/'),
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                viewModel.renameFile(oldPath, newName)
                renameTarget = null
            }
        )
    }
}

@Composable
private fun DrawerPathDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(value) }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun iconForTab(dest: PrimaryDestination, selected: Boolean) = when (dest) {
    PrimaryDestination.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    PrimaryDestination.PROJECTS -> if (selected) Icons.Filled.Folder else Icons.Outlined.Folder
    PrimaryDestination.EDITOR -> if (selected) Icons.Filled.Code else Icons.Outlined.Code
    PrimaryDestination.AI_STUDIO -> if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
    PrimaryDestination.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
}

private fun iconForLabSection(section: DeveloperLabSection) = when (section) {
    DeveloperLabSection.DEVICE_LAB -> Icons.Filled.Devices
    DeveloperLabSection.LIVE_PREVIEW -> Icons.Filled.Preview
    DeveloperLabSection.DEBUGGER_LOGCAT -> Icons.Filled.BugReport
    DeveloperLabSection.PERFORMANCE_PROFILER -> Icons.Filled.Speed
    DeveloperLabSection.TESTING_LAB -> Icons.Filled.Science
    DeveloperLabSection.APK_ANALYZER -> Icons.Filled.Inventory2
    DeveloperLabSection.BUILD_DIAGNOSTICS -> Icons.Filled.Build
    DeveloperLabSection.SDK_ADB_DIAGNOSTICS -> Icons.Filled.Terminal
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
