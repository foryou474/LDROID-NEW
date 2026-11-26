package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.LdroidSettingsState
import com.example.data.security.SecureCredentialVault
import com.example.model.DiagnosticSeverity
import com.example.model.FilePreviewKind
import com.example.model.PrimaryDestination
import com.example.ui.components.DiffLinesBlock
import com.example.ui.components.LdroidSurfaceCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.highlightSyntax
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel
import kotlinx.coroutines.launch

private sealed interface AiSegment {
    data class Prose(val text: String) : AiSegment
    data class CodeBlock(val language: String, val code: String) : AiSegment
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AiStudioScreen(
    uiState: LdroidUiState,
    settings: LdroidSettingsState,
    viewModel: LdroidViewModel,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    val clipboard = LocalClipboardManager.current
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    val chatListState = rememberLazyListState()
    val inputBringIntoView = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val configuredKey = remember(settings.aiProvider, uiState.statusMessage) {
        viewModel.vault.readSecret(viewModel.vault.slotForAiProvider(settings.aiProvider))
    }
    var promptText by remember { mutableStateOf("") }
    var isInputFocused by remember { mutableStateOf(false) }
    var attachActiveFile by remember(uiState.activeTabPath != null) { mutableStateOf(uiState.activeTabPath != null) }
    var attachedExtraPaths by remember(uiState.activeProject?.id) { mutableStateOf(emptyList<String>()) }
    var showMultiFilePicker by remember { mutableStateOf(false) }
    var showArchGraph by remember { mutableStateOf(false) }

    val issueCount = uiState.diagnostics.count {
        it.severity == DiagnosticSeverity.ERROR || it.severity == DiagnosticSeverity.WARNING || it.severity == DiagnosticSeverity.SECRET
    }

    LaunchedEffect(uiState.aiMessages.size, isImeVisible, isInputFocused) {
        if (uiState.aiMessages.isNotEmpty()) {
            chatListState.animateScrollToItem(uiState.aiMessages.lastIndex)
        }
        if (isImeVisible || isInputFocused) {
            inputBringIntoView.bringIntoView()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
            .padding(horizontal = 16.dp, vertical = if (isImeVisible) 8.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(if (isImeVisible) 8.dp else 12.dp)
    ) {
        SectionHeader(
            title = "M.A.X. Core AI",
            subtitle = "Modular AI eXecutor • Code, Audit & Patch Engine",
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.aiMessages.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.saveChatHistoryToWorkspace() },
                            modifier = Modifier.testTag("ai_save_history_btn")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = "Save Chat to History", tint = tokens.primaryAccent)
                        }
                        IconButton(
                            onClick = { viewModel.clearAiConversation() },
                            modifier = Modifier.testTag("ai_clear_chat_btn")
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear AI Conversation", tint = tokens.textMuted)
                        }
                    }
                    FilterChip(
                        selected = showArchGraph,
                        onClick = { showArchGraph = !showArchGraph },
                        label = { Text("Arch", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Filled.AccountTree, contentDescription = null, modifier = Modifier.size(15.dp)) }
                    )
                }
            }
        )

        // Active Model Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill(
                label = "${settings.aiProvider.shortName}: ${settings.aiModel}",
                color = tokens.primaryAccent,
                pulse = !settings.reducedMotion,
                modifier = Modifier.testTag("ai_active_model_badge")
            )
            StatusPill(
                label = if (configuredKey != null) "M.A.X. Online" else "Key Required",
                color = if (configuredKey != null) tokens.success else tokens.warning
            )
        }

        // BYOK Key Status Banner (auto-compacts while virtual keyboard is open)
        if (!isImeVisible) {
            LdroidSurfaceCard(accentBorder = configuredKey == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (configuredKey != null) {
                                "Encrypted Key Ready (${SecureCredentialVault.maskCredential(configuredKey)})"
                            } else {
                                "No BYOK API Key Configured"
                            },
                            color = if (configuredKey != null) tokens.success else tokens.warning,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = buildString {
                                append(if (settings.redactSecretsInAi) "Secret redaction active" else "Secret redaction disabled")
                                append(" • ")
                                append(if (settings.confirmAiChanges) "Manual patch review" else "Auto-apply patches")
                            },
                            color = tokens.textMuted,
                            fontSize = 11.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (issueCount > 0) {
                            OutlinedButton(
                                onClick = { viewModel.fixProblemsWithAi() },
                                modifier = Modifier.testTag("ai_fix_problems_btn"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(15.dp), tint = tokens.warning)
                                Spacer(Modifier.size(4.dp))
                                Text("Fix ($issueCount)", fontSize = 11.sp, color = tokens.warning)
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.selectPrimaryTab(PrimaryDestination.SETTINGS) },
                            modifier = Modifier.testTag("ai_configure_key_btn"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("Keys", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (showArchGraph) {
            LdroidSurfaceCard {
                Text(
                    "Project Architecture Graph (${uiState.architectureNodes.size} nodes)",
                    color = tokens.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                if (uiState.architectureNodes.isEmpty()) {
                    Text("No Kotlin/Gradle nodes detected in workspace.", color = tokens.textMuted, fontSize = 12.sp)
                } else {
                    val nodes = uiState.architectureNodes.take(6)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val centerX = size.width / 2f
                        val topY = 28f
                        drawCircle(color = tokens.primaryAccent, radius = 16f, center = Offset(centerX, topY))
                        val step = size.width / (nodes.size + 1)
                        nodes.forEachIndexed { idx, _ ->
                            val nx = step * (idx + 1)
                            val ny = size.height - 32f
                            drawLine(
                                color = tokens.border,
                                start = Offset(centerX, topY + 16f),
                                end = Offset(nx, ny - 14f),
                                strokeWidth = 3f
                            )
                            drawCircle(color = tokens.secondaryAccent, radius = 12f, center = Offset(nx, ny))
                        }
                    }
                    nodes.forEach { n ->
                        Text(
                            "• [${n.kind}] ${n.name}",
                            color = tokens.textMuted,
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    }
                }
            }
        }

        // Chat & Multi-File Diff Thread
        LazyColumn(
            state = chatListState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (uiState.aiMessages.isEmpty()) {
                item {
                    LdroidSurfaceCard {
                        Text("Ask AI to explain code, audit Gradle/Manifest, or propose reviewable multi-file patches.", color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { promptText = "Explain the architecture and suggest improvements for the active file." }) {
                                Text("Explain Active File", fontSize = 11.sp)
                            }
                            OutlinedButton(onClick = { promptText = "Review this code for security, Compose state bugs, and accessibility issues." }) {
                                Text("Security & A11y Audit", fontSize = 11.sp)
                            }
                            if (issueCount > 0) {
                                OutlinedButton(onClick = { viewModel.fixProblemsWithAi() }) {
                                    Text("Fix $issueCount Problems", fontSize = 11.sp, color = tokens.warning)
                                }
                            }
                        }
                    }
                }
            }

            items(uiState.aiMessages, key = { it.id }) { msg ->
                Surface(
                    color = if (msg.isUser) tokens.elevatedSurface else tokens.surface1,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        when {
                            msg.isError -> tokens.error
                            msg.isUser -> tokens.primaryAccent.copy(alpha = 0.5f)
                            else -> tokens.border
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (msg.isUser) "YOU" else "M.A.X. CORE",
                            color = if (msg.isError) tokens.error else if (msg.isUser) tokens.primaryAccent else tokens.secondaryAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                        if (msg.isUser || msg.isError) {
                            Text(
                                text = msg.text,
                                color = if (msg.isError) tokens.error else tokens.textPrimary,
                                fontSize = 13.sp
                            )
                        } else {
                            val segments = remember(msg.id, msg.text) { parseAiMarkdownSegments(msg.text) }
                            segments.forEach { seg ->
                                when (seg) {
                                    is AiSegment.Prose -> if (seg.text.isNotBlank()) {
                                        Text(
                                            text = seg.text,
                                            color = tokens.textPrimary,
                                            fontSize = 13.sp
                                        )
                                    }
                                    is AiSegment.CodeBlock -> {
                                        val langExt = seg.language.lowercase().ifBlank { "kt" }
                                        val highlighted = remember(seg.code, langExt, tokens.id) {
                                            highlightSyntax(seg.code, langExt, tokens)
                                        }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(tokens.terminalBackground)
                                                .border(1.dp, tokens.border, RoundedCornerShape(10.dp))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(tokens.surface2)
                                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = langExt.uppercase(),
                                                    color = tokens.primaryAccent,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = JetBrainsMonoFontFamily
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    TextButton(
                                                        onClick = {
                                                            viewModel.applyCodeBlockToActiveEditor(seg.code)
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                        modifier = Modifier.testTag("ai_apply_code_block_btn")
                                                    ) {
                                                        Icon(Icons.Filled.Check, contentDescription = "Apply to Editor", modifier = Modifier.size(13.dp), tint = tokens.success)
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Apply to Editor", fontSize = 10.sp, color = tokens.success, fontWeight = FontWeight.SemiBold)
                                                    }
                                                    TextButton(
                                                        onClick = {
                                                            clipboard.setText(AnnotatedString(seg.code))
                                                            viewModel.postMessage("Copied code snippet to clipboard")
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                                    ) {
                                                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Code", modifier = Modifier.size(13.dp), tint = tokens.textMuted)
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Copy", fontSize = 10.sp, color = tokens.textMuted)
                                                    }
                                                }
                                            }
                                            Text(
                                                text = highlighted,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 11.sp,
                                                lineHeight = 16.sp,
                                                modifier = Modifier
                                                    .horizontalScroll(rememberScrollState())
                                                    .padding(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Reviewable Multi-File Patch Approvals (M10)
                        msg.proposedChanges.forEach { change ->
                            Surface(
                                color = tokens.surface2,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, tokens.border)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = change.relativePath,
                                            color = tokens.primaryAccent,
                                            fontSize = 12.sp,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        )
                                        if (change.isApplied) {
                                            StatusPill("Applied", tokens.success)
                                        }
                                    }
                                    val diffLines = viewModel.fileManager.computeLineDiff(
                                        change.originalContent ?: "",
                                        change.proposedContent
                                    )
                                    DiffLinesBlock(diffLines.take(30))
                                    if (diffLines.size > 30) {
                                        Text(
                                            text = "Showing first 30 of ${diffLines.size} diff lines",
                                            color = tokens.textMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                    if (!change.isApplied) {
                                        Button(
                                            onClick = { viewModel.applyAiProposedChange(msg.id, change) },
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = tokens.success, contentColor = tokens.onPrimaryAccent)
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.size(6.dp))
                                            Text("Apply to Editor")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isAiLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = tokens.primaryAccent)
        }

        // Prompt Input Area + Multi-File Context Chips (pinned above keyboard)
        Column(
            modifier = Modifier.bringIntoViewRequester(inputBringIntoView),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val hasActiveFile = uiState.activeTabPath != null
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Checkbox(
                        checked = attachActiveFile && hasActiveFile,
                        onCheckedChange = { attachActiveFile = it },
                        enabled = hasActiveFile
                    )
                    Text(
                        text = if (hasActiveFile) "Attach ${uiState.activeTabPath?.substringAfterLast('/')}" else "No active file",
                        color = tokens.textMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(
                    onClick = { showMultiFilePicker = true },
                    modifier = Modifier.testTag("ai_attach_files_btn")
                ) {
                    Icon(Icons.Filled.AttachFile, contentDescription = "Attach Workspace Files", modifier = Modifier.size(15.dp), tint = tokens.primaryAccent)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (attachedExtraPaths.isEmpty()) "Add Files" else "+${attachedExtraPaths.size} File(s)",
                        fontSize = 11.sp,
                        color = tokens.primaryAccent
                    )
                }
            }

            if (attachedExtraPaths.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    attachedExtraPaths.forEach { relPath ->
                        InputChip(
                            selected = true,
                            onClick = { attachedExtraPaths = attachedExtraPaths - relPath },
                            label = { Text(relPath.substringAfterLast('/'), fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily) },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove $relPath", modifier = Modifier.size(13.dp)) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    placeholder = { Text("Command M.A.X. to write, audit, or patch code...", fontSize = 12.sp) },
                    maxLines = 3,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            isInputFocused = focusState.isFocused
                            if (focusState.isFocused) {
                                coroutineScope.launch {
                                    if (uiState.aiMessages.isNotEmpty()) {
                                        chatListState.animateScrollToItem(uiState.aiMessages.lastIndex)
                                    }
                                    inputBringIntoView.bringIntoView()
                                }
                            }
                        }
                        .testTag("ai_prompt_input")
                )
                IconButton(
                    onClick = {
                        if (promptText.isNotBlank()) {
                            viewModel.sendAiPrompt(
                                prompt = promptText,
                                attachActiveFile = attachActiveFile && hasActiveFile,
                                attachedExtraPaths = attachedExtraPaths
                            )
                            promptText = ""
                        }
                    },
                    enabled = !uiState.isAiLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .background(tokens.primaryAccent, RoundedCornerShape(12.dp))
                        .testTag("ai_send_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send prompt", tint = tokens.onPrimaryAccent)
                }
            }
        }
    }

    if (showMultiFilePicker) {
        val candidateFiles = remember(uiState.workspaceFiles) {
            uiState.workspaceFiles.filter { !it.isDirectory && it.extension !in setOf("png", "jpg", "jpeg", "webp", "zip", "jar", "apk", "pdf") }
        }
        AlertDialog(
            onDismissRequest = { showMultiFilePicker = false },
            title = { Text("Attach Extra Workspace Files (max 5)") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(candidateFiles, key = { it.relativePath }) { item ->
                        val checked = item.relativePath in attachedExtraPaths
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { wantAdd ->
                                    attachedExtraPaths = if (wantAdd) {
                                        (attachedExtraPaths + item.relativePath).distinct().take(5)
                                    } else {
                                        attachedExtraPaths - item.relativePath
                                    }
                                }
                            )
                            Text(
                                text = item.relativePath,
                                color = tokens.textPrimary,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showMultiFilePicker = false }) { Text("Done (${attachedExtraPaths.size})") }
            }
        )
    }
}

private fun parseAiMarkdownSegments(text: String): List<AiSegment> {
    val regex = Regex("""```([A-Za-z0-9_-]*)\n([\s\S]*?)```""")
    val segments = mutableListOf<AiSegment>()
    var lastIdx = 0
    regex.findAll(text).forEach { match ->
        if (match.range.first > lastIdx) {
            segments.add(AiSegment.Prose(text.substring(lastIdx, match.range.first).trim()))
        }
        val lang = match.groupValues[1].trim()
        val code = match.groupValues[2].trimEnd()
        segments.add(AiSegment.CodeBlock(lang, code))
        lastIdx = match.range.last + 1
    }
    if (lastIdx < text.length) {
        segments.add(AiSegment.Prose(text.substring(lastIdx).trim()))
    }
    return if (segments.isEmpty()) listOf(AiSegment.Prose(text)) else segments
}

