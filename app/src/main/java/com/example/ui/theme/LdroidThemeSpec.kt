package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

enum class LdroidThemeId(val displayName: String, val tagline: String) {
    CYBERPUNK_NEON("Cyberpunk Neon", "Dark futuristic base with cyan & purple neon accents"),
    OBSIDIAN_GLASS("Obsidian Dark", "Deep obsidian charcoal workspace with crisp copper-azure highlights"),
    MIDNIGHT_MINIMAL("Midnight Minimal", "Charcoal studio with silver typography & restrained slate"),
    ROYAL_PURPLE("Royal Purple", "Deep violet atelier with lavender gradients & gold hints"),
    EMERALD_MATRIX("Emerald Matrix", "Forest terminal dark with emerald phosphor accents"),
    ARCTIC_LIGHT("Paper Light", "Crisp editorial paper light studio with deep slate & warm accents"),
    ROYAL_SAPPHIRE("Royal Sapphire", "Deep maritime navy with sapphire & restrained gold"),
    CRIMSON_INFERNO("Crimson Inferno", "Matte obsidian black with controlled crimson ember"),
    DESERT_LUXE("Desert Luxe", "Warm cream #FAF6F0, soft beige #EFE6D8 & copper #8C583A"),
    HOLOGRAPHIC_X("Holographic X", "Deep space canvas with iridescent cyan-magenta spectrum")
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
    // Editor syntax tokens
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
    // Terminal tokens
    val terminalBackground: Color,
    val terminalPrompt: Color,
    val terminalStdout: Color,
    val terminalStderr: Color,
    // Device preview frame
    val deviceBezel: Color,
    val deviceScreenBg: Color
)

object LdroidThemes {

    val CyberpunkNeon = LdroidTokens(
        id = LdroidThemeId.CYBERPUNK_NEON,
        name = LdroidThemeId.CYBERPUNK_NEON.displayName,
        isDark = true,
        background = CyberBg,
        surface1 = CyberSurface1,
        surface2 = CyberSurface2,
        elevatedSurface = CyberElevated,
        primaryAccent = CyberPrimary,
        secondaryAccent = CyberSecondary,
        tertiaryAccent = CyberBlue,
        onPrimaryAccent = Color(0xFF04131A),
        textPrimary = CyberTextPrimary,
        textMuted = CyberTextMuted,
        textDisabled = Color(0xFF596882),
        border = CyberBorder,
        divider = Color(0xFF1B2940),
        success = CyberSuccess,
        warning = CyberWarning,
        error = CyberError,
        info = CyberInfo,
        syntaxKeyword = Color(0xFFC792EA),
        syntaxType = Color(0xFF00E5FF),
        syntaxString = Color(0xFF35D49A),
        syntaxNumber = Color(0xFFFFBF5A),
        syntaxComment = Color(0xFF6B7C99),
        syntaxAnnotation = Color(0xFFFF82A9),
        syntaxFunction = Color(0xFF62B8FF),
        editorSelection = Color(0x4400E5FF),
        editorLineNumber = Color(0xFF5D6F8C),
        editorCurrentLine = Color(0x221E2E4A),
        diffAdded = Color(0x2635D49A),
        diffRemoved = Color(0x26FF5577),
        terminalBackground = Color(0xFF050810),
        terminalPrompt = Color(0xFF00E5FF),
        terminalStdout = Color(0xFFE5ECFF),
        terminalStderr = Color(0xFFFF6B8B),
        deviceBezel = Color(0xFF1A253B),
        deviceScreenBg = Color(0xFF0A101D)
    )

    val ObsidianGlass = LdroidTokens(
        id = LdroidThemeId.OBSIDIAN_GLASS,
        name = LdroidThemeId.OBSIDIAN_GLASS.displayName,
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
        success = Color(0xFF34D399),
        warning = Color(0xFFFBBF24),
        error = Color(0xFFF87171),
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
        diffAdded = Color(0x2634D399),
        diffRemoved = Color(0x26F87171),
        terminalBackground = Color(0xFF07090D),
        terminalPrompt = Color(0xFFD69267),
        terminalStdout = Color(0xFFE5E9F0),
        terminalStderr = Color(0xFFFCA5A5),
        deviceBezel = Color(0xFF1E293B),
        deviceScreenBg = Color(0xFF0D1119)
    )

