package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Css
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlutterDash
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WorkspaceFileItem
import com.example.ui.components.formatBytes
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LdroidTokens
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState

private data class TreeDisplayEntry(
    val item: WorkspaceFileItem,
    val depth: Int
)

@Composable
fun EditorFileExplorer(
    uiState: LdroidUiState,
    onSearchChange: (String) -> Unit,
    onRefreshWorkspace: () -> Unit,
    onOpenFile: (String) -> Unit,
    onToggleSelect: (String) -> Unit,
    onNewFileClick: () -> Unit,
    onNewFolderClick: () -> Unit,
    onRenameClick: (String) -> Unit,
    onCopyClick: (String) -> Unit,
    onMoveClick: (String) -> Unit,
    onDeleteConfirm: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    var expandedFolders by remember(uiState.activeProject?.id) {
        mutableStateOf(setOf("app", "app/src", "app/src/main", "app/src/main/java"))
    }
    var pendingDeletePaths by remember { mutableStateOf<List<String>?>(null) }

    val treeEntries = remember(uiState.workspaceFiles, expandedFolders, uiState.fileSearchQuery) {
        buildVisibleTreeEntries(
            files = uiState.workspaceFiles,
            expandedFolders = expandedFolders,
            isSearching = uiState.fileSearchQuery.isNotBlank()
        )
    }

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
                Text(
                    text = "EXPLORER",
                    color = tokens.primaryAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onRefreshWorkspace, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Workspace", tint = tokens.textMuted, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onNewFileClick, modifier = Modifier.size(36.dp).testTag("explorer_new_file")) {
                        Icon(Icons.Filled.Add, contentDescription = "New File", tint = tokens.textPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onNewFolderClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = "New Folder", tint = tokens.secondaryAccent, modifier = Modifier.size(18.dp))
                    }
                    if (uiState.selectedFilePaths.isNotEmpty()) {
                        IconButton(
                            onClick = { pendingDeletePaths = uiState.selectedFilePaths.toList() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete selected", tint = tokens.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            OutlinedTextField(
                value = uiState.fileSearchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search workspace files...", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (treeEntries.isEmpty()) {
                    item {
                        Text(
                            text = if (uiState.fileSearchQuery.isBlank()) "No files in workspace." else "No matching files.",
                            color = tokens.textMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    items(treeEntries, key = { it.item.relativePath }) { entry ->
                        val item = entry.item
                        val isSelected = item.relativePath in uiState.selectedFilePaths
                        val isActive = uiState.activeTabPath == item.relativePath
                        val isExpanded = item.relativePath in expandedFolders
                        var showMenu by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isActive) tokens.primaryAccent.copy(alpha = 0.14f) else tokens.surface2,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (item.isDirectory) {
                                        expandedFolders = if (isExpanded) {
                                            expandedFolders - item.relativePath
                                        } else {
                                            expandedFolders + item.relativePath
                                        }
                                    } else {
                                        onOpenFile(item.relativePath)
                                    }
                                }
                                .padding(
                                    start = (6 + entry.depth * 14).dp,
                                    end = 4.dp,
                                    top = 4.dp,
                                    bottom = 4.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.isDirectory) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                                    contentDescription = if (isExpanded) "Collapse folder" else "Expand folder",
                                    tint = tokens.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onToggleSelect(item.relativePath) },
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            val (icon, tint) = iconForWorkspaceItem(item, isExpanded, tokens)
                            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    color = if (isActive) tokens.primaryAccent else tokens.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (item.isDirectory || isActive) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!item.isDirectory) {
                                    Text(formatBytes(item.sizeBytes), color = tokens.textMuted, fontSize = 10.sp)
                                }
                            }
                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "File actions for ${item.name}", tint = tokens.textMuted, modifier = Modifier.size(16.dp))
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    if (!item.isDirectory) {
                                        DropdownMenuItem(
                                            text = { Text("Open in Editor") },
                                            onClick = { showMenu = false; onOpenFile(item.relativePath) }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Rename") },
                                        onClick = { showMenu = false; onRenameClick(item.relativePath) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Copy To...") },
                                        onClick = { showMenu = false; onCopyClick(item.relativePath) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Move To...") },
                                        onClick = { showMenu = false; onMoveClick(item.relativePath) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = tokens.error) },
                                        onClick = {
                                            showMenu = false
                                            pendingDeletePaths = listOf(item.relativePath)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDeletePaths?.let { targets ->
        AlertDialog(
            onDismissRequest = { pendingDeletePaths = null },
            title = { Text("Delete ${targets.size} item(s)?") },
            text = {
                Text(
                    "Permanently delete:\n${targets.take(5).joinToString("\n")}${if (targets.size > 5) "\n..." else ""}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteConfirm(targets)
                        pendingDeletePaths = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePaths = null }) { Text("Cancel") }
            }
        )
    }
}

private fun buildVisibleTreeEntries(
    files: List<WorkspaceFileItem>,
    expandedFolders: Set<String>,
    isSearching: Boolean
): List<TreeDisplayEntry> {
    if (isSearching) {
        return files.map { item ->
            val depth = item.relativePath.count { it == '/' }
            TreeDisplayEntry(item, depth)
        }
    }

    val byParent = files.groupBy { item ->
        val idx = item.relativePath.lastIndexOf('/')
        if (idx < 0) "" else item.relativePath.substring(0, idx)
    }

    val result = mutableListOf<TreeDisplayEntry>()
    fun walk(parentPath: String, depth: Int) {
        val children = byParent[parentPath].orEmpty()
            .sortedWith(compareBy<WorkspaceFileItem> { !it.isDirectory }.thenBy { it.name.lowercase() })
        for (child in children) {
            result.add(TreeDisplayEntry(child, depth))
            if (child.isDirectory && child.relativePath in expandedFolders) {
                walk(child.relativePath, depth + 1)
            }
        }
    }
    walk("", 0)
    return result
}

private fun iconForWorkspaceItem(
    item: WorkspaceFileItem,
    expanded: Boolean,
    tokens: LdroidTokens
): Pair<ImageVector, Color> {
    if (item.isDirectory) {
        return (if (expanded) Icons.Filled.FolderOpen else Icons.Filled.Folder) to tokens.warning
    }
    return when (item.extension.lowercase()) {
        "kt" -> Icons.Filled.Code to tokens.primaryAccent
        "kts", "gradle" -> Icons.Filled.Build to tokens.warning
        "java" -> Icons.Filled.Coffee to tokens.warning
        "dart" -> Icons.Filled.FlutterDash to tokens.info
        "tsx", "ts", "jsx", "js" -> Icons.Filled.Javascript to tokens.secondaryAccent
        "xml" -> Icons.Filled.SettingsApplications to tokens.secondaryAccent
        "html", "htm" -> Icons.Filled.Html to tokens.info
        "css" -> Icons.Filled.Css to tokens.tertiaryAccent
        "json", "toml", "yml", "yaml" -> Icons.Filled.DataObject to tokens.success
        "png", "jpg", "jpeg", "webp" -> Icons.Filled.Image to tokens.tertiaryAccent
        "zip", "apk", "aab", "jar" -> Icons.Filled.FolderZip to tokens.warning
        else -> Icons.Filled.Description to tokens.textMuted
    }
}
