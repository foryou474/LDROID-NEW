package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AiProviderType
import com.example.model.EditorFontFamilyOption
import com.example.model.EditorWorkspaceTab
import com.example.model.ExperimentalDevFlags
import com.example.model.TabIconPreset
import com.example.ui.theme.LdroidThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.ldroidDataStore by preferencesDataStore(name = "ldroid_settings")

val DEFAULT_QUICK_SYMBOLS = listOf("<", ">", "/", "=", "\"", "@", "+", "{", "}", "TAB")
val DEFAULT_MODEL_CHIPS = listOf("Gemini 2.5", "Flash", "GPT-4o", "Claude")
val DEFAULT_PINNED_QUICK_FILES = listOf(
    ".github/workflows/android_ci.yml",
    "app/build.gradle.kts",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/res/values/colors.xml",
    "app/src/main/res/values/strings.xml"
)

data class LdroidSettingsState(
    val themeId: LdroidThemeId = LdroidThemeId.MIDNIGHT_MINIMAL,
    val customAccentHex: String? = null,
    val customThemeJson: String? = null,
    val reducedMotion: Boolean = false,
    val developerName: String = "Guest Developer",
    val developerRole: String = "Offline Android Architect",
    val editorFontFamily: EditorFontFamilyOption = EditorFontFamilyOption.JETBRAINS_MONO,
    val editorFontSizeSp: Int = 14,
    val tabWidth: Int = 2,
    val autoSaveEnabled: Boolean = true,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = true,
    val bracketMatchingEnabled: Boolean = true,
    val formatOnSave: Boolean = false,
    val customQuickSymbols: List<String> = DEFAULT_QUICK_SYMBOLS,
    val tabIconPreset: TabIconPreset = TabIconPreset.FILLED,
    val activeProjectId: String? = null,
    val aiProvider: AiProviderType = AiProviderType.GEMINI,
    val aiModel: String = AiProviderType.GEMINI.defaultModel,
    val aiFastModel: String = AiProviderType.GEMINI.defaultFastModel,
    val aiProModel: String = AiProviderType.GEMINI.defaultProModel,
    val aiBaseUrl: String = AiProviderType.GEMINI.defaultBaseUrl,
    val selectedModelChips: List<String> = DEFAULT_MODEL_CHIPS,
    val redactSecretsInAi: Boolean = true,
    val confirmAiChanges: Boolean = true,
    val defaultGitBranch: String = "main",
    val githubUsername: String = "",
    val googleAccountEmail: String = "",
    val firebaseProjectId: String = "",
    val firebaseConnected: Boolean = false,
    val pinnedQuickFiles: List<String> = DEFAULT_PINNED_QUICK_FILES,
    val targetSdkVersion: Int = 36,
    val ndkVersion: String = "27.2.12479018",
    val jdkVersion: String = "17",
    val r8MinifyEnabled: Boolean = true,
    val proguardRulesText: String = "-keep class com.ldroid.** { *; }\n-dontwarn okhttp3.**",
    val shellEnvVarsJson: String = "{\"JAVA_HOME\":\"/usr/lib/jvm/java-17\",\"ANDROID_SDK_ROOT\":\"/data/ldroid/sdk\"}",
    val experimentalFlags: ExperimentalDevFlags = ExperimentalDevFlags(),
    val guestModeAcknowledged: Boolean = true,
    val bottomPanelOpen: Boolean = false,
    val bottomPanelTabName: String = "TERMINAL",
    val bottomPanelHeightDp: Int = 230,
    val editorWorkspaceMode: String = EditorWorkspaceTab.AI_STUDIO.name,
    val persistedTabsJson: String = "{}"
)

class LdroidPreferences(private val context: Context) {

