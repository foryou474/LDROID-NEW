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

        // Secret detection (M32) across all text files
        SecureCredentialVault.scanTextForSecrets(content).forEach { (line, label) ->
            issues.add(
                DiagnosticIssue(
                    filePath = relativePath,
                    line = line,
                    severity = DiagnosticSeverity.SECRET,
                    source = "M32 Secret Scanner",
                    message = "$label detected. Move credentials to Encrypted Vault or CI secrets."
                )
            )
        }

        when (ext) {
            "kt", "java", "kts" -> issues.addAll(analyzeKotlinOrJava(relativePath, content))
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

        lines.forEachIndexed { idx, rawLine ->
            val lineNum = idx + 1
            var i = 0
            var inString = false
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
                if (!inString && c == '/' && next == '/') break
                if (!inString && c == '/' && next == '*') {
                    inBlockComment = true
                    i += 2
                    continue
                }
                if (c == '"' && rawLine.getOrNull(i - 1) != '\\') {
                    inString = !inString
                } else if (!inString) {
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
                        "Play Policy Heuristic",
                        "Broad storage permission declared. Prefer Storage Access Framework or Photo Picker."
                    )
                )
            }
            if (content.contains("usesCleartextTraffic=\"true\"")) {
                issues.add(
                    DiagnosticIssue(
                        path, 1, DiagnosticSeverity.WARNING,
                        "Security Heuristic",
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
     * Used by Code Live Preview so invalid edits fail cleanly and preserve the last known good preview.
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
        val fnMatch = Regex("""@Composable\s+fun\s+([A-Za-z0-9_]+)""").find(content)
        val title = fnMatch?.groupValues?.getOrNull(1)
            ?: Regex("""<title>([^<]+)</title>""").find(content)?.groupValues?.getOrNull(1)
            ?: relativePath.substringAfterLast('/')

        val textRegex = Regex("""Text\s*\(\s*(?:text\s*=\s*)?"([^"]+)"""")
        val buttonRegex = Regex("""Button\s*\([^)]*\)\s*\{\s*Text\s*\(\s*"([^"]+)"""")
        val htmlHeadingRegex = Regex("""<(?:h1|h2|p|button)[^>]*>([^<]+)</""")

        var idSeq = 1
        if (relativePath.endsWith(".html", ignoreCase = true)) {
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
        } else {
            val buttonTexts = buttonRegex.findAll(content).map { it.groupValues[1] }.toSet()
            content.lines().forEach { line ->
                when {
                    line.contains("Card(") -> nodes.add(
                        VisualNode("code_${idSeq++}", VisualComponentType.CARD, "Card Container", "Elevated surface")
                    )
                    line.contains("OutlinedTextField(") || line.contains("TextField(") -> nodes.add(
                        VisualNode("code_${idSeq++}", VisualComponentType.TEXT_FIELD, "Input Field", "Enter value...")
                    )
                    line.contains("Switch(") -> nodes.add(
                        VisualNode("code_${idSeq++}", VisualComponentType.SWITCH_CONTROL, "Toggle Option", "Enabled")
                    )
                    line.contains("Button(") -> {
                        val bMatch = textRegex.find(line)
                        nodes.add(
                            VisualNode(
                                "code_${idSeq++}",
                                VisualComponentType.BUTTON,
                                bMatch?.groupValues?.getOrNull(1) ?: "Action Button",
                                useAccent = true
                            )
                        )
                    }
                    else -> {
                        textRegex.findAll(line).forEach { tm ->
                            val txt = tm.groupValues[1]
                            if (txt !in buttonTexts) {
                                nodes.add(
                                    VisualNode("code_${idSeq++}", VisualComponentType.TEXT, txt)
                                )
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
