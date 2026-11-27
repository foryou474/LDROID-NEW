package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

enum class LdroidThemeId(val displayName: String, val tagline: String) {
    MIDNIGHT_MINIMAL("Midnight Minimal", "Deep #0D1117 canvas with #161B22 cards & #A8C7FA sky-blue accents"),
    OBSIDIAN_DARK("Obsidian Dark", "Warm obsidian charcoal workspace with copper-azure highlights"),
    DESERT_LUXE("Desert Luxe", "Warm cream #FAF6F0, soft beige #EFE6D8 & copper #8C583A"),
    PAPER_LIGHT("Paper Light", "Crisp editorial paper light studio with deep ink & cobalt"),
    DRACULA("Dracula", "Iconic violet-charcoal canvas with orchid, cyan & mint syntax"),
    NORD("Nord", "Arctic polar night slate with frost blue & aurora greens"),
    MONOKAI_PRO("Monokai Pro", "Warm graphite dark canvas with amber, coral & emerald"),
    SOLARIZED_DARK("Solarized Dark", "Calibrated deep teal night with solarized cyan & amber"),
    EMERALD_NIGHT("Emerald Night", "Forest night terminal with emerald phosphor & teal highlights"),
    AMOLED_PITCH_BLACK("Amoled Pitch Black", "True #000000 OLED pitch black canvas with high contrast")
}

@Immutable
data class LdroidTokens(
    val id: LdroidThemeId,
    val name: String,
    val isDark: Boolean,
    val background: Color,
    val surface1: Color,
    val surface2: Color,
    val elevatedSurface: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val tertiaryAccent: Color,
    val onPrimaryAccent: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val border: Color,
    val divider: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    val syntaxKeyword: Color,
    val syntaxType: Color,
    val syntaxString: Color,
    val syntaxNumber: Color,
    val syntaxComment: Color,
    val syntaxAnnotation: Color,
    val syntaxFunction: Color,
    val editorSelection: Color,
    val editorLineNumber: Color,
    val editorCurrentLine: Color,
    val diffAdded: Color,
    val diffRemoved: Color,
    val terminalBackground: Color,
    val terminalPrompt: Color,
    val terminalStdout: Color,
    val terminalStderr: Color,
    val deviceBezel: Color,
    val deviceScreenBg: Color
)

object LdroidThemes {

    val MidnightMinimal = LdroidTokens(
        id = LdroidThemeId.MIDNIGHT_MINIMAL,
        name = LdroidThemeId.MIDNIGHT_MINIMAL.displayName,
        isDark = true,
        background = Color(0xFF0D1117),
        surface1 = Color(0xFF161B22),
        surface2 = Color(0xFF1C222B),
        elevatedSurface = Color(0xFF212834),
        primaryAccent = Color(0xFFA8C7FA),
        secondaryAccent = Color(0xFF79C0FF),
        tertiaryAccent = Color(0xFFD2A8FF),
        onPrimaryAccent = Color(0xFF061529),
        textPrimary = Color(0xFFFFFFFF),
        textMuted = Color(0xFF9AA4B2),
        textDisabled = Color(0xFF606978),
        border = Color(0xFF1F2633),
        divider = Color(0xFF1B222E),
        success = Color(0xFF3FB950),
        warning = Color(0xFFD29922),
        error = Color(0xFFF85149),
        info = Color(0xFF58A6FF),
        syntaxKeyword = Color(0xFFFF7B72),
        syntaxType = Color(0xFF79C0FF),
        syntaxString = Color(0xFFA5D6FF),
        syntaxNumber = Color(0xFFD29922),
        syntaxComment = Color(0xFF8B949E),
        syntaxAnnotation = Color(0xFFD2A8FF),
        syntaxFunction = Color(0xFFA8C7FA),
        editorSelection = Color(0x40A8C7FA),
        editorLineNumber = Color(0xFF6E7681),
        editorCurrentLine = Color(0x281F2633),
        diffAdded = Color(0x263FB950),
        diffRemoved = Color(0x26F85149),
        terminalBackground = Color(0xFF090D12),
        terminalPrompt = Color(0xFFA8C7FA),
        terminalStdout = Color(0xFFE6EDF3),
        terminalStderr = Color(0xFFF85149),
        deviceBezel = Color(0xFF21262D),
        deviceScreenBg = Color(0xFF0D1117)
    )

