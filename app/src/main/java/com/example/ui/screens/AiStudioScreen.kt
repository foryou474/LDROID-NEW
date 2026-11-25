package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.LdroidSettingsState
import com.example.data.security.SecureCredentialVault
import com.example.model.PrimaryDestination
import com.example.ui.components.DiffLinesBlock
import com.example.ui.components.LdroidSurfaceCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

@Composable
fun AiStudioScreen(
    uiState: LdroidUiState,
    settings: LdroidSettingsState,
    viewModel: LdroidViewModel,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    val configuredKey = remember(settings.aiProvider, uiState.statusMessage) {
        viewModel.vault.readSecret(viewModel.vault.slotForAiProvider(settings.aiProvider))
    }
    var promptText by remember { mutableStateOf("") }
    var attachActiveFile by remember { mutableStateOf(true) }
    var showArchGraph by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "AI Studio (BYOK)",
            subtitle = "${settings.aiProvider.displayName} • ${settings.aiModel}",
            trailing = {
                FilterChip(
                    selected = showArchGraph,
                    onClick = { showArchGraph = !showArchGraph },
                    label = { Text("Architecture", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Filled.AccountTree, contentDescription = null, modifier = Modifier.size(15.dp)) }
                )
            }
        )

        // BYOK Key Status Banner
        LdroidSurfaceCard(accentBorder = configuredKey == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (configuredKey != null) {
                            "Encrypted Key Ready (${SecureCredentialVault.maskCredential(configuredKey)})"
                        } else {
                            "No BYOK API Key Configured"
                        },
                        color = if (configuredKey != null) tokens.success else tokens.warning,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (settings.redactSecretsInAi) {
                            "Secret redaction active before dispatch • All patches require approval"
                        } else {
                            "All AI file edits still require explicit diff approval"
                        },
                        color = tokens.textMuted,
                        fontSize = 11.sp
                    )
                }
                OutlinedButton(
                    onClick = { viewModel.selectPrimaryTab(PrimaryDestination.SETTINGS) },
                    modifier = Modifier.testTag("ai_configure_key_btn")
                ) {
                    Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Keys")
                }
            }
        }

        if (showArchGraph) {
            LdroidSurfaceCard {
                Text(
                    "Project Architecture Graph (${uiState.architectureNodes.size} nodes)",
                    color = tokens.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                if (uiState.architectureNodes.isEmpty()) {
                    Text("No Kotlin/Gradle nodes detected in workspace.", color = tokens.textMuted, fontSize = 12.sp)
                } else {
                    val nodes = uiState.architectureNodes.take(6)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val centerX = size.width / 2f
                        val topY = 28f
                        drawCircle(color = tokens.primaryAccent, radius = 16f, center = Offset(centerX, topY))
                        val step = size.width / (nodes.size + 1)
                        nodes.forEachIndexed { idx, _ ->
                            val nx = step * (idx + 1)
                            val ny = size.height - 32f
                            drawLine(
                                color = tokens.border,
                                start = Offset(centerX, topY + 16f),
                                end = Offset(nx, ny - 14f),
                                strokeWidth = 3f
                            )
                            drawCircle(color = tokens.secondaryAccent, radius = 12f, center = Offset(nx, ny))
                        }
                    }
                    nodes.forEach { n ->
                        Text(
                            "• [${n.kind}] ${n.name}",
                            color = tokens.textMuted,
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    }
                }
            }
        }

        // Chat & Multi-File Diff Thread
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (uiState.aiMessages.isEmpty()) {
                item {
                    LdroidSurfaceCard {
                        Text("Ask AI to explain code, audit Gradle/Manifest, or propose reviewable multi-file patches.", color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { promptText = "Explain the architecture and suggest improvements for the active file." }) {
                                Text("Explain Active File", fontSize = 11.sp)
                            }
                            OutlinedButton(onClick = { promptText = "Review this code for security, Compose state bugs, and accessibility issues." }) {
                                Text("Security & A11y Audit", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            items(uiState.aiMessages, key = { it.id }) { msg ->
                Surface(
                    color = if (msg.isUser) tokens.elevatedSurface else tokens.surface1,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        when {
                            msg.isError -> tokens.error
                            msg.isUser -> tokens.primaryAccent.copy(alpha = 0.5f)
                            else -> tokens.border
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (msg.isUser) "YOU" else settings.aiProvider.displayName.uppercase(),
                            color = if (msg.isError) tokens.error else if (msg.isUser) tokens.primaryAccent else tokens.secondaryAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                        Text(
                            text = msg.text,
                            color = if (msg.isError) tokens.error else tokens.textPrimary,
                            fontSize = 13.sp
                        )

                        // Reviewable Multi-File Patch Approvals (M10)
                        msg.proposedChanges.forEach { change ->
                            Surface(
                                color = tokens.surface2,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, tokens.border)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(change.relativePath, color = tokens.primaryAccent, fontSize = 12.sp, fontFamily = JetBrainsMonoFontFamily)
                                        if (change.isApplied) {
                                            StatusPill("Applied", tokens.success)
                                        }
                                    }
                                    val diffLines = viewModel.fileManager.computeLineDiff(
                                        change.originalContent ?: "",
                                        change.proposedContent
                                    )
                                    DiffLinesBlock(diffLines.take(30))
                                    if (!change.isApplied) {
                                        Button(
                                            onClick = { viewModel.applyAiProposedChange(msg.id, change) },
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = tokens.success, contentColor = tokens.onPrimaryAccent)
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.size(6.dp))
                                            Text("Approve & Apply Patch")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isAiLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = tokens.primaryAccent)
        }

        // Prompt Input Area
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = attachActiveFile, onCheckedChange = { attachActiveFile = it })
                Text(
                    text = "Attach active file (${uiState.activeTabPath ?: "none"})",
                    color = tokens.textMuted,
                    fontSize = 12.sp
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    placeholder = { Text("Ask BYOK AI or request code patch...") },
                    maxLines = 3,
                    modifier = Modifier.weight(1f).testTag("ai_prompt_input")
                )
                IconButton(
                    onClick = {
                        if (promptText.isNotBlank()) {
                            viewModel.sendAiPrompt(promptText, attachActiveFile)
                            promptText = ""
                        }
                    },
                    enabled = !uiState.isAiLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .background(tokens.primaryAccent, RoundedCornerShape(12.dp))
                        .testTag("ai_send_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send prompt", tint = tokens.onPrimaryAccent)
                }
            }
        }
    }
}
