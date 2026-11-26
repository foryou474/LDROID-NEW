package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Css
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FlutterDash
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.LdroidSettingsState
import com.example.data.terminal.TerminalSession
import com.example.model.BottomToolTab
import com.example.model.DiagnosticSeverity
import com.example.model.FilePreviewKind
import com.example.model.LineEnding
import com.example.model.OpenEditorTab
import com.example.model.PrimaryDestination
import com.example.model.TextEncodingOption
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LdroidTokens
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

private enum class EditorWorkspaceMode { CODE, VISUAL_BUILDER, LIVE_PREVIEW }
private enum class EditorPrimaryMode { CODE, TERMINAL }

private sealed class PendingTabCloseAction {
    data class Single(val path: String) : PendingTabCloseAction()
    data class Others(val keepPath: String, val dirtyPaths: List<String>) : PendingTabCloseAction()
    data class All(val dirtyPaths: List<String>) : PendingTabCloseAction()
}

@Composable
fun EditorScreen(
    uiState: LdroidUiState,
    settings: LdroidSettingsState,
    terminalSessions: List<TerminalSession>,
    viewModel: LdroidViewModel,
    isExpandedWidth: Boolean,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    val context = LocalContext.current
    var primaryMode by remember { mutableStateOf(EditorPrimaryMode.CODE) }
    var selectedTerminalTab by remember { mutableStateOf(BottomToolTab.TERMINAL) }
    var showExplorer by remember(isExpandedWidth) { mutableStateOf(isExpandedWidth) }
    var aiPanelStage by remember { mutableStateOf(AiPanelStage.HIDDEN) }
    var showCreateModal by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showAttachDialog by remember { mutableStateOf(false) }
    var attachedAiFiles by remember { mutableStateOf(setOf<String>()) }
    var showFindBar by remember { mutableStateOf(false) }
    var lastSearchOffset by remember(uiState.activeTabPath) { mutableIntStateOf(0) }

    val activeMode = remember(settings.editorWorkspaceMode) {
        runCatching { EditorWorkspaceMode.valueOf(settings.editorWorkspaceMode) }
            .getOrDefault(EditorWorkspaceMode.CODE)
    }

    // Dialog states
    var showCommandPalette by remember { mutableStateOf(false) }
    var showQuickEditorSettings by remember { mutableStateOf(false) }
    var showActiveFileDiff by remember { mutableStateOf(false) }
    var pendingTabClose by remember { mutableStateOf<PendingTabCloseAction?>(null) }

    var renameTarget by remember { mutableStateOf<String?>(null) }
    var copyTarget by remember { mutableStateOf<String?>(null) }
    var moveTarget by remember { mutableStateOf<String?>(null) }

    val activeTab = uiState.activeTab
    val hasDirtyTabs = remember(uiState.openTabs) { uiState.openTabs.any { it.isDirty } }

    val importFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val invalidFiles = mutableListOf<String>()
        val validZipUris = mutableListOf<Uri>()
        val validCodeUris = mutableListOf<Uri>()

        for (uri in uris) {
            val fileName = getUriDisplayName(context, uri)
            val ext = fileName.substringAfterLast('.', "").lowercase()
            if (ext !in SUPPORTED_PROJECT_EXTENSIONS) {
                invalidFiles.add(fileName)
            } else if (ext == "zip") {
                validZipUris.add(uri)
            } else {
                validCodeUris.add(uri)
            }
        }

        if (invalidFiles.isNotEmpty()) {
            viewModel.postMessage("Unsupported file type in: ${invalidFiles.joinToString(", ")}. Supported formats: .kt, .java, .xml, .json, .gradle, .dart, .tsx, .html, .css, .zip")
            return@rememberLauncherForActivityResult
        }

        validZipUris.forEach { viewModel.importZipProjectViaSaf(it) }
        if (validCodeUris.isNotEmpty()) {
            viewModel.importFilesViaSaf(validCodeUris)
        }
    }

    val importFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        treeUri?.let { uri ->
            viewModel.importFilesViaSaf(listOf(uri))
        }
    }

    fun sendContextAwareAiPrompt(prompt: String) {
        val clean = prompt.trim()
        if (clean.isEmpty()) return
        if (primaryMode == EditorPrimaryMode.CODE) {
            viewModel.sendAiPrompt(
                prompt = clean,
                attachActiveFile = true,
                attachedExtraPaths = attachedAiFiles.toList()
            )
        } else {
            val activeSession = terminalSessions.firstOrNull { it.id == uiState.activeTerminalSessionId } ?: terminalSessions.firstOrNull()
            val recentLogs = activeSession?.lines?.takeLast(25)?.joinToString("\n") { it.text }.orEmpty()
            val terminalPrompt = if (recentLogs.isNotBlank()) {
                "Active Terminal Output:\n```\n$recentLogs\n```\n\nTerminal Assistant Request:\n$clean"
            } else {
                "Terminal Assistant Request:\n$clean"
            }
            viewModel.sendAiPrompt(
                prompt = terminalPrompt,
                attachActiveFile = false,
                attachedExtraPaths = attachedAiFiles.toList()
            )
        }
    }

    fun requestCloseSingle(path: String) {
        val tab = uiState.openTabs.firstOrNull { it.relativePath == path }
        if (tab?.isDirty == true) {
            pendingTabClose = PendingTabCloseAction.Single(path)
        } else {
            viewModel.closeEditorTab(path)
        }
    }

    fun requestCloseOthers(keepPath: String) {
        val dirty = uiState.openTabs.filter { it.relativePath != keepPath && it.isDirty }.map { it.relativePath }
        if (dirty.isNotEmpty()) {
            pendingTabClose = PendingTabCloseAction.Others(keepPath, dirty)
        } else {
            viewModel.closeOtherTabs(keepPath)
        }
    }

    fun requestCloseAll() {
        val dirty = uiState.openTabs.filter { it.isDirty }.map { it.relativePath }
        if (dirty.isNotEmpty()) {
            pendingTabClose = PendingTabCloseAction.All(dirty)
        } else {
            viewModel.closeAllTabs()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
            .imePadding()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.isCtrlPressed) {
                    when {
                        keyEvent.key == Key.S -> {
                            viewModel.saveActiveTab()
                            viewModel.postMessage("Saved successfully")
                            true
                        }
                        keyEvent.key == Key.F -> {
                            showFindBar = !showFindBar
                            true
                        }
                        keyEvent.key == Key.P && keyEvent.isShiftPressed -> {
                            showCommandPalette = true
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 2. Unified Compact Top Action Bar (Shared Across Both Code & Terminal)
        Surface(
            color = tokens.surface1,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, tokens.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top-Left: ✨ AI Pill Button (context-aware: opens compact floating panel or toggles expanded drawer)
                OutlinedButton(
                    onClick = {
                        aiPanelStage = if (aiPanelStage == AiPanelStage.HIDDEN) AiPanelStage.COMPACT
                        else if (aiPanelStage == AiPanelStage.COMPACT) AiPanelStage.EXPANDED
                        else AiPanelStage.HIDDEN
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp).testTag("editor_ai_pill_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = tokens.primaryAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = if (primaryMode == EditorPrimaryMode.CODE) "✨ AI Assistant" else "✨ AI Fixer",
                        color = tokens.primaryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Top-Right: Compact Icon Toolbar (Files, Undo, Redo, Save)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 1. Files
                    IconButton(
                        onClick = { showExplorer = !showExplorer },
                        modifier = Modifier.size(34.dp).testTag("toolbar_files_btn")
                    ) {
                        Icon(
                            imageVector = if (showExplorer) Icons.Filled.FolderOpen else Icons.Filled.Folder,
                            contentDescription = "Toggle Files Explorer",
                            tint = if (showExplorer) tokens.primaryAccent else tokens.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    // 2. Undo
                    IconButton(
                        onClick = {
                            if (primaryMode == EditorPrimaryMode.CODE) {
                                viewModel.undoActiveTab()
                            } else {
                                viewModel.undoActiveTab()
                                viewModel.postMessage("Undone")
                            }
                        },
                        enabled = activeTab?.undoStack?.isNotEmpty() == true || primaryMode == EditorPrimaryMode.TERMINAL,
                        modifier = Modifier.size(34.dp).testTag("toolbar_undo_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (activeTab?.undoStack?.isNotEmpty() == true || primaryMode == EditorPrimaryMode.TERMINAL) tokens.textPrimary else tokens.textMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    // 3. Redo
                    IconButton(
                        onClick = {
                            if (primaryMode == EditorPrimaryMode.CODE) {
                                viewModel.redoActiveTab()
                            } else {
                                viewModel.redoActiveTab()
                                viewModel.postMessage("Redone")
                            }
                        },
                        enabled = activeTab?.redoStack?.isNotEmpty() == true || primaryMode == EditorPrimaryMode.TERMINAL,
                        modifier = Modifier.size(34.dp).testTag("toolbar_redo_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (activeTab?.redoStack?.isNotEmpty() == true || primaryMode == EditorPrimaryMode.TERMINAL) tokens.textPrimary else tokens.textMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    // 4. Save
                    IconButton(
                        onClick = {
                            viewModel.saveActiveTab()
                            viewModel.postMessage("Saved")
                        },
                        modifier = Modifier.size(34.dp).testTag("toolbar_save_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = "Save File",
                            tint = if (activeTab?.isDirty == true) tokens.success else tokens.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 3. 3 Sub-Modes & Project File Controls Inside the <> Code Workspace (Only visible when Code is active)
        if (primaryMode == EditorPrimaryMode.CODE) {
            Surface(
                color = tokens.surface2,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sub-mode tabs: Code, Visual Builder, Live Preview
                    listOf(
                        EditorWorkspaceMode.CODE to "Code",
                        EditorWorkspaceMode.VISUAL_BUILDER to "Visual Builder",
                        EditorWorkspaceMode.LIVE_PREVIEW to "Live Preview"
                    ).forEach { (mode, label) ->
                        val isSelected = activeMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setEditorWorkspaceMode(mode.name) },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("submode_${mode.name.lowercase()}")
                        )
                    }

                    // 1. Create Button
                    OutlinedButton(
                        onClick = { showCreateModal = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp).testTag("code_header_create_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Create File or Folder",
                            tint = tokens.primaryAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "Create",
                            fontSize = 11.sp,
                            color = tokens.primaryAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // 2. File (Import) Button
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp).testTag("code_header_import_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FileUpload,
                            contentDescription = "Import Files or Folder",
                            tint = tokens.secondaryAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = "File",
                            fontSize = 11.sp,
                            color = tokens.secondaryAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Find & Replace inline bar (Current File)
            if (showFindBar) {
                FindReplacePanel(
                    activeContent = activeTab?.content.orEmpty(),
                    findQuery = uiState.editorFindQuery,
                    matchCase = uiState.editorFindMatchCase,
                    onSearchChange = { q, mc ->
                        lastSearchOffset = 0
                        viewModel.setEditorSearchState(q, mc)
                    },
                    onFindNext = {
                        val content = activeTab?.content.orEmpty()
                        val range = CodeEditorHelpers.findNextOccurrenceOffset(
                            text = content,
                            query = uiState.editorFindQuery,
                            currentOffset = lastSearchOffset,
                            ignoreCase = !uiState.editorFindMatchCase
                        )
                        if (range != null) {
                            lastSearchOffset = (range.last + 1).coerceAtMost(content.length)
                        }
                        viewModel.findNextInActiveTab(lastSearchOffset.let { if (range != null) range.first else it })
                    },
                    onReplace = { f, r, all, ignoreCase -> viewModel.replaceInActiveTab(f, r, all, ignoreCase) },
                    onClose = {
                        showFindBar = false
                        viewModel.setEditorSearchState("", false)
                    }
                )
            }

            // Open File Tabs Strip
            if (uiState.openTabs.isNotEmpty() && activeMode != EditorWorkspaceMode.VISUAL_BUILDER) {
                EditorFileTabsBar(
                    tabs = uiState.openTabs,
                    activePath = uiState.activeTabPath,
                    tokens = tokens,
                    onSelectTab = { viewModel.openFileInEditor(it) },
                    onCloseTab = { requestCloseSingle(it) },
                    onCloseOtherTabs = { requestCloseOthers(it) },
                    onCloseAllTabs = { requestCloseAll() }
                )
            }
        }

        // Main Workspace Area (with File Explorer side-panel or mobile drawer overlay & Two-Stage AI Panel)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Side-by-side explorer on expanded screens
                if (showExplorer && isExpandedWidth) {
                    EditorFileExplorer(
                        uiState = uiState,
                        onSearchChange = { viewModel.updateFileSearch(it) },
                        onRefreshWorkspace = { viewModel.refreshWorkspace() },
                        onOpenFile = { path ->
                            primaryMode = EditorPrimaryMode.CODE
                            viewModel.openFileInEditor(path)
                        },
                        onToggleSelect = { viewModel.toggleFileSelection(it) },
                        onNewFileClick = { showCreateModal = true },
                        onNewFolderClick = { showCreateModal = true },
                        onRenameClick = { renameTarget = it },
                        onCopyClick = { copyTarget = it },
                        onMoveClick = { moveTarget = it },
                        onDeleteConfirm = { viewModel.deleteSelectedFiles(it) },
                        modifier = Modifier.width(260.dp).fillMaxHeight()
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (primaryMode == EditorPrimaryMode.TERMINAL) {
                        EditorBottomToolPanel(
                            uiState = uiState,
                            sessions = terminalSessions,
                            selectedTab = selectedTerminalTab,
                            panelHeightDp = 0, // 0 indicates full-screen terminal filling the workspace
                            onSelectTab = { selectedTerminalTab = it },
                            onResizeHeightDp = {},
                            onCollapse = { primaryMode = EditorPrimaryMode.CODE },
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        when (activeMode) {
                            EditorWorkspaceMode.CODE -> {
                                if (activeTab == null) {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        color = tokens.surface1,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, tokens.border)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Surface(
                                                color = tokens.primaryAccent.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(16.dp),
                                                border = BorderStroke(1.dp, tokens.primaryAccent.copy(alpha = 0.35f)),
                                                modifier = Modifier.size(56.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Code,
                                                        contentDescription = null,
                                                        tint = tokens.primaryAccent,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(14.dp))
                                            Text(
                                                text = "No file open in editor",
                                                color = tokens.textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                text = "Start coding by creating a folder/file or launching a new template file.",
                                                color = tokens.textMuted,
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(Modifier.height(18.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Button(
                                                    onClick = { showCreateModal = true },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = tokens.primaryAccent,
                                                        contentColor = tokens.onPrimaryAccent
                                                    ),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .height(48.dp)
                                                        .testTag("empty_editor_create_btn")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Add,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = "Create",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                                OutlinedButton(
                                                    onClick = { showNewFileDialog = true },
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(1.dp, tokens.primaryAccent.copy(alpha = 0.65f)),
                                                    modifier = Modifier
                                                        .height(48.dp)
                                                        .testTag("empty_editor_new_file_btn")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.NoteAdd,
                                                        contentDescription = null,
                                                        tint = tokens.primaryAccent,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = "New File",
                                                        color = tokens.primaryAccent,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else if (activeTab.previewMode != FilePreviewKind.CODE_TEXT) {
                                    RichFilePreviewSurface(
                                        projectRoot = uiState.activeProject?.rootPath,
                                        tab = activeTab,
                                        zipEntries = uiState.zipInspectionEntries
                                    )
                                } else {
                                    val jumpLine = uiState.pendingCursorJump
                                        ?.takeIf { it.first == activeTab.relativePath }
                                        ?.second
                                    val offsetJump = uiState.pendingOffsetJump
                                        ?.takeIf { it.first == activeTab.relativePath }
                                        ?.second
                                    CodeEditorSurface(
                                        tab = activeTab,
                                        settings = settings,
                                        pendingJumpLine = jumpLine,
                                        onJumpConsumed = { viewModel.consumeCursorJump() },
                                        onTextChange = { viewModel.updateActiveTabContent(it) },
                                        onCycleEncoding = {
                                            val nextEnc = TextEncodingOption.entries[(activeTab.encoding.ordinal + 1) % TextEncodingOption.entries.size]
                                            viewModel.setActiveTabEncodingAndEnding(nextEnc, activeTab.lineEnding)
                                        },
                                        onToggleLineEnding = {
                                            val nextEnd = if (activeTab.lineEnding == LineEnding.LF) LineEnding.CRLF else LineEnding.LF
                                            viewModel.setActiveTabEncodingAndEnding(activeTab.encoding, nextEnd)
                                        },
                                        searchQuery = if (showFindBar) uiState.editorFindQuery else "",
                                        searchMatchCase = uiState.editorFindMatchCase,
                                        pendingOffsetJump = offsetJump,
                                        onOffsetJumpConsumed = { viewModel.clearPendingOffsetJump() },
                                        onAdjustFontSize = { delta ->
                                            viewModel.updateEditorSettings(
                                                fontSize = (settings.editorFontSizeSp + delta).coerceIn(10, 22),
                                                tabWidth = settings.tabWidth,
                                                autoSave = settings.autoSaveEnabled,
                                                lineNumbers = settings.showLineNumbers,
                                                wordWrap = settings.wordWrap,
                                                formatOnSave = settings.formatOnSave
                                            )
                                        }
                                    )
                                }
                            }
                            EditorWorkspaceMode.VISUAL_BUILDER -> {
                                VisualBuilderPanel(uiState = uiState, viewModel = viewModel)
                            }
                            EditorWorkspaceMode.LIVE_PREVIEW -> {
                                EditorLivePreviewPane(
                                    activeTab = activeTab,
                                    uiState = uiState,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }

            // Mobile File Explorer Drawer Overlay (When compact/portrait and showExplorer is true)
            if (showExplorer && !isExpandedWidth) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showExplorer = false }
                ) {
                    Surface(
                        color = tokens.surface1,
                        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                        border = BorderStroke(1.dp, tokens.border),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                            .clickable(enabled = false) {}
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FolderOpen,
                                        contentDescription = null,
                                        tint = tokens.primaryAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Project Files",
                                        fontWeight = FontWeight.Bold,
                                        color = tokens.primaryAccent,
                                        fontSize = 13.sp
                                    )
                                }
                                IconButton(onClick = { showExplorer = false }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close Files", tint = tokens.textMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                            HorizontalDivider(color = tokens.border)
                            EditorFileExplorer(
                                uiState = uiState,
                                onSearchChange = { viewModel.updateFileSearch(it) },
                                onRefreshWorkspace = { viewModel.refreshWorkspace() },
                                onOpenFile = { path ->
                                    primaryMode = EditorPrimaryMode.CODE
                                    viewModel.openFileInEditor(path)
                                    showExplorer = false
                                },
                                onToggleSelect = { viewModel.toggleFileSelection(it) },
                                onNewFileClick = {
                                    showCreateModal = true
                                    showExplorer = false
                                },
                                onNewFolderClick = {
                                    showCreateModal = true
                                    showExplorer = false
                                },
                                onRenameClick = { renameTarget = it },
                                onCopyClick = { copyTarget = it },
                                onMoveClick = { moveTarget = it },
                                onDeleteConfirm = { viewModel.deleteSelectedFiles(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Compact Floating AI Bar (Stage 1)
            if (aiPanelStage == AiPanelStage.COMPACT) {
                CompactAiMessageBar(
                    isCodeMode = primaryMode == EditorPrimaryMode.CODE,
                    activeTab = activeTab,
                    attachedFilesCount = attachedAiFiles.size,
                    isAiLoading = uiState.isAiLoading,
                    onExpand = { aiPanelStage = AiPanelStage.EXPANDED },
                    onClose = { aiPanelStage = AiPanelStage.HIDDEN },
                    onAttachFile = { showAttachDialog = true },
                    onSendPrompt = { prompt -> sendContextAwareAiPrompt(prompt) },
                    tokens = tokens,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            // Expanded Full-Height AI Chat Drawer (Stage 2)
            if (aiPanelStage == AiPanelStage.EXPANDED) {
                ExpandedAiChatDrawer(
                    isCodeMode = primaryMode == EditorPrimaryMode.CODE,
                    activeTab = activeTab,
                    messages = uiState.aiMessages,
                    isAiLoading = uiState.isAiLoading,
                    attachedFiles = attachedAiFiles,
                    onDetachFile = { path -> attachedAiFiles = attachedAiFiles - path },
                    onAttachFile = { showAttachDialog = true },
                    onMinimize = { aiPanelStage = AiPanelStage.COMPACT },
                    onClose = { aiPanelStage = AiPanelStage.HIDDEN },
                    onSaveHistory = {
                        viewModel.saveChatHistoryToWorkspace()
                        viewModel.postMessage("Chat history saved to workspace")
                    },
                    onClearChat = { viewModel.clearAiConversation() },
                    onSendPrompt = { prompt -> sendContextAwareAiPrompt(prompt) },
                    onApplyCodeBlock = { code ->
                        if (primaryMode == EditorPrimaryMode.CODE) {
                            viewModel.applyCodeBlockToActiveEditor(code)
                            aiPanelStage = AiPanelStage.COMPACT
                        } else {
                            viewModel.runTerminalCommand(code.trim())
                            viewModel.postMessage("Sent command to terminal")
                        }
                    },
                    onApplyProposedChange = { msgId, change ->
                        viewModel.applyAiProposedChange(msgId, change)
                    },
                    tokens = tokens
                )
            }
        }

        // 1. Bottom Primary Mode Switcher (Only at the Bottom)
        Surface(
            color = tokens.surface1,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, tokens.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { primaryMode = EditorPrimaryMode.CODE },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("primary_mode_code"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (primaryMode == EditorPrimaryMode.CODE) tokens.primaryAccent.copy(alpha = 0.18f) else Color.Transparent
                    ),
                    border = BorderStroke(1.dp, if (primaryMode == EditorPrimaryMode.CODE) tokens.primaryAccent else tokens.border)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Code,
                        contentDescription = null,
                        tint = if (primaryMode == EditorPrimaryMode.CODE) tokens.primaryAccent else tokens.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = "<> Code",
                        color = if (primaryMode == EditorPrimaryMode.CODE) tokens.primaryAccent else tokens.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = { primaryMode = EditorPrimaryMode.TERMINAL },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("primary_mode_terminal"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (primaryMode == EditorPrimaryMode.TERMINAL) tokens.secondaryAccent.copy(alpha = 0.18f) else Color.Transparent
                    ),
                    border = BorderStroke(1.dp, if (primaryMode == EditorPrimaryMode.TERMINAL) tokens.secondaryAccent else tokens.border)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Terminal,
                        contentDescription = null,
                        tint = if (primaryMode == EditorPrimaryMode.TERMINAL) tokens.secondaryAccent else tokens.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = ">_ Terminal",
                        color = if (primaryMode == EditorPrimaryMode.TERMINAL) tokens.secondaryAccent else tokens.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // --- Interactive Modals & Dialogs ---

    if (showCreateModal) {
        CreateProjectItemDialog(
            packageName = uiState.activeProject?.packageName ?: "com.maxstudio.app",
            onDismiss = { showCreateModal = false },
            onCreateFile = { path, content ->
                viewModel.createFileOrFolder(path, false, content)
                showCreateModal = false
            },
            onCreateFolder = { path ->
                viewModel.createFileOrFolder(path, true)
                showCreateModal = false
            }
        )
    }

    if (showNewFileDialog) {
        NewFileTemplateDialog(
            packageName = uiState.activeProject?.packageName ?: "com.maxstudio.app",
            onDismiss = { showNewFileDialog = false },
            onCreate = { path, content ->
                viewModel.createFileOrFolder(path, false, content)
                showNewFileDialog = false
            }
        )
    }

    if (showImportDialog) {
        ImportProjectFilesDialog(
            onDismiss = { showImportDialog = false },
            onPickFiles = {
                showImportDialog = false
                importFilesLauncher.launch(arrayOf("*/*"))
            },
            onPickFolder = {
                showImportDialog = false
                importFolderLauncher.launch(null)
            }
        )
    }

    if (showAttachDialog) {
        AiAttachFilesDialog(
            workspaceFiles = uiState.workspaceFiles,
            currentlyAttached = attachedAiFiles,
            onToggleFile = { path ->
                attachedAiFiles = if (path in attachedAiFiles) attachedAiFiles - path else attachedAiFiles + path
            },
            onDismiss = { showAttachDialog = false }
        )
    }

    renameTarget?.let { oldPath ->
        SinglePathDialog(
            title = "Rename ${oldPath.substringAfterLast('/')}",
            initial = oldPath.substringAfterLast('/'),
            label = "New name",
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                viewModel.renameFile(oldPath, newName)
                renameTarget = null
            }
        )
    }

    copyTarget?.let { srcPath ->
        SinglePathDialog(
            title = "Copy File",
            initial = srcPath.replace(".", "_copy."),
            label = "Destination relative path",
            onDismiss = { copyTarget = null },
            onConfirm = { dest ->
                viewModel.copyFile(srcPath, dest)
                copyTarget = null
            }
        )
    }

    moveTarget?.let { srcPath ->
        SinglePathDialog(
            title = "Move ${srcPath.substringAfterLast('/')}",
            initial = srcPath,
            label = "Destination relative path",
            onDismiss = { moveTarget = null },
            onConfirm = { dest ->
                viewModel.moveFile(srcPath, dest)
                moveTarget = null
            }
        )
    }

    pendingTabClose?.let { action ->
        val dirtyPaths = when (action) {
            is PendingTabCloseAction.Single -> listOf(action.path)
            is PendingTabCloseAction.Others -> action.dirtyPaths
            is PendingTabCloseAction.All -> action.dirtyPaths
        }
        UnsavedChangesDialog(
            dirtyPaths = dirtyPaths,
            onSaveAndProceed = {
                when (action) {
                    is PendingTabCloseAction.Single -> viewModel.saveAndCloseTabs(listOf(action.path))
                    is PendingTabCloseAction.Others -> viewModel.saveAndCloseTabs(action.dirtyPaths, closeOthersKeeping = action.keepPath)
                    is PendingTabCloseAction.All -> viewModel.saveAndCloseTabs(action.dirtyPaths, closeAll = true)
                }
                pendingTabClose = null
            },
            onDiscardAndProceed = {
                when (action) {
                    is PendingTabCloseAction.Single -> viewModel.closeEditorTab(action.path)
                    is PendingTabCloseAction.Others -> viewModel.closeOtherTabs(action.keepPath)
                    is PendingTabCloseAction.All -> viewModel.closeAllTabs()
                }
                pendingTabClose = null
            },
            onDismiss = { pendingTabClose = null }
        )
    }
}

@Composable
internal fun EditorFileTabsBar(
    tabs: List<OpenEditorTab>,
    activePath: String?,
    tokens: LdroidTokens,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onCloseOtherTabs: (String) -> Unit,
    onCloseAllTabs: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val selected = tab.relativePath == activePath
            var showTabMenu by remember { mutableStateOf(false) }
            val fileName = tab.relativePath.substringAfterLast('/')
            val (fileIcon, fileTint) = tabIconForExtension(tab.relativePath.substringAfterLast('.', ""), tokens)

            Surface(
                color = if (selected) tokens.elevatedSurface else tokens.surface1,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (selected) tokens.primaryAccent else tokens.border),
                modifier = Modifier.clickable { onSelectTab(tab.relativePath) }
            ) {
                Row(
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(fileIcon, contentDescription = null, tint = fileTint, modifier = Modifier.size(14.dp))
                    Text(
                        text = fileName,
                        color = if (selected) tokens.primaryAccent else tokens.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        fontFamily = JetBrainsMonoFontFamily,
                        maxLines = 1
                    )
                    if (tab.isDirty) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(tokens.warning, CircleShape)
                        )
                    }
                    Box {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Tab options for $fileName",
                            tint = tokens.textMuted,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { showTabMenu = true }
                        )
                        DropdownMenu(
                            expanded = showTabMenu,
                            onDismissRequest = { showTabMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Close Tab") },
                                onClick = { showTabMenu = false; onCloseTab(tab.relativePath) }
                            )
                            DropdownMenuItem(
                                text = { Text("Close Other Tabs") },
                                enabled = tabs.size > 1,
                                onClick = { showTabMenu = false; onCloseOtherTabs(tab.relativePath) }
                            )
                            DropdownMenuItem(
                                text = { Text("Close All Tabs") },
                                onClick = { showTabMenu = false; onCloseAllTabs() }
                            )
                        }
                    }
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close $fileName",
                        tint = tokens.textMuted,
                        modifier = Modifier
                            .size(15.dp)
                            .clickable { onCloseTab(tab.relativePath) }
                    )
                }
            }
        }
    }
}

private fun tabIconForExtension(ext: String, tokens: LdroidTokens) = when (ext.lowercase()) {
    "kt" -> Icons.Filled.Code to tokens.primaryAccent
    "kts", "gradle" -> Icons.Filled.Build to tokens.warning
    "java" -> Icons.Filled.Coffee to tokens.warning
    "dart" -> Icons.Filled.FlutterDash to tokens.info
    "tsx", "ts", "jsx", "js" -> Icons.Filled.Javascript to tokens.secondaryAccent
    "xml" -> Icons.Filled.SettingsApplications to tokens.secondaryAccent
    "html", "htm" -> Icons.Filled.Html to tokens.info
    "css" -> Icons.Filled.Css to tokens.tertiaryAccent
    "json", "toml", "yml", "yaml" -> Icons.Filled.DataObject to tokens.success
    else -> Icons.Filled.Description to tokens.textMuted
}

@Composable
internal fun FindReplacePanel(
    activeContent: String,
    findQuery: String,
    matchCase: Boolean,
    onSearchChange: (String, Boolean) -> Unit,
    onFindNext: () -> Unit,
    onReplace: (String, String, Boolean, Boolean) -> Unit,
    onClose: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    var replaceText by remember { mutableStateOf("") }

    val matchesCount = remember(activeContent, findQuery, matchCase) {
        if (findQuery.isEmpty()) 0 else {
            CodeEditorHelpers.findOccurrences(activeContent, findQuery, !matchCase).size
        }
    }

    Surface(
        color = tokens.surface1,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, tokens.primaryAccent.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Find & Replace", color = tokens.primaryAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (findQuery.isEmpty()) "Enter query" else "$matchesCount match(es)",
                        color = if (matchesCount > 0) tokens.success else tokens.textMuted,
                        fontSize = 11.sp
                    )
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
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
                    value = findQuery,
                    onValueChange = { onSearchChange(it, matchCase) },
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
                    onClick = { onSearchChange(findQuery, !matchCase) },
                    label = { Text("Aa", fontSize = 11.sp) }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onFindNext,
                        enabled = findQuery.isNotEmpty() && matchesCount > 0,
                        modifier = Modifier.height(38.dp).testTag("find_next_btn")
                    ) {
                        Text("Find Next", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { onReplace(findQuery, replaceText, false, !matchCase) },
                        enabled = findQuery.isNotEmpty() && matchesCount > 0,
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text("Replace", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { onReplace(findQuery, replaceText, true, !matchCase) },
                        enabled = findQuery.isNotEmpty() && matchesCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent),
                        modifier = Modifier.height(38.dp).testTag("replace_all_btn")
                    ) {
                        Text("All", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