    private object Keys {
        val THEME_ID = stringPreferencesKey("theme_id")
        val CUSTOM_ACCENT_HEX = stringPreferencesKey("custom_accent_hex")
        val CUSTOM_THEME_JSON = stringPreferencesKey("custom_theme_json")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val DEVELOPER_NAME = stringPreferencesKey("developer_name")
        val DEVELOPER_ROLE = stringPreferencesKey("developer_role")
        val EDITOR_FONT_FAMILY = stringPreferencesKey("editor_font_family")
        val EDITOR_FONT_SIZE = intPreferencesKey("editor_font_size")
        val TAB_WIDTH = intPreferencesKey("tab_width")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val LINE_NUMBERS = booleanPreferencesKey("line_numbers")
        val WORD_WRAP = booleanPreferencesKey("word_wrap")
        val BRACKET_MATCHING = booleanPreferencesKey("bracket_matching")
        val FORMAT_ON_SAVE = booleanPreferencesKey("format_on_save")
        val QUICK_SYMBOLS_JSON = stringPreferencesKey("quick_symbols_json")
        val TAB_ICON_PRESET = stringPreferencesKey("tab_icon_preset")
        val ACTIVE_PROJECT_ID = stringPreferencesKey("active_project_id")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val AI_FAST_MODEL = stringPreferencesKey("ai_fast_model")
        val AI_PRO_MODEL = stringPreferencesKey("ai_pro_model")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val SELECTED_MODEL_CHIPS_JSON = stringPreferencesKey("selected_model_chips_json")
        val REDACT_SECRETS = booleanPreferencesKey("redact_secrets")
        val CONFIRM_AI_CHANGES = booleanPreferencesKey("confirm_ai_changes")
        val DEFAULT_GIT_BRANCH = stringPreferencesKey("default_git_branch")
        val GITHUB_USERNAME = stringPreferencesKey("github_username")
        val GOOGLE_EMAIL = stringPreferencesKey("google_email")
        val FIREBASE_PROJECT_ID = stringPreferencesKey("firebase_project_id")
        val FIREBASE_CONNECTED = booleanPreferencesKey("firebase_connected")
        val PINNED_QUICK_FILES_JSON = stringPreferencesKey("pinned_quick_files_json")
        val TARGET_SDK = intPreferencesKey("target_sdk")
        val NDK_VERSION = stringPreferencesKey("ndk_version")
        val JDK_VERSION = stringPreferencesKey("jdk_version")
        val R8_MINIFY = booleanPreferencesKey("r8_minify")
        val PROGUARD_RULES = stringPreferencesKey("proguard_rules")
        val SHELL_ENV_VARS_JSON = stringPreferencesKey("shell_env_vars_json")
        val EXP_LAYOUT_BOUNDS = booleanPreferencesKey("exp_layout_bounds")
        val EXP_FPS_HUD = booleanPreferencesKey("exp_fps_hud")
        val EXP_PREDICTIVE_BACK = booleanPreferencesKey("exp_predictive_back")
        val EXP_AST_HOT_RELOAD = booleanPreferencesKey("exp_ast_hot_reload")
        val BOTTOM_PANEL_OPEN = booleanPreferencesKey("bottom_panel_open")
        val BOTTOM_PANEL_TAB = stringPreferencesKey("bottom_panel_tab")
        val BOTTOM_PANEL_HEIGHT = intPreferencesKey("bottom_panel_height_dp")
        val EDITOR_MODE = stringPreferencesKey("editor_workspace_mode")
        val PERSISTED_TABS_JSON = stringPreferencesKey("persisted_tabs_json")
    }

