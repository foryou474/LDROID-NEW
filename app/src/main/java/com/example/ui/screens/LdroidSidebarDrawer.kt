package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.data.preferences.LdroidSettingsState
import com.example.model.DeveloperLabSection
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LdroidThemeId
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LdroidSidebarDrawer(
    uiState: LdroidUiState,
    settings: LdroidSettingsState,
    viewModel: LdroidViewModel,
    onCloseDrawer: () -> Unit,
    onOpenLabSection: (DeveloperLabSection) -> Unit,
    onOpenWorkspaceFile: (String) -> Unit,
    onExtractSdkZipClick: () -> Unit
) {
    val context = LocalContext.current
    val tokens = LocalLdroidTokens.current

    var expandSettings by remember { mutableStateOf(true) }
    var expandSettingsInterface by remember { mutableStateOf(true) }
    var expandSettingsPermissions by remember { mutableStateOf(false) }
    var expandDeveloperLab by remember { mutableStateOf(false) }
    var expandWorkspaceLab by remember { mutableStateOf(false) }
    var expandLibraryPackages by remember { mutableStateOf(false) }

    var showProfileEditDialog by remember { mutableStateOf(false) }
    var showFullSettingsSheet by remember { mutableStateOf(false) }

    // On-demand SAF Tree launcher (zero broad storage permissions)
    val storageSafLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.setStoragePermissionGranted(true)
            viewModel.postMessage("Storage & Files workspace tree authorized via SAF")
        }
    }

    // Zero-permission Android Photo Picker (Google Play policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setPhotosPermissionGranted(true)
            viewModel.importFilesViaSaf(listOf(uri))
        }
    }

    // Runtime Microphone permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setMicPermissionGranted(granted)
        viewModel.postMessage(
            if (granted) "Microphone permission granted for Voice-to-Code"
            else "Microphone permission denied"
        )
    }

    val isMicGrantedNow = remember(uiState.micPermissionGranted) {
        uiState.micPermissionGranted || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.82f).fillMaxHeight(),
        drawerContainerColor = tokens.surface1,
        drawerContentColor = tokens.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Header Card: User Avatar, Developer Name ("Guest Developer"), "Offline Guest Mode", Profile Edit
            Surface(
                color = tokens.surface2,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, tokens.primaryAccent.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(tokens.primaryAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Developer Avatar",
                            tint = tokens.primaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = settings.developerName,
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = settings.developerRole,
                            color = tokens.textMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        StatusPill(label = "Offline Guest Mode", color = tokens.primaryAccent)
                    }
                    IconButton(
                        onClick = { showProfileEditDialog = true },
                        modifier = Modifier.size(48.dp).testTag("drawer_profile_edit_btn")
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit Developer Profile",
                            tint = tokens.primaryAccent
                        )
                    }
                }
            }

            // 1. Settings (Nested Expandable Sub-sections: 1.1 & 1.2)
            AccordionSectionHeader(
                title = "1. Settings",
                icon = Icons.Filled.Settings,
                expanded = expandSettings,
                onClick = { expandSettings = !expandSettings },
                testTag = "drawer_section_settings"
            ) {
                // 1.1 App & IDE Interface
                NestedAccordionHeader(
                    title = "1.1 App & IDE Interface",
                    expanded = expandSettingsInterface,
                    onClick = { expandSettingsInterface = !expandSettingsInterface }
                ) {
                    Text("Theme Switcher", color = tokens.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Desert Luxe" to LdroidThemeId.DESERT_LUXE,
                            "Obsidian Dark" to LdroidThemeId.OBSIDIAN_GLASS,
                            "Paper Light" to LdroidThemeId.ARCTIC_LIGHT
                        ).forEach { (label, id) ->
                            FilterChip(
                                selected = settings.themeId == id,
                                onClick = { viewModel.applyTheme(id) },
                                label = { Text(label, fontSize = 10.sp, maxLines = 1) },
                                modifier = Modifier.weight(1f).testTag("drawer_theme_${id.name.lowercase()}")
                            )
                        }
                    }

                    // Editor Font & Tab Settings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Editor Font: ${settings.editorFontSizeSp}sp", color = tokens.textPrimary, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.updateEditorSettings(
                                        fontSize = (settings.editorFontSizeSp - 1).coerceAtLeast(10),
                                        tabWidth = settings.tabWidth,
                                        autoSave = settings.autoSaveEnabled,
                                        lineNumbers = settings.showLineNumbers,
                                        wordWrap = settings.wordWrap
                                    )
                                },
                                modifier = Modifier.height(34.dp)
                            ) { Text("-") }
                            OutlinedButton(
                                onClick = {
                                    viewModel.updateEditorSettings(
                                        fontSize = (settings.editorFontSizeSp + 1).coerceAtMost(24),
                                        tabWidth = settings.tabWidth,
                                        autoSave = settings.autoSaveEnabled,
                                        lineNumbers = settings.showLineNumbers,
                                        wordWrap = settings.wordWrap
                                    )
                                },
                                modifier = Modifier.height(34.dp)
                            ) { Text("+") }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tab Indent: ${settings.tabWidth} spaces", color = tokens.textPrimary, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(2, 4).forEach { tabW ->
                                FilterChip(
                                    selected = settings.tabWidth == tabW,
                                    onClick = {
                                        viewModel.updateEditorSettings(
                                            fontSize = settings.editorFontSizeSp,
                                            tabWidth = tabW,
                                            autoSave = settings.autoSaveEnabled,
                                            lineNumbers = settings.showLineNumbers,
                                            wordWrap = settings.wordWrap
                                        )
                                    },
                                    label = { Text("${tabW}sp", fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    // SDK Config (API 36) & About
                    Surface(
                        color = tokens.surface1,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, tokens.border)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("SDK Config: Android API 36 (Baklava) • ARM64", color = tokens.primaryAccent, fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily)
                            Text("About: LDROID IDE v${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})", color = tokens.textMuted, fontSize = 10.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { showFullSettingsSheet = true },
                        modifier = Modifier.fillMaxWidth().height(40.dp).testTag("drawer_all_settings_btn")
                    ) {
                        Text("All 10 Themes, BYOK AI & M50 Audit", fontSize = 11.sp)
                    }
                }

                // 1.2 Device Permissions
                NestedAccordionHeader(
                    title = "1.2 Device Permissions",
                    expanded = expandSettingsPermissions,
                    onClick = { expandSettingsPermissions = !expandSettingsPermissions }
                ) {
                    PermissionToggleRow(
                        title = "Storage & Files (SAF Tree)",
                        subtitle = "Scoped folder access without broad storage permissions",
                        checked = uiState.storagePermissionGranted,
                        testTag = "perm_toggle_storage",
                        onToggle = { enable ->
                            if (enable) storageSafLauncher.launch(null)
                            else viewModel.setStoragePermissionGranted(false)
                        }
                    )
                    PermissionToggleRow(
                        title = "Photos & Media (Photo Picker)",
                        subtitle = "Zero-permission system media picker",
                        checked = uiState.photosPermissionGranted,
                        testTag = "perm_toggle_photos",
                        onToggle = { enable ->
                            if (enable) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } else {
                                viewModel.setPhotosPermissionGranted(false)
                            }
                        }
                    )
                    PermissionToggleRow(
                        title = "Microphone (Voice Input)",
                        subtitle = "Voice-to-Code, Voice-to-UI & Browser Voice Search",
                        checked = isMicGrantedNow,
                        testTag = "perm_toggle_mic",
                        onToggle = { enable ->
                            if (enable) micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            else viewModel.setMicPermissionGranted(false)
                        }
                    )
                }
            }

            // 2. Developer Lab (8 Diagnostic Tools)
            AccordionSectionHeader(
                title = "2. Developer Lab",
                icon = Icons.Filled.Science,
                expanded = expandDeveloperLab,
                onClick = { expandDeveloperLab = !expandDeveloperLab },
                testTag = "drawer_section_dev_lab"
            ) {
                DeveloperLabSection.entries.forEach { section ->
                    Surface(
                        color = tokens.surface1,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, tokens.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenLabSection(section)
                                onCloseDrawer()
                            }
                            .testTag("drawer_lab_${section.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(iconForDrawerLab(section), contentDescription = section.title, tint = tokens.primaryAccent, modifier = Modifier.size(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(section.title, color = tokens.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(section.subtitle, color = tokens.textMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // 3. Workspace Lab (Active project file tree quick-jump)
            AccordionSectionHeader(
                title = "3. Workspace Lab",
                icon = Icons.Filled.FolderOpen,
                expanded = expandWorkspaceLab,
                onClick = { expandWorkspaceLab = !expandWorkspaceLab },
                testTag = "drawer_section_workspace_lab"
            ) {
                val quickNames = listOf(
                    "MainActivity.kt",
                    "NewScreen.kt",
                    "VisualGeneratedScreen.kt",
                    "AndroidManifest.xml",
                    "build.gradle.kts",
                    "android_ci.yml"
                )
                val workspaceFiles = uiState.workspaceFiles.filter { !it.isDirectory }
                val prioritized = quickNames.mapNotNull { q ->
                    workspaceFiles.firstOrNull { it.name == q }
                } + workspaceFiles.filter { f -> f.name !in quickNames }

                if (prioritized.isEmpty()) {
                    Text("No files indexed in active workspace.", color = tokens.textMuted, fontSize = 11.sp)
                } else {
                    prioritized.take(14).forEach { file ->
                        Surface(
                            color = tokens.surface1,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, tokens.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpenWorkspaceFile(file.relativePath)
                                    onCloseDrawer()
                                }
                                .testTag("drawer_file_${file.name}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Description, contentDescription = null, tint = tokens.primaryAccent, modifier = Modifier.size(15.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.name, color = tokens.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = JetBrainsMonoFontFamily)
                                    Text(file.relativePath, color = tokens.textMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Library & Packages
            AccordionSectionHeader(
                title = "4. Library & Packages",
                icon = Icons.Filled.LibraryBooks,
                expanded = expandLibraryPackages,
                onClick = { expandLibraryPackages = !expandLibraryPackages },
                testTag = "drawer_section_library"
            ) {
                LibraryPackagesPanel(
                    uiState = uiState,
                    viewModel = viewModel,
                    onExtractSdkZipClick = onExtractSdkZipClick
                )
            }
        }
    }

    if (showProfileEditDialog) {
        var nameInput by remember(settings.developerName) { mutableStateOf(settings.developerName) }
        var roleInput by remember(settings.developerRole) { mutableStateOf(settings.developerRole) }
        AlertDialog(
            onDismissRequest = { showProfileEditDialog = false },
            title = { Text("Edit Developer Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Developer Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = roleInput,
                        onValueChange = { roleInput = it },
                        label = { Text("Role / Bio") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateDeveloperProfile(nameInput, roleInput)
                        showProfileEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showProfileEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showFullSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFullSettingsSheet = false },
            containerColor = tokens.background
        ) {
            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
                SettingsScreen(
                    uiState = uiState,
                    settings = settings,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun AccordionSectionHeader(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onClick: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    Surface(
        color = tokens.surface2.copy(alpha = 0.65f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, tokens.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(vertical = 4.dp)
                    .testTag(testTag),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(icon, contentDescription = null, tint = tokens.primaryAccent, modifier = Modifier.size(18.dp))
                    Text(title, color = tokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = tokens.textMuted
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = tokens.divider)
                    content()
                }
            }
        }
    }
}

@Composable
private fun NestedAccordionHeader(
    title: String,
    expanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = tokens.primaryAccent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                tint = tokens.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun PermissionToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onToggle: (Boolean) -> Unit
) {
    val tokens = LocalLdroidTokens.current
    Surface(
        color = tokens.surface1,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, tokens.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(title, color = tokens.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = tokens.textMuted, fontSize = 10.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

private fun iconForDrawerLab(section: DeveloperLabSection): ImageVector = when (section) {
    DeveloperLabSection.DEVICE_LAB -> Icons.Filled.Devices
    DeveloperLabSection.LIVE_PREVIEW -> Icons.Filled.Preview
    DeveloperLabSection.DEBUGGER_LOGCAT -> Icons.Filled.BugReport
    DeveloperLabSection.PERFORMANCE_PROFILER -> Icons.Filled.Speed
    DeveloperLabSection.TESTING_LAB -> Icons.Filled.Science
    DeveloperLabSection.APK_ANALYZER -> Icons.Filled.Inventory2
    DeveloperLabSection.BUILD_DIAGNOSTICS -> Icons.Filled.Build
    DeveloperLabSection.SDK_ADB_DIAGNOSTICS -> Icons.Filled.Terminal
}
