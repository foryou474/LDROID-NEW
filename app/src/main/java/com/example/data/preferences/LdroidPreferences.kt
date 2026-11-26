package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AiProviderType
import com.example.ui.theme.LdroidThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ldroidDataStore by preferencesDataStore(name = "ldroid_settings")

data class LdroidSettingsState(
    val themeId: LdroidThemeId = LdroidThemeId.DESERT_LUXE,
    val customThemeJson: String? = null,
    val reducedMotion: Boolean = false,
    val developerName: String = "Guest Developer",
    val developerRole: String = "Offline Android Architect",
    val editorFontSizeSp: Int = 14,
    val tabWidth: Int = 2,
    val autoSaveEnabled: Boolean = true,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = true,
    val formatOnSave: Boolean = false,
    val activeProjectId: String? = null,
    val aiProvider: AiProviderType = AiProviderType.GEMINI,
    val aiModel: String = AiProviderType.GEMINI.defaultModel,
    val aiFastModel: String = AiProviderType.GEMINI.defaultFastModel,
    val aiProModel: String = AiProviderType.GEMINI.defaultProModel,
    val aiBaseUrl: String = AiProviderType.GEMINI.defaultBaseUrl,
    val redactSecretsInAi: Boolean = true,
    val confirmAiChanges: Boolean = true,
    val defaultGitBranch: String = "main",
    val guestModeAcknowledged: Boolean = true,
    val bottomPanelOpen: Boolean = false,
    val bottomPanelTabName: String = "TERMINAL",
    val bottomPanelHeightDp: Int = 230,
    val editorWorkspaceMode: String = "AI_UI_SCREEN",
    val persistedTabsJson: String = "{}"
)

class LdroidPreferences(private val context: Context) {

    private object Keys {
        val THEME_ID = stringPreferencesKey("theme_id")
        val CUSTOM_THEME_JSON = stringPreferencesKey("custom_theme_json")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val DEVELOPER_NAME = stringPreferencesKey("developer_name")
        val DEVELOPER_ROLE = stringPreferencesKey("developer_role")
        val EDITOR_FONT_SIZE = intPreferencesKey("editor_font_size")
        val TAB_WIDTH = intPreferencesKey("tab_width")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val LINE_NUMBERS = booleanPreferencesKey("line_numbers")
        val WORD_WRAP = booleanPreferencesKey("word_wrap")
        val FORMAT_ON_SAVE = booleanPreferencesKey("format_on_save")
        val ACTIVE_PROJECT_ID = stringPreferencesKey("active_project_id")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val AI_FAST_MODEL = stringPreferencesKey("ai_fast_model")
        val AI_PRO_MODEL = stringPreferencesKey("ai_pro_model")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val REDACT_SECRETS = booleanPreferencesKey("redact_secrets")
        val CONFIRM_AI_CHANGES = booleanPreferencesKey("confirm_ai_changes")
        val DEFAULT_GIT_BRANCH = stringPreferencesKey("default_git_branch")
        val BOTTOM_PANEL_OPEN = booleanPreferencesKey("bottom_panel_open")
        val BOTTOM_PANEL_TAB = stringPreferencesKey("bottom_panel_tab")
        val BOTTOM_PANEL_HEIGHT = intPreferencesKey("bottom_panel_height_dp")
        val EDITOR_MODE = stringPreferencesKey("editor_workspace_mode")
        val PERSISTED_TABS_JSON = stringPreferencesKey("persisted_tabs_json")
    }

