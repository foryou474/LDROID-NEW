package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.LdroidSettingsState
import com.example.data.terminal.TerminalSession
import com.example.data.visual.VisualBuilderCodegen
import com.example.model.FilePreviewKind
import com.example.model.LineEnding
import com.example.model.MaxStudioMode
import com.example.model.TextEncodingOption
import com.example.model.VisualComponentType
import com.example.ui.components.DeviceFramePreviewCanvas
import com.example.ui.components.LdroidSurfaceCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel
import java.io.File

@Composable
fun MaxStudioScreen(
    uiState: LdroidUiState,
    settings: LdroidSettingsState,
    terminalSessions: List<TerminalSession>,
    viewModel: LdroidViewModel,
    isExpandedWidth: Boolean,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    val context = LocalContext.current
    var showExplorer by remember(isExpandedWidth) { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showFindBar by remember { mutableStateOf(false) }
    var lastSearchOffset by remember(uiState.activeTabPath) { mutableIntStateOf(0) }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var copyTarget by remember { mutableStateOf<String?>(null) }
    var moveTarget by remember { mutableStateOf<String?>(null) }

    BackHandler { onBackToHome() }

    val attachSupportFilesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.importFilesViaSaf(uris)
    }

    val voiceInputLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spoken.isNullOrEmpty()) {
                val combined = listOf(uiState.universalInputText.trim(), spoken)
                    .filter { it.isNotEmpty() }
                    .joinToString(" ")
                viewModel.updateUniversalInput(combined)
                viewModel.postMessage("Voice captured: $spoken")
            }
        }
    }

    fun launchVoiceRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak code, UI layout, or shell command...")
        }
        val canHandle = intent.resolveActivity(context.packageManager) != null
        if (canHandle) {
            runCatching { voiceInputLauncher.launch(intent) }
                .onFailure { viewModel.postMessage("Voice recognizer unavailable: ${it.message}") }
        } else {
            viewModel.postMessage("Voice input requires a speech recognition provider on device.")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
    ) {
        // Dedicated Max Studio Top Header Bar: Left Back (←), Active Project Name, Copper "Build APK"
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
                        onClick = onBackToHome,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("max_studio_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = tokens.primaryAccent
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.activeProject?.name ?: "PulseApp",
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = uiState.maxStudioMode.label,
                            color = tokens.textMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { showExplorer = !showExplorer },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("max_studio_files_btn")
                    ) {
                        Icon(
                            Icons.Filled.FolderOpen,
                            contentDescription = "Toggle Project Files",
                            tint = if (showExplorer) tokens.primaryAccent else tokens.textMuted
                        )
                    }
                    Button(
                        onClick = { viewModel.buildApkInMaxStudio() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.primaryAccent,
                            contentColor = tokens.onPrimaryAccent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("max_studio_build_apk_btn")
                    ) {
                        Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Build APK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Dedicated Workspace Body
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            when (uiState.maxStudioMode) {
                MaxStudioMode.CODE_EG -> CodeEgBody(
                    uiState = uiState,
                    settings = settings,
                    viewModel = viewModel,
                    showFindBar = showFindBar,
                    onToggleFind = { showFindBar = !showFindBar },
                    lastSearchOffset = lastSearchOffset,
                    onUpdateSearchOffset = { lastSearchOffset = it },
                    onOpenNewFile = { showNewFileDialog = true }
                )
                MaxStudioMode.AI_UI_SCREEN -> AiUiScreenBody(
                    uiState = uiState,
                    viewModel = viewModel
                )
                MaxStudioMode.TERMINAL -> TermuxTerminalBody(
                    uiState = uiState,
                    sessions = terminalSessions,
                    viewModel = viewModel
                )
            }

            // Optional Project Files Overlay Drawer
            if (showExplorer) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { showExplorer = false }
                ) {
                    Surface(
                        color = tokens.surface1,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, tokens.border),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                            .clickable(enabled = false) {}
                    ) {
                        EditorFileExplorer(
                            uiState = uiState,
                            onSearchChange = { viewModel.updateFileSearch(it) },
                            onRefreshWorkspace = { viewModel.refreshWorkspace() },
                            onOpenFile = { path ->
                                viewModel.openFileInEditor(path, jumpToEditorTab = true)
                                showExplorer = false
                            },
                            onToggleSelect = { viewModel.toggleFileSelection(it) },
                            onNewFileClick = { showNewFileDialog = true },
                            onNewFolderClick = { showNewFileDialog = true },
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

        // Persistent 2-Row Bottom Control Stack (always above keyboard)
        MaxStudioBottomControls(
            activeMode = uiState.maxStudioMode,
            inputText = uiState.universalInputText,
            canUndoEnhance = uiState.previousPromptBeforeEnhance != null,
            onSelectMode = { viewModel.selectMaxStudioMode(it) },
            onInputChange = { viewModel.updateUniversalInput(it) },
            onNewFileClick = { showNewFileDialog = true },
            onAttachSupportFilesClick = { attachSupportFilesLauncher.launch(arrayOf("*/*")) },
            onAiEnhanceClick = { viewModel.enhancePromptForMode(uiState.maxStudioMode) },
            onUndoEnhanceClick = { viewModel.undoEnhancedPrompt() },
            onMicClick = { launchVoiceRecognizer() },
            onRunClick = { viewModel.executeUniversalRun(uiState.maxStudioMode, uiState.universalInputText) }
        )
    }

    if (showNewFileDialog) {
        CreateProjectItemDialog(
            packageName = uiState.activeProject?.packageName ?: "com.ldroid.pulseapp",
            initialTab = 0,
            onDismiss = { showNewFileDialog = false },
            onCreateFile = { path, content ->
                viewModel.createFileOrFolder(path, false, content)
                showNewFileDialog = false
            },
            onCreateFolder = { path ->
                viewModel.createFileOrFolder(path, true, "")
                showNewFileDialog = false
            }
        )
    }

    renameTarget?.let { oldPath ->
        SinglePathDialog(
            title = "Rename ${oldPath.substringAfterLast('/')}",
            initial = oldPath.substringAfterLast('/'),
            label = "New name",
            onDismiss = { renameTarget = null },
            onConfirm = {
                viewModel.renameFile(oldPath, it)
                renameTarget = null
            }
        )
    }
    copyTarget?.let { src ->
        SinglePathDialog(
            title = "Copy $src",
            initial = src,
            label = "Destination path",
            onDismiss = { copyTarget = null },
            onConfirm = {
                viewModel.copyFile(src, it)
                copyTarget = null
            }
        )
    }
    moveTarget?.let { src ->
        SinglePathDialog(
            title = "Move $src",
            initial = src,
            label = "Destination path",
            onDismiss = { moveTarget = null },
            onConfirm = {
                viewModel.moveFile(src, it)
                moveTarget = null
            }
        )
    }
}

@Composable
private fun CodeEgBody(
    uiState: LdroidUiState,
    settings: LdroidSettingsState,
    viewModel: LdroidViewModel,
    showFindBar: Boolean,
    onToggleFind: () -> Unit,
    lastSearchOffset: Int,
    onUpdateSearchOffset: (Int) -> Unit,
    onOpenNewFile: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    val activeTab = uiState.activeTab

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Code E/G Toolbar + Active File Tabs
        Surface(
            color = tokens.surface1,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, tokens.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.undoActiveTab() },
                        enabled = activeTab?.undoStack?.isNotEmpty() == true,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = tokens.textPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { viewModel.redoActiveTab() },
                        enabled = activeTab?.redoStack?.isNotEmpty() == true,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = tokens.textPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onToggleFind, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Filled.Search, contentDescription = "Find and Replace", tint = tokens.primaryAccent, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { viewModel.formatActiveTab() }, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Filled.FormatIndentIncrease, contentDescription = "Format Code", tint = tokens.secondaryAccent, modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(
                    onClick = { viewModel.saveActiveTab() },
                    modifier = Modifier.size(38.dp).testTag("code_eg_save_btn")
                ) {
                    Icon(
                        Icons.Filled.Save,
                        contentDescription = "Save File",
                        tint = if (activeTab?.isDirty == true) tokens.success else tokens.primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (showFindBar) {
            FindReplacePanel(
                activeContent = activeTab?.content.orEmpty(),
                findQuery = uiState.editorFindQuery,
                matchCase = uiState.editorFindMatchCase,
                onSearchChange = { q, mc ->
                    onUpdateSearchOffset(0)
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
                        onUpdateSearchOffset((range.last + 1).coerceAtMost(content.length))
                    }
                    viewModel.findNextInActiveTab(range?.first ?: lastSearchOffset)
                },
                onReplace = { f, r, all, ignoreCase -> viewModel.replaceInActiveTab(f, r, all, ignoreCase) },
                onClose = {
                    onToggleFind()
                    viewModel.setEditorSearchState("", false)
                }
            )
        }

        if (uiState.openTabs.isNotEmpty()) {
            EditorFileTabsBar(
                tabs = uiState.openTabs,
                activePath = uiState.activeTabPath,
                tokens = tokens,
                onSelectTab = { viewModel.openFileInEditor(it) },
                onCloseTab = { viewModel.closeEditorTab(it) },
                onCloseOtherTabs = { viewModel.closeOtherTabs(it) },
                onCloseAllTabs = { viewModel.closeAllTabs() }
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (activeTab == null) {
                LdroidSurfaceCard(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No file open in Code E/G", color = tokens.textPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onOpenNewFile) { Text("Create New File") }
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
                    pendingJumpLine = uiState.pendingCursorJump?.takeIf { it.first == activeTab.relativePath }?.second,
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
                    pendingOffsetJump = uiState.pendingOffsetJump?.takeIf { it.first == activeTab.relativePath }?.second,
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
    }
}

@Composable
private fun AiUiScreenBody(
    uiState: LdroidUiState,
    viewModel: LdroidViewModel
) {
    val tokens = LocalLdroidTokens.current
    val nodes = uiState.visualNodes
    var selectedNodeId by remember { mutableStateOf(nodes.firstOrNull()?.id) }
    val selectedNode = nodes.firstOrNull { it.id == selectedNodeId } ?: nodes.firstOrNull()
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragAccumulatedPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(nodes.size) {
        if (nodes.none { it.id == selectedNodeId }) {
            selectedNodeId = nodes.lastOrNull()?.id
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_ui_screen_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VisualBuilderCodegen.defaultDeviceProfiles.take(3).forEach { profile ->
                        FilterChip(
                            selected = uiState.selectedDeviceProfile.name == profile.name,
                            onClick = { viewModel.selectDeviceProfile(profile) },
                            label = { Text(profile.name, fontSize = 10.sp) }
                        )
                    }
                }
                IconButton(onClick = { viewModel.togglePreviewOrientation() }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.RotateRight, contentDescription = "Rotate Preview", tint = tokens.primaryAccent)
                }
                StatusPill("2-Way Synced", tokens.success)
            }
        }

        // Interactive Live Device Frame Preview
        item {
            DeviceFramePreviewCanvas(
                title = uiState.activeProject?.name ?: "PulseApp",
                nodes = nodes,
                profile = uiState.selectedDeviceProfile,
                landscape = uiState.previewLandscape,
                fontScale = uiState.previewFontScale,
                isStale = uiState.codePreviewState.isStale,
                staleError = uiState.codePreviewState.errorMessage,
                selectedNodeId = selectedNode?.id,
                onNodeClick = { selectedNodeId = it.id }
            )
        }

        // Quick Add Palette Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    VisualComponentType.TEXT,
                    VisualComponentType.CARD,
                    VisualComponentType.BUTTON,
                    VisualComponentType.COUNTER,
                    VisualComponentType.TEXT_FIELD,
                    VisualComponentType.SWITCH_CONTROL
                ).forEach { type ->
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.addVisualNode(type) },
                        label = { Text("+ ${type.displayName}", fontSize = 11.sp) }
                    )
                }
            }
        }

        // Selected Node Inline Property Editor
        selectedNode?.let { node ->
            item {
                LdroidSurfaceCard(accentBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill("Inspector: ${node.type.displayName}", tokens.primaryAccent)
                        IconButton(onClick = { viewModel.removeVisualNode(node.id) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Delete node", tint = tokens.error, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = node.label,
                        onValueChange = { viewModel.updateVisualNode(node.id, label = it) },
                        label = { Text("Label / Text", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Copper", "Default", "Emerald", "Amber", "Cyan").forEach { tag ->
                            FilterChip(
                                selected = node.colorTag == tag,
                                onClick = { viewModel.updateVisualNode(node.id, colorTag = tag) },
                                label = { Text(tag, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Drag-and-Drop Reorderable Component Tree (2-way synced with VisualGeneratedScreen.kt)
        item {
            Text(
                text = "Visual Component Tree (Long-press drag or arrows to reorder)",
                color = tokens.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        itemsIndexed(nodes, key = { _, n -> n.id }) { idx, node ->
            val isSelected = node.id == selectedNode?.id
            Surface(
                color = if (isSelected) tokens.primaryAccent.copy(alpha = 0.14f) else tokens.surface1,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isSelected) tokens.primaryAccent else tokens.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedNodeId = node.id }
                    .pointerInput(idx, nodes.size) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedIndex = idx
                                dragAccumulatedPx = 0f
                            },
                            onDragEnd = {
                                draggedIndex = null
                                dragAccumulatedPx = 0f
                            },
                            onDragCancel = {
                                draggedIndex = null
                                dragAccumulatedPx = 0f
                            },
                            onDrag = { _, dragAmount ->
                                val cur = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                dragAccumulatedPx += dragAmount.y
                                val stepPx = 110f
                                if (dragAccumulatedPx > stepPx && cur < nodes.lastIndex) {
                                    viewModel.reorderVisualNode(cur, cur + 1)
                                    draggedIndex = cur + 1
                                    dragAccumulatedPx = 0f
                                } else if (dragAccumulatedPx < -stepPx && cur > 0) {
                                    viewModel.reorderVisualNode(cur, cur - 1)
                                    draggedIndex = cur - 1
                                    dragAccumulatedPx = 0f
                                }
                            }
                        )
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.DragHandle, contentDescription = "Drag to reorder", tint = tokens.textMuted, modifier = Modifier.size(18.dp))
                        StatusPill(node.type.displayName, tokens.primaryAccent)
                        Text(
                            text = node.label,
                            color = tokens.textPrimary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row {
                        IconButton(onClick = { viewModel.moveVisualNode(node.id, -1) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up", tint = tokens.textMuted, modifier = Modifier.size(15.dp))
                        }
                        IconButton(onClick = { viewModel.moveVisualNode(node.id, 1) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down", tint = tokens.textMuted, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.writeVisualBuilderToProject(asXml = false) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.primaryAccent,
                        contentColor = tokens.onPrimaryAccent
                    ),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("Sync to Kotlin .kt", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { viewModel.writeVisualBuilderToProject(asXml = true) },
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Text("Export XML Layout", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun TermuxTerminalBody(
    uiState: LdroidUiState,
    sessions: List<TerminalSession>,
    viewModel: LdroidViewModel
) {
    val tokens = LocalLdroidTokens.current
    val activeSession = sessions.firstOrNull { it.id == uiState.activeTerminalSessionId } ?: sessions.firstOrNull()
    val lines = activeSession?.lines.orEmpty()
    val listState = rememberLazyListState()
    val projectFolder = uiState.activeProject?.name?.ifBlank { null }
        ?: activeSession?.workingDir?.let { File(it).name }
        ?: "PulseApp"
    var historyCursor by remember(activeSession?.id) { mutableIntStateOf(-1) }
    var ctrlArmed by remember { mutableStateOf(false) }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    Surface(
        color = tokens.terminalBackground,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, tokens.border),
        modifier = Modifier
            .fillMaxSize()
            .testTag("termux_terminal_surface")
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "u0_a1@ldroid:~/LDROID_Projects/$projectFolder $",
                    color = tokens.primaryAccent,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                activeSession?.lastExitCode?.let { code ->
                    StatusPill("exit $code", if (code == 0) tokens.success else tokens.error)
                }
            }

            HorizontalDivider(color = tokens.border.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                itemsIndexed(lines, key = { _, line -> line.id }) { _, line ->
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

            Spacer(Modifier.height(6.dp))
            // Termux Extra Keys Bar: ESC, TAB, CTRL, ALT, -, /, |, ↑, ↓
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val extraKeys = listOf("ESC", "TAB", "CTRL", "ALT", "-", "/", "|", "↑", "↓")
                extraKeys.forEach { key ->
                    val highlighted = key == "CTRL" && ctrlArmed
                    Surface(
                        color = if (highlighted) tokens.primaryAccent else tokens.surface1,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (highlighted) tokens.primaryAccent else tokens.border),
                        modifier = Modifier
                            .clickable {
                                when (key) {
                                    "ESC" -> {
                                        ctrlArmed = false
                                        viewModel.cancelTerminalCommand()
                                        viewModel.updateUniversalInput("")
                                    }
                                    "TAB" -> {
                                        val cur = uiState.universalInputText
                                        viewModel.updateUniversalInput(if (cur.isEmpty()) "ls " else "$cur    ")
                                    }
                                    "CTRL" -> {
                                        ctrlArmed = !ctrlArmed
                                        if (ctrlArmed) viewModel.postMessage("CTRL modifier armed (tap another key or Enter)")
                                    }
                                    "ALT" -> viewModel.runTerminalCommand("pkg list")
                                    "↑" -> {
                                        val history = activeSession?.commandHistory.orEmpty()
                                        if (history.isNotEmpty()) {
                                            val nextIdx = if (historyCursor < 0) history.lastIndex else (historyCursor - 1).coerceAtLeast(0)
                                            historyCursor = nextIdx
                                            viewModel.updateUniversalInput(history[nextIdx])
                                        }
                                    }
                                    "↓" -> {
                                        val history = activeSession?.commandHistory.orEmpty()
                                        if (history.isNotEmpty() && historyCursor >= 0) {
                                            if (historyCursor >= history.lastIndex) {
                                                historyCursor = -1
                                                viewModel.updateUniversalInput("")
                                            } else {
                                                historyCursor++
                                                viewModel.updateUniversalInput(history[historyCursor])
                                            }
                                        }
                                    }
                                    else -> {
                                        if (ctrlArmed && key == "-") {
                                            viewModel.cancelTerminalCommand()
                                            ctrlArmed = false
                                        } else {
                                            viewModel.updateUniversalInput(uiState.universalInputText + key)
                                        }
                                    }
                                }
                            }
                            .testTag("termux_key_$key")
                    ) {
                        Text(
                            text = key,
                            color = if (highlighted) tokens.onPrimaryAccent else tokens.textPrimary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
