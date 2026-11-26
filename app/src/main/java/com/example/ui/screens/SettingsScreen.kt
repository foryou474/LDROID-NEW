package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.preferences.LdroidSettingsState
import com.example.data.security.SecureCredentialVault
import com.example.model.AiProviderType
import com.example.ui.components.LdroidSurfaceCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LdroidThemeId
import com.example.ui.theme.LdroidThemes
import com.example.ui.theme.LdroidTokens
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

private data class ModelTierOption(
    val id: String,
    val title: String,
    val subtitle: String
)

@Composable
fun SettingsScreen(
    uiState: LdroidUiState,
    settings: LdroidSettingsState,
    viewModel: LdroidViewModel,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    var showThemeImportExport by remember { mutableStateOf(false) }
    var showSettingsJsonDialog by remember { mutableStateOf(false) }
    var showApiKeyGuide by remember { mutableStateOf(false) }
    var showRoadmapDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var modelPickerTarget by remember { mutableStateOf<String?>(null) } // "FAST" or "PRO"

    // 1. AI Assistant & Security Draft State
    var selectedProvider by remember(settings.aiProvider) { mutableStateOf(settings.aiProvider) }
    var aiModel by remember(settings.aiModel) { mutableStateOf(settings.aiModel) }
    var fastModelId by remember(settings.aiFastModel) { mutableStateOf(settings.aiFastModel) }
    var proModelId by remember(settings.aiProModel) { mutableStateOf(settings.aiProModel) }
    var aiBaseUrl by remember(settings.aiBaseUrl) { mutableStateOf(settings.aiBaseUrl) }
    var aiKeyInput by remember { mutableStateOf("") }
    var showAiKey by remember { mutableStateOf(false) }
    var redactSecrets by remember(settings.redactSecretsInAi) { mutableStateOf(settings.redactSecretsInAi) }
    var confirmAiChanges by remember(settings.confirmAiChanges) { mutableStateOf(settings.confirmAiChanges) }

    // 2. Editor Draft State
    var fontSize by remember(settings.editorFontSizeSp) { mutableIntStateOf(settings.editorFontSizeSp) }
    var tabWidth by remember(settings.tabWidth) { mutableIntStateOf(settings.tabWidth) }
    var lineNumbers by remember(settings.showLineNumbers) { mutableStateOf(settings.showLineNumbers) }
    var wordWrap by remember(settings.wordWrap) { mutableStateOf(settings.wordWrap) }
    var autoSave by remember(settings.autoSaveEnabled) { mutableStateOf(settings.autoSaveEnabled) }
    var formatOnSave by remember(settings.formatOnSave) { mutableStateOf(settings.formatOnSave) }

    // 3. GitHub & Integrations Draft State
    var githubPatInput by remember { mutableStateOf("") }
    var showGitHubPat by remember { mutableStateOf(false) }
    var defaultBranch by remember(settings.defaultGitBranch) { mutableStateOf(settings.defaultGitBranch) }
    var vaultRevision by remember { mutableIntStateOf(0) }

    val savedAiKey = remember(selectedProvider, vaultRevision, uiState.statusMessage) {
        viewModel.vault.readSecret(viewModel.vault.slotForAiProvider(selectedProvider))
    }
    val savedGitHubPat = remember(vaultRevision, uiState.statusMessage) {
        viewModel.vault.readSecret(viewModel.vault.githubTokenSlot)
    }

    // Auto-fetch models (debounced) when a valid key is entered or saved for the active provider
    LaunchedEffect(selectedProvider, aiKeyInput, savedAiKey) {
        val keyCandidate = aiKeyInput.trim().ifBlank { savedAiKey.orEmpty() }
        if (keyCandidate.length >= 12) {
            delay(750L)
            viewModel.fetchAiModels(selectedProvider, keyCandidate, aiBaseUrl)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
            .testTag("settings_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            SectionHeader(
                title = "Settings",
                subtitle = "Customize your AI builder & code editor",
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showSettingsJsonDialog = true },
                            modifier = Modifier.testTag("settings_export_import_btn")
                        ) {
                            Icon(
                                Icons.Filled.ImportExport,
                                contentDescription = "Export or Import Settings",
                                tint = tokens.primaryAccent
                            )
                        }
                        TextButton(
                            onClick = { showResetConfirm = true },
                            modifier = Modifier.testTag("settings_reset_all_btn")
                        ) {
                            Icon(
                                Icons.Filled.RestartAlt,
                                contentDescription = "Reset All Settings",
                                tint = tokens.primaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Reset", color = tokens.primaryAccent, fontSize = 12.sp)
                        }
                    }
                }
            )
        }

        // 1. AI Assistant & API Setup
        item {
            val isConnected = !savedAiKey.isNullOrBlank()
            val tiers = remember(selectedProvider, fastModelId, proModelId) {
                listOf(
                    ModelTierOption(
                        id = fastModelId.ifBlank { selectedProvider.defaultFastModel },
                        title = "Fast Mode",
                        subtitle = "Quick code edits & chat"
                    ),
                    ModelTierOption(
                        id = proModelId.ifBlank { selectedProvider.defaultProModel },
                        title = "Pro Mode",
                        subtitle = "Complex logic & full apps"
                    )
                )
            }

            LdroidSurfaceCard(accentBorder = !isConnected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionCardTitle(icon = Icons.Filled.AutoAwesome, title = "AI Assistant Setup")
                    StatusPill(
                        label = if (isConnected) "Connected" else "Not Connected",
                        color = if (isConnected) tokens.success else tokens.warning
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Your API key stays private and encrypted (AES-256-GCM) on this device only.",
                    color = tokens.textMuted,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(10.dp))

                // 4-tab Segmented Provider Selector (Google, OpenAI, Anthropic, Custom)
                Surface(
                    color = tokens.surface2,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, tokens.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AiProviderType.entries.forEach { p ->
                            val selected = selectedProvider == p
                            Surface(
                                color = if (selected) tokens.primaryAccent.copy(alpha = 0.18f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                border = if (selected) BorderStroke(1.dp, tokens.primaryAccent.copy(alpha = 0.55f)) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedProvider = p
                                        viewModel.clearAiConnectionTestResult()
                                        viewModel.clearFetchedModels()
                                        if (p == settings.aiProvider) {
                                            aiModel = settings.aiModel
                                            fastModelId = settings.aiFastModel
                                            proModelId = settings.aiProModel
                                            aiBaseUrl = settings.aiBaseUrl
                                        } else {
                                            aiModel = p.defaultModel
                                            fastModelId = p.defaultFastModel
                                            proModelId = p.defaultProModel
                                            aiBaseUrl = p.defaultBaseUrl
                                        }
                                    }
                                    .testTag("settings_provider_${p.name.lowercase()}")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = p.shortName,
                                        color = if (selected) tokens.primaryAccent else tokens.textMuted,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Inline Custom API fields when Custom is selected
                if (selectedProvider == AiProviderType.CUSTOM) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = aiBaseUrl,
                        onValueChange = { aiBaseUrl = it },
                        label = { Text("Custom Base URL (e.g., https://openrouter.ai/api/v1)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_custom_base_url")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = aiModel,
                        onValueChange = { aiModel = it },
                        label = { Text("Custom Model ID", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_custom_model_id")
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isConnected) {
                            "Stored Key: ${SecureCredentialVault.maskCredential(savedAiKey)}"
                        } else {
                            "Your ${selectedProvider.displayName} API Key"
                        },
                        color = if (isConnected) tokens.info else tokens.textMuted,
                        fontSize = 11.sp,
                        fontFamily = if (isConnected) JetBrainsMonoFontFamily else null,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { showApiKeyGuide = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("settings_api_key_guide_btn")
                    ) {
                        Icon(Icons.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = tokens.primaryAccent)
                        Spacer(Modifier.width(4.dp))
                        Text("How to get key?", fontSize = 11.sp, color = tokens.primaryAccent)
                    }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = aiKeyInput,
                    onValueChange = { aiKeyInput = it },
                    placeholder = { Text("Paste your ${selectedProvider.displayName} key here", fontSize = 12.sp) },
                    visualTransformation = if (showAiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showAiKey = !showAiKey }) {
                            Icon(
                                imageVector = if (showAiKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showAiKey) "Hide API Key" else "Show API Key",
                                tint = tokens.textMuted
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_ai_key_input")
                )

                // Fetch Available Models + exact count badge
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.fetchAiModels(selectedProvider, aiKeyInput, aiBaseUrl)
                        },
                        enabled = !uiState.isFetchingModels && (isConnected || aiKeyInput.isNotBlank()),
                        modifier = Modifier.weight(1f).height(42.dp).testTag("settings_fetch_models_btn"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        if (uiState.isFetchingModels) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = tokens.primaryAccent)
                            Spacer(Modifier.width(6.dp))
                            Text("Fetching Models...", fontSize = 11.sp, maxLines = 1)
                        } else {
                            Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(15.dp), tint = tokens.primaryAccent)
                            Spacer(Modifier.width(6.dp))
                            Text("Fetch Available Models", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                    if (uiState.fetchedModels.isNotEmpty()) {
                        StatusPill(
                            label = "${uiState.fetchedModels.size} Models Available",
                            color = tokens.success,
                            modifier = Modifier.testTag("settings_models_count_badge")
                        )
                    }
                }
                uiState.fetchModelsError?.let { errMsg ->
                    Spacer(Modifier.height(6.dp))
                    Text(errMsg, color = tokens.warning, fontSize = 11.sp)
                }

                Spacer(Modifier.height(10.dp))
                Text("AI Model Speed & Power (tap card to activate, dropdown to pick)", color = tokens.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tiers.forEachIndexed { idx, tier ->
                        val isFast = idx == 0
                        val selected = aiModel.trim() == tier.id
                        Surface(
                            color = if (selected) tokens.primaryAccent.copy(alpha = 0.14f) else tokens.surface2,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (selected) tokens.primaryAccent else tokens.border),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { aiModel = tier.id }
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tier.title,
                                        color = if (selected) tokens.primaryAccent else tokens.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { modelPickerTarget = if (isFast) "FAST" else "PRO" },
                                        modifier = Modifier.size(24.dp).testTag(if (isFast) "pick_fast_model_btn" else "pick_pro_model_btn")
                                    ) {
                                        Icon(
                                            Icons.Filled.UnfoldMore,
                                            contentDescription = "Select ${tier.title} model",
                                            tint = tokens.primaryAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(tier.subtitle, color = tokens.textMuted, fontSize = 10.sp)
                                Text(
                                    tier.id,
                                    color = tokens.info,
                                    fontSize = 10.sp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                TogglePrefRow("Hide secrets & API keys before sending to AI", redactSecrets) {
                    redactSecrets = it
                }
                TogglePrefRow("Ask before applying AI code changes", confirmAiChanges) {
                    confirmAiChanges = it
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val cleanModel = aiModel.trim().ifBlank { selectedProvider.defaultModel }
                            val cleanUrl = aiBaseUrl.trim().trimEnd('/').ifBlank { selectedProvider.defaultBaseUrl }
                            aiModel = cleanModel
                            aiBaseUrl = cleanUrl
                            viewModel.saveAiProviderConfig(
                                provider = selectedProvider,
                                model = cleanModel,
                                baseUrl = cleanUrl,
                                apiKey = aiKeyInput.ifBlank { null },
                                redactSecrets = redactSecrets,
                                confirmChanges = confirmAiChanges,
                                fastModel = fastModelId,
                                proModel = proModelId
                            )
                            aiKeyInput = ""
                            vaultRevision++
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("settings_save_ai_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.primaryAccent,
                            contentColor = tokens.onPrimaryAccent
                        )
                    ) { Text("Save AI Settings") }
                    OutlinedButton(
                        onClick = {
                            viewModel.clearAiProviderKey(selectedProvider)
                            viewModel.clearAiConnectionTestResult()
                            vaultRevision++
                        },
                        enabled = isConnected,
                        modifier = Modifier.height(48.dp)
                    ) { Text("Clear Key") }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.testAiConnection(
                            provider = selectedProvider,
                            apiKeyInput = aiKeyInput,
                            model = aiModel,
                            baseUrl = aiBaseUrl
                        )
                    },
                    enabled = !uiState.isTestingAiConnection && (isConnected || aiKeyInput.isNotBlank()),
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("settings_test_ai_btn")
                ) {
                    if (uiState.isTestingAiConnection) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = tokens.primaryAccent)
                        Spacer(Modifier.width(8.dp))
                        Text("Testing ${selectedProvider.displayName}...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Filled.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp), tint = tokens.primaryAccent)
                        Spacer(Modifier.width(6.dp))
                        Text("Test Connection", fontSize = 12.sp)
                    }
                }
                uiState.aiConnectionTestResult?.let { (ok, detail) ->
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = (if (ok) tokens.success else tokens.error).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, (if (ok) tokens.success else tokens.error).copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = detail,
                            color = if (ok) tokens.success else tokens.error,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // 2. Code Editor Preferences
        item {
            val fontOptions = listOf(12, 14, 16, 18)
            val tabOptions = listOf(2, 4)

            LdroidSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionCardTitle(icon = Icons.Filled.Code, title = "Code Editor")
                    StatusPill("${fontSize}sp • ${tabWidth}sp tab", tokens.primaryAccent)
                }
                Spacer(Modifier.height(10.dp))

                Text("Editor Font Size", color = tokens.textMuted, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fontOptions.forEach { sizeOption ->
                        FilterChip(
                            selected = fontSize == sizeOption,
                            onClick = {
                                fontSize = sizeOption
                                viewModel.updateEditorSettings(fontSize, tabWidth, autoSave, lineNumbers, wordWrap, formatOnSave)
                            },
                            label = { Text("${sizeOption}px", fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily) }
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text("Tab Spacing", color = tokens.textMuted, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tabOptions.forEach { spaces ->
                        FilterChip(
                            selected = tabWidth == spaces,
                            onClick = {
                                tabWidth = spaces
                                viewModel.updateEditorSettings(fontSize, tabWidth, autoSave, lineNumbers, wordWrap, formatOnSave)
                            },
                            label = { Text("$spaces Spaces", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
                TogglePrefRow("Show line numbers", lineNumbers) {
                    lineNumbers = it
                    viewModel.updateEditorSettings(fontSize, tabWidth, autoSave, lineNumbers, wordWrap, formatOnSave)
                }
                TogglePrefRow("Wrap long lines of code", wordWrap) {
                    wordWrap = it
                    viewModel.updateEditorSettings(fontSize, tabWidth, autoSave, lineNumbers, wordWrap, formatOnSave)
                }
                TogglePrefRow("Auto-Save changes as you type", autoSave) {
                    autoSave = it
                    viewModel.updateEditorSettings(fontSize, tabWidth, autoSave, lineNumbers, wordWrap, formatOnSave)
                }
                TogglePrefRow("Format on Save (trim trailing spaces)", formatOnSave) {
                    formatOnSave = it
                    viewModel.updateEditorSettings(fontSize, tabWidth, autoSave, lineNumbers, wordWrap, formatOnSave)
                }
            }
        }

        // 3. Integrations & GitHub
        item {
            val hasPat = !savedGitHubPat.isNullOrBlank()
            LdroidSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionCardTitle(icon = Icons.Filled.Source, title = "Integrations & GitHub")
                    StatusPill(
                        label = if (hasPat) "Connected" else "Optional",
                        color = if (hasPat) tokens.success else tokens.info
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (hasPat) {
                        "Stored Token: ${SecureCredentialVault.maskCredential(savedGitHubPat)}"
                    } else {
                        "GitHub Access Token (Required only for private repositories)"
                    },
                    color = tokens.textMuted,
                    fontSize = 11.sp,
                    fontFamily = if (hasPat) JetBrainsMonoFontFamily else null
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = githubPatInput,
                    onValueChange = { githubPatInput = it },
                    placeholder = { Text("Personal Access Token (TEST_DUMMY_KEY)", fontSize = 12.sp) },
                    visualTransformation = if (showGitHubPat) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showGitHubPat = !showGitHubPat }) {
                            Icon(
                                imageVector = if (showGitHubPat) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle Token Visibility",
                                tint = tokens.textMuted
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = defaultBranch,
                    onValueChange = { defaultBranch = it },
                    label = { Text("Default Branch Name", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.saveGitHubConfig(githubPatInput, defaultBranch)
                            githubPatInput = ""
                            vaultRevision++
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tokens.primaryAccent,
                            contentColor = tokens.onPrimaryAccent
                        )
                    ) { Text("Save GitHub Settings", fontSize = 12.sp) }
                    if (hasPat) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearGitHubToken()
                                vaultRevision++
                            },
                            modifier = Modifier.height(46.dp)
                        ) { Text("Clear", fontSize = 12.sp) }
                    }
                }
            }
        }

        // 4. Appearance & Display (Neon Accents + 10 Themes + Reduced Motion)
        item {
            LdroidSurfaceCard(accentBorder = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionCardTitle(icon = Icons.Filled.Palette, title = "Appearance & Display")
                    OutlinedButton(onClick = { showThemeImportExport = true }) {
                        Text("JSON Theme", fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Quick Neon Accent Presets", color = tokens.textMuted, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Cyan" to LdroidThemeId.CYBERPUNK_NEON,
                        "Purple" to LdroidThemeId.ROYAL_PURPLE,
                        "Emerald" to LdroidThemeId.EMERALD_MATRIX
                    ).forEach { (label, targetId) ->
                        FilterChip(
                            selected = settings.themeId == targetId && uiState.previewThemeTokens == null,
                            onClick = { viewModel.applyTheme(targetId) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Theme Studio (All 10 M.A.X. Themes)", color = tokens.textMuted, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
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
                TogglePrefRow("Reduce animations & glow effects", settings.reducedMotion) {
                    viewModel.setReducedMotion(it)
                }
            }
        }

        // 5. Collapsible Advanced Developer Settings
        item {
            val baseUrlWarning = remember(aiBaseUrl, selectedProvider) {
                validateApiBaseUrl(aiBaseUrl, selectedProvider)
            }
            LdroidSurfaceCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvanced = !showAdvanced }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionCardTitle(icon = Icons.Filled.Security, title = "Advanced Developer Settings")
                    Icon(
                        imageVector = if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (showAdvanced) "Collapse Advanced Settings" else "Expand Advanced Settings",
                        tint = tokens.textMuted
                    )
                }
                AnimatedVisibility(visible = showAdvanced) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(color = tokens.divider)
                        OutlinedTextField(
                            value = aiModel,
                            onValueChange = { aiModel = it },
                            label = { Text("Custom Model ID") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = aiBaseUrl,
                            onValueChange = { aiBaseUrl = it },
                            label = { Text("Custom Server URL") },
                            isError = baseUrlWarning != null,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (baseUrlWarning != null) {
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
                                    Text(baseUrlWarning, color = tokens.warning, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { aiBaseUrl = selectedProvider.defaultBaseUrl }) {
                                        Text("Use API URL", fontSize = 11.sp, color = tokens.primaryAccent)
                                    }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.saveAiProviderConfig(
                                        provider = selectedProvider,
                                        model = aiModel.trim().ifBlank { selectedProvider.defaultModel },
                                        baseUrl = aiBaseUrl.trim().trimEnd('/').ifBlank { selectedProvider.defaultBaseUrl },
                                        apiKey = aiKeyInput.ifBlank { null },
                                        redactSecrets = redactSecrets,
                                        confirmChanges = confirmAiChanges
                                    )
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent)
                            ) { Text("Apply Overrides", fontSize = 12.sp) }
                            OutlinedButton(
                                onClick = { showResetConfirm = true },
                                modifier = Modifier.height(44.dp)
                            ) { Text("Reset Defaults", fontSize = 12.sp, color = tokens.error) }
                        }
                    }
                }
            }
        }

        // 6. Accessibility Contrast & M01-M50 Roadmap Audit
        item {
            val contrast = LdroidThemes.contrastRatio(tokens.textPrimary, tokens.background)
            LdroidSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Accessibility & M50 Audit", color = tokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "Primary text contrast: ${String.format("%.2f", contrast)}:1 (WCAG AA >= 4.5:1)",
                            color = if (contrast >= 4.5) tokens.success else tokens.error,
                            fontSize = 12.sp
                        )
                        Text("App ID: ${BuildConfig.APPLICATION_ID} • v${BuildConfig.VERSION_NAME}", color = tokens.textMuted, fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { showRoadmapDialog = true }, modifier = Modifier.testTag("view_roadmap_audit_btn")) {
                        Text("Verify M01–M50")
                    }
                }
            }
        }
    }

    if (showThemeImportExport) {
        ThemeImportExportDialog(
            currentTokens = tokens,
            onImportJson = { viewModel.importThemeJson(it) },
            onDismiss = { showThemeImportExport = false }
        )
    }

    if (showSettingsJsonDialog) {
        SettingsJsonDialog(
            initialJson = viewModel.exportSettingsJsonString(),
            onImportJson = { viewModel.importSettingsJson(it) },
            onDismiss = { showSettingsJsonDialog = false }
        )
    }

    if (showApiKeyGuide) {
        ApiKeyGuideDialog(
            provider = selectedProvider,
            onDismiss = { showApiKeyGuide = false }
        )
    }

    if (showRoadmapDialog) {
        RoadmapAuditDialog(
            report = uiState.verificationReport,
            isRunning = uiState.isRunningVerification,
            onRunVerification = { viewModel.runSystemVerification() },
            onDismiss = { showRoadmapDialog = false }
        )
    }

    if (showResetConfirm) {
        ResetSettingsConfirmDialog(
            onConfirmReset = { viewModel.resetAllSettings() },
            onDismiss = { showResetConfirm = false }
        )
    }

    modelPickerTarget?.let { target ->
        val isFast = target == "FAST"
        val candidateModels = remember(uiState.fetchedModels, selectedProvider) {
            if (uiState.fetchedModels.isNotEmpty()) uiState.fetchedModels
            else listOf(selectedProvider.defaultFastModel, selectedProvider.defaultProModel).distinct()
        }
        SearchableModelPickerDialog(
            title = if (isFast) "Select Fast Mode Model" else "Select Pro Mode Model",
            models = candidateModels,
            currentModel = if (isFast) fastModelId else proModelId,
            onSelectModel = { picked ->
                if (isFast) fastModelId = picked else proModelId = picked
                aiModel = picked
            },
            onDismiss = { modelPickerTarget = null }
        )
    }
}

@Composable
private fun SectionCardTitle(icon: ImageVector, title: String) {
    val tokens = LocalLdroidTokens.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tokens.primaryAccent, modifier = Modifier.size(18.dp))
        Text(title, color = tokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = tokens.textPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun modelTiersForProvider(provider: AiProviderType): List<ModelTierOption> = when (provider) {
    AiProviderType.GEMINI -> listOf(
        ModelTierOption("gemini-2.5-flash", "Fast Mode", "Quick code edits & chat"),
        ModelTierOption("gemini-2.5-pro", "Pro Mode", "Complex logic & full apps")
    )
    AiProviderType.OPENAI_COMPATIBLE -> listOf(
        ModelTierOption("gpt-4o-mini", "Fast Mode", "Quick code edits & chat"),
        ModelTierOption("gpt-4o", "Pro Mode", "Complex logic & full apps")
    )
    AiProviderType.ANTHROPIC -> listOf(
        ModelTierOption("claude-3-5-haiku-latest", "Fast Mode", "Quick code edits & chat"),
        ModelTierOption("claude-3-5-sonnet-latest", "Pro Mode", "Complex logic & full apps")
    )
    AiProviderType.CUSTOM -> listOf(
        ModelTierOption("openai/gpt-4o-mini", "Fast Mode", "Quick code edits & chat"),
        ModelTierOption("anthropic/claude-3.5-sonnet", "Pro Mode", "Complex logic & full apps")
    )
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