    val ObsidianDark = LdroidTokens(
        id = LdroidThemeId.OBSIDIAN_DARK,
        name = LdroidThemeId.OBSIDIAN_DARK.displayName,
        isDark = true,
        background = Color(0xFF0B0D12),
        surface1 = Color(0xFF131720),
        surface2 = Color(0xFF1A202D),
        elevatedSurface = Color(0xFF232B3B),
        primaryAccent = Color(0xFFD69267),
        secondaryAccent = Color(0xFF63B3ED),
        tertiaryAccent = Color(0xFFA78BFA),
        onPrimaryAccent = Color(0xFF1B0F08),
        textPrimary = Color(0xFFF3F4F8),
        textMuted = Color(0xFF9CA6B8),
        textDisabled = Color(0xFF5D6B80),
        border = Color(0xFF2B3548),
        divider = Color(0xFF1F2736),
        success = Color(0xFF3FB950),
        warning = Color(0xFFFBBF24),
        error = Color(0xFFF85149),
        info = Color(0xFF60A5FA),
        syntaxKeyword = Color(0xFFD69267),
        syntaxType = Color(0xFF67E8F9),
        syntaxString = Color(0xFF6EE7B7),
        syntaxNumber = Color(0xFFFCD34D),
        syntaxComment = Color(0xFF6C7A91),
        syntaxAnnotation = Color(0xFFC4B5FD),
        syntaxFunction = Color(0xFF7DD3FC),
        editorSelection = Color(0x40D69267),
        editorLineNumber = Color(0xFF64748B),
        editorCurrentLine = Color(0x2223324D),
        diffAdded = Color(0x263FB950),
        diffRemoved = Color(0x26F85149),
        terminalBackground = Color(0xFF07090D),
        terminalPrompt = Color(0xFFD69267),
        terminalStdout = Color(0xFFE5E9F0),
        terminalStderr = Color(0xFFFCA5A5),
        deviceBezel = Color(0xFF1E293B),
        deviceScreenBg = Color(0xFF0D1119)
    )

    val DesertLuxe = LdroidTokens(
        id = LdroidThemeId.DESERT_LUXE,
        name = LdroidThemeId.DESERT_LUXE.displayName,
        isDark = false,
        background = Color(0xFFFAF6F0),
        surface1 = Color(0xFFEFE6D8),
        surface2 = Color(0xFFF5EEE3),
        elevatedSurface = Color(0xFFE5D7C3),
        primaryAccent = Color(0xFF8C583A),
        secondaryAccent = Color(0xFF6B5344),
        tertiaryAccent = Color(0xFFA96F4C),
        onPrimaryAccent = Color(0xFFFFFFFF),
        textPrimary = Color(0xFF3D2616),
        textMuted = Color(0xFF6D5240),
        textDisabled = Color(0xFFA08978),
        border = Color(0xFFD6C5B0),
        divider = Color(0xFFE3D5C3),
        success = Color(0xFF2D6A4F),
        warning = Color(0xFFB45309),
        error = Color(0xFFB91C1C),
        info = Color(0xFF2B5B84),
        syntaxKeyword = Color(0xFF8C3F23),
        syntaxType = Color(0xFF1E5F74),
        syntaxString = Color(0xFF2D6A4F),
        syntaxNumber = Color(0xFFB45309),
        syntaxComment = Color(0xFF7D6654),
        syntaxAnnotation = Color(0xFF7C3AED),
        syntaxFunction = Color(0xFF8C583A),
        editorSelection = Color(0x338C583A),
        editorLineNumber = Color(0xFF7D6654),
        editorCurrentLine = Color(0x188C583A),
        diffAdded = Color(0x262D6A4F),
        diffRemoved = Color(0x26B91C1C),
        terminalBackground = Color(0xFF241810),
        terminalPrompt = Color(0xFFE8B07D),
        terminalStdout = Color(0xFFFAF6F0),
        terminalStderr = Color(0xFFFCA5A5),
        deviceBezel = Color(0xFF4F3624),
        deviceScreenBg = Color(0xFFFAF6F0)
    )

