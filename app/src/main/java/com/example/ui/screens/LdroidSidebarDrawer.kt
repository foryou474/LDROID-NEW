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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.preferences.LdroidSettingsState
import com.example.model.DeveloperLabSection
import com.example.model.DiagnosticSeverity
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

/**
 * Left Sidebar Navigation Drawer (☰) — Locked 11-Category Architecture:
 * - Default Collapsed State: ALL 11 accordions MUST be collapsed by default (expandedCategory = null).
 * - Top Profile Header: Guest Developer (Offline Android Architect) + Edit icon.
 * - Pinned Bottom Linked Accounts Strip: Google | GitHub | Firebase + ⚙ Account Manager.
 */
@Composable
fun LdroidSidebarDrawer(
    uiState: LdroidUiState,
    settings: LdroidSettingsState,
    viewModel: LdroidViewModel,
    onCloseDrawer: () -> Unit,
    onOpenLabSection: (DeveloperLabSection) -> Unit,
    onOpenWorkspaceFile: (String) -> Unit,
    onExtractSdkZipClick: () -> Unit,
    onExportZipClick: (String) -> Unit = { viewModel.postMessage("Use Workspace Lab -> Export ZIP from Projects") }
) {
    val context = LocalContext.current
    val tokens = LocalLdroidTokens.current

    // Strict Rule: ALL 11 accordion categories start collapsed (null)
    var expandedCategory by remember { mutableStateOf<Int?>(null) }
    var showProfileEditDialog by remember { mutableStateOf(false) }
    var showAccountsManageDialog by remember { mutableStateOf(false) }

    val storageSafLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.setStoragePermissionGranted(true)
            viewModel.postMessage("Storage & Files workspace tree authorized via SAF")
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setPhotosPermissionGranted(true)
            viewModel.importFilesViaSaf(listOf(uri))
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setMicPermissionGranted(granted)
        viewModel.postMessage(
            if (granted) "Microphone permission granted for Voice Input"
            else "Microphone permission denied"
        )
    }

    val isMicGrantedNow = remember(uiState.micPermissionGranted) {
        uiState.micPermissionGranted || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    val errorCount = uiState.diagnostics.count {
        it.severity == DiagnosticSeverity.ERROR || it.severity == DiagnosticSeverity.SECRET
    }

    fun toggleCategory(cat: Int) {
        expandedCategory = if (expandedCategory == cat) null else cat
    }

    ModalDrawerSheet(
        modifier = Modifier.fillMaxWidth(0.84f).fillMaxHeight(),
        drawerContainerColor = tokens.surface1,
        drawerContentColor = tokens.textPrimary
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            // Top Profile Header
            Surface(
                color = tokens.surface2,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                border = BorderStroke(1.dp, tokens.border),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(tokens.primaryAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Guest Developer Avatar",
                            tint = tokens.primaryAccent,
                            modifier = Modifier.size(22.dp)
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

            // Scrollable 11 Ordered Accordion Categories (Grouped A, B, C)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DrawerGroupLabel("GROUP A • DAILY SETUP & EDITOR")

                LockedAccordionCategory(
                    title = "1. AI Setup",
                    icon = Icons.Filled.AutoAwesome,
                    expanded = expandedCategory == 1,
                    onClick = { toggleCategory(1) },
                    testTag = "drawer_cat_1"
                ) {
                    DrawerCategoryAiSetupContent(uiState = uiState, settings = settings, viewModel = viewModel)
                }

                LockedAccordionCategory(
                    title = "2. Code View Settings",
                    icon = Icons.Filled.Code,
                    expanded = expandedCategory == 2,
                    onClick = { toggleCategory(2) },
                    testTag = "drawer_cat_2"
                ) {
                    DrawerCategoryCodeViewContent(settings = settings, viewModel = viewModel)
                }

                LockedAccordionCategory(
                    title = "3. Personalization",
                    icon = Icons.Filled.Palette,
                    expanded = expandedCategory == 3,
                    onClick = { toggleCategory(3) },
                    testTag = "drawer_cat_3"
                ) {
                    DrawerCategoryPersonalizationContent(settings = settings, viewModel = viewModel)
                }

                DrawerGroupLabel("GROUP B • PROJECT, DIAGNOSTICS & SYNC")

                LockedAccordionCategory(
                    title = "4. Workspace Diagnostics",
                    icon = Icons.Filled.MedicalServices,
                    badgeText = if (errorCount > 0) "$errorCount" else null,
                    badgeColor = tokens.error,
                    expanded = expandedCategory == 4,
                    onClick = { toggleCategory(4) },
                    testTag = "drawer_cat_4"
                ) {
                    DrawerCategoryDiagnosticsContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onJumpToDiagnostic = { path, line ->
                            viewModel.jumpToFileLine(path, line)
                            onCloseDrawer()
                        }
                    )
                }

                LockedAccordionCategory(
                    title = "5. Quick Workspace",
                    icon = Icons.Filled.Bolt,
                    expanded = expandedCategory == 5,
                    onClick = { toggleCategory(5) },
                    testTag = "drawer_cat_5"
                ) {
                    DrawerCategoryQuickWorkspaceContent(
                        uiState = uiState,
                        settings = settings,
                        viewModel = viewModel,
                        onOpenWorkspaceFile = { path ->
                            onOpenWorkspaceFile(path)
                            onCloseDrawer()
                        }
                    )
                }

                LockedAccordionCategory(
                    title = "6. Workspace Lab",
                    icon = Icons.Filled.FolderOpen,
                    expanded = expandedCategory == 6,
                    onClick = { toggleCategory(6) },
                    testTag = "drawer_cat_6"
                ) {
                    DrawerCategoryWorkspaceLabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onCloseDrawer = onCloseDrawer,
                        onExportZipClick = onExportZipClick
                    )
                }

                LockedAccordionCategory(
                    title = "7. Library & Packages",
                    icon = Icons.Filled.LibraryBooks,
                    expanded = expandedCategory == 7,
                    onClick = { toggleCategory(7) },
                    testTag = "drawer_cat_7"
                ) {
                    DrawerCategoryLibraryPackagesContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onExtractSdkZipClick = onExtractSdkZipClick
                    )
                }

                LockedAccordionCategory(
                    title = "8. GitHub",
                    icon = Icons.Filled.CloudDone,
                    expanded = expandedCategory == 8,
                    onClick = { toggleCategory(8) },
                    testTag = "drawer_cat_8"
                ) {
                    DrawerCategoryGitHubContent(uiState = uiState, settings = settings, viewModel = viewModel)
                }

                LockedAccordionCategory(
                    title = "9. Integrations",
                    icon = Icons.Filled.Extension,
                    expanded = expandedCategory == 9,
                    onClick = { toggleCategory(9) },
                    testTag = "drawer_cat_9"
                ) {
                    DrawerCategoryIntegrationsContent(
                        uiState = uiState,
                        settings = settings,
                        viewModel = viewModel,
                        onCloseDrawer = onCloseDrawer
                    )
                }

                DrawerGroupLabel("GROUP C • BUILD ENGINE & POWER TOOLS")

                LockedAccordionCategory(
                    title = "10. Developer Lab",
                    icon = Icons.Filled.Science,
                    expanded = expandedCategory == 10,
                    onClick = { toggleCategory(10) },
                    testTag = "drawer_cat_10"
                ) {
                    DrawerCategoryDeveloperLabContent(
                        uiState = uiState,
                        settings = settings,
                        viewModel = viewModel,
                        onCloseDrawer = onCloseDrawer,
                        onOpenLabSection = onOpenLabSection
                    )
                }

                LockedAccordionCategory(
                    title = "11. Advanced Developer Settings",
                    icon = Icons.Filled.Tune,
                    expanded = expandedCategory == 11,
                    onClick = { toggleCategory(11) },
                    testTag = "drawer_cat_11"
                ) {
                    DrawerCategoryAdvancedSettingsContent(
                        uiState = uiState,
                        settings = settings,
                        viewModel = viewModel,
                        isMicGranted = isMicGrantedNow,
                        onToggleStorage = { enable ->
                            if (enable) storageSafLauncher.launch(null) else viewModel.setStoragePermissionGranted(false)
                        },
                        onTogglePhotos = { enable ->
                            if (enable) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } else viewModel.setPhotosPermissionGranted(false)
                        },
                        onToggleMic = { enable ->
                            if (enable) micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            else viewModel.setMicPermissionGranted(false)
                        }
                    )
                }

                Spacer(Modifier.height(6.dp))
            }

            // Pinned Bottom Linked Accounts Strip (ALWAYS Visible outside scroll region)
            HorizontalDivider(color = tokens.border)
            Surface(
                color = tokens.surface2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drawer_linked_accounts_strip")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LinkedAccountChip(
                        provider = "Google",
                        status = settings.googleAccountEmail.ifBlank { "Sign in" },
                        connected = settings.googleAccountEmail.isNotBlank(),
                        onClick = { showAccountsManageDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    LinkedAccountChip(
                        provider = "GitHub",
                        status = if (settings.githubUsername.isBlank()) "Link" else "@${settings.githubUsername}",
                        connected = settings.githubUsername.isNotBlank(),
                        onClick = { showAccountsManageDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    LinkedAccountChip(
                        provider = "Firebase",
                        status = if (settings.firebaseConnected) "Connected" else "Link",
                        connected = settings.firebaseConnected,
                        onClick = { showAccountsManageDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showAccountsManageDialog = true },
                        modifier = Modifier.size(48.dp).testTag("drawer_accounts_manage_btn")
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Manage Linked Accounts",
                            tint = tokens.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
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
                        label = { Text("Role / Subtitle") },
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

    if (showAccountsManageDialog) {
        var googleEmail by remember(settings.googleAccountEmail) { mutableStateOf(settings.googleAccountEmail) }
        var githubHandle by remember(settings.githubUsername) { mutableStateOf(settings.githubUsername) }
        var fbProject by remember(settings.firebaseProjectId) { mutableStateOf(settings.firebaseProjectId) }
        var fbConnected by remember(settings.firebaseConnected) { mutableStateOf(settings.firebaseConnected) }

        AlertDialog(
            onDismissRequest = { showAccountsManageDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = tokens.primaryAccent)
                    Text("Manage Linked Accounts", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = googleEmail,
                        onValueChange = { googleEmail = it },
                        label = { Text("Google Account (user@gmail.com)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = githubHandle,
                        onValueChange = { githubHandle = it },
                        label = { Text("GitHub Username", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fbProject,
                        onValueChange = { fbProject = it },
                        label = { Text("Firebase Project ID", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Firebase Connected", color = tokens.textPrimary, fontSize = 12.sp)
                        Switch(checked = fbConnected, onCheckedChange = { fbConnected = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateLinkedAccounts(googleEmail, githubHandle, fbProject, fbConnected)
                        showAccountsManageDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent)
                ) { Text("Save Accounts") }
            },
            dismissButton = {
                TextButton(onClick = { showAccountsManageDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DrawerGroupLabel(label: String) {
    val tokens = LocalLdroidTokens.current
    Text(
        text = label,
        color = tokens.textMuted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
    )
}

@Composable
private fun LockedAccordionCategory(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onClick: () -> Unit,
    testTag: String,
    badgeText: String? = null,
    badgeColor: Color = LocalLdroidTokens.current.primaryAccent,
    content: @Composable () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    Surface(
        color = tokens.surface2.copy(alpha = 0.72f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (expanded) tokens.primaryAccent.copy(alpha = 0.55f) else tokens.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
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
                    Icon(icon, contentDescription = null, tint = tokens.primaryAccent, modifier = Modifier.size(17.dp))
                    Text(
                        text = title,
                        color = tokens.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!badgeText.isNullOrBlank()) {
                        StatusPill(label = badgeText, color = badgeColor)
                    }
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
private fun LinkedAccountChip(
    provider: String,
    status: String,
    connected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    val accent = if (connected) tokens.success else tokens.textMuted
    Surface(
        color = tokens.surface1,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (connected) tokens.success.copy(alpha = 0.5f) else tokens.border),
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick)
            .testTag("linked_${provider.lowercase()}_chip")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(provider, color = tokens.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(status, color = accent, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
