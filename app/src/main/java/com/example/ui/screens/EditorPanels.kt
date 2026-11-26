package com.example.ui.screens

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.workspace.ZipEntryInfo
import com.example.model.FilePreviewKind
import com.example.model.OpenEditorTab
import com.example.model.VisualComponentType
import com.example.ui.components.DeviceFramePreviewCanvas
import com.example.ui.components.LdroidSurfaceCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatBytes
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel
import java.io.File

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
    val nodes = uiState.visualNodes
    var selectedNodeId by remember {
        mutableStateOf(nodes.firstOrNull()?.id)
    }
    val selectedNode = nodes.firstOrNull { it.id == selectedNodeId } ?: nodes.firstOrNull()
    val colorOptions = listOf("Default", "Cyan", "Purple", "Emerald", "Amber", "Rose")

    LaunchedEffect(nodes.size) {
        if (nodes.none { it.id == selectedNodeId }) {
            selectedNodeId = nodes.lastOrNull()?.id
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        // AI Command Panel
        item {
            AiCommandPanel(uiState = uiState, viewModel = viewModel)
        }

        // Palette header + Auto-sync badge
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Component Palette",
                    color = tokens.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                StatusPill("Synced to Code", tokens.success)
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VisualComponentType.entries.forEach { type ->
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.addVisualNode(type) },
                        label = { Text("+ ${type.displayName}", fontSize = 11.sp) },
                        modifier = Modifier.testTag("vb_add_${type.name.lowercase()}")
                    )
                }
            }
        }

        // Interactive Phone Canvas (tap any rendered component to select & edit properties)
        item {
            DeviceFramePreviewCanvas(
                title = "Visual Builder Canvas",
                nodes = nodes,
                profile = uiState.selectedDeviceProfile,
                landscape = uiState.previewLandscape,
                fontScale = uiState.previewFontScale,
                isStale = false,
                staleError = null,
                selectedNodeId = selectedNode?.id,
                onNodeClick = { clicked -> selectedNodeId = clicked.id }
            )
        }

        // Selected Component Property Editor
        selectedNode?.let { node ->
            item {
                LdroidSurfaceCard(accentBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill("Editing: ${node.type.displayName}", tokens.primaryAccent)
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = { viewModel.moveVisualNode(node.id, -1) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ArrowUpward,
                                    contentDescription = "Move up",
                                    tint = tokens.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.moveVisualNode(node.id, 1) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ArrowDownward,
                                    contentDescription = "Move down",
                                    tint = tokens.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.removeVisualNode(node.id) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove component",
                                    tint = tokens.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = node.label,
                        onValueChange = { viewModel.updateVisualNode(node.id, label = it) },
                        label = { Text("Text / Label", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("vb_prop_label")
                    )

                    if (node.type == VisualComponentType.CARD || node.type == VisualComponentType.COUNTER) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = node.secondaryText,
                            onValueChange = { viewModel.updateVisualNode(node.id, secondaryText = it) },
                            label = { Text("Subtitle / Helper Text", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Color", color = tokens.textMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        colorOptions.forEach { tag ->
                            FilterChip(
                                selected = node.colorTag == tag,
                                onClick = { viewModel.updateVisualNode(node.id, colorTag = tag) },
                                label = { Text(tag, fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Padding: ${node.paddingDp}dp", color = tokens.textMuted, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.updateVisualNode(node.id, paddingDp = node.paddingDp - 4) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) { Text("-4dp", fontSize = 10.sp) }
                            OutlinedButton(
                                onClick = { viewModel.updateVisualNode(node.id, paddingDp = node.paddingDp + 4) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) { Text("+4dp", fontSize = 10.sp) }
                        }
                    }
                }
            }
        }

        // Component Tree Quick List
        item {
            Text("UI Tree (${nodes.size} elements)", color = tokens.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        items(nodes, key = { it.id }) { node ->
            val isSelected = node.id == selectedNode?.id
            LdroidSurfaceCard(
                accentBorder = isSelected,
                onClick = { selectedNodeId = node.id }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        StatusPill(node.type.displayName, if (isSelected) tokens.primaryAccent else tokens.secondaryAccent)
                        Text(
                            text = node.label,
                            color = tokens.textPrimary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${node.paddingDp}dp • ${node.colorTag}",
                        color = tokens.textMuted,
                        fontSize = 10.sp,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
            }
        }

        // Write files directly to workspace
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.writeVisualBuilderToProject(asXml = false) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent)
                ) {
                    Text("Save Compose .kt", fontSize = 12.sp)
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
}

@Composable
fun AiCommandPanel(
    uiState: LdroidUiState,
    viewModel: LdroidViewModel
) {
    val tokens = LocalLdroidTokens.current
    var promptValue by remember { mutableStateOf(TextFieldValue("")) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val panelHeightLines by animateIntAsState(
        targetValue = if (isFocused) 4 else 1,
        animationSpec = tween(durationMillis = 200)
    )

    LdroidSurfaceCard(accentBorder = isFocused) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        tint = tokens.primaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "M.A.X. AI Command Panel",
                        color = tokens.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = {
                            focusRequester.requestFocus()
                            promptValue = promptValue.copy(
                                selection = TextRange(0, promptValue.text.length)
                            )
                        },
                        modifier = Modifier.size(32.dp).testTag("ai_panel_select_all")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SelectAll,
                            contentDescription = "Select All",
                            tint = tokens.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            promptValue = TextFieldValue("", selection = TextRange.Zero)
                        },
                        modifier = Modifier.size(32.dp).testTag("ai_panel_clear")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Clear Prompt",
                            tint = tokens.error,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = { promptValue = TextFieldValue("", selection = TextRange.Zero) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Panel",
                            tint = tokens.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = promptValue,
                onValueChange = { promptValue = it },
                placeholder = { Text("Command M.A.X. to generate or modify UI components...", fontSize = 12.sp) },
                minLines = panelHeightLines,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        isFocused = state.isFocused
                    }
                    .testTag("ai_panel_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.isAiLoading) "M.A.X. is generating UI..." else "Ready for AI prompt",
                    color = if (uiState.isAiLoading) tokens.primaryAccent else tokens.textMuted,
                    fontSize = 11.sp
                )
                Button(
                    onClick = {
                        val text = promptValue.text.trim()
                        if (text.isNotEmpty()) {
                            viewModel.sendVisualBuilderAiCommand(text)
                            promptValue = TextFieldValue("", selection = TextRange.Zero)
                        }
                    },
                    enabled = promptValue.text.isNotBlank() && !uiState.isAiLoading,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp).testTag("ai_panel_send")
                ) {
                    Text("Execute AI", fontSize = 11.sp)
                }
            }
        }
    }
}