    val PaperLight = LdroidTokens(
        id = LdroidThemeId.PAPER_LIGHT,
        name = LdroidThemeId.PAPER_LIGHT.displayName,
        isDark = false,
        background = Color(0xFFF7F7F5),
        surface1 = Color(0xFFFFFFFF),
        surface2 = Color(0xFFEFEEEA),
        elevatedSurface = Color(0xFFE4E2DC),
        primaryAccent = Color(0xFF2563EB),
        secondaryAccent = Color(0xFF7C3AED),
        tertiaryAccent = Color(0xFF0284C7),
        onPrimaryAccent = Color(0xFFFFFFFF),
        textPrimary = Color(0xFF14181F),
        textMuted = Color(0xFF4B5563),
        textDisabled = Color(0xFF9CA3AF),
        border = Color(0xFFD6D3CD),
        divider = Color(0xFFE5E3DD),
        success = Color(0xFF059669),
        warning = Color(0xFFD97706),
        error = Color(0xFFDC2626),
        info = Color(0xFF2563EB),
        syntaxKeyword = Color(0xFF7C3AED),
        syntaxType = Color(0xFF0284C7),
        syntaxString = Color(0xFF047857),
        syntaxNumber = Color(0xFFB45309),
        syntaxComment = Color(0xFF6B7280),
        syntaxAnnotation = Color(0xFFDB2777),
        syntaxFunction = Color(0xFF1D4ED8),
        editorSelection = Color(0x332563EB),
        editorLineNumber = Color(0xFF6B7280),
        editorCurrentLine = Color(0x182563EB),
        diffAdded = Color(0x26059669),
        diffRemoved = Color(0x26DC2626),
        terminalBackground = Color(0xFF14181F),
        terminalPrompt = Color(0xFF60A5FA),
        terminalStdout = Color(0xFFF3F4F6),
        terminalStderr = Color(0xFFFCA5A5),
        deviceBezel = Color(0xFF334155),
        deviceScreenBg = Color(0xFFFFFFFF)
    )

    val Dracula = LdroidTokens(
        id = LdroidThemeId.DRACULA,
        name = LdroidThemeId.DRACULA.displayName,
        isDark = true,
        background = Color(0xFF1E1F29),
        surface1 = Color(0xFF282A36),
        surface2 = Color(0xFF313442),
        elevatedSurface = Color(0xFF3A3E52),
        primaryAccent = Color(0xFFBD93F9),
        secondaryAccent = Color(0xFF8BE9FD),
        tertiaryAccent = Color(0xFFFF79C6),
        onPrimaryAccent = Color(0xFF161322),
        textPrimary = Color(0xFFF8F8F2),
        textMuted = Color(0xFFA6B2D3),
        textDisabled = Color(0xFF6272A4),
        border = Color(0xFF44475A),
        divider = Color(0xFF343746),
        success = Color(0xFF50FA7B),
        warning = Color(0xFFF1FA8C),
        error = Color(0xFFFF5555),
        info = Color(0xFF8BE9FD),
        syntaxKeyword = Color(0xFFFF79C6),
        syntaxType = Color(0xFF8BE9FD),
        syntaxString = Color(0xFFF1FA8C),
        syntaxNumber = Color(0xFFBD93F9),
        syntaxComment = Color(0xFF6272A4),
        syntaxAnnotation = Color(0xFFFFB86C),
        syntaxFunction = Color(0xFF50FA7B),
        editorSelection = Color(0x4444475A),
        editorLineNumber = Color(0xFF6272A4),
        editorCurrentLine = Color(0x2844475A),
        diffAdded = Color(0x2650FA7B),
        diffRemoved = Color(0x26FF5555),
        terminalBackground = Color(0xFF161720),
        terminalPrompt = Color(0xFFBD93F9),
        terminalStdout = Color(0xFFF8F8F2),
        terminalStderr = Color(0xFFFF5555),
        deviceBezel = Color(0xFF44475A),
        deviceScreenBg = Color(0xFF282A36)
    )

    val Nord = LdroidTokens(
        id = LdroidThemeId.NORD,
        name = LdroidThemeId.NORD.displayName,
        isDark = true,
        background = Color(0xFF242933),
        surface1 = Color(0xFF2E3440),
        surface2 = Color(0xFF3B4252),
        elevatedSurface = Color(0xFF434C5E),
        primaryAccent = Color(0xFF88C0D0),
        secondaryAccent = Color(0xFF81A1C1),
        tertiaryAccent = Color(0xFFA3BE8C),
        onPrimaryAccent = Color(0xFF161C24),
        textPrimary = Color(0xFFECEFF4),
        textMuted = Color(0xFFB4BED1),
        textDisabled = Color(0xFF616E88),
        border = Color(0xFF4C566A),
        divider = Color(0xFF3B4252),
        success = Color(0xFFA3BE8C),
        warning = Color(0xFFEBCB8B),
        error = Color(0xFFBF616A),
        info = Color(0xFF88C0D0),
        syntaxKeyword = Color(0xFF81A1C1),
        syntaxType = Color(0xFF8FBCBB),
        syntaxString = Color(0xFFA3BE8C),
        syntaxNumber = Color(0xFFB48EAD),
        syntaxComment = Color(0xFF6E7A91),
        syntaxAnnotation = Color(0xFFD08770),
        syntaxFunction = Color(0xFF88C0D0),
        editorSelection = Color(0x4088C0D0),
        editorLineNumber = Color(0xFF6C7890),
        editorCurrentLine = Color(0x26434C5E),
        diffAdded = Color(0x26A3BE8C),
        diffRemoved = Color(0x26BF616A),
        terminalBackground = Color(0xFF1D222B),
        terminalPrompt = Color(0xFF88C0D0),
        terminalStdout = Color(0xFFECEFF4),
        terminalStderr = Color(0xFFBF616A),
        deviceBezel = Color(0xFF434C5E),
        deviceScreenBg = Color(0xFF2E3440)
    )

