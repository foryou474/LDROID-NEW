package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ProjectEntity
import com.example.model.DiagnosticSeverity
import com.example.model.PrimaryDestination
import com.example.model.ProjectTemplate
import com.example.model.RecentActivityItem
import com.example.model.RecentActivityKind
import com.example.ui.components.LdroidSurfaceCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LdroidThemeId
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    uiState: LdroidUiState,
    activeThemeName: String,
    activeThemeId: LdroidThemeId = LdroidThemeId.MIDNIGHT_MINIMAL,
    reducedMotion: Boolean = false,
    onOpenDrawer: () -> Unit,
    onNavigateTab: (PrimaryDestination) -> Unit,
    onJumpToActivity: (RecentActivityItem) -> Unit,
    onApplyTheme: (LdroidThemeId) -> Unit = {},
    onNewProject: (String, String, ProjectTemplate) -> Unit,
    onSwitchProject: (ProjectEntity) -> Unit,
    onImportZipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    val probe = uiState.toolchainProbe
    val errorCount = uiState.diagnostics.count {
        it.severity == DiagnosticSeverity.ERROR || it.severity == DiagnosticSeverity.SECRET
    }
    var showCreateProjectDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
    ) {
        // 1. Top Header: Left Hamburger Menu (☰), Title "LDROID IDE", Right "● Offline Guest Mode"
        Surface(
            color = tokens.surface1,
            border = BorderStroke(1.dp, tokens.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_menu_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Open Sidebar Menu",
                            tint = tokens.primaryAccent
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "LDROID IDE",
                        color = tokens.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMonoFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(
                    label = stringResource(R.string.guest_mode_badge),
                    color = tokens.primaryAccent,
                    pulse = !reducedMotion,
                    modifier = Modifier.testTag("home_guest_mode_pill")
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_lazy_column"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 2. Hero Card: "Android Studio in Your Pocket" with Open Editor & Import ZIP
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = tokens.surface1,
                    border = BorderStroke(1.dp, tokens.border)
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        tokens.primaryAccent.copy(alpha = 0.14f),
                                        tokens.secondaryAccent.copy(alpha = 0.06f),
                                        tokens.surface1
                                    )
                                )
                            )
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = tokens.primaryAccent.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.RocketLaunch,
                                        contentDescription = null,
                                        tint = tokens.primaryAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Android Studio in Your Pocket",
                                    color = tokens.textPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Theme: $activeThemeName • Offline ARM64 Workspace",
                                    color = tokens.textMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onNavigateTab(PrimaryDestination.MAX_STUDIO) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("home_open_editor_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = tokens.primaryAccent,
                                    contentColor = tokens.onPrimaryAccent
                                )
                            ) {
                                Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    text = "Open Editor",
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = onImportZipClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("home_import_zip_btn"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, tokens.border),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = tokens.surface2,
                                    contentColor = tokens.textPrimary
                                )
                            ) {
                                Icon(
                                    Icons.Filled.FolderOpen,
                                    contentDescription = null,
                                    tint = tokens.primaryAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.size(6.dp))
                                Text("Import ZIP", color = tokens.textPrimary)
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                "Desert Luxe" to LdroidThemeId.DESERT_LUXE,
                                "Obsidian Dark" to LdroidThemeId.OBSIDIAN_DARK,
                                "Paper Light" to LdroidThemeId.PAPER_LIGHT
                            ).forEach { (label, id) ->
                                FilterChip(
                                    selected = activeThemeId == id,
                                    onClick = { onApplyTheme(id) },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Active Workspace Card
            item {
                val active = uiState.activeProject
                LdroidSurfaceCard(
                    accentBorder = true,
                    modifier = Modifier.testTag("active_workspace_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "ACTIVE WORKSPACE",
                                color = tokens.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = active?.name ?: "PulseApp",
                                color = tokens.textPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = active?.let {
                                    "${it.packageName} • ${uiState.workspaceFiles.count { f -> !f.isDirectory }} files"
                                } ?: "com.ldroid.pulseapp",
                                color = tokens.textMuted,
                                fontSize = 12.sp
                            )
                        }
                        StatusPill(
                            label = if (errorCount == 0) "0 Errors" else "$errorCount Errors",
                            color = if (errorCount == 0) tokens.success else tokens.error
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryMiniChip(
                            title = "API Level",
                            value = probe?.let { "API ${it.apiLevel}" } ?: "API 36",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryMiniChip(
                            title = "Heap",
                            value = probe?.let { "${it.javaHeapUsedMb}/${it.javaHeapMaxMb} MB" } ?: "39/256 MB",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryMiniChip(
                            title = "Storage",
                            value = probe?.let { "${it.freeStorageMb} MB free" } ?: "Free MB",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. My Projects Section
            item {
                LdroidSurfaceCard(modifier = Modifier.testTag("home_projects_card")) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "My Projects (${uiState.projects.size})",
                            color = tokens.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Isolated /LDROID_Projects/<AppName> workspaces",
                            color = tokens.textMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showCreateProjectDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tokens.primaryAccent,
                                contentColor = tokens.onPrimaryAccent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("home_new_project_btn")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = onImportZipClick,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, tokens.border),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(
                                Icons.Filled.FolderOpen,
                                contentDescription = null,
                                tint = tokens.primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Import ZIP", color = tokens.textPrimary, fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.projects.forEach { project ->
                            val isActive = project.id == uiState.activeProject?.id
                            Surface(
                                color = if (isActive) tokens.primaryAccent.copy(alpha = 0.12f) else tokens.surface2,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isActive) tokens.primaryAccent else tokens.border
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSwitchProject(project) }
                                    .testTag("project_card_${project.name}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = project.name,
                                                color = tokens.textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            if (isActive) {
                                                Icon(
                                                    Icons.Filled.CheckCircle,
                                                    contentDescription = "Active workspace",
                                                    tint = tokens.primaryAccent,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${project.packageName} • /LDROID_Projects/${project.name}",
                                            color = tokens.textMuted,
                                            fontSize = 11.sp,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Open ${project.name} in Editor",
                                        tint = tokens.primaryAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Recent Activities Timeline (replaces static diagnostics & quick files on Home)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recent_activities_section"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = tokens.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Recent Activities",
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Text(
                        text = "Tap any row to jump",
                        color = tokens.textMuted,
                        fontSize = 11.sp
                    )
                }
            }

            val timeline = uiState.syncSnapshot.recentActivities
            if (timeline.isEmpty()) {
                item {
                    LdroidSurfaceCard(onClick = { onNavigateTab(PrimaryDestination.MAX_STUDIO) }) {
                        Text(
                            text = "Ready for your first edit, AI build, or APK export",
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Open Editor Workspace to start editing code, designing UI visually, or generating apps with AI.",
                            color = tokens.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                items(timeline.take(8), key = { it.id }) { act ->
                    val kindColor = when (act.kind) {
                        RecentActivityKind.FILE_EDIT -> tokens.primaryAccent
                        RecentActivityKind.AI_APPLY -> tokens.tertiaryAccent
                        RecentActivityKind.APK_BUILD -> tokens.success
                        RecentActivityKind.PROJECT_OPEN -> tokens.secondaryAccent
                    }
                    val kindIcon = when (act.kind) {
                        RecentActivityKind.FILE_EDIT -> Icons.Filled.Edit
                        RecentActivityKind.AI_APPLY -> Icons.Filled.AutoAwesome
                        RecentActivityKind.APK_BUILD -> Icons.Filled.Build
                        RecentActivityKind.PROJECT_OPEN -> Icons.Filled.Code
                    }
                    LdroidSurfaceCard(
                        onClick = { onJumpToActivity(act) },
                        modifier = Modifier.testTag("activity_row_${act.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = kindColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = kindIcon,
                                        contentDescription = act.kind.badgeLabel,
                                        tint = kindColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = act.title,
                                        color = tokens.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Surface(
                                        color = kindColor.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = act.kind.badgeLabel,
                                            color = kindColor,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${act.timeLabel} • ${act.subtitle}",
                                    color = tokens.textMuted,
                                    fontSize = 11.sp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Jump to ${act.targetTab.label}",
                                tint = tokens.primaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateProjectDialog) {
        HomeNewProjectDialog(
            onDismiss = { showCreateProjectDialog = false },
            onCreate = { appName, pkg, tpl ->
                onNewProject(appName, pkg, tpl)
                showCreateProjectDialog = false
            }
        )
    }
}

@Composable
private fun TelemetryMiniChip(title: String, value: String, modifier: Modifier = Modifier) {
    val tokens = LocalLdroidTokens.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = tokens.surface2,
        border = BorderStroke(1.dp, tokens.border)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(title, color = tokens.textMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text(
                value,
                color = tokens.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = JetBrainsMonoFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeNewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, ProjectTemplate) -> Unit
) {
    var name by remember { mutableStateOf("NovaApp") }
    var pkg by remember { mutableStateOf("com.ldroid.novaapp") }
    var template by remember { mutableStateOf(ProjectTemplate.COMPOSE_M3_APP) }
    val tokens = LocalLdroidTokens.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Workspace") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Creates isolated /LDROID_Projects/<AppName>/app/src/main/, res/, build.gradle.kts, and build/.",
                    color = tokens.textMuted,
                    fontSize = 11.sp
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        val sanitized = it.lowercase().filter { ch -> ch.isLetterOrDigit() }.ifBlank { "app" }
                        pkg = "com.ldroid.$sanitized"
                    },
                    label = { Text("App Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_project_name_input")
                )
                OutlinedTextField(
                    value = pkg,
                    onValueChange = { pkg = it },
                    label = { Text("Package Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ProjectTemplate.entries.take(4).forEach { tpl ->
                        FilterChip(
                            selected = template == tpl,
                            onClick = { template = tpl },
                            label = { Text(tpl.badge, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name.trim(), pkg.trim(), template) },
                enabled = name.isNotBlank() && pkg.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.primaryAccent,
                    contentColor = tokens.onPrimaryAccent
                ),
                modifier = Modifier.testTag("confirm_new_project_btn")
            ) {
                Text("Create & Open")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