    val MidnightMinimal = LdroidTokens(
        id = LdroidThemeId.MIDNIGHT_MINIMAL,
        name = LdroidThemeId.MIDNIGHT_MINIMAL.displayName,
        isDark = true,
        background = Color(0xFF101216),
        surface1 = Color(0xFF16191F),
        surface2 = Color(0xFF1D2129),
        elevatedSurface = Color(0xFF252A34),
        primaryAccent = Color(0xFF8AB4F8),
        secondaryAccent = Color(0xFFC4CBD9),
        tertiaryAccent = Color(0xFF6DD5FA),
        onPrimaryAccent = Color(0xFF0A1424),
        textPrimary = Color(0xFFE8ECF4),
        textMuted = Color(0xFF9AA3B2),
        textDisabled = Color(0xFF606876),
        border = Color(0xFF2D333F),
        divider = Color(0xFF232833),
        success = Color(0xFF4ADE80),
        warning = Color(0xFFFACC15),
        error = Color(0xFFFB7185),
        info = Color(0xFF7DD3FC),
        syntaxKeyword = Color(0xFFB4C6FC),
        syntaxType = Color(0xFFD8DEE9),
        syntaxString = Color(0xFFA3BE8C),
        syntaxNumber = Color(0xFFEBCB8B),
        syntaxComment = Color(0xFF768091),
        syntaxAnnotation = Color(0xFFD08770),
        syntaxFunction = Color(0xFF88C0D0),
        editorSelection = Color(0x388AB4F8),
        editorLineNumber = Color(0xFF687080),
        editorCurrentLine = Color(0x222A303D),
        diffAdded = Color(0x264ADE80),
        diffRemoved = Color(0x26FB7185),
        terminalBackground = Color(0xFF0B0D10),
        terminalPrompt = Color(0xFF8AB4F8),
        terminalStdout = Color(0xFFE5E9F0),
        terminalStderr = Color(0xFFFDA4AF),
        deviceBezel = Color(0xFF2A2F3A),
        deviceScreenBg = Color(0xFF14171D)
    )

    val RoyalPurple = LdroidTokens(
        id = LdroidThemeId.ROYAL_PURPLE,
        name = LdroidThemeId.ROYAL_PURPLE.displayName,
        isDark = true,
        background = Color(0xFF0E081A),
        surface1 = Color(0xFF160E28),
        surface2 = Color(0xFF1E1436),
        elevatedSurface = Color(0xFF281B47),
        primaryAccent = Color(0xFFC084FC),
        secondaryAccent = Color(0xFFF472B6),
        tertiaryAccent = Color(0xFFFACC15),
        onPrimaryAccent = Color(0xFF1A072E),
        textPrimary = Color(0xFFF6F0FF),
        textMuted = Color(0xFFB3A2D1),
        textDisabled = Color(0xFF6E5C8E),
        border = Color(0xFF392761),
        divider = Color(0xFF2C1D4B),
        success = Color(0xFF34D399),
        warning = Color(0xFFFBBF24),
        error = Color(0xFFFF6384),
        info = Color(0xFF818CF8),
        syntaxKeyword = Color(0xFFF472B6),
        syntaxType = Color(0xFFD8B4FE),
        syntaxString = Color(0xFF6EE7B7),
        syntaxNumber = Color(0xFFFDE047),
        syntaxComment = Color(0xFF8673AD),
        syntaxAnnotation = Color(0xFFFB923C),
        syntaxFunction = Color(0xFF93C5FD),
        editorSelection = Color(0x40C084FC),
        editorLineNumber = Color(0xFF7D6B9E),
        editorCurrentLine = Color(0x28332159),
        diffAdded = Color(0x2634D399),
        diffRemoved = Color(0x26FF6384),
        terminalBackground = Color(0xFF090512),
        terminalPrompt = Color(0xFFC084FC),
        terminalStdout = Color(0xFFF3E8FF),
        terminalStderr = Color(0xFFFF809D),
        deviceBezel = Color(0xFF2E1E4F),
        deviceScreenBg = Color(0xFF120B21)
    )