    val MonokaiPro = LdroidTokens(
        id = LdroidThemeId.MONOKAI_PRO,
        name = LdroidThemeId.MONOKAI_PRO.displayName,
        isDark = true,
        background = Color(0xFF19181A),
        surface1 = Color(0xFF221F22),
        surface2 = Color(0xFF2D2A2E),
        elevatedSurface = Color(0xFF383539),
        primaryAccent = Color(0xFFFFD866),
        secondaryAccent = Color(0xFF78DCE8),
        tertiaryAccent = Color(0xFFFF6188),
        onPrimaryAccent = Color(0xFF1C1709),
        textPrimary = Color(0xFFFCFCFA),
        textMuted = Color(0xFFB3AFAF),
        textDisabled = Color(0xFF727072),
        border = Color(0xFF403E41),
        divider = Color(0xFF2F2D30),
        success = Color(0xFFA9DC76),
        warning = Color(0xFFFFD866),
        error = Color(0xFFFF6188),
        info = Color(0xFF78DCE8),
        syntaxKeyword = Color(0xFFFF6188),
        syntaxType = Color(0xFF78DCE8),
        syntaxString = Color(0xFFFFD866),
        syntaxNumber = Color(0xFFAB9DF2),
        syntaxComment = Color(0xFF727072),
        syntaxAnnotation = Color(0xFFFC9867),
        syntaxFunction = Color(0xFFA9DC76),
        editorSelection = Color(0x3DFFD866),
        editorLineNumber = Color(0xFF727072),
        editorCurrentLine = Color(0x26403E41),
        diffAdded = Color(0x26A9DC76),
        diffRemoved = Color(0x26FF6188),
        terminalBackground = Color(0xFF131214),
        terminalPrompt = Color(0xFFFFD866),
        terminalStdout = Color(0xFFFCFCFA),
        terminalStderr = Color(0xFFFF6188),
        deviceBezel = Color(0xFF403E41),
        deviceScreenBg = Color(0xFF221F22)
    )

    val SolarizedDark = LdroidTokens(
        id = LdroidThemeId.SOLARIZED_DARK,
        name = LdroidThemeId.SOLARIZED_DARK.displayName,
        isDark = true,
        background = Color(0xFF00212B),
        surface1 = Color(0xFF002B36),
        surface2 = Color(0xFF073642),
        elevatedSurface = Color(0xFF0E4452),
        primaryAccent = Color(0xFF2AA198),
        secondaryAccent = Color(0xFF268BD2),
        tertiaryAccent = Color(0xFFB58900),
        onPrimaryAccent = Color(0xFF001C20),
        textPrimary = Color(0xFFFDF6E3),
        textMuted = Color(0xFF93A1A1),
        textDisabled = Color(0xFF586E75),
        border = Color(0xFF134E5E),
        divider = Color(0xFF0A3D4A),
        success = Color(0xFF859900),
        warning = Color(0xFFB58900),
        error = Color(0xFFDC322F),
        info = Color(0xFF268BD2),
        syntaxKeyword = Color(0xFF859900),
        syntaxType = Color(0xFFB58900),
        syntaxString = Color(0xFF2AA198),
        syntaxNumber = Color(0xFFD33682),
        syntaxComment = Color(0xFF657B83),
        syntaxAnnotation = Color(0xFFCB4B16),
        syntaxFunction = Color(0xFF268BD2),
        editorSelection = Color(0x402AA198),
        editorLineNumber = Color(0xFF586E75),
        editorCurrentLine = Color(0x28073642),
        diffAdded = Color(0x26859900),
        diffRemoved = Color(0x26DC322F),
        terminalBackground = Color(0xFF001920),
        terminalPrompt = Color(0xFF2AA198),
        terminalStdout = Color(0xFFEEE8D5),
        terminalStderr = Color(0xFFDC322F),
        deviceBezel = Color(0xFF0E4452),
        deviceScreenBg = Color(0xFF002B36)
    )