    val settingsFlow: Flow<LdroidSettingsState> = context.ldroidDataStore.data.map { prefs ->
        val themeName = prefs[Keys.THEME_ID]
        val themeId = LdroidThemeId.entries.firstOrNull { it.name == themeName }
            ?: LdroidThemeId.DESERT_LUXE
        val providerName = prefs[Keys.AI_PROVIDER]
        val provider = AiProviderType.entries.firstOrNull { it.name == providerName }
            ?: AiProviderType.GEMINI

        LdroidSettingsState(
            themeId = themeId,
            customThemeJson = prefs[Keys.CUSTOM_THEME_JSON],
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            developerName = prefs[Keys.DEVELOPER_NAME]?.takeIf { it.isNotBlank() } ?: "Guest Developer",
            developerRole = prefs[Keys.DEVELOPER_ROLE]?.takeIf { it.isNotBlank() } ?: "Offline Android Architect",
            editorFontSizeSp = (prefs[Keys.EDITOR_FONT_SIZE] ?: 14).coerceIn(10, 22),
            tabWidth = (prefs[Keys.TAB_WIDTH] ?: 2).coerceIn(2, 8),
            autoSaveEnabled = prefs[Keys.AUTO_SAVE] ?: true,
            showLineNumbers = prefs[Keys.LINE_NUMBERS] ?: true,
            wordWrap = prefs[Keys.WORD_WRAP] ?: true,
            formatOnSave = prefs[Keys.FORMAT_ON_SAVE] ?: false,
            activeProjectId = prefs[Keys.ACTIVE_PROJECT_ID],
            aiProvider = provider,
            aiModel = prefs[Keys.AI_MODEL]?.takeIf { it.isNotBlank() } ?: provider.defaultModel,
            aiFastModel = prefs[Keys.AI_FAST_MODEL]?.takeIf { it.isNotBlank() } ?: provider.defaultFastModel,
            aiProModel = prefs[Keys.AI_PRO_MODEL]?.takeIf { it.isNotBlank() } ?: provider.defaultProModel,
            aiBaseUrl = prefs[Keys.AI_BASE_URL]?.takeIf { it.isNotBlank() } ?: provider.defaultBaseUrl,
            redactSecretsInAi = prefs[Keys.REDACT_SECRETS] ?: true,
            confirmAiChanges = prefs[Keys.CONFIRM_AI_CHANGES] ?: true,
            defaultGitBranch = prefs[Keys.DEFAULT_GIT_BRANCH]?.takeIf { it.isNotBlank() } ?: "main",
            bottomPanelOpen = prefs[Keys.BOTTOM_PANEL_OPEN] ?: false,
            bottomPanelTabName = prefs[Keys.BOTTOM_PANEL_TAB] ?: "TERMINAL",
            bottomPanelHeightDp = (prefs[Keys.BOTTOM_PANEL_HEIGHT] ?: 230).coerceIn(140, 460),
            editorWorkspaceMode = prefs[Keys.EDITOR_MODE] ?: "AI_UI_SCREEN",
            persistedTabsJson = prefs[Keys.PERSISTED_TABS_JSON] ?: "{}"
        )
    }

