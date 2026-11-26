package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.DeveloperLabSection
import com.example.model.DiagnosticSeverity
import com.example.model.PrimaryDestination
import com.example.ui.components.LdroidSurfaceCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import com.example.ui.theme.LdroidThemeId

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    uiState: LdroidUiState,
    activeThemeName: String,
    activeThemeId: LdroidThemeId = LdroidThemeId.CYBERPUNK_NEON,
    reducedMotion: Boolean = false,
    onNavigateTab: (PrimaryDestination) -> Unit,
    onOpenLabSection: (DeveloperLabSection) -> Unit,
    onOpenFile: (String) -> Unit,
    onJumpToFileLine: (String, Int) -> Unit = { _, _ -> },
    onApplyTheme: (LdroidThemeId) -> Unit = {},
    onImportZipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    val probe = uiState.toolchainProbe
    val errorCount = uiState.diagnostics.count {
        it.severity == DiagnosticSeverity.ERROR || it.severity == DiagnosticSeverity.SECRET
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
            .testTag("home_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Banner
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = tokens.surface1,
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(listOf(tokens.primaryAccent, tokens.secondaryAccent))
                )
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    tokens.primaryAccent.copy(alpha = 0.12f),
                                    tokens.secondaryAccent.copy(alpha = 0.08f),
                                    tokens.surface1
                                )
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "M.A.X. STUDIO",
                                color = tokens.primaryAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                            Text(
                                text = "Modular AI eXecutor",
                                color = tokens.textPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        StatusPill(
                            label = stringResource(R.string.guest_mode_badge),
                            color = tokens.primaryAccent,
                            pulse = !reducedMotion
                        )
                    }

                    Text(
                        text = "Theme: $activeThemeName • Workspace: ${uiState.activeProject?.name ?: "MaxStarterApp"}",
                        color = tokens.textMuted,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigateTab(PrimaryDestination.EDITOR) },
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
                                if (uiState.activeTabPath != null) "Continue Coding" else "Open Editor",
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
                            border = BorderStroke(1.dp, tokens.border)
                        ) {
                            Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = tokens.primaryAccent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Import ZIP", color = tokens.textPrimary)
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "Cyberpunk" to LdroidThemeId.CYBERPUNK_NEON,
                            "Emerald" to LdroidThemeId.EMERALD_MATRIX,
                            "Purple" to LdroidThemeId.ROYAL_PURPLE,
                            "Light" to LdroidThemeId.ARCTIC_LIGHT
                        ).forEach { (label, id) ->
                            FilterChip(
                                selected = activeThemeId == id,
                                onClick = { onApplyTheme(id) },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Active Workspace & Quick Telemetry
        item {
            val active = uiState.activeProject
            LdroidSurfaceCard(accentBorder = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ACTIVE WORKSPACE", color = tokens.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = active?.name ?: "No workspace selected",
                            color = tokens.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = active?.let { "${it.packageName} • ${uiState.workspaceFiles.count { f -> !f.isDirectory }} files" }
                                ?: "Create or import a project",
                            color = tokens.textMuted,
                            fontSize = 12.sp
                        )
                    }
                    StatusPill(
                        label = if (errorCount == 0) "0 Errors" else "$errorCount Issue(s)",
                        color = if (errorCount == 0) tokens.success else tokens.error
                    )
                }

                if (uiState.activeTabPath != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Editing: ${uiState.activeTabPath}",
                        color = tokens.primaryAccent,
                        fontSize = 11.sp,
                        fontFamily = JetBrainsMonoFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TelemetryMiniChip(
                        title = "API",
                        value = probe?.let { "${it.apiLevel} (${it.androidVersion})" } ?: "...",
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMiniChip(
                        title = "Heap",
                        value = probe?.let { "${it.javaHeapUsedMb}/${it.javaHeapMaxMb} MB" } ?: "...",
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMiniChip(
                        title = "Storage",
                        value = probe?.let { "${it.freeStorageMb} MB free" } ?: "...",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Active Diagnostics Jump Cards
        if (uiState.diagnostics.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Active Diagnostics (${uiState.diagnostics.size})",
                    subtitle = "Tap any finding to jump directly to the file and line"
                )
            }
            items(uiState.diagnostics.take(4)) { issue ->
                val sevColor = when (issue.severity) {
                    DiagnosticSeverity.ERROR, DiagnosticSeverity.SECRET -> tokens.error
                    DiagnosticSeverity.WARNING -> tokens.warning
                    DiagnosticSeverity.INFO -> tokens.info
                }
                LdroidSurfaceCard(
                    onClick = { onJumpToFileLine(issue.filePath, issue.line) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = issue.severity.name, tint = sevColor, modifier = Modifier.size(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${issue.filePath}:${issue.line}",
                                color = tokens.primaryAccent,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = issue.message,
                                color = tokens.textPrimary,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Jump to line", tint = tokens.textMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Quick Launch Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    icon = Icons.Filled.Add,
                    label = "New Project",
                    onClick = { onNavigateTab(PrimaryDestination.PROJECTS) },
                    modifier = Modifier.weight(1f).fillMaxHeight().testTag("quick_new_project")
                )
                QuickActionTile(
                    icon = Icons.Filled.AutoAwesome,
                    label = "M.A.X. AI",
                    onClick = { onNavigateTab(PrimaryDestination.AI_STUDIO) },
                    modifier = Modifier.weight(1f).fillMaxHeight().testTag("quick_ai_studio")
                )
                QuickActionTile(
                    icon = Icons.Filled.Preview,
                    label = "Live Preview",
                    onClick = { onOpenLabSection(DeveloperLabSection.LIVE_PREVIEW) },
                    modifier = Modifier.weight(1f).fillMaxHeight().testTag("quick_live_preview")
                )
            }
        }

        // Developer Lab (8 integrated sections inside Home tab, never a 6th bottom tab)
        item {
            SectionHeader(
                title = "Developer Lab",
                subtitle = "Integrated Android tools, device profiling, APK/AAB analyzer & diagnostics"
            )
        }

        items(DeveloperLabSection.entries.chunked(2)) { rowSections ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowSections.forEach { section ->
                    DeveloperLabCard(
                        section = section,
                        icon = iconForLabSection(section),
                        onClick = { onOpenLabSection(section) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                if (rowSections.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Quick Workspace Files
        if (uiState.workspaceFiles.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Workspace Files",
                    subtitle = "Tap any file to jump directly into the Editor"
                )
            }
            items(uiState.workspaceFiles.filter { !it.isDirectory }.take(6)) { item ->
                LdroidSurfaceCard(
                    onClick = { onOpenFile(item.relativePath) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                color = tokens.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = item.relativePath,
                                color = tokens.textMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open ${item.name}",
                            tint = tokens.primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
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

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    LdroidSurfaceCard(onClick = onClick, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tokens.primaryAccent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = tokens.primaryAccent, modifier = Modifier.size(20.dp))
            }
            Text(
                label,
                color = tokens.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DeveloperLabCard(
    section: DeveloperLabSection,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    LdroidSurfaceCard(
        onClick = onClick,
        modifier = modifier
            .testTag("lab_${section.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = section.title, tint = tokens.primaryAccent, modifier = Modifier.size(20.dp))
            Text(
                text = section.categoryTag,
                color = tokens.secondaryAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = JetBrainsMonoFontFamily
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = section.title,
            color = tokens.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = section.subtitle,
            color = tokens.textMuted,
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun iconForLabSection(section: DeveloperLabSection): ImageVector = when (section) {
    DeveloperLabSection.DEVICE_LAB -> Icons.Filled.Devices
    DeveloperLabSection.LIVE_PREVIEW -> Icons.Filled.Preview
    DeveloperLabSection.DEBUGGER_LOGCAT -> Icons.Filled.BugReport
    DeveloperLabSection.PERFORMANCE_PROFILER -> Icons.Filled.Speed
    DeveloperLabSection.TESTING_LAB -> Icons.Filled.Science
    DeveloperLabSection.APK_ANALYZER -> Icons.Filled.Inventory2
    DeveloperLabSection.BUILD_DIAGNOSTICS -> Icons.Filled.Build
    DeveloperLabSection.SDK_ADB_DIAGNOSTICS -> Icons.Filled.Terminal
}