    val EmeraldNight = LdroidTokens(
        id = LdroidThemeId.EMERALD_NIGHT,
        name = LdroidThemeId.EMERALD_NIGHT.displayName,
        isDark = true,
        background = Color(0xFF040D07),
        surface1 = Color(0xFF09170E),
        surface2 = Color(0xFF0E2115),
        elevatedSurface = Color(0xFF152D1E),
        primaryAccent = Color(0xFF3FB950),
        secondaryAccent = Color(0xFF69F0AE),
        tertiaryAccent = Color(0xFF26C6DA),
        onPrimaryAccent = Color(0xFF021A0D),
        textPrimary = Color(0xFFE6FFF0),
        textMuted = Color(0xFF88B89B),
        textDisabled = Color(0xFF4B725A),
        border = Color(0xFF1E402B),
        divider = Color(0xFF153020),
        success = Color(0xFF3FB950),
        warning = Color(0xFFFFD54F),
        error = Color(0xFFF85149),
        info = Color(0xFF40C4FF),
        syntaxKeyword = Color(0xFF69F0AE),
        syntaxType = Color(0xFF00E5FF),
        syntaxString = Color(0xFFB9F6CA),
        syntaxNumber = Color(0xFFFFD54F),
        syntaxComment = Color(0xFF5C8D6E),
        syntaxAnnotation = Color(0xFF80CBC4),
        syntaxFunction = Color(0xFF40C4FF),
        editorSelection = Color(0x3D3FB950),
        editorLineNumber = Color(0xFF528064),
        editorCurrentLine = Color(0x26163824),
        diffAdded = Color(0x2E3FB950),
        diffRemoved = Color(0x28F85149),
        terminalBackground = Color(0xFF020804),
        terminalPrompt = Color(0xFF3FB950),
        terminalStdout = Color(0xFFD7FFE6),
        terminalStderr = Color(0xFFFF7070),
        deviceBezel = Color(0xFF163522),
        deviceScreenBg = Color(0xFF07140C)
    )

    val AmoledPitchBlack = LdroidTokens(
        id = LdroidThemeId.AMOLED_PITCH_BLACK,
        name = LdroidThemeId.AMOLED_PITCH_BLACK.displayName,
        isDark = true,
        background = Color(0xFF000000),
        surface1 = Color(0xFF0B0D11),
        surface2 = Color(0xFF141820),
        elevatedSurface = Color(0xFF1C222D),
        primaryAccent = Color(0xFFA8C7FA),
        secondaryAccent = Color(0xFF60EFFF),
        tertiaryAccent = Color(0xFFB794F4),
        onPrimaryAccent = Color(0xFF031024),
        textPrimary = Color(0xFFFFFFFF),
        textMuted = Color(0xFF9EA8B8),
        textDisabled = Color(0xFF5C6573),
        border = Color(0xFF222938),
        divider = Color(0xFF181D27),
        success = Color(0xFF3FB950),
        warning = Color(0xFFFBBF24),
        error = Color(0xFFF85149),
        info = Color(0xFF58A6FF),
        syntaxKeyword = Color(0xFFA8C7FA),
        syntaxType = Color(0xFF60EFFF),
        syntaxString = Color(0xFF6EE7B7),
        syntaxNumber = Color(0xFFFCD34D),
        syntaxComment = Color(0xFF6D7685),
        syntaxAnnotation = Color(0xFFD6BCFA),
        syntaxFunction = Color(0xFF79C0FF),
        editorSelection = Color(0x44A8C7FA),
        editorLineNumber = Color(0xFF5F6978),
        editorCurrentLine = Color(0x241B2230),
        diffAdded = Color(0x263FB950),
        diffRemoved = Color(0x26F85149),
        terminalBackground = Color(0xFF000000),
        terminalPrompt = Color(0xFFA8C7FA),
        terminalStdout = Color(0xFFFFFFFF),
        terminalStderr = Color(0xFFF85149),
        deviceBezel = Color(0xFF1F2530),
        deviceScreenBg = Color(0xFF000000)
    )

    @Deprecated("Replaced by MidnightMinimal", ReplaceWith("MidnightMinimal"))
    val CyberpunkNeon get() = MidnightMinimal

