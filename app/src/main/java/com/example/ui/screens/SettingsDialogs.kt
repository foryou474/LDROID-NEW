package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audit.RoadmapRegistry
import com.example.data.audit.VerificationReport
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LdroidThemes
import com.example.ui.theme.LdroidTokens
import com.example.ui.theme.LocalLdroidTokens

@Composable
fun ThemeImportExportDialog(
    currentTokens: LdroidTokens,
    onImportJson: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var jsonText by remember(currentTokens.id) { mutableStateOf(LdroidThemes.exportToJson(currentTokens)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme JSON Import / Export") },
        text = {
            OutlinedTextField(
                value = jsonText,
                onValueChange = { jsonText = it },
                modifier = Modifier.fillMaxWidth().height(220.dp),
                textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
            )
        },
        confirmButton = {
            Button(onClick = {
                onImportJson(jsonText)
                onDismiss()
            }) { Text("Validate & Import") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun RoadmapAuditDialog(
    report: VerificationReport?,
    isRunning: Boolean,
    onRunVerification: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    val items = report?.items ?: RoadmapRegistry.m01ToM50Audit

    LaunchedEffect(Unit) {
        if (report == null && !isRunning) {
            onRunVerification()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("System Verification (M01–M50)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = tokens.surface2,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, tokens.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (report != null) {
                            Text(
                                text = "${report.passedCount}/${report.items.size} VERIFIED • ${report.failedCount} FAILED • ${report.durationMs}ms",
                                color = if (report.failedCount == 0) tokens.success else tokens.warning,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "Running live automated verification across storage, AST, codegen, AI & contrast...",
                                color = tokens.info,
                                fontSize = 11.sp
                            )
                        }
                        if (isRunning) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = tokens.primaryAccent)
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("${item.id} — ${item.title}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(item.summary, fontSize = 11.sp, color = tokens.textMuted)
                            }
                            StatusPill(
                                item.status,
                                when (item.status) {
                                    "VERIFIED" -> tokens.success
                                    "FAILED" -> tokens.error
                                    else -> tokens.info
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRunVerification,
                enabled = !isRunning,
                modifier = Modifier.testTag("run_system_verification_btn")
            ) { Text(if (isRunning) "Verifying..." else "Run System Verification") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ResetSettingsConfirmDialog(
    onConfirmReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset All Settings?") },
        text = {
            Text(
                "Restore theme, editor font size, tab spacing, AI model defaults, and GitHub branch name to factory defaults? Encrypted keys in Keystore are kept unless cleared individually.",
                color = tokens.textPrimary,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmReset()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = tokens.error)
            ) { Text("Reset All") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ApiKeyGuideDialog(
    provider: com.example.model.AiProviderType,
    onDismiss: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    val (steps, keyPrefix, apiEndpoint) = when (provider) {
        com.example.model.AiProviderType.GEMINI -> Triple(
            listOf(
                "1. Sign in to Google AI Studio (aistudio.google.com) in your browser.",
                "2. Select 'Get API key' -> 'Create API key' in a Google Cloud project.",
                "3. Copy the generated key and paste it into the API Key field in M.A.X. Studio.",
                "4. Keep Base URL as https://generativelanguage.googleapis.com."
            ),
            "AIza...",
            provider.defaultBaseUrl
        )
        com.example.model.AiProviderType.OPENAI_COMPATIBLE -> Triple(
            listOf(
                "1. Open platform.openai.com/api-keys and create a new secret key.",
                "2. Ensure billing/credits are active on your OpenAI organization.",
                "3. Paste the key in M.A.X. Studio and keep Base URL as https://api.openai.com."
            ),
            "sk-...",
            provider.defaultBaseUrl
        )
        com.example.model.AiProviderType.ANTHROPIC -> Triple(
            listOf(
                "1. Open console.anthropic.com -> API Keys -> Create Key.",
                "2. Copy the key immediately (Anthropic shows it only once).",
                "3. Paste it in M.A.X. Studio and keep Base URL as https://api.anthropic.com."
            ),
            "sk-ant-...",
            provider.defaultBaseUrl
        )
        com.example.model.AiProviderType.CUSTOM -> Triple(
            listOf(
                "1. Copy your OpenRouter (openrouter.ai/keys), Groq, Together AI, or self-hosted endpoint key.",
                "2. Enter the OpenAI-compatible Base URL (e.g., https://openrouter.ai/api/v1).",
                "3. Paste your API key and tap 'Fetch Available Models' to populate the live model list."
            ),
            "sk-or-... / Bearer token",
            provider.defaultBaseUrl
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Get ${provider.displayName} API Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.forEach { step ->
                    Text(step, color = tokens.textPrimary, fontSize = 12.sp)
                }
                Text(
                    "Expected key format: $keyPrefix\nExpected API URL: $apiEndpoint",
                    color = tokens.info,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp
                )
                Text(
                    "Tip: Use 'Fetch Available Models' or 'Test Connection' right after pasting your key.",
                    color = tokens.textMuted,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Got It") }
        }
    )
}

@Composable
fun SearchableModelPickerDialog(
    title: String,
    models: List<String>,
    currentModel: String,
    onSelectModel: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    var search by remember { mutableStateOf("") }
    val filtered = remember(models, search) {
        val q = search.trim().lowercase()
        if (q.isEmpty()) models else models.filter { it.lowercase().contains(q) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search ${models.size} model(s)...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (filtered.isEmpty()) {
                    Text(
                        text = if (models.isEmpty()) "No models fetched yet. Enter your API key and tap 'Fetch Available Models'."
                        else "No models match \"$search\".",
                        color = tokens.textMuted,
                        fontSize = 12.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filtered, key = { it }) { modelId ->
                            val isSelected = modelId == currentModel
                            Surface(
                                color = if (isSelected) tokens.primaryAccent.copy(alpha = 0.16f) else tokens.surface2,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) tokens.primaryAccent else tokens.border
                                ),
                                onClick = {
                                    onSelectModel(modelId)
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = modelId,
                                    color = if (isSelected) tokens.primaryAccent else tokens.textPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun SettingsJsonDialog(
    initialJson: String,
    onImportJson: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    val clipboard = LocalClipboardManager.current
    var jsonText by remember(initialJson) { mutableStateOf(initialJson) }
    var copiedNotice by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export / Import All Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Includes theme, editor preferences, AI provider/model presets, and GitHub branch. API keys stay excluded for security.",
                    color = tokens.textMuted,
                    fontSize = 11.sp
                )
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    modifier = Modifier.fillMaxWidth().height(230.dp),
                    textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                )
                if (copiedNotice) {
                    Text("Copied settings JSON to clipboard", color = tokens.success, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onImportJson(jsonText)
                onDismiss()
            }) { Text("Import JSON") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(jsonText))
                    copiedNotice = true
                }) { Text("Copy") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

