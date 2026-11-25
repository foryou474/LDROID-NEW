package com.example.data.visual

import com.example.model.DeviceProfile
import com.example.model.VisualComponentType
import com.example.model.VisualNode
import java.io.File

data class ArchitectureNode(
    val name: String,
    val kind: String, // "MODULE", "ACTIVITY", "SCREEN", "DATA"
    val fileCount: Int
)

object VisualBuilderCodegen {

    val defaultDeviceProfiles = listOf(
        DeviceProfile("Pixel 9 Pro", 412, 915, 480, 36, "Phone"),
        DeviceProfile("Compact Phone", 360, 780, 420, 34, "Phone"),
        DeviceProfile("Foldable Unfolded", 674, 841, 420, 36, "Foldable"),
        DeviceProfile("Pixel Tablet", 1024, 640, 240, 35, "Tablet")
    )

    val starterVisualNodes = listOf(
        VisualNode("n1", VisualComponentType.TEXT, "LDROID Visual Screen", "headlineMedium", 12, useAccent = false),
        VisualNode("n2", VisualComponentType.CARD, "Telemetry Summary", "Live state ready", 16, useAccent = false),
        VisualNode("n3", VisualComponentType.TEXT_FIELD, "Project Endpoint", "https://api.example.com", 12, useAccent = false),
        VisualNode("n4", VisualComponentType.SWITCH_CONTROL, "Offline Cache Enabled", "true", 12, useAccent = true),
        VisualNode("n5", VisualComponentType.BUTTON, "Deploy Build", "", 12, useAccent = true)
    )

    fun generateComposeKotlin(packageName: String, screenName: String, nodes: List<VisualNode>): String {
        val cleanPkg = packageName.ifBlank { "com.ldroid.app" }
        val cleanFn = screenName.replace(Regex("""[^A-Za-z0-9_]"""), "").ifBlank { "GeneratedScreen" }
        return buildString {
            appendLine("package $cleanPkg")
            appendLine()
            appendLine("import androidx.compose.foundation.layout.*")
            appendLine("import androidx.compose.material3.*")
            appendLine("import androidx.compose.runtime.*")
            appendLine("import androidx.compose.ui.Modifier")
            appendLine("import androidx.compose.ui.unit.dp")
            appendLine()
            appendLine("@Composable")
            appendLine("fun $cleanFn() {")
            appendLine("    var inputText by remember { mutableStateOf(\"\") }")
            appendLine("    var switchEnabled by remember { mutableStateOf(true) }")
            appendLine("    Column(")
            appendLine("        modifier = Modifier.fillMaxSize().padding(16.dp),")
            appendLine("        verticalArrangement = Arrangement.spacedBy(12.dp)")
            appendLine("    ) {")
            for (node in nodes) {
                when (node.type) {
                    VisualComponentType.TEXT -> appendLine(
                        "        Text(text = \"${escape(node.label)}\", style = MaterialTheme.typography.titleMedium)"
                    )
                    VisualComponentType.CARD -> {
                        appendLine("        Card(modifier = Modifier.fillMaxWidth()) {")
                        appendLine("            Column(modifier = Modifier.padding(${node.paddingDp}.dp)) {")
                        appendLine("                Text(\"${escape(node.label)}\", style = MaterialTheme.typography.titleSmall)")
                        if (node.secondaryText.isNotBlank()) {
                            appendLine("                Text(\"${escape(node.secondaryText)}\", style = MaterialTheme.typography.bodySmall)")
                        }
                        appendLine("            }")
                        appendLine("        }")
                    }
                    VisualComponentType.BUTTON -> appendLine(
                        "        Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text(\"${escape(node.label)}\") }"
                    )
                    VisualComponentType.TEXT_FIELD -> appendLine(
                        "        OutlinedTextField(value = inputText, onValueChange = { inputText = it }, label = { Text(\"${escape(node.label)}\") }, modifier = Modifier.fillMaxWidth())"
                    )
                    VisualComponentType.SWITCH_CONTROL -> {
                        appendLine("        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {")
                        appendLine("            Text(\"${escape(node.label)}\")")
                        appendLine("            Switch(checked = switchEnabled, onCheckedChange = { switchEnabled = it })")
                        appendLine("        }")
                    }
                    VisualComponentType.SPACER -> appendLine("        Spacer(modifier = Modifier.height(${node.paddingDp}.dp))")
                    VisualComponentType.COLUMN -> appendLine("        Column(modifier = Modifier.padding(${node.paddingDp}.dp)) { Text(\"${escape(node.label)}\") }")
                    VisualComponentType.ROW -> appendLine("        Row(modifier = Modifier.padding(${node.paddingDp}.dp)) { Text(\"${escape(node.label)}\") }")
                }
            }
            appendLine("    }")
            appendLine("}")
        }
    }

    fun generateAndroidXmlLayout(nodes: List<VisualNode>): String = buildString {
        appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        appendLine("""<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"""")
        appendLine("""    android:layout_width="match_parent"""")
        appendLine("""    android:layout_height="match_parent"""")
        appendLine("""    android:orientation="vertical"""")
        appendLine("""    android:padding="16dp">""")
        for (node in nodes) {
            when (node.type) {
                VisualComponentType.TEXT, VisualComponentType.CARD -> appendLine(
                    """    <TextView android:layout_width="match_parent" android:layout_height="wrap_content" android:padding="${node.paddingDp}dp" android:text="${escapeXml(node.label)}" />"""
                )
                VisualComponentType.BUTTON -> appendLine(
                    """    <Button android:layout_width="match_parent" android:layout_height="wrap_content" android:text="${escapeXml(node.label)}" />"""
                )
                VisualComponentType.TEXT_FIELD -> appendLine(
                    """    <EditText android:layout_width="match_parent" android:layout_height="wrap_content" android:hint="${escapeXml(node.label)}" />"""
                )
                VisualComponentType.SWITCH_CONTROL -> appendLine(
                    """    <Switch android:layout_width="match_parent" android:layout_height="wrap_content" android:text="${escapeXml(node.label)}" />"""
                )
                else -> appendLine(
                    """    <Space android:layout_width="match_parent" android:layout_height="${node.paddingDp}dp" />"""
                )
            }
        }
        appendLine("</LinearLayout>")
    }

    fun scanArchitecture(rootDir: File): List<ArchitectureNode> {
        if (!rootDir.exists()) return emptyList()
        val nodes = mutableListOf<ArchitectureNode>()
        val files = rootDir.walkTopDown().filter { it.isFile }.toList()
        val gradleModules = files.filter { it.name == "build.gradle.kts" || it.name == "build.gradle" }
            .map { it.parentFile.relativeTo(rootDir).path.ifBlank { "root" } }
        gradleModules.forEach { mod ->
            nodes.add(ArchitectureNode(mod, "MODULE", files.count { it.absolutePath.contains(mod) }))
        }
        val ktFiles = files.filter { it.extension == "kt" || it.extension == "java" }
        ktFiles.take(10).forEach { f ->
            val kind = when {
                f.name.contains("Activity") -> "ACTIVITY"
                f.name.contains("ViewModel") -> "DATA"
                else -> "SCREEN"
            }
            nodes.add(ArchitectureNode(f.nameWithoutExtension, kind, 1))
        }
        return nodes.distinctBy { it.name + it.kind }
    }

    private fun escape(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")
    private fun escapeXml(s: String): String = s.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;")
}