    val EmeraldMatrix = LdroidTokens(
        id = LdroidThemeId.EMERALD_MATRIX,
        name = LdroidThemeId.EMERALD_MATRIX.displayName,
        isDark = true,
        background = Color(0xFF040D07),
        surface1 = Color(0xFF09170E),
        surface2 = Color(0xFF0E2115),
        elevatedSurface = Color(0xFF152D1E),
        primaryAccent = Color(0xFF00E676),
        secondaryAccent = Color(0xFF69F0AE),
        tertiaryAccent = Color(0xFF26C6DA),
        onPrimaryAccent = Color(0xFF021A0D),
        textPrimary = Color(0xFFE6FFF0),
        textMuted = Color(0xFF88B89B),
        textDisabled = Color(0xFF4B725A),
        border = Color(0xFF1E402B),
        divider = Color(0xFF153020),
        success = Color(0xFF00E676),
        warning = Color(0xFFFFD54F),
        error = Color(0xFFFF5252),
        info = Color(0xFF40C4FF),
        syntaxKeyword = Color(0xFF69F0AE),
        syntaxType = Color(0xFF00E5FF),
        syntaxString = Color(0xFFB9F6CA),
        syntaxNumber = Color(0xFFFFD54F),
        syntaxComment = Color(0xFF5C8D6E),
        syntaxAnnotation = Color(0xFF80CBC4),
        syntaxFunction = Color(0xFF40C4FF),
        editorSelection = Color(0x3D00E676),
        editorLineNumber = Color(0xFF528064),
        editorCurrentLine = Color(0x26163824),
        diffAdded = Color(0x2E00E676),
        diffRemoved = Color(0x28FF5252),
        terminalBackground = Color(0xFF020804),
        terminalPrompt = Color(0xFF00E676),
        terminalStdout = Color(0xFFD7FFE6),
        terminalStderr = Color(0xFFFF7070),
        deviceBezel = Color(0xFF163522),
        deviceScreenBg = Color(0xFF07140C)
    )

