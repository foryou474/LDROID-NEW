package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiChatMessage
import com.example.model.AiProposedFileChange
import com.example.model.OpenEditorTab
import com.example.model.WorkspaceFileItem
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LdroidTokens
import com.example.ui.theme.LocalLdroidTokens

enum class AiPanelStage {
    HIDDEN,
    COMPACT,
    EXPANDED
}

data class ExtractedCodeSnippet(
    val language: String,
    val code: String
)

fun extractCodeSnippets(text: String): List<ExtractedCodeSnippet> {
    val snippets = mutableListOf<ExtractedCodeSnippet>()
    val regex = "```([a-zA-Z0-9_-]*)\\s*\\n([\\s\\S]*?)```".toRegex()
    regex.findAll(text).forEach { match ->
        val lang = match.groupValues[1].ifBlank { "code" }
        val body = match.groupValues[2].trimEnd()
        if (body.isNotBlank()) {
            snippets.add(ExtractedCodeSnippet(lang, body))
        }
    }
    return snippets
}

@Composable
fun CompactAiMessageBar(
    isCodeMode: Boolean,
    activeTab: OpenEditorTab?,
    attachedFilesCount: Int,
    isAiLoading: Boolean,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    onAttachFile: () -> Unit,
    onSendPrompt: (String) -> Unit,
    tokens: LdroidTokens,
    modifier: Modifier = Modifier
) {
    var promptInput by remember { mutableStateOf("") }
    val accentColor = if (isCodeMode) tokens.primaryAccent else tokens.secondaryAccent

    Surface(
        color = tokens.surface1,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.7f)),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onExpand() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header bar inside compact panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isCodeMode) "✨ M.A.X. Code Assistant" else "✨ M.A.X. Terminal Fixer",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (attachedFilesCount > 0) {
                        Surface(
                            color = tokens.surface2,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "📎 $attachedFilesCount",
                                color = tokens.info,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onExpand,
                        modifier = Modifier
                            .size(26.dp)
                            .testTag("ai_expand_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Expand Chat Drawer",
                            tint = tokens.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(26.dp)
                            .testTag("ai_compact_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close AI Panel",
                            tint = tokens.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Input row with attach file and send
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onAttachFile,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("ai_attach_file_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = "Attach File Context",
                        tint = if (attachedFilesCount > 0) tokens.primaryAccent else tokens.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = {
                        Text(
                            text = if (isCodeMode) "Ask M.A.X. to generate or refactor code..." else "Ask M.A.X. to fix terminal errors or commands...",
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("ai_compact_input")
                )

                IconButton(
                    onClick = {
                        val clean = promptInput.trim()
                        if (clean.isNotEmpty()) {
                            onSendPrompt(clean)
                            promptInput = ""
                        }
                    },
                    enabled = promptInput.isNotBlank() && !isAiLoading,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("ai_compact_send_btn")
                ) {
                    if (isAiLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = accentColor)
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Prompt",
                            tint = if (promptInput.isNotBlank()) accentColor else tokens.textMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandedAiChatDrawer(
    isCodeMode: Boolean,
    activeTab: OpenEditorTab?,
    messages: List<AiChatMessage>,
    isAiLoading: Boolean,
    attachedFiles: Set<String>,
    onDetachFile: (String) -> Unit,
    onAttachFile: () -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onSaveHistory: () -> Unit,
    onClearChat: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onApplyCodeBlock: (String) -> Unit,
    onApplyProposedChange: (String, AiProposedFileChange) -> Unit,
    tokens: LdroidTokens,
    modifier: Modifier = Modifier
) {
    var promptInput by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val accentColor = if (isCodeMode) tokens.primaryAccent else tokens.secondaryAccent

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable { onMinimize() }
    ) {
        Surface(
            color = tokens.surface1,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            border = BorderStroke(1.dp, tokens.border),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .align(Alignment.BottomCenter)
                .clickable(enabled = false) {} // prevent dismissing when tapping drawer content
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = if (isCodeMode) "M.A.X. AI Code Assistant" else "M.A.X. CLI & Error Assistant",
                                fontWeight = FontWeight.Bold,
                                color = tokens.textPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (isCodeMode) "Context: ${activeTab?.relativePath ?: "Active File"}" else "Context: Active Terminal Session",
                                color = tokens.textMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onMinimize,
                            modifier = Modifier.size(28.dp).testTag("ai_minimize_btn")
                        ) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Minimize", tint = tokens.textMuted, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = onSaveHistory,
                            modifier = Modifier.size(28.dp).testTag("ai_save_chat_btn")
                        ) {
                            Icon(Icons.Filled.History, contentDescription = "Save Chat to History", tint = tokens.textMuted, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = onClearChat,
                            modifier = Modifier.size(28.dp).testTag("ai_clear_chat_btn")
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear Chat", tint = tokens.textMuted, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(28.dp).testTag("ai_expanded_close_btn")
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = tokens.textMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = tokens.border)

                // Quick Action Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val chips = if (isCodeMode) {
                        listOf(
                            "✨ Refactor File" to "Refactor the active file for cleaner Compose architecture and best practices",
                            "🔍 Explain Code" to "Explain what the active file does step-by-step",
                            "🛠️ Fix Diagnostics" to "Fix any lint/compiler diagnostics in the active file",
                            "🧪 Add Unit Tests" to "Generate comprehensive unit tests for this file",
                            "🎨 Compose UI" to "Generate a modern Material 3 Jetpack Compose UI card"
                        )
                    } else {
                        listOf(
                            "🛠️ Fix Last Error" to "Analyze the terminal session output and suggest the exact command to fix the error",
                            "📜 Explain Logs" to "Explain the most recent terminal logs and their root causes",
                            "⚡ Build Command" to "Suggest the optimal gradle build command for this project",
                            "📊 Git Status Check" to "Analyze git changes and suggest the next commit message",
                            "🧹 Clean & Rebuild" to "Suggest commands to clean and rebuild workspace modules"
                        )
                    }

                    chips.forEach { (label, prompt) ->
                        FilterChip(
                            selected = false,
                            onClick = { onSendPrompt(prompt) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                // Attached Files Chips Strip
                if (attachedFiles.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Attached:", color = tokens.textMuted, fontSize = 10.sp)
                        attachedFiles.forEach { filePath ->
                            Surface(
                                color = tokens.surface2,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, tokens.primaryAccent.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = filePath.substringAfterLast('/'),
                                        fontSize = 10.sp,
                                        color = tokens.primaryAccent,
                                        fontFamily = JetBrainsMonoFontFamily
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove attachment",
                                        tint = tokens.textMuted,
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable { onDetachFile(filePath) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Chat Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = accentColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = if (isCodeMode) "M.A.X. Code Assistant is ready" else "M.A.X. Terminal Assistant is ready",
                                        fontWeight = FontWeight.SemiBold,
                                        color = tokens.textPrimary,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (isCodeMode) "Ask questions, refactor active code, or request patches." else "Ask for command fixes, build troubleshooting, or terminal scripts.",
                                        color = tokens.textMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    items(messages) { msg ->
                        ChatMessageItem(
                            message = msg,
                            isCodeMode = isCodeMode,
                            onApplyCode = onApplyCodeBlock,
                            onApplyChange = { change -> onApplyProposedChange(msg.id, change) },
                            tokens = tokens
                        )
                    }

                    if (isAiLoading) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = accentColor)
                                Text("M.A.X. is thinking...", color = tokens.textMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Pinned Bottom Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onAttachFile,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("ai_expanded_attach_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AttachFile,
                            contentDescription = "Attach File",
                            tint = if (attachedFiles.isNotEmpty()) tokens.primaryAccent else tokens.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = {
                            Text(
                                text = if (isCodeMode) "Ask M.A.X. to generate or refactor code..." else "Ask M.A.X. for command suggestions...",
                                fontSize = 12.sp
                            )
                        },
                        minLines = 1,
                        maxLines = 4,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_expanded_input")
                    )

                    IconButton(
                        onClick = {
                            val clean = promptInput.trim()
                            if (clean.isNotEmpty()) {
                                onSendPrompt(clean)
                                promptInput = ""
                            }
                        },
                        enabled = promptInput.isNotBlank() && !isAiLoading,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("ai_expanded_send_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = if (promptInput.isNotBlank()) accentColor else tokens.textMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: AiChatMessage,
    isCodeMode: Boolean,
    onApplyCode: (String) -> Unit,
    onApplyChange: (AiProposedFileChange) -> Unit,
    tokens: LdroidTokens
) {
    val clipboard = LocalClipboardManager.current
    val snippets = remember(message.text) { extractCodeSnippets(message.text) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (message.isUser) tokens.primaryAccent.copy(alpha = 0.16f) else tokens.surface2,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (message.isUser) tokens.primaryAccent.copy(alpha = 0.4f) else tokens.border),
            modifier = Modifier.fillMaxWidth(if (message.isUser) 0.85f else 0.95f)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Sender label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (message.isUser) "You" else "M.A.X.",
                        fontWeight = FontWeight.Bold,
                        color = if (message.isUser) tokens.primaryAccent else tokens.secondaryAccent,
                        fontSize = 11.sp
                    )
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(message.text))
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy message", tint = tokens.textMuted, modifier = Modifier.size(12.dp))
                    }
                }

                Text(
                    text = message.text,
                    color = tokens.textPrimary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                // Code Snippets with Actions
                snippets.forEach { snip ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(tokens.terminalBackground)
                            .border(1.dp, tokens.border, RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(tokens.surface1)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = snip.language.uppercase(),
                                color = tokens.primaryAccent,
                                fontSize = 10.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = { onApplyCode(snip.code) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCodeMode) Icons.Filled.Check else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = if (isCodeMode) tokens.success else tokens.secondaryAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = if (isCodeMode) "Apply to Editor" else "Run in Terminal",
                                        fontSize = 10.sp,
                                        color = if (isCodeMode) tokens.success else tokens.secondaryAccent,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                TextButton(
                                    onClick = { clipboard.setText(AnnotatedString(snip.code)) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("Copy", fontSize = 10.sp, color = tokens.textMuted)
                                }
                            }
                        }

                        Text(
                            text = snip.code,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = tokens.textPrimary,
                            lineHeight = 14.sp,
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(8.dp)
                        )
                    }
                }

                // Proposed changes
                message.proposedChanges.forEach { change ->
                    Surface(
                        color = tokens.surface1,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, tokens.warning),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Proposed Patch: ${change.relativePath.substringAfterLast('/')}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = tokens.textPrimary
                                )
                                Text(
                                    text = if (change.isApplied) "Patch Applied" else change.relativePath,
                                    color = tokens.textMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Button(
                                onClick = { onApplyChange(change) },
                                colors = ButtonDefaults.buttonColors(containerColor = tokens.warning, contentColor = Color.Black),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Apply Patch", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiAttachFilesDialog(
    workspaceFiles: List<WorkspaceFileItem>,
    currentlyAttached: Set<String>,
    onToggleFile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    val eligibleFiles = remember(workspaceFiles) {
        workspaceFiles.filter { !it.isDirectory }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.AttachFile, contentDescription = null, tint = tokens.primaryAccent)
                Text("Attach Project Files to AI", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select files from the project to include in M.A.X.'s context:",
                    color = tokens.textMuted,
                    fontSize = 11.sp
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (eligibleFiles.isEmpty()) {
                        item {
                            Text("No workspace files found.", color = tokens.textMuted, fontSize = 11.sp)
                        }
                    }

                    items(eligibleFiles) { file ->
                        val isChecked = file.relativePath in currentlyAttached
                        Surface(
                            color = if (isChecked) tokens.primaryAccent.copy(alpha = 0.12f) else tokens.surface2,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isChecked) tokens.primaryAccent else tokens.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleFile(file.relativePath) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { onToggleFile(file.relativePath) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.relativePath.substringAfterLast('/'),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = tokens.textPrimary,
                                        fontFamily = JetBrainsMonoFontFamily
                                    )
                                    Text(
                                        text = file.relativePath,
                                        color = tokens.textMuted,
                                        fontSize = 9.sp,
                                        fontFamily = JetBrainsMonoFontFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent)
            ) {
                Text("Done")
            }
        }
    )
}
