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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.LdroidSettingsState
import com.example.data.terminal.TerminalSession
import com.example.model.EditorWorkspaceTab
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

/**
 * Editor Workspace (`Your Project`) — 7-Tab Architecture & Master AI Dock
 * - Clean Top Header (NO Build APK button here):
 *   Left: ☰ (Left Sidebar Drawer) + bold "Your Project" (~/LDROID_Projects/<Name>)
 *   Right: 📁 (File Explorer Drawer) + ▶ (Quick Run)
 */
@Composable
fun MaxStudioScreen(
    uiState: LdroidUiState,
    settings: LdroidSettingsState,
    terminalSessions: List<TerminalSession>,
    viewModel: LdroidViewModel,
    isExpandedWidth: Boolean,
    onBackToHome: () -> Unit,
    onOpenDrawer: () -> Unit = {},
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
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Message AI or run command...")
        }
        val canHandle = intent.resolveActivity(context.packageManager) != null
        if (canHandle) {
            runCatching { voiceInputLauncher.launch(intent) }
                .onFailure { viewModel.postMessage("Voice recognizer unavailable: ${it.message}") }
        } else {
            viewModel.postMessage("Voice input requires a speech recognition provider on device.")
        }
    }

    val projectTitle = uiState.activeProject?.name ?: "Your Project"
    val projectFolderName = projectTitle.replace(" ", "_")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
    ) {
        // Clean Top Header (No Build APK button here)
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
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("max_studio_menu_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Open Sidebar Drawer",
                            tint = tokens.primaryAccent
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = projectTitle,
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "~/LDROID_Projects/$projectFolderName • ${uiState.maxStudioMode.label}",
                            color = tokens.textMuted,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                            contentDescription = "File Explorer & Asset Importer",
                            tint = if (showExplorer) tokens.primaryAccent else tokens.textMuted
                        )
                    }
                    IconButton(
                        onClick = { viewModel.triggerQuickRun() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("max_studio_quick_run_btn")
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Quick Run Preview",
                            tint = tokens.success
                        )
                    }
                }
            }
        }

        // 7-Tab Workspace Body
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            when (uiState.maxStudioMode) {
                EditorWorkspaceTab.CODE_EG -> EditorTabCodeEg(
                    uiState = uiState,
                    settings = settings,
                    viewModel = viewModel,
                    showFindBar = showFindBar,
                    onToggleFind = { showFindBar = !showFindBar },
                    lastSearchOffset = lastSearchOffset,
                    onUpdateSearchOffset = { lastSearchOffset = it },
                    onOpenNewFile = { showNewFileDialog = true }
                )
                EditorWorkspaceTab.CODE_LIVE_PREVIEW -> EditorTabCodeLivePreview(
                    uiState = uiState,
                    viewModel = viewModel
                )
                EditorWorkspaceTab.AI_STUDIO -> EditorTabAiStudio(
                    uiState = uiState,
                    viewModel = viewModel
                )
                EditorWorkspaceTab.TERMINAL -> EditorTabTerminal(
                    uiState = uiState,
                    sessions = terminalSessions,
                    viewModel = viewModel
                )
                EditorWorkspaceTab.LIVE_PREVIEW -> EditorTabLivePreview(
                    uiState = uiState,
                    viewModel = viewModel
                )
                EditorWorkspaceTab.VISUAL_BUILDER -> EditorTabVisualBuilder(
                    uiState = uiState,
                    viewModel = viewModel
                )
                EditorWorkspaceTab.BUILD_APK -> EditorTabBuildApk(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }

            // Slide-over Project Files & Asset Importer Drawer
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
                            .align(Alignment.CenterEnd)
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

        // Persistent Universal ChatGPT-Style Dock + Single-Row 7-Icon Bottom Bar
        MaxStudioBottomControls(
            activeMode = uiState.maxStudioMode,
            inputText = uiState.universalInputText,
            activeModelChip = uiState.activeModelChip,
            modelChips = settings.selectedModelChips,
            attachedAssets = uiState.attachedAiAssetLabels,
            sentinelErrorCount = uiState.syncSnapshot.errorCount,
            tabIconPreset = settings.tabIconPreset,
            onSelectMode = { viewModel.selectMaxStudioMode(it) },
            onInputChange = { viewModel.updateUniversalInput(it) },
            onSelectModelChip = { viewModel.selectActiveModelChip(it) },
            onOpenModelSetup = onOpenDrawer,
            onAttachAsset = { viewModel.attachAiAssetLabel(it) },
            onRemoveAttachedAsset = { viewModel.removeAiAssetLabel(it) },
            onNewFileClick = { showNewFileDialog = true },
            onAttachSupportFilesClick = { attachSupportFilesLauncher.launch(arrayOf("*/*")) },
            onMicClick = { launchVoiceRecognizer() },
            onSendClick = { viewModel.executeUniversalRun(uiState.maxStudioMode, uiState.universalInputText) }
        )
    }

    if (showNewFileDialog) {
        CreateProjectItemDialog(
            packageName = uiState.activeProject?.packageName ?: "com.ldroid.app",
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