    suspend fun setTheme(themeId: LdroidThemeId, customJson: String? = null) {
        context.ldroidDataStore.edit { prefs ->
            prefs[Keys.THEME_ID] = themeId.name
            if (customJson == null) prefs.remove(Keys.CUSTOM_THEME_JSON)
            else prefs[Keys.CUSTOM_THEME_JSON] = customJson
        }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.ldroidDataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun setDeveloperProfile(name: String, role: String) {
        context.ldroidDataStore.edit { prefs ->
            prefs[Keys.DEVELOPER_NAME] = name.trim().ifBlank { "Guest Developer" }
            prefs[Keys.DEVELOPER_ROLE] = role.trim().ifBlank { "Offline Android Architect" }
        }
    }

    suspend fun updateEditorPrefs(
        fontSizeSp: Int,
        tabWidth: Int,
        autoSave: Boolean,
        lineNumbers: Boolean,
        wordWrap: Boolean,
        formatOnSave: Boolean = false
    ) {
        context.ldroidDataStore.edit { prefs ->
            prefs[Keys.EDITOR_FONT_SIZE] = fontSizeSp.coerceIn(10, 22)
            prefs[Keys.TAB_WIDTH] = tabWidth.coerceIn(2, 8)
            prefs[Keys.AUTO_SAVE] = autoSave
            prefs[Keys.LINE_NUMBERS] = lineNumbers
            prefs[Keys.WORD_WRAP] = wordWrap
            prefs[Keys.FORMAT_ON_SAVE] = formatOnSave
        }
    }

    suspend fun setActiveProjectId(projectId: String?) {
        context.ldroidDataStore.edit { prefs ->
            if (projectId == null) prefs.remove(Keys.ACTIVE_PROJECT_ID)
            else prefs[Keys.ACTIVE_PROJECT_ID] = projectId
        }
    }

    suspend fun updateAiConfig(
        provider: AiProviderType,
        model: String,
        baseUrl: String,
        redactSecrets: Boolean,
        confirmChanges: Boolean = true,
        fastModel: String? = null,
        proModel: String? = null
    ) {
        context.ldroidDataStore.edit { prefs ->
            prefs[Keys.AI_PROVIDER] = provider.name
            prefs[Keys.AI_MODEL] = model.trim().ifBlank { provider.defaultModel }
            prefs[Keys.AI_BASE_URL] = baseUrl.trim().ifBlank { provider.defaultBaseUrl }
            prefs[Keys.AI_FAST_MODEL] = fastModel?.trim()?.ifBlank { provider.defaultFastModel } ?: provider.defaultFastModel
            prefs[Keys.AI_PRO_MODEL] = proModel?.trim()?.ifBlank { provider.defaultProModel } ?: provider.defaultProModel
            prefs[Keys.REDACT_SECRETS] = redactSecrets
            prefs[Keys.CONFIRM_AI_CHANGES] = confirmChanges
        }
    }

    suspend fun setDefaultGitBranch(branch: String) {
        context.ldroidDataStore.edit { prefs ->
            prefs[Keys.DEFAULT_GIT_BRANCH] = branch.trim().ifBlank { "main" }
        }
    }

    suspend fun resetAllToDefaults() {
        context.ldroidDataStore.edit { prefs ->
            prefs[Keys.THEME_ID] = LdroidThemeId.DESERT_LUXE.name
            prefs.remove(Keys.CUSTOM_THEME_JSON)
            prefs[Keys.REDUCED_MOTION] = false
            prefs[Keys.DEVELOPER_NAME] = "Guest Developer"
            prefs[Keys.DEVELOPER_ROLE] = "Offline Android Architect"
            prefs[Keys.EDITOR_FONT_SIZE] = 14
            prefs[Keys.TAB_WIDTH] = 2
            prefs[Keys.AUTO_SAVE] = true
            prefs[Keys.LINE_NUMBERS] = true
            prefs[Keys.WORD_WRAP] = true
            prefs[Keys.FORMAT_ON_SAVE] = false
            prefs[Keys.AI_PROVIDER] = AiProviderType.GEMINI.name
            prefs[Keys.AI_MODEL] = AiProviderType.GEMINI.defaultModel
            prefs[Keys.AI_FAST_MODEL] = AiProviderType.GEMINI.defaultFastModel
            prefs[Keys.AI_PRO_MODEL] = AiProviderType.GEMINI.defaultProModel
            prefs[Keys.AI_BASE_URL] = AiProviderType.GEMINI.defaultBaseUrl
            prefs[Keys.REDACT_SECRETS] = true
            prefs[Keys.CONFIRM_AI_CHANGES] = true
            prefs[Keys.DEFAULT_GIT_BRANCH] = "main"
            prefs[Keys.BOTTOM_PANEL_OPEN] = false
            prefs[Keys.EDITOR_MODE] = "AI_UI_SCREEN"
        }
    }

    suspend fun updateBottomPanelState(isOpen: Boolean, tabName: String, heightDp: Int) {
        context.ldroidDataStore.edit { prefs ->
            prefs[Keys.BOTTOM_PANEL_OPEN] = isOpen
            prefs[Keys.BOTTOM_PANEL_TAB] = tabName
            prefs[Keys.BOTTOM_PANEL_HEIGHT] = heightDp.coerceIn(140, 460)
        }
    }

    suspend fun setEditorWorkspaceMode(modeName: String) {
        context.ldroidDataStore.edit { prefs ->
            prefs[Keys.EDITOR_MODE] = modeName
        }
    }

    suspend fun saveProjectOpenTabs(projectId: String, tabPaths: List<String>, activeTabPath: String?) {
        context.ldroidDataStore.edit { prefs ->
            val rootObj = runCatching {
                org.json.JSONObject(prefs[Keys.PERSISTED_TABS_JSON] ?: "{}")
            }.getOrElse { org.json.JSONObject() }
            val projObj = org.json.JSONObject().apply {
                put("tabs", org.json.JSONArray(tabPaths))
                if (activeTabPath != null) put("active", activeTabPath)
            }
            rootObj.put(projectId, projObj)
            prefs[Keys.PERSISTED_TABS_JSON] = rootObj.toString()
        }
    }

    suspend fun importSettingsJson(rawJson: String): Result<Unit> = runCatching {
        val parsed = parseSettingsJson(rawJson).getOrThrow()
        context.ldroidDataStore.edit { prefs ->
            prefs[Keys.THEME_ID] = parsed.themeId.name
            if (parsed.customThemeJson == null) prefs.remove(Keys.CUSTOM_THEME_JSON)
            else prefs[Keys.CUSTOM_THEME_JSON] = parsed.customThemeJson
            prefs[Keys.REDUCED_MOTION] = parsed.reducedMotion
            prefs[Keys.EDITOR_FONT_SIZE] = parsed.editorFontSizeSp
            prefs[Keys.TAB_WIDTH] = parsed.tabWidth
            prefs[Keys.AUTO_SAVE] = parsed.autoSaveEnabled
            prefs[Keys.LINE_NUMBERS] = parsed.showLineNumbers
            prefs[Keys.WORD_WRAP] = parsed.wordWrap
            prefs[Keys.FORMAT_ON_SAVE] = parsed.formatOnSave
            prefs[Keys.AI_PROVIDER] = parsed.aiProvider.name
            prefs[Keys.AI_MODEL] = parsed.aiModel
            prefs[Keys.AI_FAST_MODEL] = parsed.aiFastModel
            prefs[Keys.AI_PRO_MODEL] = parsed.aiProModel
            prefs[Keys.AI_BASE_URL] = parsed.aiBaseUrl
            prefs[Keys.REDACT_SECRETS] = parsed.redactSecretsInAi
            prefs[Keys.CONFIRM_AI_CHANGES] = parsed.confirmAiChanges
            prefs[Keys.DEFAULT_GIT_BRANCH] = parsed.defaultGitBranch
        }
    }

    companion object {
        fun exportSettingsJson(state: LdroidSettingsState): String {
            return org.json.JSONObject().apply {
                put("schemaVersion", 1)
                put("themeId", state.themeId.name)
                if (state.customThemeJson != null) put("customThemeJson", state.customThemeJson)
                put("reducedMotion", state.reducedMotion)
                put("editorFontSizeSp", state.editorFontSizeSp)
                put("tabWidth", state.tabWidth)
                put("autoSaveEnabled", state.autoSaveEnabled)
                put("showLineNumbers", state.showLineNumbers)
                put("wordWrap", state.wordWrap)
                put("formatOnSave", state.formatOnSave)
                put("aiProvider", state.aiProvider.name)
                put("aiModel", state.aiModel)
                put("aiFastModel", state.aiFastModel)
                put("aiProModel", state.aiProModel)
                put("aiBaseUrl", state.aiBaseUrl)
                put("redactSecretsInAi", state.redactSecretsInAi)
                put("confirmAiChanges", state.confirmAiChanges)
                put("defaultGitBranch", state.defaultGitBranch)
            }.toString(2)
        }

        fun parseSettingsJson(rawJson: String): Result<LdroidSettingsState> = runCatching {
            val obj = org.json.JSONObject(rawJson.trim())
            val themeId = LdroidThemeId.entries.firstOrNull {
                it.name == obj.optString("themeId")
            } ?: LdroidThemeId.DESERT_LUXE
            val provider = AiProviderType.entries.firstOrNull {
                it.name == obj.optString("aiProvider")
            } ?: AiProviderType.GEMINI
            val customTheme = if (obj.has("customThemeJson") && !obj.isNull("customThemeJson")) {
                obj.optString("customThemeJson").takeIf { it.isNotBlank() }
            } else null

            LdroidSettingsState(
                themeId = themeId,
                customThemeJson = customTheme,
                reducedMotion = obj.optBoolean("reducedMotion", false),
                editorFontSizeSp = obj.optInt("editorFontSizeSp", 14).coerceIn(10, 22),
                tabWidth = obj.optInt("tabWidth", 2).coerceIn(2, 8),
                autoSaveEnabled = obj.optBoolean("autoSaveEnabled", true),
                showLineNumbers = obj.optBoolean("showLineNumbers", true),
                wordWrap = obj.optBoolean("wordWrap", true),
                formatOnSave = obj.optBoolean("formatOnSave", false),
                aiProvider = provider,
                aiModel = obj.optString("aiModel").ifBlank { provider.defaultModel },
                aiFastModel = obj.optString("aiFastModel").ifBlank { provider.defaultFastModel },
                aiProModel = obj.optString("aiProModel").ifBlank { provider.defaultProModel },
                aiBaseUrl = obj.optString("aiBaseUrl").ifBlank { provider.defaultBaseUrl },
                redactSecretsInAi = obj.optBoolean("redactSecretsInAi", true),
                confirmAiChanges = obj.optBoolean("confirmAiChanges", true),
                defaultGitBranch = obj.optString("defaultGitBranch").ifBlank { "main" }
            )
        }
    }
}
