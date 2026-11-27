package com.example.data.ai

import com.example.model.AiProposedFileChange
import com.example.model.AiVariantProposal
import com.example.model.DiagnosticIssue
import com.example.model.VisualComponentType
import com.example.model.VisualNode

/**
 * Generates multi-variant ChatGPT-style UI & code proposals (`v1 / v2 / v3`) and deterministic
 * 1-tap Auto-Fix patches for Workspace Diagnostics & Smart Error Sentinel.
 */
object MasterAiOrchestrator {

    fun generateMultiVariantProposals(
        prompt: String,
        packageName: String,
        existingKtPath: String,
        existingKtContent: String?,
        existingXmlPath: String,
        existingXmlContent: String?
    ): List<AiVariantProposal> {
        val cleanPrompt = prompt.trim().ifBlank { "Modern Material 3 Screen" }
        val safeTitle = cleanPrompt.split(Regex("\\s+")).take(4).joinToString(" ")
            .replace("\"", "")
            .replaceFirstChar { it.uppercase() }
            .ifBlank { "Studio Screen" }
        val pkg = packageName.ifBlank { "com.ldroid.app" }

        val v1Kt = buildKotlinVariant(pkg, safeTitle, variantStyle = 1, prompt = cleanPrompt)
        val v1Xml = buildXmlVariant(safeTitle, variantStyle = 1)

        val v2Kt = buildKotlinVariant(pkg, safeTitle, variantStyle = 2, prompt = cleanPrompt)
        val v2Xml = buildXmlVariant(safeTitle, variantStyle = 2)

        val v3Kt = buildKotlinVariant(pkg, safeTitle, variantStyle = 3, prompt = cleanPrompt)
        val v3Xml = buildXmlVariant(safeTitle, variantStyle = 3)

        return listOf(
            AiVariantProposal(
                variantTag = "v1",
                title = "Midnight Card Layout ($safeTitle)",
                diffSummary = "+28 lines Compose M3 card & button + activity_main.xml sync",
                proposedChanges = listOf(
                    AiProposedFileChange(existingKtPath, existingKtContent, v1Kt),
                    AiProposedFileChange(existingXmlPath, existingXmlContent, v1Xml)
                )
            ),
            AiVariantProposal(
                variantTag = "v2",
                title = "Interactive Stateful Dashboard",
                diffSummary = "+38 lines counter/input state + metric cards + MaterialButton XML",
                proposedChanges = listOf(
                    AiProposedFileChange(existingKtPath, existingKtContent, v2Kt),
                    AiProposedFileChange(existingXmlPath, existingXmlContent, v2Xml)
                )
            ),
            AiVariantProposal(
                variantTag = "v3",
                title = "Compact Action Form",
                diffSummary = "+32 lines switch toggle + quick field + primary CTA",
                proposedChanges = listOf(
                    AiProposedFileChange(existingKtPath, existingKtContent, v3Kt),
                    AiProposedFileChange(existingXmlPath, existingXmlContent, v3Xml)
                )
            )
        )
    }

    fun visualNodesForVariant(variant: AiVariantProposal): List<VisualNode> {
        val baseTitle = variant.title
        return when (variant.variantTag) {
            "v1" -> listOf(
                VisualNode("var_1", VisualComponentType.TEXT, baseTitle, "headlineMedium", 12, colorTag = "Cyan"),
                VisualNode("var_2", VisualComponentType.CARD, "Midnight Minimal Card", variant.diffSummary, 14, colorTag = "Default"),
                VisualNode("var_3", VisualComponentType.BUTTON, "Run Action", "", 12, useAccent = true, colorTag = "Emerald")
            )
            "v2" -> listOf(
                VisualNode("var_1", VisualComponentType.TEXT, baseTitle, "headlineMedium", 12, colorTag = "Cyan"),
                VisualNode("var_2", VisualComponentType.COUNTER, "Live State Counter", "Tap + or -", 12, useAccent = true, colorTag = "Cyan"),
                VisualNode("var_3", VisualComponentType.TEXT_FIELD, "Quick Filter Input", "", 10),
                VisualNode("var_4", VisualComponentType.BUTTON, "Apply & Sync", "", 12, useAccent = true, colorTag = "Emerald")
            )
            else -> listOf(
                VisualNode("var_1", VisualComponentType.TEXT, baseTitle, "titleMedium", 10, colorTag = "Amber"),
                VisualNode("var_2", VisualComponentType.TEXT_FIELD, "Enter Value", "", 10),
                VisualNode("var_3", VisualComponentType.SWITCH_CONTROL, "Enable Live Sync", "", 10),
                VisualNode("var_4", VisualComponentType.BUTTON, "Submit Form", "", 12, useAccent = true, colorTag = "Cyan")
            )
        }
    }

