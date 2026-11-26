package com.example.data.analysis

import com.example.data.security.SecureCredentialVault
import com.example.model.DiagnosticIssue
import com.example.model.DiagnosticSeverity
import com.example.model.VisualComponentType
import com.example.model.VisualNode
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader

data class ComposeParseResult(
    val isSuccess: Boolean,
    val title: String,
    val nodes: List<VisualNode>,
    val errorMessage: String? = null
)

object StaticAnalyzer {

    fun analyzeWorkspace(rootDir: File): List<DiagnosticIssue> {
        if (!rootDir.exists()) return emptyList()
        val issues = mutableListOf<DiagnosticIssue>()
        rootDir.walkTopDown()
            .filter { it.isFile && it.length() <= 512 * 1024 }
            .forEach { file ->
                val rel = file.relativeTo(rootDir).path.replace(File.separatorChar, '/')
                val content = runCatching { file.readText() }.getOrNull() ?: return@forEach
                issues.addAll(analyzeFile(rel, content))
            }
        return issues
    }

    fun analyzeFile(relativePath: String, content: String): List<DiagnosticIssue> {
        val issues = mutableListOf<DiagnosticIssue>()
        val ext = relativePath.substringAfterLast('.', "").lowercase()

        // Secret detection across all text files
        SecureCredentialVault.scanTextForSecrets(content).forEach { (line, label) ->
            issues.add(
                DiagnosticIssue(
                    filePath = relativePath,
                    line = line,
                    severity = DiagnosticSeverity.SECRET,
                    source = "Secret Scanner",
                    message = "$label detected. Move credentials to Encrypted Vault or CI secrets."
                )
            )
        }

        when (ext) {
            "kt", "java", "kts", "gradle", "dart", "tsx", "ts", "jsx", "js", "css" ->
                issues.addAll(analyzeKotlinOrJava(relativePath, content))
            "xml" -> issues.addAll(analyzeXml(relativePath, content))
            "json" -> issues.addAll(analyzeJson(relativePath, content))
        }
        return issues
    }

    private fun analyzeKotlinOrJava(path: String, content: String): List<DiagnosticIssue> {
        val issues = mutableListOf<DiagnosticIssue>()
        val lines = content.lines()
        var braceBalance = 0
        var parenBalance = 0
        var inBlockComment = false
        val allowSingleQuoteStrings = path.substringAfterLast('.', "").lowercase() in setOf("dart", "tsx", "ts", "jsx", "js", "css")

        lines.forEachIndexed { idx, rawLine ->
            val lineNum = idx + 1
            var i = 0
            var stringQuote: Char? = null
            while (i < rawLine.length) {
                val c = rawLine[i]
                val next = rawLine.getOrNull(i + 1)
                if (inBlockComment) {
                    if (c == '*' && next == '/') {
                        inBlockComment = false
                        i += 2
                        continue
                    }
                    i++
                    continue
                }
                if (stringQuote == null && c == '/' && next == '/') break
                if (stringQuote == null && c == '/' && next == '*') {
                    inBlockComment = true
                    i += 2
                    continue
                }
                val isQuote = c == '"' || (allowSingleQuoteStrings && (c == '\'' || c == '`'))
                if (isQuote && rawLine.getOrNull(i - 1) != '\\') {
                    stringQuote = if (stringQuote == null) c else if (stringQuote == c) null else stringQuote
                } else if (stringQuote == null) {
                    when (c) {
                        '{' -> braceBalance++
                        '}' -> {
                            braceBalance--
                            if (braceBalance < 0) {
                                issues.add(
                                    DiagnosticIssue(
                                        path, lineNum, DiagnosticSeverity.ERROR,
                                        "Syntax Check", "Unexpected closing brace '}'"
                                    )
                                )
                                braceBalance = 0
                            }
                        }
                        '(' -> parenBalance++
                        ')' -> {
                            parenBalance--
                            if (parenBalance < 0) {
                                issues.add(
                                    DiagnosticIssue(
                                        path, lineNum, DiagnosticSeverity.ERROR,
                                        "Syntax Check", "Unexpected closing parenthesis ')'"
                                    )
                                )
                                parenBalance = 0
                            }
                        }
                    }
                }
                i++
            }
            if (rawLine.contains("TODO(") || rawLine.contains("FIXME")) {
                issues.add(
                    DiagnosticIssue(
                        path, lineNum, DiagnosticSeverity.INFO,
                        "Code Quality", "Pending TODO/FIXME marker"
                    )
                )
            }
        }

        if (braceBalance > 0) {
            issues.add(
                DiagnosticIssue(
                    path, lines.size, DiagnosticSeverity.ERROR,
                    "Syntax Check", "Unclosed '{' ($braceBalance unclosed block(s))"
                )
            )
        }
        if (parenBalance > 0) {
            issues.add(
                DiagnosticIssue(
                    path, lines.size, DiagnosticSeverity.ERROR,
                    "Syntax Check", "Unclosed '(' ($parenBalance unclosed parenthesis)"
                )
            )
        }
        if (path.endsWith("build.gradle.kts") && content.contains("http://")) {
            issues.add(
                DiagnosticIssue(
                    path, 1, DiagnosticSeverity.WARNING,
                    "Supply Chain", "Insecure http:// repository or endpoint detected; prefer https://"
                )
            )
        }
        return issues
    }

