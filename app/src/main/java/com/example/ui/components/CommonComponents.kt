package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceProfile
import com.example.model.FileDiffLine
import com.example.model.VisualComponentType
import com.example.model.VisualNode
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LdroidTokens
import com.example.ui.theme.LocalLdroidTokens

@Composable
fun LdroidSurfaceCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    accentBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalLdroidTokens.current
    val bg = if (elevated) tokens.elevatedSurface else tokens.surface1
    val borderColor = if (accentBorder) tokens.primaryAccent.copy(alpha = 0.55f) else tokens.border
    val shape = RoundedCornerShape(20.dp)
    val cardColors = CardDefaults.cardColors(containerColor = bg, contentColor = tokens.textPrimary)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = cardColors,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(18.dp), content = content)
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = cardColors,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(18.dp), content = content)
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    pulse: Boolean = false
) {
    val dotScale = if (pulse) {
        val transition = rememberInfiniteTransition(label = "status_pulse")
        val s by transition.animateFloat(
            initialValue = 0.82f,
            targetValue = 1.28f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_scale"
        )
        s
    } else 1f

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = if (pulse) 0.20f else 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = if (pulse) 0.72f else 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .scale(dotScale)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val tokens = LocalLdroidTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = tokens.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = tokens.textMuted,
                    fontSize = 12.sp
                )
            }
        }
        trailing?.invoke()
    }
}

class SyntaxHighlightedTransformation(
    private val tokens: LdroidTokens,
    private val extension: String,
    private val matchedBracketIndices: Pair<Int, Int>? = null,
    private val searchHighlightRanges: List<IntRange> = emptyList(),
    private val activeSearchRange: IntRange? = null
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            highlightSyntax(
                code = text.text,
                ext = extension,
                tokens = tokens,
                matchedBracketIndices = matchedBracketIndices,
                searchHighlightRanges = searchHighlightRanges,
                activeSearchRange = activeSearchRange
            ),
            OffsetMapping.Identity
        )
    }
}

private val KOTLIN_KEYWORDS = setOf(
    "package", "import", "class", "interface", "object", "fun", "val", "var", "if", "else",
    "when", "for", "while", "return", "override", "private", "public", "internal", "suspend",
    "data", "sealed", "enum", "true", "false", "null", "by", "remember", "Composable",
    "extends", "implements", "new", "void", "static", "final", "boolean", "int", "plugins",
    "android", "dependencies", "implementation", "testImplementation"
)

private val DART_KEYWORDS = setOf(
    "import", "library", "part", "class", "extends", "implements", "with", "mixin", "abstract",
    "final", "const", "var", "late", "required", "void", "dynamic", "override", "return",
    "if", "else", "for", "while", "switch", "case", "break", "continue", "async", "await",
    "yield", "try", "catch", "finally", "super", "this", "true", "false", "null", "setState"
)

private val TS_JS_KEYWORDS = setOf(
    "import", "export", "default", "from", "function", "const", "let", "var", "return",
    "if", "else", "for", "while", "switch", "case", "break", "type", "interface", "extends",
    "implements", "async", "await", "try", "catch", "new", "true", "false", "null",
    "undefined", "useState", "useEffect", "useMemo", "useCallback", "useRef"
)

