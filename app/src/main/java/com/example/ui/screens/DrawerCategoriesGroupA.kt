package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.DEFAULT_MODEL_CHIPS
import com.example.data.preferences.DEFAULT_QUICK_SYMBOLS
import com.example.data.preferences.LdroidSettingsState
import com.example.model.AiProviderType
import com.example.model.EditorFontFamilyOption
import com.example.model.TabIconPreset
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LdroidThemeId
import com.example.ui.theme.LdroidThemes
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

/**
 * Group A of the Left Sidebar Navigation Drawer:
 * 1. AI Setup (Encrypted AES-256-GCM BYOK Vault, Fetch Models, Model Chips, Safety Toggles)
 * 2. Code View Settings (Font Family, Size, Line Numbers, Word Wrap, Tab Width, Bracket Matching, Quick Symbols)
 * 3. Personalization (10 Built-in IDE Themes, Custom Accents, 7-Tab Icon Customization)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrawerCategoryAiSetupContent(
    uiState: LdroidUiState,
    settings: LdroidSettingsState,
    viewModel: LdroidViewModel
) {
    val tokens = LocalLdroidTokens.current
    var selectedProvider by remember(settings.aiProvider) { mutableStateOf(settings.aiProvider) }
    var apiKeyInput by remember(selectedProvider) { mutableStateOf("") }
    var modelInput by remember(selectedProvider, settings.aiModel) {
        mutableStateOf(
            if (selectedProvider == settings.aiProvider) settings.aiModel else selectedProvider.defaultModel
        )
    }
    var baseUrlInput by remember(selectedProvider, settings.aiBaseUrl) {
        mutableStateOf(
            if (selectedProvider == settings.aiProvider) settings.aiBaseUrl else selectedProvider.defaultBaseUrl
        )
    }
    var redactSecrets by remember(settings.redactSecretsInAi) { mutableStateOf(settings.redactSecretsInAi) }
    var askBeforeApplying by remember(settings.confirmAiChanges) { mutableStateOf(settings.confirmAiChanges) }
    val hasStoredKey = remember(selectedProvider, uiState.statusMessage) {
        !viewModel.vault.readSecret(viewModel.vault.slotForAiProvider(selectedProvider)).isNullOrBlank()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AES-256-GCM BYOK Vault", color = tokens.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            StatusPill(
                label = if (hasStoredKey) "Key Encrypted" else "No Key Saved",
                color = if (hasStoredKey) tokens.success else tokens.textMuted
            )
        }

        // Provider Selector: Google, OpenAI, Anthropic, Custom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AiProviderType.entries.forEach { provider ->
                FilterChip(
                    selected = selectedProvider == provider,
                    onClick = {
                        selectedProvider = provider
                        modelInput = provider.defaultModel
                        baseUrlInput = provider.defaultBaseUrl
                        apiKeyInput = ""
                    },
                    label = { Text(provider.shortName, fontSize = 11.sp) },
                    modifier = Modifier.testTag("ai_provider_chip_${provider.name.lowercase()}")
                )
            }
        }

        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            label = {
                Text(
                    if (hasStoredKey) "${selectedProvider.shortName} API Key (Encrypted stored)" else "${selectedProvider.shortName} API Key",
                    fontSize = 11.sp
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.fillMaxWidth().testTag("ai_byok_key_input")
        )

        OutlinedTextField(
            value = modelInput,
            onValueChange = { modelInput = it },
            label = { Text("Active Model ID", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (selectedProvider == AiProviderType.CUSTOM || selectedProvider == AiProviderType.OPENAI_COMPATIBLE) {
            OutlinedTextField(
                value = baseUrlInput,
                onValueChange = { baseUrlInput = it },
                label = { Text("Endpoint Base URL", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = {
                    viewModel.saveAiProviderConfig(
                        provider = selectedProvider,
                        model = modelInput,
                        baseUrl = baseUrlInput,
                        apiKey = apiKeyInput.takeIf { it.isNotBlank() },
                        redactSecrets = redactSecrets,
                        confirmChanges = askBeforeApplying
                    )
                    apiKeyInput = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f).height(40.dp).testTag("ai_save_vault_btn")
            ) {
                Text("Save Vault", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { viewModel.fetchAiModels(selectedProvider, apiKeyInput, baseUrlInput) },
                enabled = !uiState.isFetchingModels,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f).height(40.dp).testTag("ai_fetch_models_btn")
            ) {
                if (uiState.isFetchingModels) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text("Fetch Models", fontSize = 11.sp, maxLines = 1)
            }
        }

        if (uiState.fetchedModels.isNotEmpty()) {
            Text("Fetched Models (${uiState.fetchedModels.size}):", color = tokens.success, fontSize = 10.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                uiState.fetchedModels.take(12).forEach { fetched ->
                    FilterChip(
                        selected = modelInput == fetched,
                        onClick = { modelInput = fetched },
                        label = { Text(fetched, fontSize = 10.sp) }
                    )
                }
            }
        }
        uiState.fetchModelsError?.let { err ->
            Text(err, color = tokens.warning, fontSize = 10.sp)
        }

        // Visible Chat-Bar Model Chips Selector
        Text("Visible Chat-Bar Model Chips", color = tokens.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        val candidateChips = remember(uiState.fetchedModels, settings.selectedModelChips) {
            (DEFAULT_MODEL_CHIPS + uiState.fetchedModels.take(4) + listOf("Claude 3.7", "DeepSeek R1")).distinct()
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            candidateChips.forEach { chip ->
                val selected = chip in settings.selectedModelChips
                FilterChip(
                    selected = selected,
                    onClick = {
                        val updated = if (selected) settings.selectedModelChips - chip else settings.selectedModelChips + chip
                        viewModel.updateSelectedModelChips(updated)
                    },
                    label = { Text(chip, fontSize = 10.sp) }
                )
            }
        }

        // Safety Toggles
        DrawerSwitchRow(
            title = "Hide secrets before sending",
            subtitle = "Scrub API keys & tokens automatically",
            checked = redactSecrets,
            testTag = "ai_toggle_hide_secrets",
            onCheckedChange = { next ->
                redactSecrets = next
                viewModel.saveAiProviderConfig(selectedProvider, modelInput, baseUrlInput, null, next, askBeforeApplying)
            }
        )
        DrawerSwitchRow(
            title = "Ask before applying changes",
            subtitle = "Require preview/confirm before writing files",
            checked = askBeforeApplying,
            testTag = "ai_toggle_confirm_apply",
            onCheckedChange = { next ->
                askBeforeApplying = next
                viewModel.saveAiProviderConfig(selectedProvider, modelInput, baseUrlInput, null, redactSecrets, next)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrawerCategoryCodeViewContent(
    settings: LdroidSettingsState,
    viewModel: LdroidViewModel
) {
    val tokens = LocalLdroidTokens.current
    var newSymbol by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Editor Font Family", color = tokens.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            EditorFontFamilyOption.entries.forEach { option ->
                FilterChip(
                    selected = settings.editorFontFamily == option,
                    onClick = {
                        viewModel.updateCodeViewPrefs(
                            fontFamily = option,
                            fontSize = settings.editorFontSizeSp,
                            tabWidth = settings.tabWidth,
                            lineNumbers = settings.showLineNumbers,
                            wordWrap = settings.wordWrap,
                            bracketMatching = settings.bracketMatchingEnabled,
                            quickSymbols = settings.customQuickSymbols
                        )
                    },
                    label = { Text(option.label, fontSize = 10.sp) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Font Size: ${settings.editorFontSizeSp}sp", color = tokens.textPrimary, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        viewModel.updateCodeViewPrefs(
                            settings.editorFontFamily,
                            (settings.editorFontSizeSp - 1).coerceAtLeast(10),
                            settings.tabWidth,
                            settings.showLineNumbers,
                            settings.wordWrap,
                            settings.bracketMatchingEnabled,
                            settings.customQuickSymbols
                        )
                    },
                    modifier = Modifier.height(34.dp)
                ) { Text("-") }
                OutlinedButton(
                    onClick = {
                        viewModel.updateCodeViewPrefs(
                            settings.editorFontFamily,
                            (settings.editorFontSizeSp + 1).coerceAtMost(22),
                            settings.tabWidth,
                            settings.showLineNumbers,
                            settings.wordWrap,
                            settings.bracketMatchingEnabled,
                            settings.customQuickSymbols
                        )
                    },
                    modifier = Modifier.height(34.dp)
                ) { Text("+") }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tab Width", color = tokens.textPrimary, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(2, 4).forEach { w ->
                    FilterChip(
                        selected = settings.tabWidth == w,
                        onClick = {
                            viewModel.updateCodeViewPrefs(
                                settings.editorFontFamily,
                                settings.editorFontSizeSp,
                                w,
                                settings.showLineNumbers,
                                settings.wordWrap,
                                settings.bracketMatchingEnabled,
                                settings.customQuickSymbols
                            )
                        },
                        label = { Text("$w spaces", fontSize = 10.sp) }
                    )
                }
            }
        }

        DrawerSwitchRow(
            title = "Line Numbers",
            subtitle = "Display gutter line indices",
            checked = settings.showLineNumbers,
            testTag = "code_view_line_numbers",
            onCheckedChange = { checked ->
                viewModel.updateCodeViewPrefs(
                    settings.editorFontFamily,
                    settings.editorFontSizeSp,
                    settings.tabWidth,
                    checked,
                    settings.wordWrap,
                    settings.bracketMatchingEnabled,
                    settings.customQuickSymbols
                )
            }
        )
        DrawerSwitchRow(
            title = "Word Wrap",
            subtitle = "Wrap long lines inside editor viewport",
            checked = settings.wordWrap,
            testTag = "code_view_word_wrap",
            onCheckedChange = { checked ->
                viewModel.updateCodeViewPrefs(
                    settings.editorFontFamily,
                    settings.editorFontSizeSp,
                    settings.tabWidth,
                    settings.showLineNumbers,
                    checked,
                    settings.bracketMatchingEnabled,
                    settings.customQuickSymbols
                )
            }
        )
        DrawerSwitchRow(
            title = "Bracket Matching",
            subtitle = "Highlight matching { } ( ) [ ] pairs",
            checked = settings.bracketMatchingEnabled,
            testTag = "code_view_brackets",
            onCheckedChange = { checked ->
                viewModel.updateCodeViewPrefs(
                    settings.editorFontFamily,
                    settings.editorFontSizeSp,
                    settings.tabWidth,
                    settings.showLineNumbers,
                    settings.wordWrap,
                    checked,
                    settings.customQuickSymbols
                )
            }
        )

        // Custom Quick-Symbol Bar Editor
        Text("Quick-Symbol Bar Tokens", color = tokens.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            settings.customQuickSymbols.forEach { sym ->
                FilterChip(
                    selected = true,
                    onClick = {
                        val next = settings.customQuickSymbols - sym
                        viewModel.updateCodeViewPrefs(
                            settings.editorFontFamily,
                            settings.editorFontSizeSp,
                            settings.tabWidth,
                            settings.showLineNumbers,
                            settings.wordWrap,
                            settings.bracketMatchingEnabled,
                            next.ifEmpty { DEFAULT_QUICK_SYMBOLS }
                        )
                    },
                    label = { Text("$sym ×", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newSymbol,
                onValueChange = { newSymbol = it.take(4) },
                placeholder = { Text("Add symbol (e.g. ->)", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = {
                    val clean = newSymbol.trim()
                    if (clean.isNotEmpty()) {
                        viewModel.updateCodeViewPrefs(
                            settings.editorFontFamily,
                            settings.editorFontSizeSp,
                            settings.tabWidth,
                            settings.showLineNumbers,
                            settings.wordWrap,
                            settings.bracketMatchingEnabled,
                            (settings.customQuickSymbols + clean).distinct()
                        )
                        newSymbol = ""
                    }
                },
                modifier = Modifier.height(42.dp)
            ) { Text("Add", fontSize = 11.sp) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrawerCategoryPersonalizationContent(
    settings: LdroidSettingsState,
    viewModel: LdroidViewModel
) {
    val tokens = LocalLdroidTokens.current
    var accentHexInput by remember(settings.customAccentHex) {
        mutableStateOf(settings.customAccentHex ?: "")
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("10 Built-In IDE Themes", color = tokens.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LdroidThemeId.entries.forEach { themeId ->
                val themeTokens = LdroidThemes.byId(themeId)
                val isSelected = settings.themeId == themeId
                Surface(
                    color = if (isSelected) tokens.primaryAccent.copy(alpha = 0.16f) else tokens.surface1,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isSelected) tokens.primaryAccent else tokens.border),
                    modifier = Modifier
                        .clickable { viewModel.applyTheme(themeId) }
                        .testTag("drawer_theme_${themeId.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(themeTokens.background)
                                .border(2.dp, themeTokens.primaryAccent, CircleShape)
                        )
                        Text(
                            text = themeId.displayName,
                            color = if (isSelected) tokens.primaryAccent else tokens.textPrimary,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Text("Custom Accent Color", color = tokens.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        val swatches = listOf("#A8C7FA", "#3FB950", "#D2A8FF", "#FFBF5A", "#FF5577", "#8C583A")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            swatches.forEach { hex ->
                val swatchColor = Color(android.graphics.Color.parseColor(hex))
                val active = settings.customAccentHex.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .border(if (active) 2.5.dp else 1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                        .clickable {
                            accentHexInput = hex
                            viewModel.setCustomAccentHex(hex)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (active) {
                        Icon(Icons.Filled.Check, contentDescription = "Selected Accent", tint = Color.Black, modifier = Modifier.size(14.dp))
                    }
                }
            }
            OutlinedButton(
                onClick = {
                    accentHexInput = ""
                    viewModel.setCustomAccentHex(null)
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(32.dp)
            ) { Text("Reset", fontSize = 10.sp) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = accentHexInput,
                onValueChange = { accentHexInput = it },
                placeholder = { Text("#A8C7FA", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = { viewModel.setCustomAccentHex(accentHexInput) },
                modifier = Modifier.height(42.dp)
            ) { Text("Apply", fontSize = 11.sp) }
        }

        Text("7-Tab Icon Customization", color = tokens.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TabIconPreset.entries.forEach { preset ->
                FilterChip(
                    selected = settings.tabIconPreset == preset,
                    onClick = { viewModel.setTabIconPreset(preset) },
                    label = { Text(preset.label.substringBefore(" "), fontSize = 10.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
internal fun DrawerSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val tokens = LocalLdroidTokens.current
    Surface(
        color = tokens.surface1,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, tokens.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(title, color = tokens.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = tokens.textMuted, fontSize = 10.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
