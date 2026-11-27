package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LdroidSurfaceCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

/**
 * Tab 2 ({/}): Code Live Preview — Pure XML code-linked canvas (+ activity_main.xml),
 * view hierarchy breadcrumb (LinearLayout > CardView > MaterialButton),
 * tap-to-jump to exact XML line (Jump to Line X ↗), resource badges, and { } Clean formatter.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorTabCodeLivePreview(
    uiState: LdroidUiState,
    viewModel: LdroidViewModel,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    val syncSnap = uiState.syncSnapshot
    val nodes = syncSnap.xmlNodes
    val selectedNode = nodes.firstOrNull { it.id == syncSnap.selectedXmlNodeId } ?: nodes.lastOrNull()
    val breadcrumb = if (nodes.isEmpty()) {
        "LinearLayout > CardView > MaterialButton"
    } else {
        nodes.take(4).joinToString(" > ") { it.tagName }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("tab_code_live_preview"),
        contentPadding = PaddingValues(bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header: + activity_main.xml + { } Clean Formatter
        item {
            Surface(
                color = tokens.surface1,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, tokens.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Code, contentDescription = null, tint = tokens.primaryAccent, modifier = Modifier.size(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "+ ${syncSnap.activeXmlPath.substringAfterLast('/')}",
                                color = tokens.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                            Text(
                                text = breadcrumb,
                                color = tokens.primaryAccent,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.formatActiveXmlClean() },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(38.dp).testTag("xml_clean_format_btn")
                    ) {
                        Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("{ } Clean", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Selected Node Quick Jump Banner
        selectedNode?.let { node ->
            item {
                Surface(
                    color = tokens.primaryAccent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, tokens.primaryAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "<${node.tagName}> ${node.viewId?.let { "@+id/$it" } ?: ""}",
                                color = tokens.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Synchronized XML AST node • Line ${node.lineNumber}",
                                color = tokens.textMuted,
                                fontSize = 10.sp
                            )
                        }
                        Button(
                            onClick = { viewModel.jumpToXmlLine(node.lineNumber) },
                            colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(38.dp).testTag("xml_jump_to_line_btn")
                        ) {
                            Text("Jump to Line ${node.lineNumber}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.size(4.dp))
                            Icon(Icons.AutoMirrored.Filled.CallMade, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Visual Canvas Representation of XML Tree Nodes
        item {
            LdroidSurfaceCard(accentBorder = false) {
                Text("Code-Linked XML Canvas", color = tokens.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    nodes.forEach { node ->
                        val isSelected = node.id == selectedNode?.id
                        val indentDp = (node.depth * 14).coerceAtMost(42).dp
                        Surface(
                            color = if (isSelected) tokens.primaryAccent.copy(alpha = 0.14f) else tokens.surface2,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isSelected) tokens.primaryAccent else tokens.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = indentDp)
                                .clickable { viewModel.syncEngine.selectXmlNode(node.id) }
                                .testTag("xml_node_${node.tagName}")
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "<${node.tagName}>",
                                        color = tokens.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Line ${node.lineNumber} ↗",
                                        color = tokens.primaryAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable { viewModel.jumpToXmlLine(node.lineNumber) }
                                    )
                                }
                                // Resource Badges
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    node.textAttr?.let { txt ->
                                        StatusPill("android:text=\"$txt\"", tokens.success)
                                    }
                                    node.toolsTextAttr?.let { toolsTxt ->
                                        StatusPill("tools:text=\"$toolsTxt\"", tokens.info)
                                    }
                                    node.colorAttr?.let { colorVal ->
                                        StatusPill(colorVal, tokens.tertiaryAccent)
                                    }
                                    if (node.viewId != null) {
                                        StatusPill("@+id/${node.viewId}", tokens.textMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