fun highlightSyntax(
    code: String,
    ext: String,
    tokens: LdroidTokens,
    matchedBracketIndices: Pair<Int, Int>? = null,
    searchHighlightRanges: List<IntRange> = emptyList(),
    activeSearchRange: IntRange? = null
): AnnotatedString {
    if (code.length > 48_000) return AnnotatedString(code)
    return buildAnnotatedString {
        append(code)
        when (ext.lowercase()) {
            "kt", "java", "kts", "gradle" -> {
                Regex("""//[^\n]*""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxComment), it.range.first, it.range.last + 1)
                }
                Regex("""/\*[\s\S]*?\*/""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxComment), it.range.first, it.range.last + 1)
                }
                Regex(""""([^"\\]|\\.)*"""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxString), it.range.first, it.range.last + 1)
                }
                Regex("""@[A-Za-z_][A-Za-z0-9_]*""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxAnnotation), it.range.first, it.range.last + 1)
                }
                Regex("""\b([a-z_][A-Za-z0-9_]*)\s*(?=\()""").findAll(code).forEach { m ->
                    val nameGroup = m.groups[1] ?: return@forEach
                    if (nameGroup.value !in KOTLIN_KEYWORDS) {
                        addStyle(SpanStyle(color = tokens.syntaxFunction), nameGroup.range.first, nameGroup.range.last + 1)
                    }
                }
                Regex("""\b[A-Za-z_][A-Za-z0-9_]*\b""").findAll(code).forEach { m ->
                    when {
                        m.value in KOTLIN_KEYWORDS ->
                            addStyle(SpanStyle(color = tokens.syntaxKeyword, fontWeight = FontWeight.SemiBold), m.range.first, m.range.last + 1)
                        m.value.firstOrNull()?.isUpperCase() == true ->
                            addStyle(SpanStyle(color = tokens.syntaxType), m.range.first, m.range.last + 1)
                    }
                }
                Regex("""\b\d+(\.\d+)?\b""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxNumber), it.range.first, it.range.last + 1)
                }
            }
            "dart" -> {
                Regex("""//[^\n]*""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxComment), it.range.first, it.range.last + 1)
                }
                Regex("""/\*[\s\S]*?\*/""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxComment), it.range.first, it.range.last + 1)
                }
                Regex(""""([^"\\]|\\.)*"|'([^'\\]|\\.)*'""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxString), it.range.first, it.range.last + 1)
                }
                Regex("""@[A-Za-z_][A-Za-z0-9_]*""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxAnnotation), it.range.first, it.range.last + 1)
                }
                Regex("""\b([a-z_][A-Za-z0-9_]*)\s*(?=\()""").findAll(code).forEach { m ->
                    val nameGroup = m.groups[1] ?: return@forEach
                    if (nameGroup.value !in DART_KEYWORDS) {
                        addStyle(SpanStyle(color = tokens.syntaxFunction), nameGroup.range.first, nameGroup.range.last + 1)
                    }
                }
                Regex("""\b[A-Za-z_][A-Za-z0-9_]*\b""").findAll(code).forEach { m ->
                    when {
                        m.value in DART_KEYWORDS ->
                            addStyle(SpanStyle(color = tokens.syntaxKeyword, fontWeight = FontWeight.SemiBold), m.range.first, m.range.last + 1)
                        m.value.firstOrNull()?.isUpperCase() == true ->
                            addStyle(SpanStyle(color = tokens.syntaxType), m.range.first, m.range.last + 1)
                    }
                }
                Regex("""\b\d+(\.\d+)?\b""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxNumber), it.range.first, it.range.last + 1)
                }
            }
            "tsx", "ts", "jsx", "js" -> {
                Regex("""//[^\n]*""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxComment), it.range.first, it.range.last + 1)
                }
                Regex("""/\*[\s\S]*?\*/""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxComment), it.range.first, it.range.last + 1)
                }
                Regex("""</?[A-Za-z][A-Za-z0-9._-]*|/?>""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxType, fontWeight = FontWeight.SemiBold), it.range.first, it.range.last + 1)
                }
                Regex(""""([^"\\]|\\.)*"|'([^'\\]|\\.)*'|`([^`\\]|\\.)*`""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxString), it.range.first, it.range.last + 1)
                }
                Regex("""\b([a-z_][A-Za-z0-9_]*)\s*(?=\()""").findAll(code).forEach { m ->
                    val nameGroup = m.groups[1] ?: return@forEach
                    if (nameGroup.value !in TS_JS_KEYWORDS) {
                        addStyle(SpanStyle(color = tokens.syntaxFunction), nameGroup.range.first, nameGroup.range.last + 1)
                    }
                }
                Regex("""\b[A-Za-z_][A-Za-z0-9_]*\b""").findAll(code).forEach { m ->
                    when {
                        m.value in TS_JS_KEYWORDS ->
                            addStyle(SpanStyle(color = tokens.syntaxKeyword, fontWeight = FontWeight.SemiBold), m.range.first, m.range.last + 1)
                        m.value.firstOrNull()?.isUpperCase() == true ->
                            addStyle(SpanStyle(color = tokens.syntaxType), m.range.first, m.range.last + 1)
                    }
                }
                Regex("""\b\d+(\.\d+)?\b""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxNumber), it.range.first, it.range.last + 1)
                }
            }
            "css" -> {
                Regex("""/\*[\s\S]*?\*/""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxComment), it.range.first, it.range.last + 1)
                }
                Regex("""([.#]?[A-Za-z_-][A-Za-z0-9_.-]*)\s*(?=\{)""").findAll(code).forEach { m ->
                    addStyle(SpanStyle(color = tokens.syntaxType, fontWeight = FontWeight.SemiBold), m.range.first, m.range.last + 1)
                }
                Regex("""([a-z-]+)\s*(?=:)""").findAll(code).forEach { m ->
                    val prop = m.groups[1] ?: return@forEach
                    addStyle(SpanStyle(color = tokens.syntaxKeyword), prop.range.first, prop.range.last + 1)
                }
                Regex("""#[0-9A-Fa-f]{3,8}\b|\b\d+(\.\d+)?(px|dp|sp|rem|em|vh|vw|dvh|%)\b""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxNumber), it.range.first, it.range.last + 1)
                }
                Regex(""""([^"\\]|\\.)*"|'([^'\\]|\\.)*'""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxString), it.range.first, it.range.last + 1)
                }
            }
            "xml", "html", "htm" -> {
                Regex("""</?[A-Za-z0-9:._-]+|>|/>""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxKeyword), it.range.first, it.range.last + 1)
                }
                Regex(""""([^"\\]|\\.)*"""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxString), it.range.first, it.range.last + 1)
                }
                Regex("""<!--[\s\S]*?-->""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxComment), it.range.first, it.range.last + 1)
                }
            }
            "json", "toml", "yml", "yaml" -> {
                Regex(""""([^"\\]|\\.)*"(?=\s*:)""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxType, fontWeight = FontWeight.SemiBold), it.range.first, it.range.last + 1)
                }
                Regex(""":\s*"([^"\\]|\\.)*"""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxString), it.range.first, it.range.last + 1)
                }
                Regex("""^[A-Za-z0-9_.-]+(?=\s*:)""", RegexOption.MULTILINE).findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxType, fontWeight = FontWeight.SemiBold), it.range.first, it.range.last + 1)
                }
                Regex("""#[^\n]*""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxComment), it.range.first, it.range.last + 1)
                }
            }
        }
        searchHighlightRanges.forEach { range ->
            val start = range.first.coerceIn(0, code.length)
            val end = (range.last + 1).coerceIn(start, code.length)
            if (start < end) {
                val isActive = activeSearchRange == range
                addStyle(
                    SpanStyle(
                        background = if (isActive) tokens.warning.copy(alpha = 0.45f) else tokens.primaryAccent.copy(alpha = 0.22f),
                        color = tokens.textPrimary,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                    ),
                    start,
                    end
                )
            }
        }
        if (matchedBracketIndices != null) {
            val (a, b) = matchedBracketIndices
            if (a in code.indices) {
                addStyle(
                    SpanStyle(
                        background = tokens.primaryAccent.copy(alpha = 0.28f),
                        color = tokens.primaryAccent,
                        fontWeight = FontWeight.Bold
                    ),
                    a,
                    a + 1
                )
            }
            if (b in code.indices) {
                addStyle(
                    SpanStyle(
                        background = tokens.primaryAccent.copy(alpha = 0.28f),
                        color = tokens.primaryAccent,
                        fontWeight = FontWeight.Bold
                    ),
                    b,
                    b + 1
                )
            }
        }
    }
}

