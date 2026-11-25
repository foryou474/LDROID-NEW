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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.model.VisualComponentType
import com.example.model.VisualNode
import com.example.ui.components.StatusPill
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.LdroidSettingsState
import com.example.data.terminal.TerminalSession
import com.example.model.FilePreviewKind
import com.example.model.LineEnding
import com.example.model.TextEncodingOption
import com.example.ui.components.DeviceFramePreviewCanvas
import com.example.ui.components.SyntaxHighlightedTransformation
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

private enum class EditorWorkspaceMode { CODE, VISUAL_BUILDER, LIVE_PREVIEW }

@Composable
fun EditorScreen(
    uiState: LdroidUiState,
    settings: LdroidSettingsState,
    terminalSessions: List<TerminalSession>,
    viewModel: LdroidViewModel,
    isExpandedWidth: Boolean,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    var showExplorer by remember(isExpandedWidth) { mutableStateOf(isExpandedWidth) }
    var showTerminalDock by remember { mutableStateOf(true) }
    var showFindBar by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(EditorWorkspaceMode.CODE) }

    var showNewFileDialog by remember { mutableStateOf<Boolean?>(null) } // true=dir, false=file
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var copyTarget by remember { mutableStateOf<String?>(null) }

    val activeTab = uiState.activeTab

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Editor Top Action Toolbar
        Surface(
            color = tokens.surface1,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, tokens.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { showExplorer = !showExplorer },
                    modifier = Modifier.size(48.dp).testTag("toggle_explorer_btn")
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = "Toggle File Explorer", tint = if (showExplorer) tokens.primaryAccent else tokens.textMuted)
                }

                FilterChip(
                    selected = mode == EditorWorkspaceMode.CODE,
                    onClick = { mode = EditorWorkspaceMode.CODE },
                    label = { Text("Code", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = mode == EditorWorkspaceMode.VISUAL_BUILDER,
                    onClick = { mode = EditorWorkspaceMode.VISUAL_BUILDER },
                    label = { Text("Visual Builder", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = mode == EditorWorkspaceMode.LIVE_PREVIEW,
                    onClick = { mode = EditorWorkspaceMode.LIVE_PREVIEW },
                    label = { Text("Live Preview", fontSize = 11.sp) }
                )

                IconButton(
                    onClick = { viewModel.undoActiveTab() },
                    enabled = (activeTab?.undoStack?.isNotEmpty() == true),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = tokens.textPrimary)
                }
                IconButton(
                    onClick = { viewModel.redoActiveTab() },
                    enabled = (activeTab?.redoStack?.isNotEmpty() == true),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = tokens.textPrimary)
                }
                IconButton(
                    onClick = { showFindBar = !showFindBar },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Find & Replace", tint = if (showFindBar) tokens.primaryAccent else tokens.textMuted)
                }
                IconButton(
                    onClick = { viewModel.saveActiveTab() },
                    enabled = activeTab?.isDirty == true,
                    modifier = Modifier.size(48.dp).testTag("editor_save_btn")
                ) {
                    Icon(
                        Icons.Filled.Save,
                        contentDescription = "Save file",
                        tint = if (activeTab?.isDirty == true) tokens.success else tokens.textMuted
                    )
                }
                IconButton(
                    onClick = { showTerminalDock = !showTerminalDock },
                    modifier = Modifier.size(48.dp).testTag("toggle_terminal_btn")
                ) {
                    Icon(Icons.Filled.Terminal, contentDescription = "Toggle Terminal Dock", tint = if (showTerminalDock) tokens.primaryAccent else tokens.textMuted)
                }
            }
        }

        // Find & Replace Panel
        if (showFindBar) {
            FindReplacePanel(
                activeContent = activeTab?.content.orEmpty(),
                onReplace = { f, r, all, ignoreCase -> viewModel.replaceInActiveTab(f, r, all, ignoreCase) },
                onClose = { showFindBar = false }
            )
        }

        // Open Tabs Strip
        if (uiState.openTabs.isNotEmpty() && mode == EditorWorkspaceMode.CODE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                uiState.openTabs.forEach { tab ->
                    val selected = tab.relativePath == uiState.activeTabPath
                    Surface(
                        color = if (selected) tokens.elevatedSurface else tokens.surface1,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (selected) tokens.primaryAccent else tokens.border),
                        modifier = Modifier.clickable { viewModel.openFileInEditor(tab.relativePath) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val title = tab.relativePath.substringAfterLast('/') + if (tab.isDirty) " *" else ""
                            Text(title, color = if (selected) tokens.primaryAccent else tokens.textPrimary, fontSize = 12.sp, fontFamily = JetBrainsMonoFontFamily)
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Close tab",
                                tint = tokens.textMuted,
                                modifier = Modifier.size(14.dp).clickable { viewModel.closeEditorTab(tab.relativePath) }
                            )
                        }
                    }
                }
            }
        }

        // Main Workspace Area (Explorer + Editor/Visual/Preview)
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showExplorer) {
                FileExplorerPane(
                    uiState = uiState,
                    onSearchChange = { viewModel.updateFileSearch(it) },
                    onOpenFile = {
                        viewModel.openFileInEditor(it)
                        if (!isExpandedWidth) showExplorer = false
                    },
                    onToggleSelect = { viewModel.toggleFileSelection(it) },
                    onNewFileClick = { showNewFileDialog = false },
                    onNewFolderClick = { showNewFileDialog = true },
                    onRenameClick = { renameTarget = it },
                    onCopyClick = { copyTarget = it },
                    onDeleteSelectedClick = { viewModel.deleteSelectedFiles(uiState.selectedFilePaths.toList()) },
                    modifier = if (isExpandedWidth) Modifier.width(260.dp).fillMaxHeight() else Modifier.fillMaxSize()
                )
            }

            if (!showExplorer || isExpandedWidth) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (mode) {
                        EditorWorkspaceMode.CODE -> {
                            if (activeTab == null) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = tokens.surface1,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, tokens.border)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
                                        Text("Open a file from the Explorer to start editing.", color = tokens.textMuted)
                                    }
                                }
                            } else if (activeTab.previewMode != FilePreviewKind.CODE_TEXT) {
                                RichFilePreviewSurface(
                                    projectRoot = uiState.activeProject?.rootPath,
                                    tab = activeTab,
                                    zipEntries = uiState.zipInspectionEntries
                                )
                            } else {
                                CodeEditorSurface(
                                    tab = activeTab,
                                    settings = settings,
                                    onTextChange = { viewModel.updateActiveTabContent(it) },
                                    onCycleEncoding = {
                                        val nextEnc = TextEncodingOption.entries[(activeTab.encoding.ordinal + 1) % TextEncodingOption.entries.size]
                                        viewModel.setActiveTabEncodingAndEnding(nextEnc, activeTab.lineEnding)
                                    },
                                    onToggleLineEnding = {
                                        val nextEnd = if (activeTab.lineEnding == LineEnding.LF) LineEnding.CRLF else LineEnding.LF
                                        viewModel.setActiveTabEncodingAndEnding(activeTab.encoding, nextEnd)
                                    }
                                )
                            }
                        }
                        EditorWorkspaceMode.VISUAL_BUILDER -> VisualBuilderPanel(uiState = uiState, viewModel = viewModel)
                        EditorWorkspaceMode.LIVE_PREVIEW -> {
                            LiveContentPreviewPane(activeTab = activeTab, uiState = uiState)
                        }
                    }
                }
            }
        }

        if (showTerminalDock) {
            BottomTerminalAndProblemsDock(
                sessions = terminalSessions,
                activeSessionId = uiState.activeTerminalSessionId,
                diagnostics = uiState.diagnostics,
                viewModel = viewModel
            )
        }
    }

    // Dialogs for File Ops
    showNewFileDialog?.let { isDir ->
        SinglePathDialog(
            title = if (isDir) "New Folder" else "New File",
            initial = if (isDir) "app/src/main/java/newdir" else "app/src/main/java/NewScreen.kt",
            onDismiss = { showNewFileDialog = null },
            onConfirm = { path ->
                viewModel.createFileOrFolder(path, isDir)
                showNewFileDialog = null
            }
        )
    }

    renameTarget?.let { oldPath ->
        SinglePathDialog(
            title = "Rename ${oldPath.substringAfterLast('/')}",
            initial = oldPath.substringAfterLast('/'),
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                viewModel.renameFile(oldPath, newName)
                renameTarget = null
            }
        )
    }

    copyTarget?.let { srcPath ->
        SinglePathDialog(
            title = "Copy To Relative Path",
            initial = srcPath.replace(".", "_copy."),
            onDismiss = { copyTarget = null },
            onConfirm = { dest ->
                viewModel.copyFile(srcPath, dest)
                copyTarget = null
            }
        )
    }
}

