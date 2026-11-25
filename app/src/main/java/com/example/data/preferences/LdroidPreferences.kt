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
    val themeId: LdroidThemeId = LdroidThemeId.CYBERPUNK_NEON,
    val customThemeJson: String? = null,
    val reducedMotion: Boolean = false,
    val editorFontSizeSp: Int = 13,
    val tabWidth: Int = 4,
    val autoSaveEnabled: Boolean = true,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val activeProjectId: String? = null,
    val aiProvider: AiProviderType = AiProviderType.GEMINI,
    val aiModel: String = AiProviderType.GEMINI.defaultModel,
    val aiBaseUrl: String = AiProviderType.GEMINI.defaultBaseUrl,
    val redactSecretsInAi: Boolean = true,
    val guestModeAcknowledged: Boolean = true
)

class LdroidPreferences(private val context: Context) {

    private object Keys {
        val THEME_ID = stringPreferencesKey("theme_id")
        val CUSTOM_THEME_JSON = stringPreferencesKey("custom_theme_json")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val EDITOR_FONT_SIZE = intPreferencesKey("editor_font_size")
        val TAB_WIDTH = intPreferencesKey("tab_width")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val LINE_NUMBERS = booleanPreferencesKey("line_numbers")
        val WORD_WRAP = booleanPreferencesKey("word_wrap")
        val ACTIVE_PROJECT_ID = stringPreferencesKey("active_project_id")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val REDACT_SECRETS = booleanPreferencesKey("redact_secrets")
    }

    val settingsFlow: Flow<LdroidSettingsState> = context.ldroidDataStore.data.map { prefs ->
        val themeName = prefs[Keys.THEME_ID]
        val themeId = LdroidThemeId.entries.firstOrNull { it.name == themeName }
            ?: LdroidThemeId.CYBERPUNK_NEON
        val providerName = prefs[Keys.AI_PROVIDER]
        val provider = AiProviderType.entries.firstOrNull { it.name == providerName }
            ?: AiProviderType.GEMINI

        LdroidSettingsState(
            themeId = themeId,
            customThemeJson = prefs[Keys.CUSTOM_THEME_JSON],
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            editorFontSizeSp = (prefs[Keys.EDITOR_FONT_SIZE] ?: 13).coerceIn(10, 22),
            tabWidth = (prefs[Keys.TAB_WIDTH] ?: 4).coerceIn(2, 8),
            autoSaveEnabled = prefs[Keys.AUTO_SAVE] ?: true,
            showLineNumbers = prefs[Keys.LINE_NUMBERS] ?: true,
            wordWrap = prefs[Keys.WORD_WRAP] ?: false,
            activeProjectId = prefs[Keys.ACTIVE_PROJECT_ID],
            aiProvider = provider,
            aiModel = prefs[Keys.AI_MODEL]?.takeIf { it.isNotBlank() } ?: provider.defaultModel,
            aiBaseUrl = prefs[Keys.AI_BASE_URL]?.takeIf { it.isNotBlank() } ?: provider.defaultBaseUrl,
            redactSecretsInAi = prefs[Keys.REDACT_SECRETS] ?: true
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

    suspend fun updateEditorPrefs(
        fontSizeSp: Int,
        tabWidth: Int,
        autoSave: Boolean,
        lineNumbers: Boolean,
        wordWrap: Boolean
    ) {
        context.ldroidDataStore.edit { prefs ->
            prefs[Keys.EDITOR_FONT_SIZE] = fontSizeSp.coerceIn(10, 22)
            prefs[Keys.TAB_WIDTH] = tabWidth.coerceIn(2, 8)
            prefs[Keys.AUTO_SAVE] = autoSave
            prefs[Keys.LINE_NUMBERS] = lineNumbers
            prefs[Keys.WORD_WRAP] = wordWrap
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
        redactSecrets: Boolean
    ) {
        context.ldroidDataStore.edit { prefs ->
            prefs[Keys.AI_PROVIDER] = provider.name
            prefs[Keys.AI_MODEL] = model.trim().ifBlank { provider.defaultModel }
            prefs[Keys.AI_BASE_URL] = baseUrl.trim().ifBlank { provider.defaultBaseUrl }
            prefs[Keys.REDACT_SECRETS] = redactSecrets
        }
    }
}
