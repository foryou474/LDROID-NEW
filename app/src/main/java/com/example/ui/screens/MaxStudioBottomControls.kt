package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MaxStudioMode
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens

@Composable
fun MaxStudioBottomControls(
    activeMode: MaxStudioMode,
    inputText: String,
    canUndoEnhance: Boolean,
    onSelectMode: (MaxStudioMode) -> Unit,
    onInputChange: (String) -> Unit,
    onNewFileClick: () -> Unit,
    onAttachSupportFilesClick: () -> Unit,
    onAiEnhanceClick: () -> Unit,
    onUndoEnhanceClick: () -> Unit,
    onMicClick: () -> Unit,
    onRunClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    var showPlusMenu by remember { mutableStateOf(false) }
    var showExpandedModal by remember { mutableStateOf(false) }

    Surface(
        color = tokens.surface1,
        border = BorderStroke(1.dp, tokens.border),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: 3-Mode Bottom Switch Bar (<> Code E/G | ✨ AI UI Screen [Default] | >_ Terminal)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ModeSwitchPill(
                    label = MaxStudioMode.CODE_EG.label,
                    icon = Icons.Filled.Code,
                    selected = activeMode == MaxStudioMode.CODE_EG,
                    onClick = { onSelectMode(MaxStudioMode.CODE_EG) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("max_mode_code_eg")
                )
                ModeSwitchPill(
                    label = MaxStudioMode.AI_UI_SCREEN.label,
                    icon = Icons.Filled.AutoAwesome,
                    selected = activeMode == MaxStudioMode.AI_UI_SCREEN,
                    onClick = { onSelectMode(MaxStudioMode.AI_UI_SCREEN) },
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("max_mode_ai_ui")
                )
                ModeSwitchPill(
                    label = MaxStudioMode.TERMINAL.label,
                    icon = Icons.Filled.Terminal,
                    selected = activeMode == MaxStudioMode.TERMINAL,
                    onClick = { onSelectMode(MaxStudioMode.TERMINAL) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("max_mode_terminal")
                )
            }

            // Optional Undo bar after ✨ AI Prompt Enhancement
            if (canUndoEnhance) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI enhanced prompt for ${activeMode.label}",
                        color = tokens.primaryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Surface(
                        color = tokens.primaryAccent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, tokens.primaryAccent.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clickable(onClick = onUndoEnhanceClick)
                            .testTag("universal_ai_undo_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo AI prompt enhancement",
                                tint = tokens.primaryAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text("Undo", color = tokens.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            HorizontalDivider(color = tokens.divider.copy(alpha = 0.7f))

            // Row 2: Universal Expandable Action & Search Bar (+ | Input + ⤢/⤡ | ✨ AI | 🎙️ Mic | ▶️ Run)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Left: "+" popup button
                Box {
                    IconButton(
                        onClick = { showPlusMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("universal_plus_btn")
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "New File or Attach Support Files",
                            tint = tokens.primaryAccent
                        )
                    }
                    DropdownMenu(
                        expanded = showPlusMenu,
                        onDismissRequest = { showPlusMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("New File") },
                            leadingIcon = { Icon(Icons.Filled.NoteAdd, contentDescription = null) },
                            onClick = {
                                showPlusMenu = false
                                onNewFileClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Attach Support Files") },
                            leadingIcon = { Icon(Icons.Filled.AttachFile, contentDescription = null) },
                            onClick = {
                                showPlusMenu = false
                                onAttachSupportFilesClick()
                            }
                        )
                    }
                }

                // Center: Auto-growing Input with Expand/Collapse (⤢ / ⤡) button
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    placeholder = {
                        Text(
                            text = when (activeMode) {
                                MaxStudioMode.CODE_EG -> "Generate or refactor code..."
                                MaxStudioMode.AI_UI_SCREEN -> "Describe UI screen or component..."
                                MaxStudioMode.TERMINAL -> "Run bash, gradle, pkg, build-apk..."
                            },
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    minLines = 1,
                    maxLines = 4,
                    trailingIcon = {
                        IconButton(
                            onClick = { showExpandedModal = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("universal_expand_btn")
                        ) {
                            Icon(
                                imageVector = if (showExpandedModal) Icons.Filled.CloseFullscreen else Icons.Filled.OpenInFull,
                                contentDescription = "Expand input modal",
                                tint = tokens.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("universal_input")
                )

                // Right Action Trio: ✨ AI | 🎙️ Mic | ▶️ Run
                Surface(
                    color = tokens.primaryAccent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, tokens.primaryAccent.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .height(48.dp)
                        .clickable(onClick = onAiEnhanceClick)
                        .testTag("universal_ai_enhance_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = "Enhance Prompt with AI",
                            tint = tokens.primaryAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "AI",
                            color = tokens.primaryAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("universal_mic_btn")
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = "Voice Input",
                        tint = tokens.secondaryAccent
                    )
                }

                Button(
                    onClick = onRunClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.primaryAccent,
                        contentColor = tokens.onPrimaryAccent
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("universal_run_btn")
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Run Action",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // 50%-height Expandable Modal for viewing/editing long prompts or code
    if (showExpandedModal) {
        AlertDialog(
            onDismissRequest = { showExpandedModal = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${activeMode.label} Input", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showExpandedModal = false }) {
                        Icon(Icons.Filled.CloseFullscreen, contentDescription = "Collapse modal")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        placeholder = { Text("Type or paste multi-line prompt, code, or shell commands...") },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            color = tokens.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("universal_expanded_modal_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = onAiEnhanceClick) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("✨ AI Enhance")
                        }
                        if (canUndoEnhance) {
                            TextButton(onClick = onUndoEnhanceClick) {
                                Text("Undo")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExpandedModal = false
                        onRunClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.primaryAccent,
                        contentColor = tokens.onPrimaryAccent
                    )
                ) {
                    Text("▶️ Run")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpandedModal = false }) { Text("Done") }
            }
        )
    }
}

@Composable
private fun ModeSwitchPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    Surface(
        color = if (selected) tokens.primaryAccent else tokens.surface2,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) tokens.primaryAccent else tokens.border),
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) tokens.onPrimaryAccent else tokens.textMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                color = if (selected) tokens.onPrimaryAccent else tokens.textPrimary,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