    private fun analyzeXml(path: String, content: String): List<DiagnosticIssue> {
        val issues = mutableListOf<DiagnosticIssue>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(content))
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                parser.next()
            }
        } catch (e: Exception) {
            issues.add(
                DiagnosticIssue(
                    path, 1, DiagnosticSeverity.ERROR,
                    "XML Parser", e.message ?: "Malformed XML document"
                )
            )
        }

        if (path.endsWith("AndroidManifest.xml")) {
            if (content.contains("MANAGE_EXTERNAL_STORAGE") || content.contains("READ_EXTERNAL_STORAGE")) {
                issues.add(
                    DiagnosticIssue(
                        path, 1, DiagnosticSeverity.WARNING,
                        "Play Policy Check",
                        "Broad storage permission declared. Prefer Storage Access Framework or Photo Picker."
                    )
                )
            }
            if (content.contains("usesCleartextTraffic=\"true\"")) {
                issues.add(
                    DiagnosticIssue(
                        path, 1, DiagnosticSeverity.WARNING,
                        "Security Check",
                        "Cleartext HTTP traffic enabled. Disable in production builds."
                    )
                )
            }
        }
        return issues
    }

    private fun analyzeJson(path: String, content: String): List<DiagnosticIssue> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return emptyList()
        return try {
            if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONObject(trimmed)
            emptyList()
        } catch (e: Exception) {
            listOf(
                DiagnosticIssue(
                    path, 1, DiagnosticSeverity.ERROR,
                    "JSON Validator", e.message ?: "Invalid JSON syntax"
                )
            )
        }
    }

    /**
     * Parses active Kotlin Compose or HTML source into previewable VisualNodes ONLY when syntax passes.
     * Preserves template variables like $count so the interactive device frame resolves live reactive state.
     */
    fun parseCodeForLivePreview(relativePath: String, content: String): ComposeParseResult {
        val issues = analyzeFile(relativePath, content)
        val firstError = issues.firstOrNull { it.severity == DiagnosticSeverity.ERROR }
        if (firstError != null) {
            return ComposeParseResult(
                isSuccess = false,
                title = relativePath.substringAfterLast('/'),
                nodes = emptyList(),
                errorMessage = "Line ${firstError.line}: ${firstError.message}"
            )
        }

        val nodes = mutableListOf<VisualNode>()
        val ext = relativePath.substringAfterLast('.', "").lowercase()
        val fnMatch = Regex("""@Composable\s+fun\s+([A-Za-z0-9_]+)""").find(content)
            ?: Regex("""class\s+([A-Za-z0-9_]+)\s+extends\s+(?:StatelessWidget|StatefulWidget|AppCompatActivity)""").find(content)
            ?: Regex("""(?:export\s+default\s+)?function\s+([A-Za-z0-9_]+)""").find(content)
        val title = fnMatch?.groupValues?.getOrNull(1)
            ?: Regex("""<title>([^<]+)</title>""").find(content)?.groupValues?.getOrNull(1)
            ?: Regex("""appBar:\s*AppBar\([^)]*Text\(\s*['"]([^'"]+)['"]""").find(content)?.groupValues?.getOrNull(1)
            ?: relativePath.substringAfterLast('/')

        val textRegex = Regex("""Text\s*\(\s*(?:text\s*=\s*)?"([^"]+)"""")
        val buttonRegex = Regex("""Button\s*\([^)]*\)\s*\{\s*Text\s*\(\s*"([^"]+)"""")
        val htmlHeadingRegex = Regex("""<(?:h1|h2|p|button)[^>]*>([^<]+)</""")

        var idSeq = 1
        when (ext) {
            "html", "htm" -> {
                htmlHeadingRegex.findAll(content).forEach { match ->
                    val rawTag = match.value.lowercase()
                    val type = if (rawTag.startsWith("<button")) VisualComponentType.BUTTON else VisualComponentType.TEXT
                    nodes.add(
                        VisualNode(
                            id = "code_${idSeq++}",
                            type = type,
                            label = match.groupValues[1].trim(),
                            useAccent = type == VisualComponentType.BUTTON
                        )
                    )
                }
            }
            "dart" -> {
                val dartString = Regex("""['"]([^'"]+)['"]""")
                val dartText = Regex("""Text\s*\(\s*(?:const\s+)?['"]([^'"]+)['"]""")
                val dartBtn = Regex("""(?:ElevatedButton|FilledButton|OutlinedButton|TextButton|FloatingActionButton)\s*\(""")
                var pendingBtn = false
                content.lines().forEach { line ->
                    val t = line.trim()
                    when {
                        dartBtn.containsMatchIn(t) -> {
                            val inlineLabel = dartText.find(t)?.groupValues?.getOrNull(1)
                            if (inlineLabel != null) {
                                nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.BUTTON, inlineLabel, useAccent = true, colorTag = "Cyan"))
                            } else {
                                pendingBtn = true
                            }
                        }
                        pendingBtn -> {
                            val lbl = dartText.find(t)?.groupValues?.getOrNull(1)
                            if (lbl != null) {
                                nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.BUTTON, lbl, useAccent = true, colorTag = "Cyan"))
                                pendingBtn = false
                            } else if (t.contains("),") || t == ")") {
                                nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.BUTTON, "Action", useAccent = true, colorTag = "Cyan"))
                                pendingBtn = false
                            }
                        }
                        t.contains("TextField(") || t.contains("TextFormField(") -> {
                            val hint = dartString.find(t)?.groupValues?.getOrNull(1) ?: "Input"
                            nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.TEXT_FIELD, hint))
                        }
                        t.contains("Switch(") -> nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.SWITCH_CONTROL, "Toggle Option"))
                        t.contains("Card(") -> {
                            val cardTxt = dartText.find(t)?.groupValues?.getOrNull(1)
                            if (cardTxt != null) nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.CARD, cardTxt, "Flutter Card"))
                        }
                        else -> {
                            dartText.findAll(t).forEach { m ->
                                val normalized = m.groupValues[1]
                                    .replace("\$_counter", "\$count")
                                    .replace("\$counter", "\$count")
                                if (normalized.contains("\$count")) {
                                    nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.CARD, normalized, "Flutter StatefulWidget", colorTag = "Cyan"))
                                } else {
                                    nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.TEXT, normalized))
                                }
                            }
                        }
                    }
                }
            }
            "tsx", "ts", "jsx", "js" -> {
                val rnText = Regex("""<Text[^>]*>([^<]+)</Text>""")
                val rnButton = Regex("""<Button[^>]*title\s*=\s*["']([^"']+)["']""")
                val rnTouchBtn = Regex("""<(?:TouchableOpacity|Pressable|button)[^>]*>[\s\S]*?(?:<Text[^>]*>)?([^<{]+)(?:</Text>)?[\s\S]*?</(?:TouchableOpacity|Pressable|button)>""")
                val rnInput = Regex("""<TextInput[^>]*placeholder\s*=\s*["']([^"']+)["']""")
                val webHeading = Regex("""<(?:h1|h2|p)[^>]*>([^<]+)</(?:h1|h2|p)>""")

                val btnLabels = mutableSetOf<String>()
                rnButton.findAll(content).forEach { m ->
                    val lbl = m.groupValues[1].trim()
                    btnLabels.add(lbl)
                    nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.BUTTON, lbl, useAccent = true, colorTag = "Cyan"))
                }
                rnTouchBtn.findAll(content).forEach { m ->
                    val lbl = m.groupValues[1].trim()
                    if (lbl.isNotBlank()) {
                        btnLabels.add(lbl)
                        nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.BUTTON, lbl, useAccent = true, colorTag = "Cyan"))
                    }
                }
                rnInput.findAll(content).forEach { m ->
                    nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.TEXT_FIELD, m.groupValues[1].trim()))
                }
                if (content.contains("<Switch")) {
                    nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.SWITCH_CONTROL, "Toggle Switch"))
                }
                rnText.findAll(content).forEach { m ->
                    val raw = m.groupValues[1].trim()
                    if (raw.isNotBlank() && raw !in btnLabels) {
                        val normalized = raw
                            .replace("{count}", "\$count")
                            .replace("\${count}", "\$count")
                        if (normalized.contains("\$count")) {
                            nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.CARD, normalized, "React State Hook", colorTag = "Cyan"))
                        } else {
                            nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.TEXT, normalized))
                        }
                    }
                }
                webHeading.findAll(content).forEach { m ->
                    nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.TEXT, m.groupValues[1].trim()))
                }
            }
            "java" -> {
                val layoutMatch = Regex("""setContentView\s*\(\s*R\.layout\.([A-Za-z0-9_]+)\s*\)""").find(content)
                if (layoutMatch != null) {
                    nodes.add(
                        VisualNode(
                            "code_${idSeq++}",
                            VisualComponentType.CARD,
                            "Layout: ${layoutMatch.groupValues[1]}.xml",
                            "Bound via AppCompatActivity",
                            colorTag = "Cyan"
                        )
                    )
                }
                Regex("""setText\s*\(\s*"([^"]+)"(?:\s*\+\s*count)?\s*\)""").findAll(content).forEach { m ->
                    val txt = m.groupValues[1]
                    val display = if (m.value.contains("count")) "$txt\$count" else txt
                    nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.TEXT, display))
                }
                if (content.contains("Button")) {
                    val btnName = Regex("""Button\s+([A-Za-z0-9_]+)""").find(content)?.groupValues?.getOrNull(1) ?: "Action Button"
                    nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.BUTTON, btnName, useAccent = true, colorTag = "Emerald"))
                }
            }
            "css" -> {
                val ruleRegex = Regex("""([.#]?[A-Za-z_-][A-Za-z0-9_.-]*)\s*\{([^}]*)\}""")
                ruleRegex.findAll(content).take(8).forEach { m ->
                    nodes.add(
                        VisualNode(
                            "code_${idSeq++}",
                            VisualComponentType.CARD,
                            "CSS Selector: ${m.groupValues[1].trim()}",
                            m.groupValues[2].trim().replace(Regex("""\s+"""), " ").take(72),
                            colorTag = "Purple"
                        )
                    )
                }
            }
            "kts", "gradle" -> {
                val appId = Regex("""applicationId\s*=\s*"([^"]+)"""").find(content)?.groupValues?.getOrNull(1)
                val minSdk = Regex("""minSdk\s*=\s*(\d+)""").find(content)?.groupValues?.getOrNull(1)
                val targetSdk = Regex("""targetSdk\s*=\s*(\d+)""").find(content)?.groupValues?.getOrNull(1)
                if (appId != null) {
                    nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.CARD, "App ID: $appId", "minSdk ${minSdk ?: "?"} • targetSdk ${targetSdk ?: "?"}", colorTag = "Cyan"))
                }
                Regex("""(?:implementation|testImplementation)\s*\(\s*"([^"]+)"\s*\)""").findAll(content).take(6).forEach { m ->
                    nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.TEXT, "dep: ${m.groupValues[1]}"))
                }
            }
            "json" -> {
                Regex(""""([A-Za-z0-9_-]+)"\s*:\s*"([^"]*)"""").findAll(content).take(8).forEach { m ->
                    nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.CARD, m.groupValues[1], m.groupValues[2], colorTag = "Emerald"))
                }
            }
            else -> {
            val buttonTexts = buttonRegex.findAll(content).map { it.groupValues[1] }.toSet()
            val lines = content.lines()
            var insideCard = false
            var cardTitle: String? = null
            var cardBraceDepth = 0

            lines.forEach { line ->
                val trimmed = line.trim()
                val colorTag = when {
                    trimmed.contains("0xFF00E5FF") -> "Cyan"
                    trimmed.contains("0xFF9E7CFF") -> "Purple"
                    trimmed.contains("0xFF35D49A") -> "Emerald"
                    trimmed.contains("0xFFFFBF5A") -> "Amber"
                    trimmed.contains("0xFFFF5577") -> "Rose"
                    else -> "Default"
                }

                when {
                    trimmed.contains("Card(") -> {
                        insideCard = true
                        cardTitle = null
                        cardBraceDepth = trimmed.count { it == '{' } - trimmed.count { it == '}' }
                        if (cardBraceDepth <= 0) {
                            nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.CARD, "Surface Card", "Live interactive container", colorTag = colorTag))
                            insideCard = false
                        }
                    }
                    insideCard -> {
                        cardBraceDepth += trimmed.count { it == '{' } - trimmed.count { it == '}' }
                        val tm = textRegex.find(trimmed)
                        if (tm != null) {
                            val txt = tm.groupValues[1]
                            if (txt.contains("\$count") || txt.contains("\${count}")) {
                                nodes.add(
                                    VisualNode(
                                        "code_${idSeq++}",
                                        VisualComponentType.CARD,
                                        txt,
                                        "Reactive state synced",
                                        colorTag = "Cyan"
                                    )
                                )
                                cardTitle = txt
                            } else if (cardTitle == null) {
                                cardTitle = txt
                                nodes.add(
                                    VisualNode(
                                        "code_${idSeq++}",
                                        VisualComponentType.CARD,
                                        txt,
                                        "",
                                        colorTag = colorTag
                                    )
                                )
                            } else {
                                // Update previous card's secondary text or emit text
                                val prev = nodes.lastOrNull()
                                if (prev?.type == VisualComponentType.CARD && prev.secondaryText.isBlank()) {
                                    nodes[nodes.lastIndex] = prev.copy(secondaryText = txt)
                                } else {
                                    nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.TEXT, txt, colorTag = colorTag))
                                }
                            }
                        }
                        if (trimmed.contains("OutlinedButton(") || trimmed.contains("Button(")) {
                            val bMatch = textRegex.find(trimmed)
                            val bText = bMatch?.groupValues?.getOrNull(1) ?: "Action"
                            val actionKind = when {
                                trimmed.contains("count--") || bText == "-" -> "DECREMENT"
                                trimmed.contains("count = 0") || bText.contains("Reset", true) -> "RESET"
                                else -> "INCREMENT"
                            }
                            nodes.add(
                                VisualNode(
                                    "code_${idSeq++}",
                                    VisualComponentType.BUTTON,
                                    bText,
                                    secondaryText = actionKind,
                                    useAccent = !trimmed.contains("OutlinedButton("),
                                    colorTag = colorTag
                                )
                            )
                        }
                        if (cardBraceDepth <= 0) {
                            if (cardTitle == null) {
                                nodes.add(VisualNode("code_${idSeq++}", VisualComponentType.CARD, "Card Container", "Live surface"))
                            }
                            insideCard = false
                        }
                    }
                    trimmed.contains("OutlinedTextField(") || trimmed.contains("TextField(") -> nodes.add(
                        VisualNode("code_${idSeq++}", VisualComponentType.TEXT_FIELD, "Input Field", "Enter value...")
                    )
                    trimmed.contains("Switch(") -> nodes.add(
                        VisualNode("code_${idSeq++}", VisualComponentType.SWITCH_CONTROL, "Toggle Option", "Enabled")
                    )
                    trimmed.contains("OutlinedButton(") || trimmed.contains("Button(") -> {
                        val bMatch = textRegex.find(trimmed)
                        val bText = bMatch?.groupValues?.getOrNull(1) ?: "Action Button"
                        val actionKind = when {
                            trimmed.contains("count--") || bText == "-" -> "DECREMENT"
                            trimmed.contains("count = 0") || bText.contains("Reset", true) -> "RESET"
                            else -> "INCREMENT"
                        }
                        nodes.add(
                            VisualNode(
                                "code_${idSeq++}",
                                VisualComponentType.BUTTON,
                                bText,
                                secondaryText = actionKind,
                                useAccent = true,
                                colorTag = colorTag
                            )
                        )
                    }
                    else -> {
                        textRegex.findAll(trimmed).forEach { tm ->
                            val txt = tm.groupValues[1]
                            if (txt !in buttonTexts) {
                                nodes.add(
                                    VisualNode("code_${idSeq++}", VisualComponentType.TEXT, txt, colorTag = colorTag)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (nodes.isEmpty()) {
            nodes.add(
                VisualNode(
                    id = "code_summary",
                    type = VisualComponentType.CARD,
                    label = title,
                    secondaryText = "${content.lines().size} lines validated cleanly (0 syntax errors)"
                )
            )
        }

        return ComposeParseResult(
            isSuccess = true,
            title = title,
            nodes = nodes,
            errorMessage = null
        )
    }
}
