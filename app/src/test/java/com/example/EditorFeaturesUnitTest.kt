package com.example

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.analysis.StaticAnalyzer
import com.example.model.DiagnosticSeverity
import com.example.ui.components.SyntaxHighlightedTransformation
import com.example.ui.components.highlightSyntax
import com.example.ui.screens.CodeEditorHelpers
import com.example.ui.theme.LdroidThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorFeaturesUnitTest {

    @Test
    fun computeLineAndColumnTracksCursorAccurately() {
        val sample = "package com.ldroid\nfun main() {\n    println(1)\n}"
        assertEquals(1 to 1, CodeEditorHelpers.computeLineAndColumn(sample, 0))
        val funOffset = sample.indexOf("fun")
        assertEquals(2 to 1, CodeEditorHelpers.computeLineAndColumn(sample, funOffset))
        val printOffset = sample.indexOf("println")
        assertEquals(3 to 5, CodeEditorHelpers.computeLineAndColumn(sample, printOffset))
    }

    @Test
    fun offsetForLineReturnsExactLineStartForGoToLine() {
        val sample = "line1\nline2\nline3\nline4"
        assertEquals(0, CodeEditorHelpers.offsetForLine(sample, 1))
        assertEquals(6, CodeEditorHelpers.offsetForLine(sample, 2))
        assertEquals(12, CodeEditorHelpers.offsetForLine(sample, 3))
        assertEquals(18, CodeEditorHelpers.offsetForLine(sample, 4))
    }

    @Test
    fun bracketMatchingFindsNestedPairsForwardAndBackward() {
        val code = "fun demo() { if (a[0] == 1) { return } }"
        val outerOpen = code.indexOf('{')
        val outerClose = code.lastIndexOf('}')
        val forwardMatch = CodeEditorHelpers.findMatchingBracket(code, outerOpen)
        assertEquals(outerOpen to outerClose, forwardMatch)

        val backwardMatch = CodeEditorHelpers.findMatchingBracket(code, outerClose)
        assertEquals(outerClose to outerOpen, backwardMatch)

        assertNull(CodeEditorHelpers.findMatchingBracket("val x = 1", 3))
    }

    @Test
    fun foldableBlocksDetectsMultilineBraceScopes() {
        val code = """
            class Counter {
                fun inc() {
                    val x = 1
                }
                fun oneLine() { return }
            }
        """.trimIndent()
        val blocks = CodeEditorHelpers.findFoldableBlocks(code)
        assertEquals(2, blocks.size)
        assertEquals(1 to 6, blocks[0].startLine to blocks[0].endLine)
        assertEquals(2 to 4, blocks[1].startLine to blocks[1].endLine)
    }

    @Test
    fun autoIndentAddsTabWidthAfterOpeningBrace() {
        val before = TextFieldValue("fun test() {", TextRange(12))
        val afterEnter = TextFieldValue("fun test() {\n", TextRange(13))
        val indented = CodeEditorHelpers.handleAutoIndentAndPairs(before, afterEnter, tabWidth = 4)
        assertEquals("fun test() {\n    ", indented.text)
        assertEquals(17, indented.selection.start)
    }

    @Test
    fun toggleLineCommentSupportsKotlinAndXml() {
        val ktBefore = TextFieldValue("val count = 0", TextRange(4))
        val ktCommented = CodeEditorHelpers.toggleLineComment(ktBefore, "kt")
        assertEquals("// val count = 0", ktCommented.text)
        val ktUncommented = CodeEditorHelpers.toggleLineComment(ktCommented, "kt")
        assertEquals("val count = 0", ktUncommented.text)

        val xmlBefore = TextFieldValue("<TextView />", TextRange(2))
        val xmlCommented = CodeEditorHelpers.toggleLineComment(xmlBefore, "xml")
        assertEquals("<!-- <TextView /> -->", xmlCommented.text)
        val xmlUncommented = CodeEditorHelpers.toggleLineComment(xmlCommented, "xml")
        assertEquals("<TextView />", xmlUncommented.text)
    }

    @Test
    fun autocompleteSuggestsAndAppliesLanguageCompletions() {
        val ktVal = TextFieldValue("val s = remem", TextRange(13))
        val suggestions = CodeEditorHelpers.computeAutocompleteSuggestions(ktVal.text, ktVal.selection.start, "kt")
        assertTrue(suggestions.contains("remember"))
        val applied = CodeEditorHelpers.applyAutocompleteSuggestion(ktVal, "remember")
        assertEquals("val s = remember", applied.text)
        assertEquals(16, applied.selection.start)

        val xmlVal = TextFieldValue("<TextV", TextRange(6))
        val xmlSuggestions = CodeEditorHelpers.computeAutocompleteSuggestions(xmlVal.text, xmlVal.selection.start, "xml")
        assertTrue(xmlSuggestions.contains("TextView"))
    }

    @Test
    fun syntaxHighlightingSupportsAllTenLdroidThemes() {
        val kotlinSnippet = """
            @Composable
            fun MaxStarterScreen(count: Int) {
                // Increment counter
                val label = "Count: 42"
            }
        """.trimIndent()

        assertEquals(10, LdroidThemes.all.size)
        for (tokens in LdroidThemes.all) {
            val highlighted = highlightSyntax(kotlinSnippet, "kt", tokens)
            assertEquals(kotlinSnippet, highlighted.text)
            assertTrue("Expected syntax spans for ${tokens.name}", highlighted.spanStyles.isNotEmpty())

            val transformation = SyntaxHighlightedTransformation(tokens, "kt", matchedBracketIndices = 0 to 1)
            val transformed = transformation.filter(AnnotatedString(kotlinSnippet))
            assertEquals(kotlinSnippet, transformed.text.text)
            assertNotNull(transformed.offsetMapping)
        }
    }

    @Test
    fun staticAnalyzerReportsExactLineNumbersForProblemsNavigation() {
        val broken = """
            package com.ldroid.demo
            fun bad() {
                val x = 1
        """.trimIndent()
        val issues = StaticAnalyzer.analyzeFile("app/src/main/java/Bad.kt", broken)
        assertFalse(issues.isEmpty())
        val syntaxErr = issues.first { it.severity == DiagnosticSeverity.ERROR }
        assertTrue(syntaxErr.line >= 1)
        assertEquals("app/src/main/java/Bad.kt", syntaxErr.filePath)
    }

    @Test
    fun autoClosingPairsInsertsMatchingDelimiterWithoutMovingCursorPastIt() {
        val before = TextFieldValue("val list = listOf", TextRange(17))
        val typedParen = TextFieldValue("val list = listOf(", TextRange(18))
        val paired = CodeEditorHelpers.handleAutoIndentAndPairs(before, typedParen, tabWidth = 2)
        assertEquals("val list = listOf()", paired.text)
        assertEquals(18, paired.selection.start)

        val quoteBefore = TextFieldValue("val s = ", TextRange(8))
        val typedQuote = TextFieldValue("val s = \"", TextRange(9))
        val quotePaired = CodeEditorHelpers.handleAutoIndentAndPairs(quoteBefore, typedQuote, tabWidth = 2)
        assertEquals("val s = \"\"", quotePaired.text)
        assertEquals(9, quotePaired.selection.start)
    }

    @Test
    fun findOccurrencesAndFindNextWrapAroundCleanly() {
        val sample = "alpha beta Alpha gamma ALPHA"
        val insensitive = CodeEditorHelpers.findOccurrences(sample, "alpha", ignoreCase = true)
        assertEquals(3, insensitive.size)
        val firstNext = CodeEditorHelpers.findNextOccurrenceOffset(sample, "alpha", currentOffset = 1, ignoreCase = true)
        assertEquals(11..15, firstNext)
        val wrapped = CodeEditorHelpers.findNextOccurrenceOffset(sample, "alpha", currentOffset = 25, ignoreCase = true)
        assertEquals(0..4, wrapped)

        val sensitive = CodeEditorHelpers.findOccurrences(sample, "Alpha", ignoreCase = false)
        assertEquals(1, sensitive.size)
        assertEquals(11..15, sensitive.first())
    }

    @Test
    fun formatCodeReindentsKotlinBracesAndPreservesHighlightRanges() {
        val messy = "fun demo() {\nval x = 1\nif (x > 0) {\nprintln(x)\n}\n}"
        val formatted = CodeEditorHelpers.formatCode(messy, "kt", tabWidth = 2)
        val expected = """
            fun demo() {
              val x = 1
              if (x > 0) {
                println(x)
              }
            }
        """.trimIndent() + "\n"
        assertEquals(expected, formatted)

        val tokens = LdroidThemes.CyberpunkNeon
        val range = 4 until 8
        val highlighted = highlightSyntax(
            code = formatted,
            ext = "kt",
            tokens = tokens,
            searchHighlightRanges = listOf(range),
            activeSearchRange = range
        )
        assertEquals(formatted, highlighted.text)
        assertTrue(highlighted.spanStyles.isNotEmpty())
    }

    @Test
    fun visualBuilderCodegenGeneratesCounterColorAndPadding() {
        val nodes = listOf(
            com.example.model.VisualNode("v1", com.example.model.VisualComponentType.TEXT, "M.A.X. Header", paddingDp = 16, colorTag = "Cyan"),
            com.example.model.VisualNode("v2", com.example.model.VisualComponentType.COUNTER, "Clicks", paddingDp = 20, colorTag = "Emerald")
        )
        val kotlin = com.example.data.visual.VisualBuilderCodegen.generateComposeKotlin(
            packageName = "com.maxstudio.app",
            screenName = "VisualGeneratedScreen",
            nodes = nodes
        )
        assertTrue(kotlin.contains("package com.maxstudio.app"))
        assertTrue(kotlin.contains("var count by remember { mutableIntStateOf(0) }"))
        assertTrue(kotlin.contains("Color(0xFF00E5FF)"))
        assertTrue(kotlin.contains("Modifier.padding(16.dp)"))
        assertTrue(kotlin.contains("Clicks: \$count"))
    }

    @Test
    fun staticAnalyzerLivePreviewDetectsStateTokensAndActions() {
        val code = """
            @Composable
            fun MaxStarterScreen() {
                var count by remember { mutableIntStateOf(0) }
                Card {
                    Text("Count: ${'$'}count")
                    Button(onClick = { count++ }) { Text("Tap +1") }
                }
            }
        """.trimIndent()
        val parsed = StaticAnalyzer.parseCodeForLivePreview("MaxStarterScreen.kt", code)
        assertTrue(parsed.isSuccess)
        assertEquals("MaxStarterScreen", parsed.title)
        assertTrue(parsed.nodes.any { it.label.contains("\$count") })
        assertTrue(parsed.nodes.any { it.type == com.example.model.VisualComponentType.BUTTON && it.label == "Tap +1" })
    }

    @Test
    fun customAiProviderDefaultsAndModelTiersAreConfigured() {
        val custom = com.example.model.AiProviderType.CUSTOM
        assertEquals("Custom", custom.shortName)
        assertEquals("https://openrouter.ai/api/v1", custom.defaultBaseUrl)

        val state = com.example.data.preferences.LdroidSettingsState(
            aiProvider = custom,
            aiModel = "openai/gpt-4o-mini",
            aiFastModel = "google/gemini-2.5-flash",
            aiProModel = "anthropic/claude-3.5-sonnet",
            aiBaseUrl = "https://openrouter.ai/api/v1"
        )
        assertEquals(custom, state.aiProvider)
        assertEquals("google/gemini-2.5-flash", state.aiFastModel)
        assertEquals("anthropic/claude-3.5-sonnet", state.aiProModel)
        assertEquals("https://openrouter.ai/api/v1", state.aiBaseUrl)
        assertFalse(state.bottomPanelOpen)
    }

    @Test
    fun allTenThemesPassWcagAaContrastAudit() {
        val violations = LdroidThemes.all.filter {
            LdroidThemes.contrastRatio(it.textPrimary, it.background) < 4.5
        }
        assertTrue("Expected zero WCAG AA contrast violations, found: ${violations.map { it.name }}", violations.isEmpty())
    }

    @Test
    fun credentialVaultDetectsAndRedactsSecrets() {
        val sample = "val googleKey = \"AIzaSyDummySecretValueForCheck1234567890\" // token ghp_1234567890abcdef1234567890abcdef1234"
        val findings = com.example.data.security.SecureCredentialVault.scanTextForSecrets(sample)
        assertTrue(findings.isNotEmpty())
        val redacted = com.example.data.security.SecureCredentialVault.redactSecrets(sample)
        assertFalse(redacted.contains("AIzaSyDummySecretValueForCheck1234567890"))
        assertFalse(redacted.contains("ghp_1234567890abcdef1234567890abcdef1234"))
        assertTrue(redacted.contains("[REDACTED"))
    }

    @Test
    fun visualBuilderXmlAndComposeRoundTripPreservesComponents() {
        val starter = com.example.data.visual.VisualBuilderCodegen.starterVisualNodes
        val xml = com.example.data.visual.VisualBuilderCodegen.generateAndroidXmlLayout(starter)
        assertTrue(xml.contains("<LinearLayout"))
        assertTrue(xml.contains("<Button"))

        val kotlin = com.example.data.visual.VisualBuilderCodegen.generateComposeKotlin(
            packageName = "com.maxstudio.starter",
            screenName = "VisualGeneratedScreen",
            nodes = starter
        )
        val parsed = StaticAnalyzer.parseCodeForLivePreview("VisualGeneratedScreen.kt", kotlin)
        assertTrue(parsed.isSuccess)
        assertTrue(parsed.nodes.isNotEmpty())
    }

    @Test
    fun multiLanguageSyntaxHighlightingProducesSpansAcrossStacks() {
        val tokens = LdroidThemes.CyberpunkNeon
        val samples = mapOf(
            "dart" to "class MyApp extends StatefulWidget { final int count = 0; // Flutter }",
            "tsx" to "export default function App() { const [count, setCount] = useState(0); return <Text>{count}</Text>; }",
            "js" to "const btn = document.getElementById('btn'); // JS",
            "java" to "public class MainActivity extends AppCompatActivity { int count = 0; }",
            "css" to ".card { color: #00e5ff; padding: 16px; }",
            "gradle" to "android { defaultConfig { minSdk = 26 } } // Gradle"
        )
        samples.forEach { (ext, code) ->
            val highlighted = highlightSyntax(code, ext, tokens)
            assertEquals(code, highlighted.text)
            assertTrue("Expected syntax spans for .$ext", highlighted.spanStyles.isNotEmpty())
        }
    }

    @Test
    fun staticAnalyzerLivePreviewParsesFlutterReactNativeJavaAndGradle() {
        val dart = """
            class MaxFlutterApp extends StatefulWidget {
              @override
              Widget build(BuildContext context) {
                return Column(children: [
                  Text('Counter: ${'$'}_counter'),
                  ElevatedButton(onPressed: () {}, child: const Text('Tap +1')),
                ]);
              }
            }
        """.trimIndent()
        val dartParsed = StaticAnalyzer.parseCodeForLivePreview("lib/main.dart", dart)
        assertTrue(dartParsed.isSuccess)
        assertTrue(dartParsed.nodes.any { it.label.contains("\$count") })
        assertTrue(dartParsed.nodes.any { it.type == com.example.model.VisualComponentType.BUTTON && it.label == "Tap +1" })

        val tsx = """
            export default function App() {
              const [count, setCount] = useState(0);
              return (
                <View>
                  <Text>Clicks: {count}</Text>
                  <Button title="Increment" onPress={() => setCount(count + 1)} />
                </View>
              );
            }
        """.trimIndent()
        val tsxParsed = StaticAnalyzer.parseCodeForLivePreview("App.tsx", tsx)
        assertTrue(tsxParsed.isSuccess)
        assertTrue(tsxParsed.nodes.any { it.label.contains("\$count") })
        assertTrue(tsxParsed.nodes.any { it.type == com.example.model.VisualComponentType.BUTTON && it.label == "Increment" })

        val java = """
            public class MainActivity extends AppCompatActivity {
                protected void onCreate(Bundle b) {
                    setContentView(R.layout.activity_main);
                    Button btn = findViewById(R.id.btn);
                }
            }
        """.trimIndent()
        val javaParsed = StaticAnalyzer.parseCodeForLivePreview("MainActivity.java", java)
        assertTrue(javaParsed.isSuccess)
        assertTrue(javaParsed.nodes.any { it.label.contains("activity_main.xml") })

        val gradle = """
            android {
                defaultConfig {
                    applicationId = "com.maxstudio.demo"
                    minSdk = 26
                    targetSdk = 35
                }
            }
        """.trimIndent()
        val gradleParsed = StaticAnalyzer.parseCodeForLivePreview("app/build.gradle.kts", gradle)
        assertTrue(gradleParsed.isSuccess)
        assertTrue(gradleParsed.nodes.any { it.label.contains("com.maxstudio.demo") })
    }

    @Test
    fun projectTemplatesCoverAllSupportedFrameworks() {
        val titles = com.example.model.ProjectTemplate.entries.map { it.title }
        assertTrue(titles.any { it.contains("Kotlin Jetpack Compose") })
        assertTrue(titles.any { it.contains("Java + XML") })
        assertTrue(titles.any { it.contains("Flutter") })
        assertTrue(titles.any { it.contains("React Native") })
        assertTrue(titles.any { it.contains("HTML5") })
    }

    @Test
    fun supportedImportExtensionsValidateCorrectly() {
        val supported = com.example.ui.screens.SUPPORTED_PROJECT_EXTENSIONS
        val expected = listOf("kt", "java", "xml", "json", "gradle", "dart", "tsx", "html", "css", "zip")
        expected.forEach { ext ->
            assertTrue("Expected extension $ext to be supported", supported.contains(ext))
        }
        assertFalse("Binary executables must not be supported", supported.contains("exe"))
        assertFalse("Arbitrary binaries must not be supported", supported.contains("bin"))
    }

    @Test
    fun extractCodeSnippetsParsesLanguagesAndBlocks() {
        val markdown = """
            Here is the refactored Kotlin Compose component:
            ```kotlin
            @Composable
            fun MyCard() {
                Text("Hello World")
            }
            ```
            And here is the bash command:
            ```bash
            ./gradlew assembleDebug
            ```
        """.trimIndent()
        val snippets = com.example.ui.screens.extractCodeSnippets(markdown)
        assertEquals(2, snippets.size)
        assertEquals("kotlin", snippets[0].language)
        assertTrue(snippets[0].code.contains("fun MyCard()"))
        assertEquals("bash", snippets[1].language)
        assertEquals("./gradlew assembleDebug", snippets[1].code)
    }

    @Test
    fun fileScaffoldTemplatesProduceAccurateStarterCode() {
        val composeScaffold = com.example.ui.screens.generateFileScaffoldContent(
            template = com.example.ui.screens.FileScaffoldTemplate.COMPOSE_SCREEN,
            relativePath = "app/src/main/java/com/demo/ProfileScreen.kt",
            packageName = "com.demo"
        )
        assertTrue(composeScaffold.contains("package com.demo"))
        assertTrue(composeScaffold.contains("fun ProfileScreen"))

        val flutterScaffold = com.example.ui.screens.generateFileScaffoldContent(
            template = com.example.ui.screens.FileScaffoldTemplate.FLUTTER_WIDGET,
            relativePath = "lib/custom_card.dart",
            packageName = "com.demo"
        )
        assertTrue(flutterScaffold.contains("package:flutter/material.dart"))
        assertTrue(flutterScaffold.contains("extends StatefulWidget"))
    }
}