    val all: List<LdroidTokens> = listOf(
        MidnightMinimal,
        ObsidianDark,
        DesertLuxe,
        PaperLight,
        Dracula,
        Nord,
        MonokaiPro,
        SolarizedDark,
        EmeraldNight,
        AmoledPitchBlack
    )

    fun byId(id: LdroidThemeId): LdroidTokens = all.firstOrNull { it.id == id } ?: MidnightMinimal

    fun byName(name: String?): LdroidTokens {
        val parsed = LdroidThemeId.entries.firstOrNull { it.name == name }
        return if (parsed != null) byId(parsed) else MidnightMinimal
    }

    fun withCustomAccent(base: LdroidTokens, accentHex: String?): LdroidTokens {
        val cleaned = accentHex?.trim()?.takeIf { it.isNotBlank() } ?: return base
        val parsed = runCatching { parseHexColor(cleaned) }.getOrNull() ?: return base
        val onAccent = if (parsed.luminance() > 0.45f) Color(0xFF0B111B) else Color(0xFFFFFFFF)
        return base.copy(
            primaryAccent = parsed,
            onPrimaryAccent = onAccent,
            terminalPrompt = parsed,
            editorSelection = parsed.copy(alpha = 0.25f)
        )
    }

    fun exportToJson(tokens: LdroidTokens): String {
        return JSONObject().apply {
            put("schema", "ldroid.theme.v1")
            put("id", tokens.id.name)
            put("name", tokens.name)
            put("isDark", tokens.isDark)
            put("background", colorToHex(tokens.background))
            put("surface1", colorToHex(tokens.surface1))
            put("surface2", colorToHex(tokens.surface2))
            put("elevatedSurface", colorToHex(tokens.elevatedSurface))
            put("primaryAccent", colorToHex(tokens.primaryAccent))
            put("secondaryAccent", colorToHex(tokens.secondaryAccent))
            put("textPrimary", colorToHex(tokens.textPrimary))
            put("textMuted", colorToHex(tokens.textMuted))
            put("border", colorToHex(tokens.border))
        }.toString(2)
    }

    fun validateAndParseThemeJson(rawJson: String): Result<LdroidTokens> {
        return runCatching {
            val obj = JSONObject(rawJson.trim())
            val schema = obj.optString("schema", "")
            require(schema == "ldroid.theme.v1") { "Unsupported theme schema '$schema'" }
            val base = byName(obj.optString("id", "MIDNIGHT_MINIMAL"))
            val name = obj.optString("name", base.name).takeIf { it.isNotBlank() } ?: base.name
            val isDark = obj.optBoolean("isDark", base.isDark)
            val bg = parseHexColor(obj.getString("background"))
            val s1 = parseHexColor(obj.getString("surface1"))
            val s2 = parseHexColor(obj.getString("surface2"))
            val elev = parseHexColor(obj.optString("elevatedSurface", colorToHex(base.elevatedSurface)))
            val primary = parseHexColor(obj.getString("primaryAccent"))
            val secondary = parseHexColor(obj.getString("secondaryAccent"))
            val textPrimary = parseHexColor(obj.getString("textPrimary"))
            val textMuted = parseHexColor(obj.optString("textMuted", colorToHex(base.textMuted)))
            val border = parseHexColor(obj.optString("border", colorToHex(base.border)))
            require(contrastRatio(textPrimary, bg) >= 4.5) {
                "Primary text contrast against background must be >= 4.5:1 for WCAG AA readability"
            }
            base.copy(
                name = name,
                isDark = isDark,
                background = bg,
                surface1 = s1,
                surface2 = s2,
                elevatedSurface = elev,
                primaryAccent = primary,
                secondaryAccent = secondary,
                textPrimary = textPrimary,
                textMuted = textMuted,
                border = border
            )
        }
    }

    fun colorToHex(color: Color): String {
        val argb = color.toArgb()
        return String.format("#%08X", argb)
    }

    fun parseHexColor(hex: String): Color {
        val cleaned = hex.trim().removePrefix("#")
        require(cleaned.length == 6 || cleaned.length == 8) { "Invalid hex color: $hex" }
        val value = cleaned.toLong(16)
        val argb = if (cleaned.length == 6) (0xFF000000L or value) else value
        return Color(argb.toInt())
    }

    fun contrastRatio(foreground: Color, background: Color): Double {
        val l1 = foreground.luminance().toDouble()
        val l2 = background.luminance().toDouble()
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }
}
