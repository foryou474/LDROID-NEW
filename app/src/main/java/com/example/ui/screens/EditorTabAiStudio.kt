package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiVariantProposal
import com.example.model.DiagnosticSeverity
import com.example.ui.components.LdroidSurfaceCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

/**
 * Tab 3 (✨): AI Studio — Central Orchestrator & ChatGPT-Style Conversational UI
 * with Collapsible UI Variant Cards (v1/v2/v3), Diff Summaries, Preview, ✓ Apply,
 * Smart Error Sentinel alert with ✨ Auto-Fix Now + 📍 Jump to Error, and Zero-Code starters.
 */
@Composable
fun EditorTabAiStudio(
    uiState: LdroidUiState,
    viewModel: LdroidViewModel,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    val listState = rememberLazyListState()
    val firstError = uiState.diagnostics.firstOrNull {
        it.severity == DiagnosticSeverity.ERROR || it.severity == DiagnosticSeverity.SECRET
    }
    val errorCount = uiState.diagnostics.count {
        it.severity == DiagnosticSeverity.ERROR || it.severity == DiagnosticSeverity.SECRET
    }

    LaunchedEffect(uiState.aiMessages.size) {
        if (uiState.aiMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.aiMessages.lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Top Orchestrator Header & Clear Conversation
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = tokens.primaryAccent, modifier = Modifier.size(16.dp))
                Text("AI Orchestrator", color = tokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                StatusPill(uiState.activeModelChip, tokens.primaryAccent)
            }
            if (uiState.aiMessages.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearAiConversation() }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear AI Chat", tint = tokens.textMuted, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Smart Error Sentinel Real-Time Banner
        if (firstError != null) {
            Surface(
                color = tokens.error.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, tokens.error.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth().testTag("smart_error_sentinel_banner")
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Warning, contentDescription = "Smart Error Sentinel", tint = tokens.error, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Smart Error Sentinel • $errorCount Issue(s) Active",
                            color = tokens.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "${firstError.filePath.substringAfterLast('/')}:${firstError.line} — ${firstError.message}",
                        color = tokens.textPrimary,
                        fontSize = 11.sp,
                        fontFamily = JetBrainsMonoFontFamily,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.autoFixAllDiagnostics() },
                            colors = ButtonDefaults.buttonColors(containerColor = tokens.error, contentColor = tokens.onPrimaryAccent),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(34.dp).testTag("sentinel_autofix_now_btn")
                        ) {
                            Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("✨ Auto-Fix Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { viewModel.jumpToFileLine(firstError.filePath, firstError.line) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(34.dp).testTag("sentinel_jump_error_btn")
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("📍 Jump to Error", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (uiState.isAiLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = tokens.primaryAccent)
        }

        // ChatGPT-Style Message List + Collapsible Variant Cards (v1/v2/v3)
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("ai_studio_feed"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            if (uiState.aiMessages.isEmpty()) {
                item {
                    LdroidSurfaceCard(accentBorder = true) {
                        Text(
                            text = "Zero-Code Autonomous App Builder",
                            color = tokens.primaryAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Describe any feature or screen in plain language below. A single prompt generates v1/v2/v3 UI & code variants and orchestrates Code EG, Terminal verification, Live Preview & Visual Builder together.",
                            color = tokens.textMuted,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Expense Tracker Dashboard",
                                "Workout Timer with Counter",
                                "Minimal Notes Reader UI",
                                "Sign-In + Onboarding Card"
                            ).forEach { starter ->
                                FilterChip(
                                    selected = false,
                                    onClick = { viewModel.sendOrchestratedAiPrompt(starter) },
                                    label = { Text("✨ $starter", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            } else {
                items(uiState.aiMessages, key = { it.id }) { msg ->
                    val isUser = msg.isUser
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Surface(
                            color = if (isUser) tokens.primaryAccent.copy(alpha = 0.16f) else tokens.surface1,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                when {
                                    msg.isError -> tokens.error
                                    isUser -> tokens.primaryAccent.copy(alpha = 0.55f)
                                    else -> tokens.border
                                }
                            ),
                            modifier = Modifier.fillMaxWidth(if (isUser) 0.88f else 0.98f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (isUser) "You" else "LDROID AI Studio",
                                    color = if (isUser) tokens.primaryAccent else tokens.textMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = msg.text,
                                    color = if (msg.isError) tokens.error else tokens.textPrimary,
                                    fontSize = 12.sp
                                )

                                // Collapsible UI Variant Cards (v1 / v2 / v3)
                                if (msg.variants.isNotEmpty()) {
                                    msg.variants.forEach { variant ->
                                        AiVariantCard(
                                            variant = variant,
                                            onPreview = { viewModel.previewAiVariant(variant) },
                                            onApply = { viewModel.applyAiVariant(msg.id, variant) }
                                        )
                                    }
                                }

                                // Direct Proposed File Patches from remote BYOK models
                                if (msg.proposedChanges.isNotEmpty()) {
                                    msg.proposedChanges.forEach { patch ->
                                        Surface(
                                            color = tokens.surface2,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, tokens.border),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = patch.relativePath.substringAfterLast('/'),
                                                    color = tokens.primaryAccent,
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Button(
                                                    onClick = { viewModel.applyAiProposedChange(msg.id, patch) },
                                                    enabled = !patch.isApplied,
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text(if (patch.isApplied) "✓ Applied" else "✓ Apply", fontSize = 10.sp)
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
        }
    }
}

@Composable
private fun AiVariantCard(
    variant: AiVariantProposal,
    onPreview: () -> Unit,
    onApply: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    var expanded by remember(variant.id) { mutableStateOf(variant.variantTag == "v1") }

    Surface(
        color = tokens.surface2,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (variant.isApplied) tokens.success else tokens.border),
        modifier = Modifier.fillMaxWidth().testTag("ai_variant_card_${variant.variantTag}")
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    StatusPill(variant.variantTag, if (variant.isApplied) tokens.success else tokens.primaryAccent)
                    Text(
                        text = variant.title,
                        color = tokens.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse variant" else "Expand variant",
                    tint = tokens.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(variant.diffSummary, color = tokens.textMuted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onPreview,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(36.dp).testTag("variant_preview_${variant.variantTag}")
                        ) {
                            Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Preview", fontSize = 11.sp)
                        }
                        Button(
                            onClick = onApply,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (variant.isApplied) tokens.success else tokens.primaryAccent,
                                contentColor = tokens.onPrimaryAccent
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(36.dp).testTag("variant_apply_${variant.variantTag}")
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (variant.isApplied) "✓ Applied" else "✓ Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
