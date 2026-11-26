package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.LdroidSettingsState
import com.example.model.OpenEditorTab
import com.example.ui.components.SyntaxHighlightedTransformation
import com.example.ui.components.highlightSyntax
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens

@Composable
fun CodeEditorSurface(
    tab: OpenEditorTab,
    settings: LdroidSettingsState,
    pendingJumpLine: Int?,
    onJumpConsumed: () -> Unit,
    onTextChange: (String) -> Unit,
    onCycleEncoding: () -> Unit,
    onToggleLineEnding: () -> Unit,
    searchQuery: String = "",
    searchMatchCase: Boolean = false,
    pendingOffsetJump: IntRange? = null,
    onOffsetJumpConsumed: () -> Unit = {},
    onAdjustFontSize: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    val ext = tab.relativePath.substringAfterLast('.', "")
    val scrollVertical = rememberScrollState()
    val scrollHorizontal = rememberScrollState()
    val density = LocalDensity.current

    var textFieldValue by remember(tab.relativePath) {
        mutableStateOf(TextFieldValue(text = tab.content, selection = TextRange.Zero))
    }

    // Keep TextFieldValue synced when external operations (Undo/Redo/FindReplace/AutoSave/Reload) alter tab.content
    LaunchedEffect(tab.content, tab.relativePath) {
        if (textFieldValue.text != tab.content) {
            val safeCursor = textFieldValue.selection.start.coerceIn(0, tab.content.length)
            textFieldValue = TextFieldValue(text = tab.content, selection = TextRange(safeCursor))
        }
    }

    val lineHeightSp = (settings.editorFontSizeSp + 6)
    val lineHeightPx = with(density) { lineHeightSp.sp.toPx() }
    val indentGuideStepPx = with(density) { (settings.editorFontSizeSp * 0.6f * settings.tabWidth).sp.toPx() }

    // Jump to line (from Go To Line, Problems tap, or Find-in-Files match)
    LaunchedEffect(pendingJumpLine, tab.relativePath) {
        if (pendingJumpLine != null && pendingJumpLine > 0) {
            val offset = CodeEditorHelpers.offsetForLine(textFieldValue.text, pendingJumpLine)
            textFieldValue = textFieldValue.copy(selection = TextRange(offset))
            val targetScrollPx = ((pendingJumpLine - 2).coerceAtLeast(0) * lineHeightPx).toInt()
            scrollVertical.animateScrollTo(targetScrollPx)
            onJumpConsumed()
        }
    }

    // Jump to character range (from Find Next)
    LaunchedEffect(pendingOffsetJump, tab.relativePath) {
        if (pendingOffsetJump != null && textFieldValue.text.isNotEmpty()) {
            val start = pendingOffsetJump.first.coerceIn(0, textFieldValue.text.length)
            val end = (pendingOffsetJump.last + 1).coerceIn(start, textFieldValue.text.length)
            textFieldValue = textFieldValue.copy(selection = TextRange(start, end))
            val (targetLine, _) = CodeEditorHelpers.computeLineAndColumn(textFieldValue.text, start)
            val targetScrollPx = ((targetLine - 2).coerceAtLeast(0) * lineHeightPx).toInt()
            scrollVertical.animateScrollTo(targetScrollPx)
            onOffsetJumpConsumed()
        }
    }

    val lines = remember(textFieldValue.text) { textFieldValue.text.lines() }
    val linesCount = lines.size.coerceAtLeast(1)
    val (cursorLine, cursorCol) = remember(textFieldValue.text, textFieldValue.selection.start) {
        CodeEditorHelpers.computeLineAndColumn(textFieldValue.text, textFieldValue.selection.start)
    }
    val matchedBrackets = remember(textFieldValue.text, textFieldValue.selection.start) {
        CodeEditorHelpers.findMatchingBracket(textFieldValue.text, textFieldValue.selection.start)
    }
    val foldableBlocks = remember(textFieldValue.text) {
        CodeEditorHelpers.findFoldableBlocks(textFieldValue.text)
    }
    val blockByStartLine = remember(foldableBlocks) {
        foldableBlocks.associateBy { it.startLine }
    }
    var foldedStartLines by remember(tab.relativePath) { mutableStateOf(emptySet<Int>()) }

    val searchMatches = remember(textFieldValue.text, searchQuery, searchMatchCase) {
        CodeEditorHelpers.findOccurrences(textFieldValue.text, searchQuery, !searchMatchCase)
    }
    val activeSearchRange = remember(searchMatches, textFieldValue.selection) {
        searchMatches.firstOrNull {
            it.first == textFieldValue.selection.min ||
                (textFieldValue.selection.start in it)
        }
    }

    val autocompleteSuggestions = remember(textFieldValue.text, textFieldValue.selection.start, ext) {
        if (tab.isLargeFilePreview) emptyList()
        else CodeEditorHelpers.computeAutocompleteSuggestions(textFieldValue.text, textFieldValue.selection.start, ext)
    }

    val selectionColors = remember(tokens.primaryAccent, tokens.editorSelection) {
        TextSelectionColors(
            handleColor = tokens.primaryAccent,
            backgroundColor = tokens.editorSelection
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = tokens.surface1,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, tokens.border)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (tab.isLargeFilePreview) {
                Surface(color = tokens.warning.copy(alpha = 0.16f)) {
                    Text(
                        "Large file streaming preview (first 128 KB of ${tab.totalBytes / 1024} KB). Read-only to prevent memory exhaustion.",
                        color = tokens.warning,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }
            }

            if (foldedStartLines.isNotEmpty()) {
                Surface(
                    color = tokens.primaryAccent.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, tokens.primaryAccent.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Code Folding active (${foldedStartLines.size} block(s) collapsed — unfold to edit)",
                            color = tokens.primaryAccent,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Unfold All",
                            color = tokens.secondaryAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { foldedStartLines = emptySet() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Main scrollable editor viewport
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollVertical)
                    .padding(vertical = 8.dp)
            ) {
                if (settings.showLineNumbers) {
                    Column(
                        modifier = Modifier
                            .background(tokens.surface2)
                            .padding(horizontal = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        var lineIdx = 1
                        while (lineIdx <= linesCount) {
                            val block = blockByStartLine[lineIdx]
                            val isFolded = lineIdx in foldedStartLines && block != null
                            val isCurrent = lineIdx == cursorLine
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(if (isCurrent) tokens.editorCurrentLine else tokens.surface2)
                                    .padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = lineIdx.toString(),
                                    color = if (isCurrent) tokens.primaryAccent else tokens.editorLineNumber,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = settings.editorFontSizeSp.sp,
                                    lineHeight = lineHeightSp.sp
                                )
                                if (block != null) {
                                    Text(
                                        text = if (isFolded) " ▸" else " ▾",
                                        color = if (isFolded) tokens.secondaryAccent else tokens.textMuted,
                                        fontSize = 10.sp,
                                        lineHeight = lineHeightSp.sp,
                                        modifier = Modifier.clickable {
                                            foldedStartLines = if (isFolded) {
                                                foldedStartLines - lineIdx
                                            } else {
                                                foldedStartLines + lineIdx
                                            }
                                        }
                                    )
                                } else {
                                    Spacer(Modifier.width(12.dp))
                                }
                            }
                            lineIdx = if (isFolded && block != null) block.endLine + 1 else lineIdx + 1
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(if (settings.wordWrap) Modifier else Modifier.horizontalScroll(scrollHorizontal))
                        .drawBehind {
                            // Subtle indentation guide lines
                            var x = indentGuideStepPx
                            val maxGuides = 6
                            var count = 0
                            while (x < size.width && count < maxGuides) {
                                drawLine(
                                    color = tokens.divider.copy(alpha = 0.55f),
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = 1f
                                )
                                x += indentGuideStepPx
                                count++
                            }
                            // Active cursor line highlight
                            if (foldedStartLines.isEmpty()) {
                                val topY = (cursorLine - 1) * lineHeightPx
                                drawRect(
                                    color = tokens.editorCurrentLine,
                                    topLeft = Offset(0f, topY),
                                    size = Size(size.width, lineHeightPx)
                                )
                            }
                        }
                        .padding(horizontal = 10.dp)
                ) {
                    if (foldedStartLines.isNotEmpty()) {
                        // Non-destructive folded view: tapping any collapsed chip unfolds it cleanly
                        Column(modifier = Modifier.fillMaxWidth()) {
                            var lineIdx = 1
                            while (lineIdx <= linesCount) {
                                val rawLine = lines.getOrNull(lineIdx - 1).orEmpty()
                                val block = blockByStartLine[lineIdx]
                                val isFolded = lineIdx in foldedStartLines && block != null
                                if (isFolded && block != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = highlightSyntax(rawLine, ext, tokens),
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = settings.editorFontSizeSp.sp,
                                            lineHeight = lineHeightSp.sp
                                        )
                                        Surface(
                                            color = tokens.secondaryAccent.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, tokens.secondaryAccent.copy(alpha = 0.5f)),
                                            modifier = Modifier.clickable {
                                                foldedStartLines = foldedStartLines - lineIdx
                                            }
                                        ) {
                                            Text(
                                                text = "... ${block.endLine - block.startLine} lines (tap to unfold)",
                                                color = tokens.secondaryAccent,
                                                fontSize = 10.sp,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    lineIdx = block.endLine + 1
                                } else {
                                    Text(
                                        text = highlightSyntax(rawLine, ext, tokens),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = settings.editorFontSizeSp.sp,
                                        lineHeight = lineHeightSp.sp,
                                        modifier = Modifier.clickable {
                                            // Tapping unfolded lines while folded returns to live editing
                                            foldedStartLines = emptySet()
                                        }
                                    )
                                    lineIdx++
                                }
                            }
                        }
                    } else {
                        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = { incoming ->
                                    if (!tab.isLargeFilePreview) {
                                        val processed = CodeEditorHelpers.handleAutoIndentAndPairs(
                                            oldValue = textFieldValue,
                                            newValue = incoming,
                                            tabWidth = settings.tabWidth
                                        )
                                        textFieldValue = processed
                                        if (processed.text != tab.content) {
                                            onTextChange(processed.text)
                                        }
                                    }
                                },
                                readOnly = tab.isLargeFilePreview,
                                textStyle = TextStyle(
                                    color = tokens.textPrimary,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = settings.editorFontSizeSp.sp,
                                    lineHeight = lineHeightSp.sp
                                ),
                                cursorBrush = SolidColor(tokens.primaryAccent),
                                visualTransformation = remember(tokens, ext, matchedBrackets, searchMatches, activeSearchRange) {
                                    SyntaxHighlightedTransformation(
                                        tokens = tokens,
                                        extension = ext,
                                        matchedBracketIndices = matchedBrackets,
                                        searchHighlightRanges = searchMatches,
                                        activeSearchRange = activeSearchRange
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("code_editor_input")
                            )
                        }
                    }
                }
            }

            // Live Autocomplete Suggestion Strip
            if (autocompleteSuggestions.isNotEmpty() && foldedStartLines.isEmpty()) {
                Surface(color = tokens.surface2, border = BorderStroke(1.dp, tokens.primaryAccent.copy(alpha = 0.35f))) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        autocompleteSuggestions.forEach { suggestion ->
                            Surface(
                                color = tokens.primaryAccent.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, tokens.primaryAccent.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable {
                                    val next = CodeEditorHelpers.applyAutocompleteSuggestion(textFieldValue, suggestion)
                                    textFieldValue = next
                                    onTextChange(next.text)
                                }
                            ) {
                                Text(
                                    text = suggestion,
                                    color = tokens.primaryAccent,
                                    fontSize = 11.sp,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Mobile Coding Accessory Bar (Indent, Brackets, Comment, Fold)
            if (!tab.isLargeFilePreview) {
                Surface(color = tokens.surface2.copy(alpha = 0.85f), border = BorderStroke(1.dp, tokens.divider)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val applySnippet: (String, Int) -> Unit = { snippet, delta ->
                            foldedStartLines = emptySet()
                            val next = CodeEditorHelpers.insertTextAtCursor(textFieldValue, snippet, delta)
                            textFieldValue = next
                            onTextChange(next.text)
                        }
                        AccessoryKeyChip("TAB") { applySnippet(" ".repeat(settings.tabWidth), settings.tabWidth) }
                        AccessoryKeyChip("{ }") { applySnippet("{\n    \n}", 6) }
                        AccessoryKeyChip("( )") { applySnippet("()", 1) }
                        AccessoryKeyChip("[ ]") { applySnippet("[]", 1) }
                        AccessoryKeyChip("\" \"") { applySnippet("\"\"", 1) }
                        AccessoryKeyChip("->") { applySnippet(" -> ", 4) }
                        AccessoryKeyChip("// Comment") {
                            foldedStartLines = emptySet()
                            val next = CodeEditorHelpers.toggleLineComment(textFieldValue, ext)
                            textFieldValue = next
                            onTextChange(next.text)
                        }
                        if (foldableBlocks.isNotEmpty()) {
                            Surface(
                                color = tokens.surface1,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, tokens.border),
                                modifier = Modifier.clickable {
                                    foldedStartLines = if (foldedStartLines.isEmpty()) {
                                        foldableBlocks.map { it.startLine }.toSet()
                                    } else emptySet()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (foldedStartLines.isEmpty()) Icons.Filled.UnfoldLess else Icons.Filled.UnfoldMore,
                                        contentDescription = "Toggle code folding",
                                        tint = tokens.secondaryAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (foldedStartLines.isEmpty()) "Fold" else "Unfold",
                                        color = tokens.textPrimary,
                                        fontSize = 11.sp,
                                        fontFamily = JetBrainsMonoFontFamily
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Editor Status Bar (Ln/Col, Lines, Modified/Saved, Bracket Match, Encoding, Line Ending)
            Surface(color = tokens.surface2, border = BorderStroke(1.dp, tokens.divider)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ln $cursorLine, Col $cursorCol • $linesCount lines • ${if (tab.isDirty) "Modified" else "Saved"}",
                        color = if (tab.isDirty) tokens.warning else tokens.success,
                        fontSize = 11.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (matchedBrackets != null) {
                            Text(
                                text = "[]",
                                color = tokens.primaryAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "−",
                                color = tokens.primaryAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onAdjustFontSize(-1) }
                                    .padding(horizontal = 4.dp)
                            )
                            Text(
                                text = "${settings.editorFontSizeSp}sp",
                                color = tokens.textMuted,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                            Text(
                                text = "+",
                                color = tokens.primaryAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onAdjustFontSize(1) }
                                    .padding(horizontal = 4.dp)
                            )
                        }
                        Text(
                            tab.encoding.charsetName,
                            color = tokens.primaryAccent,
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            modifier = Modifier.clickable(onClick = onCycleEncoding)
                        )
                        Text(
                            tab.lineEnding.label,
                            color = tokens.secondaryAccent,
                            fontSize = 11.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            modifier = Modifier.clickable(onClick = onToggleLineEnding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessoryKeyChip(label: String, onClick: () -> Unit) {
    val tokens = LocalLdroidTokens.current
    Surface(
        color = tokens.surface1,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, tokens.border),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = tokens.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = JetBrainsMonoFontFamily,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}
