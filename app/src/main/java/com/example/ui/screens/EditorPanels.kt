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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import coil.compose.AsyncImage
import com.example.data.terminal.TerminalSession
import com.example.data.workspace.ZipEntryInfo
import com.example.model.DiagnosticIssue
import com.example.model.DiagnosticSeverity
import com.example.model.FilePreviewKind
import com.example.model.OpenEditorTab
import com.example.model.VisualComponentType
import com.example.model.WorkspaceFileItem
import com.example.ui.components.LdroidSurfaceCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatBytes
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel
import java.io.File

@Composable
fun FileExplorerPane(
    uiState: LdroidUiState,
    onSearchChange: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onToggleSelect: (String) -> Unit,
    onNewFileClick: () -> Unit,
    onNewFolderClick: () -> Unit,
    onRenameClick: (String) -> Unit,
    onCopyClick: (String) -> Unit,
    onDeleteSelectedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    Surface(
        modifier = modifier,
        color = tokens.surface1,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, tokens.border)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("EXPLORER", color = tokens.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = onNewFileClick, modifier = Modifier.size(36.dp).testTag("explorer_new_file")) {
                        Icon(Icons.Filled.Add, contentDescription = "New File", tint = tokens.textPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onNewFolderClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = "New Folder", tint = tokens.textPrimary, modifier = Modifier.size(18.dp))
                    }
                    if (uiState.selectedFilePaths.isNotEmpty()) {
                        IconButton(onClick = onDeleteSelectedClick, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete selected", tint = tokens.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            OutlinedTextField(
                value = uiState.fileSearchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Filter files...", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.workspaceFiles, key = { it.relativePath }) { item ->
                    val isSelected = item.relativePath in uiState.selectedFilePaths
                    val isActive = uiState.activeTabPath == item.relativePath
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isActive) tokens.primaryAccent.copy(alpha = 0.14f) else tokens.surface2,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                if (!item.isDirectory) onOpenFile(item.relativePath)
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect(item.relativePath) },
                            modifier = Modifier.size(28.dp)
                        )
                        Icon(
                            imageVector = if (item.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                            contentDescription = null,
                            tint = if (item.isDirectory) tokens.warning else tokens.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.relativePath,
                                color = tokens.textPrimary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!item.isDirectory) {
                                Text(formatBytes(item.sizeBytes), color = tokens.textMuted, fontSize = 10.sp)
                            }
                        }
                        if (!item.isDirectory) {
                            IconButton(onClick = { onRenameClick(item.relativePath) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = "Rename", tint = tokens.textMuted, modifier = Modifier.size(15.dp))
                            }
                            IconButton(onClick = { onCopyClick(item.relativePath) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = tokens.textMuted, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RichFilePreviewSurface(
    projectRoot: String?,
    tab: OpenEditorTab,
    zipEntries: List<ZipEntryInfo>
) {
    val tokens = LocalLdroidTokens.current
    val file = projectRoot?.let { File(it, tab.relativePath) }
    LdroidSurfaceCard(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(tab.relativePath, color = tokens.textPrimary, fontWeight = FontWeight.Bold)
            StatusPill(tab.previewMode.name, tokens.info)
        }
        Spacer(Modifier.height(10.dp))
        when (tab.previewMode) {
            FilePreviewKind.IMAGE -> {
                if (file != null && file.exists()) {
                    AsyncImage(
                        model = file,
                        contentDescription = tab.relativePath,
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    )
                }
            }
            FilePreviewKind.ZIP_ARCHIVE -> {
                Text("Archive entries (${zipEntries.size}):", color = tokens.textMuted, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(zipEntries) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(entry.name, color = tokens.textPrimary, fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily, modifier = Modifier.weight(1f))
                            Text(formatBytes(entry.uncompressedBytes), color = tokens.textMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
            FilePreviewKind.PDF_BINARY, FilePreviewKind.UNSUPPORTED_BINARY -> {
                Text(
                    "Binary/PDF file (${formatBytes(tab.totalBytes)}). Inline text editing is disabled to prevent file corruption.",
                    color = tokens.warning,
                    fontSize = 13.sp
                )
            }
            FilePreviewKind.CODE_TEXT -> Unit
        }
    }
}

@Composable
fun VisualBuilderPanel(
    uiState: LdroidUiState,
    viewModel: LdroidViewModel
) {
    val tokens = LocalLdroidTokens.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Add Component to Visual Tree", color = tokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VisualComponentType.entries.forEach { type ->
                FilterChip(
                    selected = false,
                    onClick = { viewModel.addVisualNode(type) },
                    label = { Text("+ ${type.displayName}", fontSize = 11.sp) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(uiState.visualNodes, key = { it.id }) { node ->
                LdroidSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusPill(node.type.displayName, tokens.secondaryAccent)
                        OutlinedTextField(
                            value = node.label,
                            onValueChange = { viewModel.updateVisualNodeLabel(node.id, it) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.removeVisualNode(node.id) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove component", tint = tokens.error)
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.writeVisualBuilderToProject(asXml = false) },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent)
            ) {
                Text("Generate Compose .kt", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { viewModel.writeVisualBuilderToProject(asXml = true) },
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("Generate XML Layout", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun BottomTerminalAndProblemsDock(
    sessions: List<TerminalSession>,
    activeSessionId: String?,
    diagnostics: List<DiagnosticIssue>,
    viewModel: LdroidViewModel
) {
    val tokens = LocalLdroidTokens.current
    var showProblems by remember { mutableStateOf(false) }
    var commandInput by remember { mutableStateOf("") }
    val activeSession = sessions.firstOrNull { it.id == activeSessionId } ?: sessions.firstOrNull()

    Surface(
        modifier = Modifier.fillMaxWidth().height(230.dp),
        color = tokens.terminalBackground,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, tokens.border)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = !showProblems,
                        onClick = { showProblems = false },
                        label = { Text("Terminal (${sessions.size})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = showProblems,
                        onClick = { showProblems = true },
                        label = { Text("Problems (${diagnostics.size})", fontSize = 11.sp) }
                    )
                }
                if (!showProblems) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        sessions.forEach { sess ->
                            FilterChip(
                                selected = sess.id == activeSession?.id,
                                onClick = { viewModel.selectTerminalSession(sess.id) },
                                label = { Text(sess.title, fontSize = 10.sp) }
                            )
                        }
                        IconButton(onClick = { viewModel.createTerminalSession() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Add, contentDescription = "New Terminal", tint = tokens.primaryAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (!showProblems) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(activeSession?.lines.orEmpty(), key = { it.id }) { line ->
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        placeholder = { Text("sh command (ls, pwd, uname -a)...", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("terminal_command_input")
                    )
                    if (activeSession?.isRunning == true) {
                        IconButton(
                            onClick = { viewModel.cancelTerminalCommand() },
                            modifier = Modifier.size(48.dp).testTag("terminal_cancel_btn")
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = "Cancel process", tint = tokens.error)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (commandInput.isNotBlank()) {
                                    viewModel.runTerminalCommand(commandInput)
                                    commandInput = ""
                                }
                            },
                            modifier = Modifier.size(48.dp).testTag("terminal_run_btn")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Run command", tint = tokens.primaryAccent)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (diagnostics.isEmpty()) {
                        item {
                            Text("0 problems detected in workspace.", color = tokens.success, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                        }
                    } else {
                        items(diagnostics) { issue ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.openFileInEditor(issue.filePath) }
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "[${issue.severity.name}] ${issue.filePath}:${issue.line} — ${issue.message}",
                                    color = if (issue.severity == DiagnosticSeverity.ERROR || issue.severity == DiagnosticSeverity.SECRET) tokens.error else tokens.warning,
                                    fontSize = 11.sp,
                                    fontFamily = JetBrainsMonoFontFamily
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