    val ArcticLight = LdroidTokens(
        id = LdroidThemeId.ARCTIC_LIGHT,
        name = LdroidThemeId.ARCTIC_LIGHT.displayName,
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

    val RoyalSapphire = LdroidTokens(
        id = LdroidThemeId.ROYAL_SAPPHIRE,
        name = LdroidThemeId.ROYAL_SAPPHIRE.displayName,
        isDark = true,
        background = Color(0xFF060F21),
        surface1 = Color(0xFF0B1933),
        surface2 = Color(0xFF112244),
        elevatedSurface = Color(0xFF182F5C),
        primaryAccent = Color(0xFF3B82F6),
        secondaryAccent = Color(0xFFF5C451),
        tertiaryAccent = Color(0xFF38BDF8),
        onPrimaryAccent = Color(0xFFFFFFFF),
        textPrimary = Color(0xFFF0F6FF),
        textMuted = Color(0xFF94ACCF),
        textDisabled = Color(0xFF566E94),
        border = Color(0xFF223C6E),
        divider = Color(0xFF192E57),
        success = Color(0xFF2DD4BF),
        warning = Color(0xFFF5C451),
        error = Color(0xFFFB7185),
        info = Color(0xFF60A5FA),
        syntaxKeyword = Color(0xFF60A5FA),
        syntaxType = Color(0xFFF5C451),
        syntaxString = Color(0xFF5EEAD4),
        syntaxNumber = Color(0xFFFDE68A),
        syntaxComment = Color(0xFF6C85AD),
        syntaxAnnotation = Color(0xFFA5B4FC),
        syntaxFunction = Color(0xFF7DD3FC),
        editorSelection = Color(0x443B82F6),
        editorLineNumber = Color(0xFF647EA8),
        editorCurrentLine = Color(0x261D396E),
        diffAdded = Color(0x262DD4BF),
        diffRemoved = Color(0x26FB7185),
        terminalBackground = Color(0xFF040A17),
        terminalPrompt = Color(0xFFF5C451),
        terminalStdout = Color(0xFFE2EEFF),
        terminalStderr = Color(0xFFFDA4AF),
        deviceBezel = Color(0xFF1E3A6E),
        deviceScreenBg = Color(0xFF09152B)
    )

    val CrimsonInferno = LdroidTokens(
        id = LdroidThemeId.CRIMSON_INFERNO,
        name = LdroidThemeId.CRIMSON_INFERNO.displayName,
        isDark = true,
        background = Color(0xFF0D0608),
        surface1 = Color(0xFF170B0E),
        surface2 = Color(0xFF211015),
        elevatedSurface = Color(0xFF2E161D),
        primaryAccent = Color(0xFFFF3355),
        secondaryAccent = Color(0xFFFF884D),
        tertiaryAccent = Color(0xFFFFC145),
        onPrimaryAccent = Color(0xFFFFFFFF),
        textPrimary = Color(0xFFFFF1F3),
        textMuted = Color(0xFFC298A0),
        textDisabled = Color(0xFF7A555C),
        border = Color(0xFF47212B),
        divider = Color(0xFF331820),
        success = Color(0xFF4ADE80),
        warning = Color(0xFFFFB74D),
        error = Color(0xFFFF3355),
        info = Color(0xFFFF8A65),
        syntaxKeyword = Color(0xFFFF5277),
        syntaxType = Color(0xFFFFAB70),
        syntaxString = Color(0xFFFFD580),
        syntaxNumber = Color(0xFFFF8A65),
        syntaxComment = Color(0xFF8C636B),
        syntaxAnnotation = Color(0xFFF472B6),
        syntaxFunction = Color(0xFFFF758F),
        editorSelection = Color(0x40FF3355),
        editorLineNumber = Color(0xFF8A5D66),
        editorCurrentLine = Color(0x283B1922),
        diffAdded = Color(0x264ADE80),
        diffRemoved = Color(0x2EFF3355),
        terminalBackground = Color(0xFF080305),
        terminalPrompt = Color(0xFFFF5277),
        terminalStdout = Color(0xFFFFE4E8),
        terminalStderr = Color(0xFFFF6E87),
        deviceBezel = Color(0xFF3D1A23),
        deviceScreenBg = Color(0xFF12080B)
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

    val HolographicX = LdroidTokens(
        id = LdroidThemeId.HOLOGRAPHIC_X,
        name = LdroidThemeId.HOLOGRAPHIC_X.displayName,
        isDark = true,
        background = Color(0xFF070812),
        surface1 = Color(0xFF0E1022),
        surface2 = Color(0xFF151833),
        elevatedSurface = Color(0xFF1E2247),
        primaryAccent = Color(0xFF00F0FF),
        secondaryAccent = Color(0xFFFF4FD8),
        tertiaryAccent = Color(0xFF8B5CF6),
        onPrimaryAccent = Color(0xFF04151A),
        textPrimary = Color(0xFFF5F7FF),
        textMuted = Color(0xFFA2ABD0),
        textDisabled = Color(0xFF60698F),
        border = Color(0xFF2D3363),
        divider = Color(0xFF20254A),
        success = Color(0xFF2CEAA3),
        warning = Color(0xFFFFCE5C),
        error = Color(0xFFFF4F7E),
        info = Color(0xFF5CC8FF),
        syntaxKeyword = Color(0xFFFF6BE3),
        syntaxType = Color(0xFF00F0FF),
        syntaxString = Color(0xFF4EF2B2),
        syntaxNumber = Color(0xFFFFD166),
        syntaxComment = Color(0xFF727BA6),
        syntaxAnnotation = Color(0xFFA78BFA),
        syntaxFunction = Color(0xFF60C8FF),
        editorSelection = Color(0x40FF4FD8),
        editorLineNumber = Color(0xFF6A739E),
        editorCurrentLine = Color(0x28232854),
        diffAdded = Color(0x262CEAA3),
        diffRemoved = Color(0x26FF4F7E),
        terminalBackground = Color(0xFF05060D),
        terminalPrompt = Color(0xFFFF4FD8),
        terminalStdout = Color(0xFFEEF2FF),
        terminalStderr = Color(0xFFFF7096),
        deviceBezel = Color(0xFF252A54),
        deviceScreenBg = Color(0xFF0A0C1C)
    )

    val all: List<LdroidTokens> = listOf(
        DesertLuxe,
        ObsidianGlass,
        ArcticLight,
        CyberpunkNeon,
        MidnightMinimal,
        RoyalPurple,
        EmeraldMatrix,
        RoyalSapphire,
        CrimsonInferno,
        HolographicX
    )

    fun byId(id: LdroidThemeId): LdroidTokens = all.firstOrNull { it.id == id } ?: DesertLuxe

    fun byName(name: String?): LdroidTokens {
        val parsed = LdroidThemeId.entries.firstOrNull { it.name == name }
        return if (parsed != null) byId(parsed) else DesertLuxe
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
            val base = byName(obj.optString("id", "CYBERPUNK_NEON"))
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
