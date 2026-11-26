package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.terminal.TerminalSession
import com.example.model.BottomToolTab
import com.example.model.DiagnosticSeverity
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel
import java.io.File

@Composable
fun EditorBottomToolPanel(
    uiState: LdroidUiState,
    sessions: List<TerminalSession>,
    selectedTab: BottomToolTab,
    panelHeightDp: Int,
    onSelectTab: (BottomToolTab) -> Unit,
    onResizeHeightDp: (Int) -> Unit,
    onCollapse: () -> Unit,
    viewModel: LdroidViewModel,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    val density = LocalDensity.current
    val clipboard = LocalClipboardManager.current
    val errorCount = uiState.diagnostics.count { it.severity == DiagnosticSeverity.ERROR || it.severity == DiagnosticSeverity.SECRET }
    val warnCount = uiState.diagnostics.count { it.severity == DiagnosticSeverity.WARNING }
    val activeSession = sessions.firstOrNull { it.id == uiState.activeTerminalSessionId } ?: sessions.firstOrNull()
    val isExpandedMax = panelHeightDp >= 360

    Surface(
        modifier = if (panelHeightDp > 0) modifier.fillMaxWidth().height(panelHeightDp.dp) else modifier.fillMaxSize(),
        color = tokens.terminalBackground,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, tokens.border)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            // Ultra-clean VS Code-style compact header with drag resize & action icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(panelHeightDp) {
                        detectVerticalDragGestures { _, dragAmount ->
                            val deltaDp = with(density) { dragAmount.toDp().value }.toInt()
                            onResizeHeightDp((panelHeightDp - deltaDp).coerceIn(150, 440))
                        }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomToolTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val tabLabel = when (tab) {
                            BottomToolTab.TERMINAL -> "Terminal"
                            BottomToolTab.PROBLEMS -> if (errorCount + warnCount > 0) "Problems (${errorCount + warnCount})" else "Problems"
                            BottomToolTab.OUTPUT -> "Output"
                            BottomToolTab.BUILD -> "Build"
                            BottomToolTab.LOGCAT -> "Logcat"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectTab(tab) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = tabLabel,
                                    color = if (isSelected) tokens.primaryAccent else tokens.textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                                if (isSelected) {
                                    Spacer(Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(24.dp)
                                            .height(2.dp)
                                            .background(tokens.primaryAccent, RoundedCornerShape(1.dp))
                                    )
                                }
                            }
                        }
                    }
                }

                // Right-side VS Code utility icons: Clear, Copy, Expand/Restore, Collapse
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            when (selectedTab) {
                                BottomToolTab.TERMINAL -> activeSession?.let { viewModel.clearTerminalSession(it.id) }
                                BottomToolTab.OUTPUT -> viewModel.clearIdeOutputLogs()
                                BottomToolTab.PROBLEMS -> viewModel.runFullWorkspaceStaticAudit()
                                BottomToolTab.BUILD -> viewModel.runWorkspaceBuildCheck()
                                BottomToolTab.LOGCAT -> viewModel.refreshLogcat()
                            }
                        },
                        modifier = Modifier.size(30.dp).testTag("bottom_panel_clear_btn")
                    ) {
                        Icon(
                            Icons.Filled.ClearAll,
                            contentDescription = "Clear panel",
                            tint = tokens.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            val copyText = when (selectedTab) {
                                BottomToolTab.TERMINAL -> activeSession?.lines?.joinToString("\n") { it.text }.orEmpty()
                                BottomToolTab.PROBLEMS -> uiState.diagnostics.joinToString("\n") { "[${it.severity.name}] ${it.filePath}:${it.line} ${it.message}" }
                                BottomToolTab.OUTPUT -> uiState.ideOutputLogs.joinToString("\n") { "[${it.timestamp}] [${it.source}] ${it.message}" }
                                BottomToolTab.BUILD -> uiState.buildState.logs.joinToString("\n")
                                BottomToolTab.LOGCAT -> uiState.logcatLines.joinToString("\n")
                            }
                            clipboard.setText(AnnotatedString(copyText))
                            viewModel.postMessage(
                                if (copyText.isBlank()) "Nothing to copy from ${selectedTab.label}"
                                else "Copied ${selectedTab.label} output"
                            )
                        },
                        modifier = Modifier.size(30.dp).testTag("bottom_panel_copy_btn")
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "Copy output",
                            tint = tokens.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = { onResizeHeightDp(if (isExpandedMax) 220 else 380) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpandedMax) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                            contentDescription = if (isExpandedMax) "Restore panel size" else "Maximize panel",
                            tint = tokens.textMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier.size(30.dp).testTag("bottom_panel_collapse_btn")
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Collapse panel",
                            tint = tokens.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = tokens.border.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 3.dp))

            // Tab Contents
            when (selectedTab) {
                BottomToolTab.TERMINAL -> TerminalTabContent(
                    projectName = uiState.activeProject?.name ?: "MaxStarterApp",
                    sessions = sessions,
                    activeSessionId = uiState.activeTerminalSessionId,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
                BottomToolTab.PROBLEMS -> ProblemsTabContent(
                    uiState = uiState,
                    onJumpToLine = { path, line -> viewModel.jumpToFileLine(path, line) },
                    modifier = Modifier.weight(1f)
                )
                BottomToolTab.OUTPUT -> OutputTabContent(
                    uiState = uiState,
                    modifier = Modifier.weight(1f)
                )
                BottomToolTab.BUILD -> BuildTabContent(
                    uiState = uiState,
                    onRunBuild = { viewModel.runWorkspaceBuildCheck() },
                    modifier = Modifier.weight(1f)
                )
                BottomToolTab.LOGCAT -> LogcatTabContent(
                    uiState = uiState,
                    onRefreshLogcat = { tag, lvl -> viewModel.refreshLogcat(tag, lvl) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TerminalTabContent(
    projectName: String,
    sessions: List<TerminalSession>,
    activeSessionId: String?,
    viewModel: LdroidViewModel,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    val activeSession = sessions.firstOrNull { it.id == activeSessionId } ?: sessions.firstOrNull()
    var commandInput by remember { mutableStateOf("") }
    var historyCursor by remember(activeSession?.id) { mutableIntStateOf(-1) }
    val terminalListState = rememberLazyListState()
    val lines = activeSession?.lines.orEmpty()
    val cleanFolder = remember(activeSession?.workingDir, projectName) {
        projectName.ifBlank {
            activeSession?.workingDir?.let {
                File(it).name.replace(Regex("_[a-fA-F0-9]{6,}$"), "")
            }?.ifBlank { "MaxStarterApp" } ?: "MaxStarterApp"
        }
    }
    val quickCommands = remember {
        listOf("ls", "pwd", "git status", "git diff", "gradle build", "help", "clear")
    }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            terminalListState.animateScrollToItem(lines.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Crisp monospace prompt + compact session pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "max@studio:~/$cleanFolder $",
                color = tokens.primaryAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = JetBrainsMonoFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            sessions.forEach { sess ->
                val selected = sess.id == activeSession?.id
                Surface(
                    color = if (selected) tokens.primaryAccent.copy(alpha = 0.14f) else tokens.surface1,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, if (selected) tokens.primaryAccent else tokens.border),
                    modifier = Modifier.clickable { viewModel.selectTerminalSession(sess.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(sess.title, color = if (selected) tokens.primaryAccent else tokens.textMuted, fontSize = 10.sp)
                        if (sessions.size > 1) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Close ${sess.title}",
                                tint = tokens.textMuted,
                                modifier = Modifier.size(11.dp).clickable { viewModel.closeTerminalSession(sess.id) }
                            )
                        }
                    }
                }
            }
            IconButton(onClick = { viewModel.createTerminalSession() }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "New Terminal Session", tint = tokens.primaryAccent, modifier = Modifier.size(14.dp))
            }
            activeSession?.lastExitCode?.let { code ->
                StatusPill("exit $code", if (code == 0) tokens.success else tokens.error)
            }
        }

        // Scrollable terminal output
        LazyColumn(
            state = terminalListState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 3.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(lines, key = { it.id }) { line ->
                Text(
                    text = line.text,
                    color = when {
                        line.isError -> tokens.terminalStderr
                        line.isPrompt -> tokens.terminalPrompt
                        else -> tokens.terminalStdout
                    },
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp
                )
            }
        }

        // One-tap quick command chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            quickCommands.forEach { cmd ->
                Surface(
                    color = tokens.surface1,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, tokens.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.runTerminalCommand(cmd) }
                        .testTag("terminal_chip_${cmd.replace(" ", "_")}")
                ) {
                    Text(
                        text = cmd,
                        color = tokens.textPrimary,
                        fontSize = 10.sp,
                        fontFamily = JetBrainsMonoFontFamily,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Clean `>` command input bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val history = activeSession?.commandHistory.orEmpty()
            val runAction = {
                if (commandInput.isNotBlank()) {
                    viewModel.runTerminalCommand(commandInput)
                    commandInput = ""
                    historyCursor = -1
                }
            }
            IconButton(
                onClick = {
                    if (history.isNotEmpty()) {
                        val nextIdx = if (historyCursor < 0) history.lastIndex else (historyCursor - 1).coerceAtLeast(0)
                        historyCursor = nextIdx
                        commandInput = history[nextIdx]
                    }
                },
                enabled = history.isNotEmpty(),
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Filled.ArrowUpward, contentDescription = "Previous command", tint = tokens.textMuted, modifier = Modifier.size(14.dp))
            }
            IconButton(
                onClick = {
                    if (history.isNotEmpty() && historyCursor >= 0) {
                        if (historyCursor >= history.lastIndex) {
                            historyCursor = -1
                            commandInput = ""
                        } else {
                            historyCursor++
                            commandInput = history[historyCursor]
                        }
                    }
                },
                enabled = historyCursor >= 0,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Filled.ArrowDownward, contentDescription = "Next command", tint = tokens.textMuted, modifier = Modifier.size(14.dp))
            }
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                prefix = {
                    Text(
                        text = "> ",
                        color = tokens.primaryAccent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                },
                placeholder = { Text("Enter command...", fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily) },
                textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = tokens.textPrimary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { runAction() }),
                modifier = Modifier.weight(1f).testTag("terminal_command_input")
            )
            if (activeSession?.isRunning == true) {
                IconButton(
                    onClick = { viewModel.cancelTerminalCommand() },
                    modifier = Modifier.size(44.dp).testTag("terminal_cancel_btn")
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = "Cancel process", tint = tokens.error)
                }
            } else {
                IconButton(
                    onClick = runAction,
                    modifier = Modifier.size(44.dp).testTag("terminal_run_btn")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Run command", tint = tokens.primaryAccent)
                }
            }
        }
    }
}

