package com.example

import com.example.data.ai.ByokAiGateway
import com.example.data.analysis.StaticAnalyzer
import com.example.data.security.SecureCredentialVault
import com.example.data.visual.VisualBuilderCodegen
import com.example.model.DiagnosticSeverity
import com.example.ui.theme.LdroidThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun allTenThemesMeetWcagAaContrast() {
        assertEquals(10, LdroidThemes.all.size)
        for (theme in LdroidThemes.all) {
            val ratio = LdroidThemes.contrastRatio(theme.textPrimary, theme.background)
            assertTrue(
                "Theme ${theme.name} contrast $ratio must be >= 4.5",
                ratio >= 4.5
            )
        }
    }

    @Test
    fun staticAnalyzerDetectsSyntaxAndSecrets() {
        val syntheticKey = "AIza" + "SyD123456789012345678901234567"
        val badKotlin = """
            package com.test
            fun broken() {
                val k = "$syntheticKey"
        """.trimIndent()
        val issues = StaticAnalyzer.analyzeFile("Broken.kt", badKotlin)
        assertTrue(issues.any { it.severity == DiagnosticSeverity.ERROR })
        assertTrue(issues.any { it.severity == DiagnosticSeverity.SECRET })
    }

    @Test
    fun composeLivePreviewParserPreservesSyntaxGate() {
        val valid = """
            @Composable
            fun DemoScreen() {
                Column {
                    Text("Hello LDROID")
                    Button(onClick = {}) { Text("Build") }
                }
            }
        """.trimIndent()
        val ok = StaticAnalyzer.parseCodeForLivePreview("DemoScreen.kt", valid)
        assertTrue(ok.isSuccess)
        assertEquals("DemoScreen", ok.title)
        assertTrue(ok.nodes.isNotEmpty())

        val invalid = valid + "\nfun oops( {"
        val fail = StaticAnalyzer.parseCodeForLivePreview("DemoScreen.kt", invalid)
        assertFalse(fail.isSuccess)
    }

    @Test
    fun secretRedactorMasksTokens() {
        val syntheticToken = "ghp_" + "1234567890abcdefghij1234567890abcd"
        val raw = "token=$syntheticToken"
        val redacted = SecureCredentialVault.redactSecrets(raw)
        assertFalse(redacted.contains("1234567890"))
        assertTrue(redacted.contains("[REDACTED:"))
    }

    @Test
    fun visualBuilderGeneratesComposeAndXml() {
        val kt = VisualBuilderCodegen.generateComposeKotlin(
            "com.ldroid.demo",
            "SampleScreen",
            VisualBuilderCodegen.starterVisualNodes
        )
        assertTrue(kt.contains("@Composable"))
        assertTrue(kt.contains("fun SampleScreen()"))

        val xml = VisualBuilderCodegen.generateAndroidXmlLayout(VisualBuilderCodegen.starterVisualNodes)
        assertTrue(xml.contains("<LinearLayout"))
        assertTrue(xml.contains("</LinearLayout>"))
    }

    @Test
    fun byokGatewayParsesMultiFileProposals() {
        val reply = """
            Here is the updated file:
            <<<FILE: app/src/main/java/Test.kt>>>
            package com.test
            fun ok() = Unit
            <<<END>>>
        """.trimIndent()
        val proposals = ByokAiGateway().parseProposedFiles(reply) { "old" }
        assertEquals(1, proposals.size)
        assertEquals("app/src/main/java/Test.kt", proposals.first().relativePath)
        assertTrue(proposals.first().proposedContent.contains("fun ok() = Unit"))
    }
}
