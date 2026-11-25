package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.audit.RoadmapRegistry
import com.example.data.preferences.LdroidSettingsState
import com.example.data.security.SecureCredentialVault
import com.example.model.AiProviderType
import com.example.ui.components.LdroidSurfaceCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LdroidThemes
import com.example.ui.theme.LdroidTokens
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    uiState: LdroidUiState,
    settings: LdroidSettingsState,
    viewModel: LdroidViewModel,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    var showThemeImportExport by remember { mutableStateOf(false) }
    var showRoadmapDialog by remember { mutableStateOf(false) }

    // Local draft state for BYOK AI config
    var selectedProvider by remember(settings.aiProvider) { mutableStateOf(settings.aiProvider) }
    var aiModel by remember(settings.aiModel) { mutableStateOf(settings.aiModel) }
    var aiBaseUrl by remember(settings.aiBaseUrl) { mutableStateOf(settings.aiBaseUrl) }
    var aiKeyInput by remember { mutableStateOf("") }
    var redactSecrets by remember(settings.redactSecretsInAi) { mutableStateOf(settings.redactSecretsInAi) }
    var githubPatInput by remember { mutableStateOf("") }
    var vaultRevision by remember { mutableIntStateOf(0) }

    // Editor prefs draft
    var fontSize by remember(settings.editorFontSizeSp) { mutableIntStateOf(settings.editorFontSizeSp) }
    var lineNumbers by remember(settings.showLineNumbers) { mutableStateOf(settings.showLineNumbers) }
    var wordWrap by remember(settings.wordWrap) { mutableStateOf(settings.wordWrap) }
    var autoSave by remember(settings.autoSaveEnabled) { mutableStateOf(settings.autoSaveEnabled) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
            .testTag("settings_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Settings & Themes",
                subtitle = "10 complete design systems, BYOK security vault, editor config & M50 audit"
            )
        }

        // 10 Built-in Themes with Instant Preview + Apply/Cancel (M02 / M22)
        item {
            LdroidSurfaceCard(accentBorder = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Theme Studio (10 Themes)", color = tokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    OutlinedButton(onClick = { showThemeImportExport = true }) {
                        Text("Import/Export", fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(LdroidThemes.all, key = { it.id.name }) { candidate ->
                        val isApplied = settings.themeId == candidate.id && uiState.previewThemeTokens == null
                        val isPreviewing = uiState.previewThemeTokens?.id == candidate.id
                        ThemePreviewCard(
                            candidate = candidate,
                            isSelected = isApplied || isPreviewing,
                            onClick = { viewModel.previewTheme(candidate) }
                        )
                    }
                }
                uiState.previewThemeTokens?.let { preview ->
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Previewing: ${preview.name}", color = tokens.primaryAccent, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { viewModel.previewTheme(null) }) { Text("Cancel") }
                        Button(
                            onClick = { viewModel.applyTheme(preview.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent)
                        ) { Text("Apply") }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reduced Motion", color = tokens.textPrimary, fontSize = 13.sp)
                    Switch(
                        checked = settings.reducedMotion,
                        onCheckedChange = { viewModel.setReducedMotion(it) }
                    )
                }
            }
        }

        // BYOK AI & GitHub Credential Vault (M09 / M32)
        item {
            val currentMask = remember(selectedProvider, vaultRevision, uiState.statusMessage) {
                SecureCredentialVault.maskCredential(
                    viewModel.vault.readSecret(viewModel.vault.slotForAiProvider(selectedProvider))
                )
            }
            val ghMask = remember(vaultRevision, uiState.statusMessage) {
                SecureCredentialVault.maskCredential(
                    viewModel.vault.readSecret(viewModel.vault.githubTokenSlot)
                )
            }
            val baseUrlWarning = remember(aiBaseUrl, selectedProvider) {
                validateApiBaseUrl(aiBaseUrl, selectedProvider)
            }
            val modelPresets = remember(selectedProvider) {
                when (selectedProvider) {
                    AiProviderType.GEMINI -> listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash")
                    AiProviderType.OPENAI_COMPATIBLE -> listOf("gpt-4o-mini", "gpt-4o", "o3-mini")
                    AiProviderType.ANTHROPIC -> listOf("claude-3-5-sonnet-latest", "claude-3-7-sonnet-latest", "claude-3-5-haiku-latest")
                }
            }

            LdroidSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("BYOK AI & Secrets Vault", color = tokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextButton(
                        onClick = {
                            aiModel = selectedProvider.defaultModel
                            aiBaseUrl = selectedProvider.defaultBaseUrl
                        }
                    ) {
                        Text("Reset Defaults", fontSize = 11.sp, color = tokens.primaryAccent)
                    }
                }
                Text("Keys are AES-256-GCM encrypted on-device and never committed or logged.", color = tokens.textMuted, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AiProviderType.entries.forEach { p ->
                        FilterChip(
                            selected = selectedProvider == p,
                            onClick = {
                                selectedProvider = p
                                if (p == settings.aiProvider) {
                                    aiModel = settings.aiModel
                                    aiBaseUrl = settings.aiBaseUrl
                                } else {
                                    aiModel = p.defaultModel
                                    aiBaseUrl = p.defaultBaseUrl
                                }
                            },
                            label = { Text(p.displayName.substringBefore(' '), fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Stored Key: $currentMask", color = tokens.info, fontSize = 12.sp, fontFamily = JetBrainsMonoFontFamily)
                OutlinedTextField(
                    value = aiKeyInput,
                    onValueChange = { aiKeyInput = it },
                    label = { Text("Enter ${selectedProvider.displayName} API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_ai_key_input")
                )
                Spacer(Modifier.height(6.dp))
                Text("Model Presets", color = tokens.textMuted, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(modelPresets) { preset ->
                        FilterChip(
                            selected = aiModel.trim() == preset,
                            onClick = { aiModel = preset },
                            label = { Text(preset, fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily) }
                        )
                    }
                }
                OutlinedTextField(
                    value = aiModel,
                    onValueChange = { aiModel = it },
                    label = { Text("Model ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = aiBaseUrl,
                    onValueChange = { aiBaseUrl = it },
                    label = { Text("API Base URL") },
                    isError = baseUrlWarning != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (baseUrlWarning != null) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = tokens.warning.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, tokens.warning.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = baseUrlWarning,
                                color = tokens.warning,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { aiBaseUrl = selectedProvider.defaultBaseUrl }) {
                                Text("Use API URL", fontSize = 11.sp, color = tokens.primaryAccent)
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Redact secrets before AI dispatch", color = tokens.textPrimary, fontSize = 12.sp)
                    Switch(checked = redactSecrets, onCheckedChange = { redactSecrets = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val cleanModel = aiModel.trim().ifBlank { selectedProvider.defaultModel }
                            val cleanUrl = aiBaseUrl.trim().trimEnd('/').ifBlank { selectedProvider.defaultBaseUrl }
                            aiModel = cleanModel
                            aiBaseUrl = cleanUrl
                            viewModel.saveAiProviderConfig(selectedProvider, cleanModel, cleanUrl, aiKeyInput.ifBlank { null }, redactSecrets)
                            aiKeyInput = ""
                            vaultRevision++
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("settings_save_ai_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent)
                    ) { Text("Save AI Config") }
                    OutlinedButton(
                        onClick = {
                            viewModel.clearAiProviderKey(selectedProvider)
                            vaultRevision++
                        },
                        modifier = Modifier.height(48.dp)
                    ) { Text("Clear Key") }
                }

                Spacer(Modifier.height(12.dp))
                Text("GitHub PAT (Optional for private repos): $ghMask", color = tokens.textMuted, fontSize = 11.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = githubPatInput,
                        onValueChange = { githubPatInput = it },
                        placeholder = { Text("ghp_...", fontSize = 12.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            viewModel.saveGitHubToken(githubPatInput)
                            githubPatInput = ""
                            vaultRevision++
                        },
                        modifier = Modifier.height(48.dp)
                    ) { Text("Save PAT") }
                }
            }
        }

        // Editor Preferences
        item {
            LdroidSurfaceCard {
                Text("Code Editor Preferences", color = tokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Font Size", color = tokens.textMuted, fontSize = 12.sp)
                    StatusPill("${fontSize}sp", tokens.primaryAccent)
                }
                Slider(
                    value = fontSize.toFloat(),
                    onValueChange = { fontSize = it.roundToInt().coerceIn(10, 22) },
                    onValueChangeFinished = {
                        viewModel.updateEditorSettings(fontSize, settings.tabWidth, autoSave, lineNumbers, wordWrap)
                    },
                    valueRange = 10f..22f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = tokens.primaryAccent,
                        activeTrackColor = tokens.primaryAccent,
                        inactiveTrackColor = tokens.border,
                        activeTickColor = tokens.onPrimaryAccent.copy(alpha = 0.6f),
                        inactiveTickColor = tokens.textMuted.copy(alpha = 0.5f)
                    )
                )
                TogglePrefRow("Line Numbers", lineNumbers) {
                    lineNumbers = it
                    viewModel.updateEditorSettings(fontSize, settings.tabWidth, autoSave, lineNumbers, wordWrap)
                }
                TogglePrefRow("Word Wrap", wordWrap) {
                    wordWrap = it
                    viewModel.updateEditorSettings(fontSize, settings.tabWidth, autoSave, lineNumbers, wordWrap)
                }
                TogglePrefRow("Auto-Save Policy", autoSave) {
                    autoSave = it
                    viewModel.updateEditorSettings(fontSize, settings.tabWidth, autoSave, lineNumbers, wordWrap)
                }
            }
        }

        // Accessibility Contrast & M01-M50 Roadmap Audit (M47 / M50)
        item {
            val contrast = LdroidThemes.contrastRatio(tokens.textPrimary, tokens.background)
            LdroidSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Accessibility & M50 Audit", color = tokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "Primary text contrast: ${String.format("%.2f", contrast)}:1 (WCAG AA >= 4.5:1)",
                            color = if (contrast >= 4.5) tokens.success else tokens.error,
                            fontSize = 12.sp
                        )
                        Text("App ID: ${BuildConfig.APPLICATION_ID} • v${BuildConfig.VERSION_NAME}", color = tokens.textMuted, fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { showRoadmapDialog = true }, modifier = Modifier.testTag("view_roadmap_audit_btn")) {
                        Text("M01–M50")
                    }
                }
            }
        }
    }

    if (showThemeImportExport) {
        var jsonText by remember { mutableStateOf(LdroidThemes.exportToJson(tokens)) }
        AlertDialog(
            onDismissRequest = { showThemeImportExport = false },
            title = { Text("Theme JSON Import / Export") },
            text = {
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.importThemeJson(jsonText)
                    showThemeImportExport = false
                }) { Text("Validate & Import") }
            },
            dismissButton = {
                TextButton(onClick = { showThemeImportExport = false }) { Text("Close") }
            }
        )
    }

    if (showRoadmapDialog) {
        AlertDialog(
            onDismissRequest = { showRoadmapDialog = false },
            title = { Text("M01–M50 Traceability Matrix") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(RoadmapRegistry.m01ToM50Audit) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${item.id} — ${item.title}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(item.summary, fontSize = 11.sp, color = tokens.textMuted)
                            }
                            StatusPill(
                                item.status,
                                if (item.status == "VERIFIED") tokens.success else tokens.info
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoadmapDialog = false }) { Text("Done") }
            }
        )
    }
}

