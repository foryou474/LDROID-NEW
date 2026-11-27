package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VisualComponentType
import com.example.ui.components.LdroidSurfaceCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

/**
 * Tab 6 (⊞): Visual Builder — Drag-and-drop UI canvas with snap alignment guides
 * & dp spacing indicators, scrollable widget palette (+ Text, + Button, + Card,
 * + Image, + Row, + Column), and 2-way synced Property Inspector.
 */
@Composable
fun EditorTabVisualBuilder(
    uiState: LdroidUiState,
    viewModel: LdroidViewModel,
    modifier: Modifier = Modifier
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
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_ui_screen_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        // Scrollable Widget Palette (+ Text, + Button, + Card, + Image, + Row, + Column, + Counter)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Widget Palette", color = tokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    StatusPill("2-Way Kotlin/XML Synced", tokens.success)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        VisualComponentType.TEXT,
                        VisualComponentType.BUTTON,
                        VisualComponentType.CARD,
                        VisualComponentType.IMAGE,
                        VisualComponentType.ROW,
                        VisualComponentType.COLUMN,
                        VisualComponentType.COUNTER,
                        VisualComponentType.TEXT_FIELD,
                        VisualComponentType.SWITCH_CONTROL
                    ).forEach { type ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.addVisualNode(type) },
                            label = { Text("+ ${type.displayName}", fontSize = 11.sp) },
                            modifier = Modifier.testTag("palette_add_${type.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Interactive Snap Alignment Canvas + dp Spacing Indicators
        item {
            Surface(
                color = tokens.surface1,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, tokens.primaryAccent.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth().testTag("visual_builder_canvas")
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    // Snap Grid & Crosshair Alignment Guides
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        drawLine(
                            color = tokens.primaryAccent.copy(alpha = 0.25f),
                            start = Offset(size.width / 2f, 0f),
                            end = Offset(size.width / 2f, size.height),
                            pathEffect = dash,
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = tokens.border.copy(alpha = 0.5f),
                            start = Offset(0f, size.height / 2f),
                            end = Offset(size.width, size.height / 2f),
                            pathEffect = dash,
                            strokeWidth = 1.5f
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Snap Canvas • Long-press drag to reorder", color = tokens.primaryAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            StatusPill(if (draggedIndex != null) "Snapping 8dp Grid" else "16dp margins", tokens.info)
                        }

                        nodes.forEachIndexed { idx, node ->
                            val isSelected = node.id == selectedNode?.id
                            Surface(
                                color = if (isSelected) tokens.primaryAccent.copy(alpha = 0.16f) else tokens.surface2,
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
                                                val stepPx = 100f
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
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.DragHandle, contentDescription = "Drag to reorder", tint = tokens.textMuted, modifier = Modifier.size(16.dp))
                                        StatusPill(node.type.displayName, tokens.primaryAccent)
                                        Text(
                                            text = node.label,
                                            color = tokens.textPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${node.paddingDp}dp", color = tokens.secondaryAccent, fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily)
                                        IconButton(onClick = { viewModel.moveVisualNode(node.id, -1) }, modifier = Modifier.size(30.dp)) {
                                            Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up", tint = tokens.textMuted, modifier = Modifier.size(14.dp))
                                        }
                                        IconButton(onClick = { viewModel.moveVisualNode(node.id, 1) }, modifier = Modifier.size(30.dp)) {
                                            Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down", tint = tokens.textMuted, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                            if (idx < nodes.lastIndex) {
                                Text(
                                    text = "↕ ${node.paddingDp}dp spacing",
                                    color = tokens.textMuted,
                                    fontSize = 9.sp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2-Way Synced Property Inspector
        selectedNode?.let { node ->
            item {
                LdroidSurfaceCard(accentBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill("Property Inspector • ${node.type.displayName}", tokens.primaryAccent)
                        IconButton(onClick = { viewModel.removeVisualNode(node.id) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Delete component", tint = tokens.error, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = node.label,
                        onValueChange = { viewModel.updateVisualNode(node.id, label = it) },
                        label = { Text("Text / Label", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Padding: ${node.paddingDp}dp", color = tokens.textPrimary, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.updateVisualNode(node.id, paddingDp = (node.paddingDp - 4).coerceAtLeast(0)) },
                                modifier = Modifier.height(32.dp)
                            ) { Text("-4dp", fontSize = 10.sp) }
                            OutlinedButton(
                                onClick = { viewModel.updateVisualNode(node.id, paddingDp = (node.paddingDp + 4).coerceAtMost(48)) },
                                modifier = Modifier.height(32.dp)
                            ) { Text("+4dp", fontSize = 10.sp) }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Default", "Cyan", "Emerald", "Amber", "Purple", "Rose").forEach { tag ->
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

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.writeVisualBuilderToProject(asXml = false) },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) { Text("Sync to Kotlin .kt", fontSize = 12.sp) }
                OutlinedButton(
                    onClick = { viewModel.writeVisualBuilderToProject(asXml = true) },
                    modifier = Modifier.weight(1f).height(44.dp)
                ) { Text("Export XML Layout", fontSize = 12.sp) }
            }
        }
    }
}
