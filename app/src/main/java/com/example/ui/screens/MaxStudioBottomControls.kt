package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.ViewQuilt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.EditorWorkspaceTab
import com.example.model.TabIconPreset
import com.example.ui.theme.LocalLdroidTokens

/**
 * Universal ChatGPT-Style Bottom Dock + Single-Row 7-Icon Bottom Navigation Bar:
 * - Elevated rounded input bar with `+` (attach UI screenshot, sketch, @file, New File),
 *   "Message AI..." input, microphone icon, and solid sky-blue circular `↑` Send button.
 * - Dynamic Horizontal Scrollable AI Model Chips (`✦ Gemini 2.5`, `⚡ Flash`, `🧠 GPT-4o`, `🤖 Claude`, `+`).
 * - Single-row 7-icon Bottom Bar (`<>`, `{/}`, `✨` w/ red Sentinel badge, `>_`, `▶`, `⊞`, `📦`).
 */
@Composable
fun MaxStudioBottomControls(
    activeMode: EditorWorkspaceTab,
    inputText: String,
    activeModelChip: String,
    modelChips: List<String>,
    attachedAssets: List<String>,
    sentinelErrorCount: Int,
    tabIconPreset: TabIconPreset,
    onSelectMode: (EditorWorkspaceTab) -> Unit,
    onInputChange: (String) -> Unit,
    onSelectModelChip: (String) -> Unit,
    onOpenModelSetup: () -> Unit,
    onAttachAsset: (String) -> Unit,
    onRemoveAttachedAsset: (String) -> Unit,
    onNewFileClick: () -> Unit,
    onAttachSupportFilesClick: () -> Unit,
    onMicClick: () -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    var showPlusMenu by remember { mutableStateOf(false) }

    Surface(
        color = tokens.surface1,
        border = BorderStroke(1.dp, tokens.border),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (attachedAssets.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    attachedAssets.forEach { asset ->
                        Surface(
                            color = tokens.primaryAccent.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, tokens.primaryAccent.copy(alpha = 0.45f)),
                            modifier = Modifier.clickable { onRemoveAttachedAsset(asset) }
                        ) {
                            Text(
                                text = "$asset ×",
                                color = tokens.primaryAccent,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Elevated Rounded ChatGPT-Style Input Bar
            Surface(
                color = tokens.surface2,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, tokens.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box {
                        IconButton(
                            onClick = { showPlusMenu = true },
                            modifier = Modifier.size(48.dp).testTag("universal_plus_btn")
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Attach UI screenshot, sketch or file",
                                tint = tokens.primaryAccent
                            )
                        }
                        DropdownMenu(
                            expanded = showPlusMenu,
                            onDismissRequest = { showPlusMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Attach UI Screenshot") },
                                leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null) },
                                onClick = {
                                    showPlusMenu = false
                                    onAttachSupportFilesClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Attach Wireframe Sketch") },
                                leadingIcon = { Icon(Icons.Filled.Draw, contentDescription = null) },
                                onClick = {
                                    showPlusMenu = false
                                    onAttachAsset("Wireframe Sketch")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reference @MainActivity.kt") },
                                leadingIcon = { Icon(Icons.Filled.AttachFile, contentDescription = null) },
                                onClick = {
                                    showPlusMenu = false
                                    onAttachAsset("@MainActivity.kt")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New File") },
                                leadingIcon = { Icon(Icons.Filled.NoteAdd, contentDescription = null) },
                                onClick = {
                                    showPlusMenu = false
                                    onNewFileClick()
                                }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = if (activeMode == EditorWorkspaceTab.TERMINAL) {
                                    "Run bash, gradle, aapt2, kotlinc..."
                                } else {
                                    stringResource(R.string.ai_dock_hint)
                                },
                                color = tokens.textMuted,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = onInputChange,
                            singleLine = false,
                            maxLines = 4,
                            textStyle = TextStyle(color = tokens.textPrimary, fontSize = 13.sp),
                            cursorBrush = SolidColor(tokens.primaryAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("universal_input")
                        )
                    }

                    IconButton(
                        onClick = onMicClick,
                        modifier = Modifier.size(48.dp).testTag("universal_mic_btn")
                    ) {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = "Voice Input",
                            tint = tokens.textMuted
                        )
                    }

                    // Solid sky-blue circular ↑ Send button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(tokens.primaryAccent)
                            .clickable(onClick = onSendClick)
                            .testTag("universal_run_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = "Send Prompt",
                            tint = tokens.onPrimaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Dynamic Horizontal Scrollable AI Model Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("ai_model_chips_bar"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                modelChips.forEach { chip ->
                    val selected = activeModelChip == chip
                    val chipGlyph = when {
                        chip.contains("Gemini", true) -> "✦"
                        chip.contains("Flash", true) -> "⚡"
                        chip.contains("GPT", true) -> "🧠"
                        chip.contains("Claude", true) -> "🤖"
                        else -> "◈"
                    }
                    Surface(
                        color = if (selected) tokens.primaryAccent.copy(alpha = 0.2f) else tokens.surface2,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (selected) tokens.primaryAccent else tokens.border),
                        modifier = Modifier
                            .clickable { onSelectModelChip(chip) }
                            .testTag("model_chip_${chip.lowercase().replace(" ", "_")}")
                    ) {
                        Text(
                            text = "$chipGlyph $chip",
                            color = if (selected) tokens.primaryAccent else tokens.textMuted,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                Surface(
                    color = tokens.surface2,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, tokens.border),
                    modifier = Modifier
                        .clickable(onClick = onOpenModelSetup)
                        .testTag("model_chip_add_btn")
                ) {
                    Text(
                        text = "+",
                        color = tokens.primaryAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = tokens.divider.copy(alpha = 0.65f))

            // Single-Row 7-Icon Bottom Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_7tab_bottom_bar"),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditorWorkspaceTab.entries.forEach { tab ->
                    val selected = activeMode == tab
                    val showErrorDot = tab == EditorWorkspaceTab.AI_STUDIO && sentinelErrorCount > 0
                    val testId = when (tab) {
                        EditorWorkspaceTab.CODE_EG -> "max_mode_code_eg"
                        EditorWorkspaceTab.CODE_LIVE_PREVIEW -> "max_mode_code_live"
                        EditorWorkspaceTab.AI_STUDIO -> "max_mode_ai_ui"
                        EditorWorkspaceTab.TERMINAL -> "max_mode_terminal"
                        EditorWorkspaceTab.LIVE_PREVIEW -> "max_mode_live_preview"
                        EditorWorkspaceTab.VISUAL_BUILDER -> "max_mode_visual_builder"
                        EditorWorkspaceTab.BUILD_APK -> "max_mode_build_apk"
                    }
                    Surface(
                        color = if (selected) tokens.primaryAccent.copy(alpha = 0.18f) else tokens.surface2.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (selected) tokens.primaryAccent else tokens.border),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { onSelectMode(tab) }
                            .testTag(testId)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (tabIconPreset == TabIconPreset.MINIMAL) {
                                Text(
                                    text = tab.badgeGlyph,
                                    color = if (selected) tokens.primaryAccent else tokens.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = iconForWorkspaceTab(tab, tabIconPreset == TabIconPreset.OUTLINED),
                                    contentDescription = tab.label,
                                    tint = if (selected) tokens.primaryAccent else tokens.textMuted,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            if (showErrorDot) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 5.dp, end = 6.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(tokens.error)
                                        .testTag("sentinel_red_badge")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun iconForWorkspaceTab(tab: EditorWorkspaceTab, outlined: Boolean): ImageVector {
    return if (outlined) {
        when (tab) {
            EditorWorkspaceTab.CODE_EG -> Icons.Outlined.Code
            EditorWorkspaceTab.CODE_LIVE_PREVIEW -> Icons.Outlined.DataObject
            EditorWorkspaceTab.AI_STUDIO -> Icons.Outlined.AutoAwesome
            EditorWorkspaceTab.TERMINAL -> Icons.Outlined.Terminal
            EditorWorkspaceTab.LIVE_PREVIEW -> Icons.Outlined.PlayArrow
            EditorWorkspaceTab.VISUAL_BUILDER -> Icons.Outlined.ViewQuilt
            EditorWorkspaceTab.BUILD_APK -> Icons.Outlined.Build
        }
    } else {
        when (tab) {
            EditorWorkspaceTab.CODE_EG -> Icons.Filled.Code
            EditorWorkspaceTab.CODE_LIVE_PREVIEW -> Icons.Filled.DataObject
            EditorWorkspaceTab.AI_STUDIO -> Icons.Filled.AutoAwesome
            EditorWorkspaceTab.TERMINAL -> Icons.Filled.Terminal
            EditorWorkspaceTab.LIVE_PREVIEW -> Icons.Filled.PlayArrow
            EditorWorkspaceTab.VISUAL_BUILDER -> Icons.Filled.ViewQuilt
            EditorWorkspaceTab.BUILD_APK -> Icons.Filled.Build
        }
    }
}