@Composable
private fun CodeEditorSurface(
    tab: com.example.model.OpenEditorTab,
    settings: LdroidSettingsState,
    onTextChange: (String) -> Unit,
    onCycleEncoding: () -> Unit,
    onToggleLineEnding: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    val ext = tab.relativePath.substringAfterLast('.', "")
    val linesCount = remember(tab.content) { tab.content.count { it == '\n' } + 1 }
    val scrollVertical = rememberScrollState()
    val scrollHorizontal = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = tokens.surface1,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, tokens.border)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (tab.isLargeFilePreview) {
                Surface(color = tokens.warning.copy(alpha = 0.16f)) {
                    Text(
                        "Large file streaming preview (first 128 KB of ${tab.totalBytes / 1024} KB). Read-only to prevent memory exhaustion.",
                        color = tokens.warning,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollVertical)
                    .padding(vertical = 8.dp)
            ) {
                if (settings.showLineNumbers) {
                    Column(
                        modifier = Modifier
                            .background(tokens.surface2)
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        for (i in 1..linesCount) {
                            Text(
                                text = i.toString(),
                                color = tokens.editorLineNumber,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = settings.editorFontSizeSp.sp,
                                lineHeight = (settings.editorFontSizeSp + 6).sp
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(if (settings.wordWrap) Modifier else Modifier.horizontalScroll(scrollHorizontal))
                        .padding(horizontal = 10.dp)
                ) {
                    BasicTextField(
                        value = tab.content,
                        onValueChange = { if (!tab.isLargeFilePreview) onTextChange(it) },
                        readOnly = tab.isLargeFilePreview,
                        textStyle = TextStyle(
                            color = tokens.textPrimary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = settings.editorFontSizeSp.sp,
                            lineHeight = (settings.editorFontSizeSp + 6).sp
                        ),
                        cursorBrush = SolidColor(tokens.primaryAccent),
                        visualTransformation = remember(tokens, ext) {
                            SyntaxHighlightedTransformation(tokens, ext)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("code_editor_input")
                    )
                }
            }

            // Editor Status Bar (Encoding, Line Ending, Lines)
            Surface(color = tokens.surface2, border = BorderStroke(1.dp, tokens.divider)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${linesCount} lines • ${if (tab.isDirty) "Modified" else "Saved"}",
                        color = if (tab.isDirty) tokens.warning else tokens.success,
                        fontSize = 11.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            tab.encoding.charsetName,
                            color = tokens.primaryAccent,
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            modifier = Modifier.clickable(onClick = onCycleEncoding)
                        )
                        Text(
                            tab.lineEnding.label,
                            color = tokens.secondaryAccent,
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            modifier = Modifier.clickable(onClick = onToggleLineEnding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FindReplacePanel(
    activeContent: String,
    onReplace: (String, String, Boolean, Boolean) -> Unit,
    onClose: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var matchCase by remember { mutableStateOf(false) }

    val matchesCount = remember(activeContent, findText, matchCase) {
        if (findText.isEmpty()) 0 else {
            val options = if (matchCase) setOf() else setOf(RegexOption.IGNORE_CASE)
            runCatching { Regex(Regex.escape(findText), options).findAll(activeContent).count() }.getOrDefault(0)
        }
    }

    Surface(
        color = tokens.surface1,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, tokens.primaryAccent.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Find & Replace",
                    color = tokens.primaryAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (findText.isEmpty()) "Enter search query" else "$matchesCount match(es)",
                        color = if (matchesCount > 0) tokens.success else tokens.textMuted,
                        fontSize = 11.sp
                    )
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Find & Replace", tint = tokens.textMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = findText,
                    onValueChange = { findText = it },
                    placeholder = { Text("Find text...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("find_input")
                )
                OutlinedTextField(
                    value = replaceText,
                    onValueChange = { replaceText = it },
                    placeholder = { Text("Replace with...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("replace_input")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = matchCase,
                    onClick = { matchCase = !matchCase },
                    label = { Text("Aa Case", fontSize = 11.sp) }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onReplace(findText, replaceText, false, !matchCase) },
                        enabled = findText.isNotEmpty() && matchesCount > 0,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Replace Next", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { onReplace(findText, replaceText, true, !matchCase) },
                        enabled = findText.isNotEmpty() && matchesCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent),
                        modifier = Modifier.height(40.dp).testTag("replace_all_btn")
                    ) {
                        Text("Replace All", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SinglePathDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(value) }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun LiveContentPreviewPane(
    activeTab: com.example.model.OpenEditorTab?,
    uiState: LdroidUiState,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    val path = activeTab?.relativePath?.lowercase().orEmpty()
    val content = activeTab?.content.orEmpty()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = tokens.surface1,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, tokens.border)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Preview: ${activeTab?.relativePath ?: "No file active"}",
                    color = tokens.primaryAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                StatusPill(
                    label = when {
                        path.endsWith(".html") || path.endsWith(".htm") -> "HTML / CSS WebView"
                        path.endsWith(".xml") -> "XML Layout Inspector"
                        else -> "Compose Code Preview"
                    },
                    color = tokens.success
                )
            }

            Spacer(Modifier.height(4.dp))

            when {
                path.endsWith(".html") || path.endsWith(".htm") -> {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                webViewClient = WebViewClient()
                                tag = content
                                loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)
                            }
                        },
                        update = { webView ->
                            if (webView.tag != content) {
                                webView.tag = content
                                webView.loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)
                            }
                        }
                    )
                }
                path.endsWith(".xml") -> {
                    val parsedNodes = remember(content) { parseXmlLayoutToNodes(content) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .background(tokens.surface2, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (parsedNodes.isEmpty()) {
                            Text(
                                text = "No layout components detected in XML. Type elements like <TextView ... android:text=\"Hello\" /> or <Button />",
                                color = tokens.textMuted,
                                fontSize = 12.sp
                            )
                        } else {
                            parsedNodes.forEach { node ->
                                when (node.type) {
                                    VisualComponentType.TEXT -> Text(node.label, color = tokens.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    VisualComponentType.BUTTON -> Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text(node.label) }
                                    VisualComponentType.TEXT_FIELD -> OutlinedTextField(value = "", onValueChange = {}, label = { Text(node.label) }, modifier = Modifier.fillMaxWidth())
                                    VisualComponentType.SWITCH_CONTROL -> Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(node.label, color = tokens.textPrimary)
                                        Switch(checked = true, onCheckedChange = {})
                                    }
                                    VisualComponentType.CARD -> Surface(
                                        color = tokens.surface1,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, tokens.border),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(node.label, color = tokens.textPrimary, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                                    }
                                    else -> Surface(color = tokens.surface1, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        Text(node.label, color = tokens.textMuted, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    val codePrev = uiState.codePreviewState
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DeviceFramePreviewCanvas(
                            title = codePrev.lastGoodTitle,
                            nodes = codePrev.lastGoodNodes,
                            profile = uiState.selectedDeviceProfile,
                            landscape = uiState.previewLandscape,
                            fontScale = uiState.previewFontScale,
                            isStale = codePrev.isStale,
                            staleError = codePrev.errorMessage
                        )
                    }
                }
            }
        }
    }
}

private fun parseXmlLayoutToNodes(xml: String): List<VisualNode> {
    val nodes = mutableListOf<VisualNode>()
    val tagRegex = Regex("""<([A-Za-z0-9._]+)([^>]*?)>""")
    val textAttrRegex = Regex("""android:text\s*=\s*"([^"]*)"""")
    val hintAttrRegex = Regex("""android:hint\s*=\s*"([^"]*)"""")
    val titleAttrRegex = Regex("""app:title\s*=\s*"([^"]*)"""")

    val containerTags = setOf(
        "LinearLayout", "FrameLayout", "RelativeLayout", "ConstraintLayout",
        "CoordinatorLayout", "ScrollView", "NestedScrollView", "merge"
    )

    tagRegex.findAll(xml).forEach { match ->
        val rawTag = match.groupValues[1]
        val simpleTag = rawTag.substringAfterLast('.')
        val attrs = match.groupValues[2]
        val explicitLabel = textAttrRegex.find(attrs)?.groupValues?.get(1)
            ?: hintAttrRegex.find(attrs)?.groupValues?.get(1)
            ?: titleAttrRegex.find(attrs)?.groupValues?.get(1)

        if (simpleTag in containerTags && explicitLabel == null) {
            return@forEach
        }

        val label = explicitLabel ?: simpleTag
        val compType = when {
            simpleTag.contains("TextView", ignoreCase = true) || simpleTag.contains("Toolbar", ignoreCase = true) -> VisualComponentType.TEXT
            simpleTag.contains("Button", ignoreCase = true) || simpleTag.contains("FloatingActionButton", ignoreCase = true) -> VisualComponentType.BUTTON
            simpleTag.contains("EditText", ignoreCase = true) || simpleTag.contains("TextInput", ignoreCase = true) -> VisualComponentType.TEXT_FIELD
            simpleTag.contains("Switch", ignoreCase = true) || simpleTag.contains("CheckBox", ignoreCase = true) || simpleTag.contains("RadioButton", ignoreCase = true) -> VisualComponentType.SWITCH_CONTROL
            simpleTag.contains("Card", ignoreCase = true) -> VisualComponentType.CARD
            else -> VisualComponentType.COLUMN
        }
        nodes.add(VisualNode(id = java.util.UUID.randomUUID().toString().take(6), type = compType, label = label))
    }
    return nodes
}
