package com.example.ui.screens

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

data class FoldableBlock(
    val startLine: Int, // 1-based line containing '{'
    val endLine: Int    // 1-based line containing matching '}'
)

object CodeEditorHelpers {

    fun computeLineAndColumn(text: String, offset: Int): Pair<Int, Int> {
        val safe = offset.coerceIn(0, text.length)
        var line = 1
        var lastNewline = -1
        for (i in 0 until safe) {
            if (text[i] == '\n') {
                line++
                lastNewline = i
            }
        }
        val col = (safe - lastNewline)
        return line to col
    }

    fun offsetForLine(text: String, targetLine: Int): Int {
        if (targetLine <= 1 || text.isEmpty()) return 0
        var line = 1
        for (i in text.indices) {
            if (line == targetLine) return i
            if (text[i] == '\n') line++
        }
        return text.length
    }

    fun findMatchingBracket(text: String, cursorOffset: Int): Pair<Int, Int>? {
        if (text.isEmpty()) return null
        val candidates = listOf(cursorOffset - 1, cursorOffset).filter { it in text.indices }
        for (idx in candidates) {
            val ch = text[idx]
            val match = when (ch) {
                '{' -> scanForward(text, idx, '{', '}')
                '(' -> scanForward(text, idx, '(', ')')
                '[' -> scanForward(text, idx, '[', ']')
                '}' -> scanBackward(text, idx, '}', '{')
                ')' -> scanBackward(text, idx, ')', '(')
                ']' -> scanBackward(text, idx, ']', '[')
                else -> null
            }
            if (match != null) return idx to match
        }
        return null
    }

