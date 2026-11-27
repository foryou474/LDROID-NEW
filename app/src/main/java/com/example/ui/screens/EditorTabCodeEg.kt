package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.LdroidSettingsState
import com.example.model.EditorWorkspaceTab
import com.example.model.FilePreviewKind
import com.example.model.LineEnding
import com.example.model.TextEncodingOption
import com.example.ui.components.LdroidSurfaceCard
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

/**
 * Tab 1 (<>): Code EG — Multi-tab syntax-highlighted editor, toolbar, and vertical
 * bottom-left floating control pill (Live-Sync switch, </> code view jump, Wi-Fi preview sync indicator).
 */
@Composable
fun EditorTabCodeEg(
    uiState: LdroidUiState,
    settings: LdroidSettingsState,
    viewModel: LdroidViewModel,
    showFindBar: Boolean,
    onToggleFind: () -> Unit,
    lastSearchOffset: Int,
    onUpdateSearchOffset: (Int) -> Unit,
    onOpenNewFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    val activeTab = uiState.activeTab
    val syncSnap = uiState.syncSnapshot

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Editor Action Bar
        Surface(
            color = tokens.surface1,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, tokens.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.undoActiveTab() },
                        enabled = activeTab?.undoStack?.isNotEmpty() == true,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = tokens.textPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { viewModel.redoActiveTab() },
                        enabled = activeTab?.redoStack?.isNotEmpty() == true,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = tokens.textPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onToggleFind, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Search, contentDescription = "Find and Replace", tint = tokens.primaryAccent, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { viewModel.formatActiveTab() }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.FormatIndentIncrease, contentDescription = "Format Code", tint = tokens.secondaryAccent, modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(
                    onClick = { viewModel.saveActiveTab() },
                    modifier = Modifier.size(40.dp).testTag("code_eg_save_btn")
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
                        Text("No file open in Code EG", color = tokens.textPrimary, fontWeight = FontWeight.Bold)
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

            // Vertical bottom-left control pill: Live-sync toggle switch, </> code icon, Wi-Fi live-preview sync indicator
            Surface(
                color = tokens.elevatedSurface.copy(alpha = 0.94f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, tokens.primaryAccent.copy(alpha = 0.55f)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 46.dp)
                    .testTag("code_eg_bottom_left_pill")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = syncSnap.liveSyncEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.syncEngine.toggleLiveSync(enabled)
                            viewModel.postMessage(if (enabled) "Live-sync AST enabled" else "Live-sync paused")
                        },
                        modifier = Modifier.scale(0.75f).testTag("code_eg_live_sync_switch")
                    )
                    Icon(
                        imageVector = Icons.Filled.Code,
                        contentDescription = "Open Code Live Preview",
                        tint = tokens.primaryAccent,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { viewModel.selectMaxStudioMode(EditorWorkspaceTab.CODE_LIVE_PREVIEW) }
                    )
                    Icon(
                        imageVector = if (syncSnap.wifiLivePreviewActive) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                        contentDescription = "Wi-Fi Live Preview Sync",
                        tint = if (syncSnap.wifiLivePreviewActive) tokens.success else tokens.textMuted,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                val next = !syncSnap.wifiLivePreviewActive
                                viewModel.syncEngine.toggleWifiPreviewSync(next)
                                viewModel.postMessage(
                                    if (next) "Wi-Fi Live Preview sync active"
                                    else "Wi-Fi Live Preview sync paused"
                                )
                            }
                            .testTag("code_eg_wifi_sync_btn")
                    )
                }
            }
        }
    }
}