@Composable
fun DiffLinesBlock(lines: List<FileDiffLine>) {
    val tokens = LocalLdroidTokens.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tokens.terminalBackground)
            .border(1.dp, tokens.border, RoundedCornerShape(12.dp))
    ) {
        val minWidth = maxWidth
        Column(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .widthIn(min = minWidth)
                .width(IntrinsicSize.Max)
                .padding(vertical = 8.dp)
        ) {
            lines.forEach { line ->
                val bg = when (line.marker) {
                    '+' -> tokens.diffAdded
                    '-' -> tokens.diffRemoved
                    else -> Color.Transparent
                }
                val fg = when (line.marker) {
                    '+' -> tokens.success
                    '-' -> tokens.error
                    else -> tokens.textMuted
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bg)
                        .padding(horizontal = 10.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "${line.marker} ",
                        color = fg,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        softWrap = false
                    )
                    Text(
                        text = line.text,
                        color = if (line.marker == ' ') tokens.textMuted else tokens.textPrimary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceFramePreviewCanvas(
    title: String,
    nodes: List<VisualNode>,
    profile: DeviceProfile,
    landscape: Boolean,
    fontScale: Float,
    isStale: Boolean,
    staleError: String?,
    modifier: Modifier = Modifier,
    resetNonce: Int = 0,
    darkModeOverride: Boolean? = null,
    selectedNodeId: String? = null,
    onNodeClick: ((VisualNode) -> Unit)? = null
) {
    val tokens = LocalLdroidTokens.current
    val isDarkCanvas = darkModeOverride ?: tokens.isDark
    val screenBg = if (isDarkCanvas) tokens.deviceScreenBg else Color(0xFFF6F8FC)
    val screenSurface = if (isDarkCanvas) tokens.surface2 else Color(0xFFFFFFFF)
    val screenText = if (isDarkCanvas) tokens.textPrimary else Color(0xFF0E1726)
    val screenMuted = if (isDarkCanvas) tokens.textMuted else Color(0xFF52627A)
    val screenBorder = if (isDarkCanvas) tokens.border else Color(0xFFD5DFED)
    val screenAccent = if (isDarkCanvas) tokens.primaryAccent else Color(0xFF00789E)

    val maxWidthDp = when {
        landscape -> 460.dp
        profile.category == "Tablet" -> 420.dp
        profile.category == "Foldable" -> 380.dp
        else -> 320.dp
    }

    var counterValue by remember(resetNonce) { mutableIntStateOf(0) }
    var inputState by remember(resetNonce) { mutableStateOf("") }
    var switchState by remember(resetNonce) { mutableStateOf(true) }
    var interactionStatus by remember(resetNonce) { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxWidthDp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(tokens.deviceBezel)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(listOf(tokens.primaryAccent, tokens.secondaryAccent)),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(10.dp)
        ) {
            // Device status chrome
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${profile.name} • API ${profile.apiLevel}",
                    color = tokens.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (landscape) "${profile.heightDp}x${profile.widthDp}dp" else "${profile.widthDp}x${profile.heightDp}dp",
                    color = tokens.primaryAccent,
                    fontSize = 11.sp,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(screenBg)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isStale) {
                    Surface(
                        color = tokens.warning.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, tokens.warning.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "Showing last working UI (${staleError ?: "Syntax error"})",
                            color = tokens.warning,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = screenAccent,
                        fontSize = (17f * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (interactionStatus != null) {
                        Text(
                            text = interactionStatus.orEmpty(),
                            color = tokens.success,
                            fontSize = 10.sp,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    }
                }

                nodes.forEach { node ->
                    val isSelected = selectedNodeId == node.id
                    val nodeAccent = resolvePreviewColor(node.colorTag, screenAccent)
                    val resolvedLabel = resolvePreviewStateTokens(node.label, counterValue, inputState)
                    val resolvedSecondary = resolvePreviewStateTokens(node.secondaryText, counterValue, inputState)
                    val selectionModifier = if (onNodeClick != null) {
                        Modifier
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) tokens.primaryAccent else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onNodeClick(node) }
                    } else {
                        Modifier
                    }

                    when (node.type) {
                        VisualComponentType.TEXT -> Text(
                            text = resolvedLabel,
                            color = resolvePreviewColor(node.colorTag, screenText),
                            fontSize = (14f * fontScale).sp,
                            fontWeight = FontWeight.Medium,
                            modifier = selectionModifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = (node.paddingDp / 4).coerceIn(1, 10).dp)
                        )
                        VisualComponentType.CARD -> Surface(
                            modifier = selectionModifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (node.colorTag != "Default") nodeAccent.copy(alpha = 0.12f) else screenSurface,
                            border = BorderStroke(1.dp, if (isSelected) tokens.primaryAccent else screenBorder)
                        ) {
                            Column(modifier = Modifier.padding(node.paddingDp.coerceIn(6, 24).dp)) {
                                Text(
                                    text = resolvedLabel,
                                    color = screenText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = (14f * fontScale).sp
                                )
                                if (resolvedSecondary.isNotBlank()) {
                                    Text(
                                        text = resolvedSecondary,
                                        color = screenMuted,
                                        fontSize = (12f * fontScale).sp
                                    )
                                }
                            }
                        }
                        VisualComponentType.COUNTER -> Surface(
                            modifier = selectionModifier.fillMaxWidth().testTag("preview_counter_card"),
                            shape = RoundedCornerShape(12.dp),
                            color = nodeAccent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, if (isSelected) tokens.primaryAccent else nodeAccent.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(node.paddingDp.coerceIn(6, 22).dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = resolvedLabel,
                                        color = screenText,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = (13f * fontScale).sp
                                    )
                                    Text(
                                        text = "Count: $counterValue",
                                        color = nodeAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = (16f * fontScale).sp,
                                        modifier = Modifier.testTag("preview_counter_value")
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(
                                        onClick = {
                                            if (counterValue > 0) counterValue--
                                            interactionStatus = "count=$counterValue"
                                            onNodeClick?.invoke(node)
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(34.dp).testTag("preview_counter_dec")
                                    ) { Text("-", fontWeight = FontWeight.Bold) }
                                    Button(
                                        onClick = {
                                            counterValue++
                                            interactionStatus = "count=$counterValue"
                                            onNodeClick?.invoke(node)
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = nodeAccent, contentColor = tokens.onPrimaryAccent),
                                        modifier = Modifier.height(34.dp).testTag("preview_counter_inc")
                                    ) { Text("+", fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                        VisualComponentType.BUTTON -> Button(
                            onClick = {
                                val action = node.secondaryText.uppercase()
                                when {
                                    action == "DECREMENT" || node.label == "-" || node.label.contains("Decrement", true) -> {
                                        if (counterValue > 0) counterValue--
                                    }
                                    action == "RESET" || node.label.contains("Reset", true) -> {
                                        counterValue = 0
                                    }
                                    else -> counterValue++
                                }
                                interactionStatus = "count=$counterValue"
                                onNodeClick?.invoke(node)
                            },
                            modifier = selectionModifier.fillMaxWidth().testTag("preview_button_${node.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (node.colorTag != "Default") nodeAccent else if (node.useAccent) screenAccent else tokens.secondaryAccent,
                                contentColor = tokens.onPrimaryAccent
                            )
                        ) {
                            Text(resolvedLabel, fontSize = (13f * fontScale).sp, fontWeight = FontWeight.SemiBold)
                        }
                        VisualComponentType.TEXT_FIELD -> OutlinedTextField(
                            value = inputState,
                            onValueChange = { inputState = it },
                            label = { Text(resolvedLabel, fontSize = (12f * fontScale).sp) },
                            singleLine = true,
                            modifier = selectionModifier.fillMaxWidth()
                        )
                        VisualComponentType.SWITCH_CONTROL -> Row(
                            modifier = selectionModifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(resolvedLabel, color = screenText, fontSize = (13f * fontScale).sp)
                            Switch(checked = switchState, onCheckedChange = { switchState = it })
                        }
                        VisualComponentType.SPACER -> Spacer(modifier = Modifier.height(node.paddingDp.dp))
                        VisualComponentType.IMAGE -> Surface(
                            modifier = selectionModifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = nodeAccent.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, if (isSelected) tokens.primaryAccent else screenBorder)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .padding(node.paddingDp.coerceIn(4, 16).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🖼 ${resolvedLabel.ifBlank { "Image Asset" }}",
                                    color = screenAccent,
                                    fontSize = (12f * fontScale).sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        VisualComponentType.COLUMN, VisualComponentType.ROW -> Surface(
                            modifier = selectionModifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = screenSurface,
                            border = BorderStroke(1.dp, if (isSelected) tokens.primaryAccent else screenBorder)
                        ) {
                            Text(
                                text = "${node.type.displayName}: $resolvedLabel",
                                color = screenMuted,
                                fontSize = (12f * fontScale).sp,
                                modifier = Modifier.padding(node.paddingDp.coerceIn(6, 20).dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resolvePreviewStateTokens(raw: String, count: Int, inputText: String): String {
    return raw
        .replace("\${count}", count.toString())
        .replace("\$count", count.toString())
        .replace("\${counter}", count.toString())
        .replace("\$counter", count.toString())
        .replace("\${inputText}", inputText)
        .replace("\$inputText", inputText)
}

private fun resolvePreviewColor(tag: String, fallback: Color): Color = when (tag) {
    "Copper" -> Color(0xFF8C583A)
    "Cyan" -> Color(0xFF00E5FF)
    "Purple" -> Color(0xFF9E7CFF)
    "Emerald" -> Color(0xFF35D49A)
    "Amber" -> Color(0xFFFFBF5A)
    "Rose" -> Color(0xFFFF5577)
    else -> fallback
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
    }
}