    fun computeSmartAutoFix(issue: DiagnosticIssue, fileContent: String): String? {
        val ext = issue.filePath.substringAfterLast('.', "").lowercase()
        val msg = issue.message
        return when {
            msg.contains("Unclosed '{'") -> {
                val match = Regex("""\((\d+)\s+unclosed""").find(msg)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
                fileContent.trimEnd() + "\n" + "}\n".repeat(match)
            }
            msg.contains("Unclosed '('") -> {
                val match = Regex("""\((\d+)\s+unclosed""").find(msg)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
                fileContent + ")".repeat(match)
            }
            msg.contains("Unexpected closing brace '}'") -> {
                val lines = fileContent.lines().toMutableList()
                val idx = (issue.line - 1).coerceIn(0, lines.lastIndex)
                lines[idx] = lines[idx].replaceFirst("}", "")
                lines.joinToString("\n")
            }
            msg.contains("Unexpected closing parenthesis ')'") -> {
                val lines = fileContent.lines().toMutableList()
                val idx = (issue.line - 1).coerceIn(0, lines.lastIndex)
                lines[idx] = lines[idx].replaceFirst(")", "")
                lines.joinToString("\n")
            }
            issue.source == "Secret Scanner" -> {
                com.example.data.security.SecureCredentialVault.redactSecrets(fileContent)
            }
            msg.contains("http://") -> fileContent.replace("http://", "https://")
            msg.contains("usesCleartextTraffic=\"true\"") ->
                fileContent.replace("usesCleartextTraffic=\"true\"", "usesCleartextTraffic=\"false\"")
            ext == "xml" && msg.contains("Malformed XML", ignoreCase = true) -> {
                val trimmed = fileContent.trim()
                if (!trimmed.startsWith("<?xml")) "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n$trimmed\n"
                else if (!trimmed.endsWith(">")) "$trimmed>\n"
                else null
            }
            else -> null
        }
    }

    private fun buildKotlinVariant(pkg: String, title: String, variantStyle: Int, prompt: String): String {
        val escapedPrompt = prompt.replace("\"", "'").take(72)
        return when (variantStyle) {
            1 -> """
                package $pkg

                import android.os.Bundle
                import androidx.activity.ComponentActivity
                import androidx.activity.compose.setContent
                import androidx.compose.foundation.layout.*
                import androidx.compose.material3.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                class MainActivity : ComponentActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContent {
                            MaterialTheme(colorScheme = darkColorScheme()) {
                                Surface(modifier = Modifier.fillMaxSize()) {
                                    ${title.replace(Regex("[^A-Za-z0-9]"), "")}Screen()
                                }
                            }
                        }
                    }
                }

                @Composable
                fun ${title.replace(Regex("[^A-Za-z0-9]"), "")}Screen() {
                    var status by remember { mutableStateOf("Ready") }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("$title", style = MaterialTheme.typography.headlineMedium)
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("$escapedPrompt", style = MaterialTheme.typography.bodyMedium)
                                Text("Status: ${'$'}status", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Button(onClick = { status = "Active" }, modifier = Modifier.fillMaxWidth()) {
                            Text("Launch Feature")
                        }
                    }
                }
            """.trimIndent() + "\n"
            2 -> """
                package $pkg

                import android.os.Bundle
                import androidx.activity.ComponentActivity
                import androidx.activity.compose.setContent
                import androidx.compose.foundation.layout.*
                import androidx.compose.material3.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                class MainActivity : ComponentActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContent {
                            MaterialTheme(colorScheme = darkColorScheme()) {
                                Surface(modifier = Modifier.fillMaxSize()) {
                                    DashboardScreen()
                                }
                            }
                        }
                    }
                }

                @Composable
                fun DashboardScreen() {
                    var count by remember { mutableIntStateOf(0) }
                    var query by remember { mutableStateOf("") }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("$title", style = MaterialTheme.typography.headlineMedium)
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Search workspace") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Count: ${'$'}count", style = MaterialTheme.typography.titleMedium)
                                Button(onClick = { count++ }) { Text("Increment +1") }
                            }
                        }
                    }
                }
            """.trimIndent() + "\n"
            else -> """
                package $pkg

                import android.os.Bundle
                import androidx.activity.ComponentActivity
                import androidx.activity.compose.setContent
                import androidx.compose.foundation.layout.*
                import androidx.compose.material3.*
                import androidx.compose.runtime.*
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.unit.dp

                class MainActivity : ComponentActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContent {
                            MaterialTheme(colorScheme = darkColorScheme()) {
                                Surface(modifier = Modifier.fillMaxSize()) {
                                    CompactActionFormScreen()
                                }
                            }
                        }
                    }
                }

                @Composable
                fun CompactActionFormScreen() {
                    var enabled by remember { mutableStateOf(true) }
                    var inputText by remember { mutableStateOf("") }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("$title", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            label = { Text("Enter Details") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Enable Live Sync")
                            Switch(checked = enabled, onCheckedChange = { enabled = it })
                        }
                        Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                            Text("Save & Apply")
                        }
                    }
                }
            """.trimIndent() + "\n"
        }
    }

    private fun buildXmlVariant(title: String, variantStyle: Int): String {
        val actionLabel = when (variantStyle) {
            1 -> "Launch Feature"
            2 -> "Increment Counter"
            else -> "Submit Form"
        }
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:tools="http://schemas.android.com/tools"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:orientation="vertical"
                android:padding="16dp"
                android:background="#0D1117">

                <androidx.cardview.widget.CardView
                    android:id="@+id/variantCard"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginBottom="12dp">

                    <TextView
                        android:id="@+id/variantTitle"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="$title"
                        tools:text="AI Variant v$variantStyle"
                        android:textColor="@color/white" />
                </androidx.cardview.widget.CardView>

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/variantPrimaryBtn"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="$actionLabel" />

            </LinearLayout>
        """.trimIndent() + "\n"
    }
}