    private fun scanForward(text: String, start: Int, open: Char, close: Char): Int? {
        var depth = 0
        val limit = minOf(text.length - 1, start + 8000)
        for (i in start..limit) {
            when (text[i]) {
                open -> depth++
                close -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return null
    }

    private fun scanBackward(text: String, start: Int, close: Char, open: Char): Int? {
        var depth = 0
        val limit = maxOf(0, start - 8000)
        for (i in start downTo limit) {
            when (text[i]) {
                close -> depth++
                open -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return null
    }

    fun findFoldableBlocks(text: String): List<FoldableBlock> {
        val lines = text.lines()
        val stack = ArrayDeque<Int>()
        val blocks = mutableListOf<FoldableBlock>()
        var inString = false
        lines.forEachIndexed { idx, line ->
            val lineNum = idx + 1
            var i = 0
            while (i < line.length) {
                val c = line[i]
                val next = line.getOrNull(i + 1)
                if (!inString && c == '/' && next == '/') break
                if (c == '"' && line.getOrNull(i - 1) != '\\') {
                    inString = !inString
                } else if (!inString) {
                    if (c == '{') {
                        stack.addLast(lineNum)
                    } else if (c == '}' && stack.isNotEmpty()) {
                        val startLine = stack.removeLast()
                        if (lineNum - startLine >= 2) {
                            blocks.add(FoldableBlock(startLine = startLine, endLine = lineNum))
                        }
                    }
                }
                i++
            }
            inString = false
        }
        return blocks.sortedBy { it.startLine }
    }

    fun handleAutoIndentAndPairs(
        oldValue: TextFieldValue,
        newValue: TextFieldValue,
        tabWidth: Int
    ): TextFieldValue {
        val oldText = oldValue.text
        val newText = newValue.text
        if (newText.length == oldText.length + 1 && newValue.selection.collapsed) {
            val cursor = newValue.selection.start
            if (cursor > 0) {
                val insertedChar = newText[cursor - 1]
                if (insertedChar == '\n') {
                    val beforeNewline = newText.substring(0, cursor - 1)
                    val prevLineStart = beforeNewline.lastIndexOf('\n') + 1
                    val prevLine = beforeNewline.substring(prevLineStart)
                    val baseIndent = prevLine.takeWhile { it == ' ' || it == '\t' }
                    val trimmedPrev = prevLine.trimEnd()
                    val extraIndent = if (trimmedPrev.endsWith("{") || trimmedPrev.endsWith("(")) {
                        " ".repeat(tabWidth)
                    } else ""
                    val indent = baseIndent + extraIndent
                    if (indent.isNotEmpty()) {
                        val inserted = newText.substring(0, cursor) + indent + newText.substring(cursor)
                        return TextFieldValue(
                            text = inserted,
                            selection = TextRange(cursor + indent.length)
                        )
                    }
                } else {
                    val closingPair = when (insertedChar) {
                        '(' -> ')'
                        '[' -> ']'
                        '{' -> '}'
                        '"' -> '"'
                        else -> null
                    }
                    if (closingPair != null) {
                        val nextChar = newText.getOrNull(cursor)
                        val shouldAutoClose = nextChar == null ||
                            nextChar.isWhitespace() ||
                            nextChar == ')' ||
                            nextChar == ']' ||
                            nextChar == '}' ||
                            nextChar == ',' ||
                            nextChar == ';'
                        if (shouldAutoClose) {
                            val paired = newText.substring(0, cursor) + closingPair + newText.substring(cursor)
                            return TextFieldValue(
                                text = paired,
                                selection = TextRange(cursor)
                            )
                        }
                    }
                }
            }
        }
        return newValue
    }

    fun findOccurrences(
        text: String,
        query: String,
        ignoreCase: Boolean = true
    ): List<IntRange> {
        if (query.isEmpty() || text.isEmpty()) return emptyList()
        val results = mutableListOf<IntRange>()
        var idx = text.indexOf(query, startIndex = 0, ignoreCase = ignoreCase)
        while (idx >= 0 && results.size < 1000) {
            results.add(idx until (idx + query.length))
            idx = text.indexOf(query, startIndex = idx + maxOf(1, query.length), ignoreCase = ignoreCase)
        }
        return results
    }

    fun findNextOccurrenceOffset(
        text: String,
        query: String,
        currentOffset: Int,
        ignoreCase: Boolean = true
    ): IntRange? {
        val matches = findOccurrences(text, query, ignoreCase)
        if (matches.isEmpty()) return null
        return matches.firstOrNull { it.first >= currentOffset } ?: matches.first()
    }

    fun formatCode(code: String, ext: String, tabWidth: Int = 2): String {
        val normalizedExt = ext.lowercase()
        val lines = code.replace("\r\n", "\n").lines()
        val braceFormatted = normalizedExt in setOf("kt", "kts", "gradle", "java", "dart", "tsx", "ts", "jsx", "js", "css", "json")
        if (!braceFormatted) {
            val trimmed = lines.joinToString("\n") { it.trimEnd() }.trimEnd()
            return if (trimmed.isEmpty()) "" else "$trimmed\n"
        }
        var indentLevel = 0
        val indentUnit = " ".repeat(tabWidth.coerceIn(2, 8))
        val out = ArrayList<String>(lines.size)
        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) {
                out.add("")
                continue
            }
            val leadingClosers = trimmed.takeWhile { it == '}' || it == ']' || it == ')' }.length
            val lineLevel = (indentLevel - leadingClosers).coerceAtLeast(0)
            out.add(indentUnit.repeat(lineLevel) + trimmed)
            var opens = 0
            var closes = 0
            var inQuotes = false
            var escaped = false
            var idx = 0
            while (idx < trimmed.length) {
                val c = trimmed[idx]
                if (escaped) {
                    escaped = false
                    idx++
                    continue
                }
                if (c == '\\' && inQuotes) {
                    escaped = true
                    idx++
                    continue
                }
                if (c == '"') {
                    inQuotes = !inQuotes
                } else if (!inQuotes) {
                    if (c == '/' && trimmed.getOrNull(idx + 1) == '/') break
                    when (c) {
                        '{', '[' -> opens++
                        '}', ']' -> closes++
                    }
                }
                idx++
            }
            indentLevel = (indentLevel + opens - closes).coerceAtLeast(0)
        }
        val collapsed = out.joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trimEnd()
        return if (collapsed.isEmpty()) "" else "$collapsed\n"
    }

    fun insertTextAtCursor(value: TextFieldValue, snippet: String, cursorOffsetDelta: Int = snippet.length): TextFieldValue {
        val start = value.selection.min.coerceIn(0, value.text.length)
        val end = value.selection.max.coerceIn(0, value.text.length)
        val nextText = value.text.substring(0, start) + snippet + value.text.substring(end)
        val nextCursor = (start + cursorOffsetDelta).coerceIn(0, nextText.length)
        return TextFieldValue(text = nextText, selection = TextRange(nextCursor))
    }

    fun toggleLineComment(value: TextFieldValue, extension: String): TextFieldValue {
        val ext = extension.lowercase()
        val isXmlLike = ext == "xml" || ext == "html" || ext == "htm"
        val isHashComment = ext in setOf("yaml", "yml", "toml")
        val text = value.text
        val selStart = value.selection.min.coerceIn(0, text.length)
        val selEnd = value.selection.max.coerceIn(0, text.length)

        val lineStart = text.lastIndexOf('\n', (selStart - 1).coerceAtLeast(0))
            .let { if (selStart == 0 || it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', selEnd).let { if (it < 0) text.length else it }

        val block = text.substring(lineStart, lineEnd)
        val lines = block.lines()
        val updatedLines = if (isXmlLike) {
            val allCommented = lines.all { it.trim().startsWith("<!--") && it.trim().endsWith("-->") }
            lines.map { line ->
                val trimmed = line.trim()
                val indent = line.takeWhile { it == ' ' || it == '\t' }
                if (allCommented) {
                    indent + trimmed.removePrefix("<!--").removeSuffix("-->").trim()
                } else {
                    if (trimmed.isEmpty()) line else "$indent<!-- $trimmed -->"
                }
            }
        } else if (isHashComment) {
            val nonBlank = lines.filter { it.isNotBlank() }
            val allCommented = nonBlank.isNotEmpty() && nonBlank.all { it.trimStart().startsWith("#") }
            lines.map { line ->
                if (line.isBlank()) line
                else if (allCommented) {
                    val idx = line.indexOf('#')
                    if (idx >= 0) {
                        val after = line.substring(idx + 1)
                        line.substring(0, idx) + if (after.startsWith(" ")) after.drop(1) else after
                    } else line
                } else {
                    val indent = line.takeWhile { it == ' ' || it == '\t' }
                    "$indent# ${line.drop(indent.length)}"
                }
            }
        } else {
            val nonBlank = lines.filter { it.isNotBlank() }
            val allCommented = nonBlank.isNotEmpty() && nonBlank.all { it.trimStart().startsWith("//") }
            lines.map { line ->
                if (line.isBlank()) line
                else if (allCommented) {
                    val idx = line.indexOf("//")
                    if (idx >= 0) {
                        val after = line.substring(idx + 2)
                        line.substring(0, idx) + if (after.startsWith(" ")) after.drop(1) else after
                    } else line
                } else {
                    val indent = line.takeWhile { it == ' ' || it == '\t' }
                    "$indent// ${line.drop(indent.length)}"
                }
            }
        }
        val replaced = updatedLines.joinToString("\n")
        val nextText = text.substring(0, lineStart) + replaced + text.substring(lineEnd)
        return TextFieldValue(text = nextText, selection = TextRange(lineStart + replaced.length))
    }

    private val KOTLIN_COMPLETIONS = listOf(
        "Composable", "remember", "mutableStateOf", "mutableIntStateOf", "LaunchedEffect",
        "Modifier", "fillMaxWidth", "fillMaxSize", "padding", "Column", "Row", "Box",
        "Surface", "Button", "Text", "OutlinedTextField", "LazyColumn", "Scaffold",
        "MaterialTheme", "Spacer", "Alignment", "Arrangement", "override fun", "private val",
        "data class", "viewModelScope.launch", "collectAsStateWithLifecycle"
    )

    private val XML_COMPLETIONS = listOf(
        "TextView", "Button", "EditText", "LinearLayout", "ConstraintLayout", "CardView",
        "android:layout_width=\"match_parent\"", "android:layout_height=\"wrap_content\"",
        "android:text=\"\"", "android:padding=\"16dp\"", "android:orientation=\"vertical\""
    )

    private val HTML_COMPLETIONS = listOf(
        "<div></div>", "<section></section>", "<h1></h1>", "<p></p>", "<button></button>",
        "class=\"\"", "style=\"\"", "display: flex;", "background:", "border-radius:"
    )

    private val DART_COMPLETIONS = listOf(
        "StatelessWidget", "StatefulWidget", "BuildContext", "Widget build", "Scaffold",
        "AppBar", "Column", "Row", "Container", "Card", "ElevatedButton", "TextField",
        "setState(() {", "EdgeInsets.all(16)", "MaterialApp", "SizedBox"
    )

    private val REACT_NATIVE_COMPLETIONS = listOf(
        "useState", "useEffect", "SafeAreaView", "View", "Text", "TouchableOpacity",
        "Pressable", "TextInput", "Switch", "ScrollView", "StyleSheet.create",
        "export default function", "React.JSX.Element", "document.getElementById"
    )

    private val JAVA_COMPLETIONS = listOf(
        "AppCompatActivity", "onCreate", "setContentView", "findViewById", "TextView",
        "Button", "EditText", "setOnClickListener", "setText", "Override", "public class"
    )

    fun computeAutocompleteSuggestions(text: String, cursorOffset: Int, ext: String): List<String> {
        val safeCursor = cursorOffset.coerceIn(0, text.length)
        var start = safeCursor
        while (start > 0 && (text[start - 1].isLetterOrDigit() || text[start - 1] == '_' || text[start - 1] == ':')) {
            start--
        }
        val prefix = text.substring(start, safeCursor)
        if (prefix.length < 2) return emptyList()
        val pool = when (ext.lowercase()) {
            "xml" -> XML_COMPLETIONS
            "html", "htm", "css" -> HTML_COMPLETIONS
            "dart" -> DART_COMPLETIONS
            "tsx", "ts", "jsx", "js" -> REACT_NATIVE_COMPLETIONS
            "java" -> JAVA_COMPLETIONS
            else -> KOTLIN_COMPLETIONS
        }
        return pool.filter { it.startsWith(prefix, ignoreCase = true) && !it.equals(prefix, ignoreCase = false) }
            .take(6)
    }

    fun applyAutocompleteSuggestion(value: TextFieldValue, suggestion: String): TextFieldValue {
        val safeCursor = value.selection.start.coerceIn(0, value.text.length)
        var start = safeCursor
        while (start > 0 && (value.text[start - 1].isLetterOrDigit() || value.text[start - 1] == '_' || value.text[start - 1] == ':')) {
            start--
        }
        val nextText = value.text.substring(0, start) + suggestion + value.text.substring(safeCursor)
        return TextFieldValue(text = nextText, selection = TextRange(start + suggestion.length))
    }
}