@Composable
private fun ThemePreviewCard(
    candidate: LdroidTokens,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(width = 148.dp, height = 104.dp)
            .clickable(onClick = onClick)
            .testTag("theme_card_${candidate.id.name}"),
        color = candidate.background,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) candidate.primaryAccent else candidate.border
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(candidate.primaryAccent))
                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(candidate.secondaryAccent))
                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(candidate.surface2).border(1.dp, candidate.border, CircleShape))
            }
            Column {
                Text(candidate.name, color = candidate.textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                Text(if (candidate.isDark) "Dark Mode" else "Light Mode", color = candidate.textMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun TogglePrefRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val tokens = LocalLdroidTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = tokens.textPrimary, fontSize = 13.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun validateApiBaseUrl(url: String, provider: AiProviderType): String? {
    val clean = url.trim().lowercase()
    if (clean.isEmpty()) return null
    if (!clean.startsWith("https://") && !clean.startsWith("http://")) {
        return "Base URL should start with https:// (expected ${provider.defaultBaseUrl})."
    }
    if (clean.contains("aistudio.google.com") || clean.contains("platform.openai.com") || clean.contains("console.anthropic.com")) {
        return "Web console URLs return HTML instead of JSON. Use ${provider.defaultBaseUrl}."
    }
    return null
}