    val settingsFlow: Flow<LdroidSettingsState> = context.ldroidDataStore.data.map { prefs ->
        val themeName = prefs[Keys.THEME_ID]
        val themeId = LdroidThemeId.entries.firstOrNull { it.name == themeName }
            ?: LdroidThemeId.MIDNIGHT_MINIMAL
        val providerName = prefs[Keys.AI_PROVIDER]
        val provider = AiProviderType.entries.firstOrNull { it.name == providerName }
            ?: AiProviderType.GEMINI
        val fontOption = EditorFontFamilyOption.entries.firstOrNull {
            it.name == prefs[Keys.EDITOR_FONT_FAMILY]
        } ?: EditorFontFamilyOption.JETBRAINS_MONO
        val iconPreset = TabIconPreset.entries.firstOrNull {
            it.name == prefs[Keys.TAB_ICON_PRESET]
        } ?: TabIconPreset.FILLED

        val quickSymbols = parseStringList(prefs[Keys.QUICK_SYMBOLS_JSON], DEFAULT_QUICK_SYMBOLS)
        val modelChips = parseStringList(prefs[Keys.SELECTED_MODEL_CHIPS_JSON], DEFAULT_MODEL_CHIPS)
        val pinnedFiles = parseStringList(prefs[Keys.PINNED_QUICK_FILES_JSON], DEFAULT_PINNED_QUICK_FILES)

        val storedMode = prefs[Keys.EDITOR_MODE] ?: EditorWorkspaceTab.AI_STUDIO.name
        val normalizedMode = when (storedMode) {
            "AI_UI_SCREEN" -> EditorWorkspaceTab.AI_STUDIO.name
            else -> EditorWorkspaceTab.entries.firstOrNull { it.name == storedMode }?.name
                ?: EditorWorkspaceTab.AI_STUDIO.name
        }

        LdroidSettingsState(
            themeId = themeId,
            customAccentHex = prefs[Keys.CUSTOM_ACCENT_HEX],
            customThemeJson = prefs[Keys.CUSTOM_THEME_JSON],
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            developerName = prefs[Keys.DEVELOPER_NAME]?.takeIf { it.isNotBlank() } ?: "Guest Developer",
            developerRole = prefs[Keys.DEVELOPER_ROLE]?.takeIf { it.isNotBlank() } ?: "Offline Android Architect",
            editorFontFamily = fontOption,
            editorFontSizeSp = (prefs[Keys.EDITOR_FONT_SIZE] ?: 14).coerceIn(10, 22),
            tabWidth = (prefs[Keys.TAB_WIDTH] ?: 2).coerceIn(2, 8),
            autoSaveEnabled = prefs[Keys.AUTO_SAVE] ?: true,
            showLineNumbers = prefs[Keys.LINE_NUMBERS] ?: true,
            wordWrap = prefs[Keys.WORD_WRAP] ?: true,
            bracketMatchingEnabled = prefs[Keys.BRACKET_MATCHING] ?: true,
            formatOnSave = prefs[Keys.FORMAT_ON_SAVE] ?: false,
            customQuickSymbols = quickSymbols,
            tabIconPreset = iconPreset,
            activeProjectId = prefs[Keys.ACTIVE_PROJECT_ID],
            aiProvider = provider,
            aiModel = prefs[Keys.AI_MODEL]?.takeIf { it.isNotBlank() } ?: provider.defaultModel,
            aiFastModel = prefs[Keys.AI_FAST_MODEL]?.takeIf { it.isNotBlank() } ?: provider.defaultFastModel,
            aiProModel = prefs[Keys.AI_PRO_MODEL]?.takeIf { it.isNotBlank() } ?: provider.defaultProModel,
            aiBaseUrl = prefs[Keys.AI_BASE_URL]?.takeIf { it.isNotBlank() } ?: provider.defaultBaseUrl,
            selectedModelChips = modelChips,
            redactSecretsInAi = prefs[Keys.REDACT_SECRETS] ?: true,
            confirmAiChanges = prefs[Keys.CONFIRM_AI_CHANGES] ?: true,
            defaultGitBranch = prefs[Keys.DEFAULT_GIT_BRANCH]?.takeIf { it.isNotBlank() } ?: "main",
            githubUsername = prefs[Keys.GITHUB_USERNAME] ?: "",
            googleAccountEmail = prefs[Keys.GOOGLE_EMAIL] ?: "",
            firebaseProjectId = prefs[Keys.FIREBASE_PROJECT_ID] ?: "",
            firebaseConnected = prefs[Keys.FIREBASE_CONNECTED] ?: false,
            pinnedQuickFiles = pinnedFiles,
            targetSdkVersion = (prefs[Keys.TARGET_SDK] ?: 36).coerceIn(24, 36),
            ndkVersion = prefs[Keys.NDK_VERSION]?.ifBlank { "27.2.12479018" } ?: "27.2.12479018",
            jdkVersion = prefs[Keys.JDK_VERSION]?.ifBlank { "17" } ?: "17",
            r8MinifyEnabled = prefs[Keys.R8_MINIFY] ?: true,
            proguardRulesText = prefs[Keys.PROGUARD_RULES] ?: "-keep class com.ldroid.** { *; }\n-dontwarn okhttp3.**",
            shellEnvVarsJson = prefs[Keys.SHELL_ENV_VARS_JSON]
                ?: "{\"JAVA_HOME\":\"/usr/lib/jvm/java-17\",\"ANDROID_SDK_ROOT\":\"/data/ldroid/sdk\"}",
            experimentalFlags = ExperimentalDevFlags(
                layoutBoundsInspector = prefs[Keys.EXP_LAYOUT_BOUNDS] ?: false,
                fpsMemoryHud = prefs[Keys.EXP_FPS_HUD] ?: false,
                predictiveBackPreview = prefs[Keys.EXP_PREDICTIVE_BACK] ?: true,
                astHotReload = prefs[Keys.EXP_AST_HOT_RELOAD] ?: true
            ),
            bottomPanelOpen = prefs[Keys.BOTTOM_PANEL_OPEN] ?: false,
            bottomPanelTabName = prefs[Keys.BOTTOM_PANEL_TAB] ?: "TERMINAL",
            bottomPanelHeightDp = (prefs[Keys.BOTTOM_PANEL_HEIGHT] ?: 230).coerceIn(140, 460),
            editorWorkspaceMode = normalizedMode,
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

    suspend fun setCustomAccentHex(hex: String?) {
        context.ldroidDataStore.edit { prefs ->
            if (hex.isNullOrBlank()) prefs.remove(Keys.CUSTOM_ACCENT_HEX)
            else prefs[Keys.CUSTOM_ACCENT_HEX] = hex.trim()
        }
    }

    suspend fun setTabIconPreset(preset: TabIconPreset) {
        context.ldroidDataStore.edit { it[Keys.TAB_ICON_PRESET] = preset.name }
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
        formatOnSave: Boolean = false,
        fontFamily: EditorFontFamilyOption = EditorFontFamilyOption.JETBRAINS_MONO,
        bracketMatching: Boolean = true,
        quickSymbols: List<String>? = null
    ) {
        context.ldroidDataStore.edit { prefs ->
            prefs[Keys.EDITOR_FONT_FAMILY] = fontFamily.name
            prefs[Keys.EDITOR_FONT_SIZE] = fontSizeSp.coerceIn(10, 22)
            prefs[Keys.TAB_WIDTH] = tabWidth.coerceIn(2, 8)
            prefs[Keys.AUTO_SAVE] = autoSave
            prefs[Keys.LINE_NUMBERS] = lineNumbers
            prefs[Keys.WORD_WRAP] = wordWrap
            prefs[Keys.BRACKET_MATCHING] = bracketMatching
            prefs[Keys.FORMAT_ON_SAVE] = formatOnSave
            if (quickSymbols != null) {
                prefs[Keys.QUICK_SYMBOLS_JSON] = JSONArray(quickSymbols).toString()
            }
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
        proModel: String? = null,
        selectedChips: List<String>? = null
    ) {
        context.ldroidDataStore.edit { prefs ->
            prefs[Keys.AI_PROVIDER] = provider.name
            prefs[Keys.AI_MODEL] = model.trim().ifBlank { provider.defaultModel }
            prefs[Keys.AI_BASE_URL] = baseUrl.trim().ifBlank { provider.defaultBaseUrl }
            prefs[Keys.AI_FAST_MODEL] = fastModel?.trim()?.ifBlank { provider.defaultFastModel } ?: provider.defaultFastModel
            prefs[Keys.AI_PRO_MODEL] = proModel?.trim()?.ifBlank { provider.defaultProModel } ?: provider.defaultProModel
            prefs[Keys.REDACT_SECRETS] = redactSecrets
            prefs[Keys.CONFIRM_AI_CHANGES] = confirmChanges
            if (selectedChips != null) {
                prefs[Keys.SELECTED_MODEL_CHIPS_JSON] = JSONArray(selectedChips.distinct()).toString()
            }
        }
    }

    suspend fun updateSelectedModelChips(chips: List<String>) {
        context.ldroidDataStore.edit { prefs ->
            val sanitized = chips.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            prefs[Keys.SELECTED_MODEL_CHIPS_JSON] = JSONArray(sanitized.ifEmpty { DEFAULT_MODEL_CHIPS }).toString()
        }
    }

    suspend fun setDefaultGitBranch(branch: String) {
        context.ldroidDataStore.edit { prefs ->
            prefs[Keys.DEFAULT_GIT_BRANCH] = branch.trim().ifBlank { "main" }
        }
    }

    suspend fun updateLinkedAccounts(
        googleEmail: String? = null,
        githubUsername: String? = null,
        firebaseProjectId: String? = null,
        firebaseConnected: Boolean? = null
    ) {
        context.ldroidDataStore.edit { prefs ->
            if (googleEmail != null) prefs[Keys.GOOGLE_EMAIL] = googleEmail.trim()
            if (githubUsername != null) prefs[Keys.GITHUB_USERNAME] = githubUsername.trim().removePrefix("@")
            if (firebaseProjectId != null) prefs[Keys.FIREBASE_PROJECT_ID] = firebaseProjectId.trim()
            if (firebaseConnected != null) prefs[Keys.FIREBASE_CONNECTED] = firebaseConnected
        }
    }

    suspend fun updatePinnedQuickFiles(paths: List<String>) {
        context.ldroidDataStore.edit { prefs ->
            prefs[Keys.PINNED_QUICK_FILES_JSON] = JSONArray(paths.distinct()).toString()
        }
    }

    suspend fun updateAdvancedBuildSettings(
        targetSdk: Int,
        ndkVersion: String,
        jdkVersion: String,
        r8Enabled: Boolean,
        proguardRules: String,
        envVarsJson: String
    ) {
        context.ldroidDataStore.edit { prefs ->
            prefs[Keys.TARGET_SDK] = targetSdk.coerceIn(24, 36)
            prefs[Keys.NDK_VERSION] = ndkVersion.trim().ifBlank { "27.2.12479018" }
            prefs[Keys.JDK_VERSION] = jdkVersion.trim().ifBlank { "17" }
            prefs[Keys.R8_MINIFY] = r8Enabled
            prefs[Keys.PROGUARD_RULES] = proguardRules
            prefs[Keys.SHELL_ENV_VARS_JSON] = envVarsJson
        }
    }

    suspend fun updateExperimentalFlags(flags: ExperimentalDevFlags) {
        context.ldroidDataStore.edit { prefs ->
            prefs[Keys.EXP_LAYOUT_BOUNDS] = flags.layoutBoundsInspector
            prefs[Keys.EXP_FPS_HUD] = flags.fpsMemoryHud
            prefs[Keys.EXP_PREDICTIVE_BACK] = flags.predictiveBackPreview
            prefs[Keys.EXP_AST_HOT_RELOAD] = flags.astHotReload
        }
    }

    suspend fun resetAllToDefaults() {
        context.ldroidDataStore.edit { prefs ->
            prefs[Keys.THEME_ID] = LdroidThemeId.MIDNIGHT_MINIMAL.name
            prefs.remove(Keys.CUSTOM_ACCENT_HEX)
            prefs.remove(Keys.CUSTOM_THEME_JSON)
            prefs[Keys.REDUCED_MOTION] = false
            prefs[Keys.DEVELOPER_NAME] = "Guest Developer"
            prefs[Keys.DEVELOPER_ROLE] = "Offline Android Architect"
            prefs[Keys.EDITOR_FONT_FAMILY] = EditorFontFamilyOption.JETBRAINS_MONO.name
            prefs[Keys.EDITOR_FONT_SIZE] = 14
            prefs[Keys.TAB_WIDTH] = 2
            prefs[Keys.AUTO_SAVE] = true
            prefs[Keys.LINE_NUMBERS] = true
            prefs[Keys.WORD_WRAP] = true
            prefs[Keys.BRACKET_MATCHING] = true
            prefs[Keys.FORMAT_ON_SAVE] = false
            prefs[Keys.QUICK_SYMBOLS_JSON] = JSONArray(DEFAULT_QUICK_SYMBOLS).toString()
            prefs[Keys.TAB_ICON_PRESET] = TabIconPreset.FILLED.name
            prefs[Keys.AI_PROVIDER] = AiProviderType.GEMINI.name
            prefs[Keys.AI_MODEL] = AiProviderType.GEMINI.defaultModel
            prefs[Keys.AI_FAST_MODEL] = AiProviderType.GEMINI.defaultFastModel
            prefs[Keys.AI_PRO_MODEL] = AiProviderType.GEMINI.defaultProModel
            prefs[Keys.AI_BASE_URL] = AiProviderType.GEMINI.defaultBaseUrl
            prefs[Keys.SELECTED_MODEL_CHIPS_JSON] = JSONArray(DEFAULT_MODEL_CHIPS).toString()
            prefs[Keys.REDACT_SECRETS] = true
            prefs[Keys.CONFIRM_AI_CHANGES] = true
            prefs[Keys.DEFAULT_GIT_BRANCH] = "main"
            prefs[Keys.BOTTOM_PANEL_OPEN] = false
            prefs[Keys.EDITOR_MODE] = EditorWorkspaceTab.AI_STUDIO.name
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
                JSONObject(prefs[Keys.PERSISTED_TABS_JSON] ?: "{}")
            }.getOrElse { JSONObject() }
            val projObj = JSONObject().apply {
                put("tabs", JSONArray(tabPaths))
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

    private fun parseStringList(rawJson: String?, fallback: List<String>): List<String> {
        if (rawJson.isNullOrBlank()) return fallback
        return runCatching {
            val arr = JSONArray(rawJson)
            buildList {
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i).trim()
                    if (v.isNotEmpty()) add(v)
                }
            }.ifEmpty { fallback }
        }.getOrDefault(fallback)
    }

    companion object {
        fun exportSettingsJson(state: LdroidSettingsState): String {
            return JSONObject().apply {
                put("schemaVersion", 2)
                put("themeId", state.themeId.name)
                if (state.customThemeJson != null) put("customThemeJson", state.customThemeJson)
                put("reducedMotion", state.reducedMotion)
                put("editorFontFamily", state.editorFontFamily.name)
                put("editorFontSizeSp", state.editorFontSizeSp)
                put("tabWidth", state.tabWidth)
                put("autoSaveEnabled", state.autoSaveEnabled)
                put("showLineNumbers", state.showLineNumbers)
                put("wordWrap", state.wordWrap)
                put("bracketMatchingEnabled", state.bracketMatchingEnabled)
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
            val obj = JSONObject(rawJson.trim())
            val themeId = LdroidThemeId.entries.firstOrNull {
                it.name == obj.optString("themeId")
            } ?: LdroidThemeId.MIDNIGHT_MINIMAL
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
                bracketMatchingEnabled = obj.optBoolean("bracketMatchingEnabled", true),
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