@Composable
private fun ProblemsTabContent(
    uiState: LdroidUiState,
    onJumpToLine: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (uiState.diagnostics.isEmpty()) {
            item {
                Text("0 problems detected in workspace.", color = tokens.success, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
            }
        } else {
            items(uiState.diagnostics) { issue ->
                val color = when (issue.severity) {
                    DiagnosticSeverity.ERROR, DiagnosticSeverity.SECRET -> tokens.error
                    DiagnosticSeverity.WARNING -> tokens.warning
                    DiagnosticSeverity.INFO -> tokens.info
                }
                Surface(
                    color = tokens.surface1,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onJumpToLine(issue.filePath, issue.line) }
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "[${issue.severity.name}] ${issue.filePath}:${issue.line}",
                                color = color,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                            Text(issue.source, color = tokens.textMuted, fontSize = 10.sp)
                        }
                        Text(issue.message, color = tokens.textPrimary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun OutputTabContent(
    uiState: LdroidUiState,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (uiState.ideOutputLogs.isEmpty()) {
            item {
                Text("No IDE tool output recorded yet.", color = tokens.textMuted, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
            }
        } else {
            items(uiState.ideOutputLogs.asReversed(), key = { it.id }) { entry ->
                Text(
                    text = "[${entry.timestamp}] [${entry.source}] ${entry.message}",
                    color = if (entry.isError) tokens.error else tokens.terminalStdout,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun BuildTabContent(
    uiState: LdroidUiState,
    onRunBuild: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    val build = uiState.buildState
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill(
                label = build.statusLabel,
                color = when (build.succeeded) {
                    true -> tokens.success
                    false -> tokens.error
                    null -> tokens.info
                }
            )
            Button(
                onClick = onRunBuild,
                enabled = !build.isRunning,
                colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (build.isRunning) "Running..." else "Run Build Check", fontSize = 11.sp)
            }
        }
        if (build.isRunning || build.progress > 0f) {
            LinearProgressIndicator(
                progress = { build.progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (build.succeeded == false) tokens.error else tokens.primaryAccent
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (build.logs.isEmpty()) {
                item {
                    Text(
                        "Tap 'Run Build Check' to run Gradle (when installed) or workspace pre-flight validation.",
                        color = tokens.textMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            } else {
                items(build.logs) { line ->
                    Text(
                        text = line,
                        color = if (line.contains("ERROR", true) || line.contains("Failed", true)) tokens.error else tokens.terminalStdout,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LogcatTabContent(
    uiState: LdroidUiState,
    onRefreshLogcat: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    var filterTag by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("V") }

    LaunchedEffect(Unit) {
        if (uiState.logcatLines.isEmpty()) {
            onRefreshLogcat(filterTag, level)
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = filterTag,
                onValueChange = { filterTag = it },
                placeholder = { Text("Tag/keyword...", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            listOf("V", "D", "W", "E").forEach { lvl ->
                FilterChip(
                    selected = level == lvl,
                    onClick = {
                        level = lvl
                        onRefreshLogcat(filterTag, lvl)
                    },
                    label = { Text(lvl, fontSize = 10.sp) }
                )
            }
            OutlinedButton(
                onClick = { onRefreshLogcat(filterTag, level) },
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh Logcat", modifier = Modifier.size(14.dp))
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (uiState.logcatLines.isEmpty()) {
                item {
                    Text(
                        "No Logcat output available in current sandbox. Tap refresh to retry process logcat.",
                        color = tokens.textMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            } else {
                items(uiState.logcatLines) { line ->
                    Text(
                        text = line,
                        color = when {
                            line.contains(" E ") || line.contains("unavailable", true) -> tokens.error
                            line.contains(" W ") -> tokens.warning
                            else -> tokens.terminalStdout
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
