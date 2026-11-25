package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
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
    private val extension: String
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(highlightSyntax(text.text, extension, tokens), OffsetMapping.Identity)
    }
}

private val KOTLIN_KEYWORDS = setOf(
    "package", "import", "class", "interface", "object", "fun", "val", "var", "if", "else",
    "when", "for", "while", "return", "override", "private", "public", "internal", "suspend",
    "data", "sealed", "enum", "true", "false", "null", "by", "remember", "Composable", "import"
)

fun highlightSyntax(code: String, ext: String, tokens: LdroidTokens): AnnotatedString {
    if (code.length > 48_000) return AnnotatedString(code)
    return buildAnnotatedString {
        append(code)
        when (ext.lowercase()) {
            "kt", "java", "kts" -> {
                Regex("""//[^\n]*""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxComment), it.range.first, it.range.last + 1)
                }
                Regex(""""([^"\\]|\\.)*"""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxString), it.range.first, it.range.last + 1)
                }
                Regex("""@[A-Za-z_][A-Za-z0-9_]*""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxAnnotation), it.range.first, it.range.last + 1)
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
            "xml", "html" -> {
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
                Regex("""#[^\n]*""").findAll(code).forEach {
                    addStyle(SpanStyle(color = tokens.syntaxComment), it.range.first, it.range.last + 1)
                }
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
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    val maxWidthDp = when {
        landscape -> 440.dp
        profile.category == "Tablet" -> 420.dp
        profile.category == "Foldable" -> 380.dp
        else -> 320.dp
    }

    var inputState by remember { mutableStateOf("") }
    var switchState by remember { mutableStateOf(true) }

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
                    .background(tokens.deviceScreenBg)
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
                            text = "Stale preview (last known good state kept): ${staleError ?: "Syntax error"}",
                            color = tokens.warning,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Text(
                    text = title,
                    color = tokens.primaryAccent,
                    fontSize = (17f * fontScale).sp,
                    fontWeight = FontWeight.Bold
                )

                nodes.forEach { node ->
                    when (node.type) {
                        VisualComponentType.TEXT -> Text(
                            text = node.label,
                            color = tokens.textPrimary,
                            fontSize = (14f * fontScale).sp
                        )
                        VisualComponentType.CARD -> Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = tokens.surface2,
                            border = BorderStroke(1.dp, tokens.border)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(node.label, color = tokens.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = (14f * fontScale).sp)
                                if (node.secondaryText.isNotBlank()) {
                                    Text(node.secondaryText, color = tokens.textMuted, fontSize = (12f * fontScale).sp)
                                }
                            }
                        }
                        VisualComponentType.BUTTON -> Button(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (node.useAccent) tokens.primaryAccent else tokens.secondaryAccent,
                                contentColor = tokens.onPrimaryAccent
                            )
                        ) {
                            Text(node.label, fontSize = (13f * fontScale).sp, fontWeight = FontWeight.SemiBold)
                        }
                        VisualComponentType.TEXT_FIELD -> OutlinedTextField(
                            value = inputState,
                            onValueChange = { inputState = it },
                            label = { Text(node.label, fontSize = (12f * fontScale).sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        VisualComponentType.SWITCH_CONTROL -> Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(node.label, color = tokens.textPrimary, fontSize = (13f * fontScale).sp)
                            Switch(checked = switchState, onCheckedChange = { switchState = it })
                        }
                        VisualComponentType.SPACER -> Spacer(modifier = Modifier.height(node.paddingDp.dp))
                        VisualComponentType.COLUMN, VisualComponentType.ROW -> Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = tokens.surface1,
                            border = BorderStroke(1.dp, tokens.border)
                        ) {
                            Text(
                                text = "${node.type.displayName}: ${node.label}",
                                color = tokens.textMuted,
                                fontSize = (12f * fontScale).sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
    }
}
