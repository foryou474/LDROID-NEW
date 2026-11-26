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
        VisualNode("n1", VisualComponentType.TEXT, "LDROID IDE Canvas", "headlineMedium", 12, useAccent = false, colorTag = "Copper"),
        VisualNode("n2", VisualComponentType.CARD, "PulseApp Workspace", "API 36 Compose M3 Interactive UI", 14, useAccent = false, colorTag = "Default"),
        VisualNode("n3", VisualComponentType.COUNTER, "Active Counter", "Tap + or - to update state", 12, useAccent = true, colorTag = "Copper"),
        VisualNode("n4", VisualComponentType.BUTTON, "Build APK", "", 12, useAccent = true, colorTag = "Emerald")
    )

    fun generateComposeKotlin(packageName: String, screenName: String, nodes: List<VisualNode>): String {
        val cleanPkg = packageName.ifBlank { "com.ldroid.pulseapp" }
        val cleanFn = screenName.replace(Regex("""[^A-Za-z0-9_]"""), "").ifBlank { "VisualGeneratedScreen" }
        val hasCounter = nodes.any { it.type == VisualComponentType.COUNTER }
        val hasField = nodes.any { it.type == VisualComponentType.TEXT_FIELD }
        val hasSwitch = nodes.any { it.type == VisualComponentType.SWITCH_CONTROL }
        val hasColor = nodes.any { it.colorTag != "Default" }

        return buildString {
            appendLine("package $cleanPkg")
            appendLine()
            appendLine("import androidx.compose.foundation.layout.*")
            appendLine("import androidx.compose.material3.*")
            appendLine("import androidx.compose.runtime.*")
            appendLine("import androidx.compose.ui.Alignment")
            appendLine("import androidx.compose.ui.Modifier")
            if (hasColor) {
                appendLine("import androidx.compose.ui.graphics.Color")
            }
            appendLine("import androidx.compose.ui.unit.dp")
            appendLine()
            appendLine("@Composable")
            appendLine("fun $cleanFn() {")
            if (hasCounter) appendLine("    var count by remember { mutableIntStateOf(0) }")
            if (hasField) appendLine("    var inputText by remember { mutableStateOf(\"\") }")
            if (hasSwitch) appendLine("    var switchEnabled by remember { mutableStateOf(true) }")
            appendLine("    Column(")
            appendLine("        modifier = Modifier.fillMaxSize().padding(16.dp),")
            appendLine("        verticalArrangement = Arrangement.spacedBy(12.dp)")
            appendLine("    ) {")
            for (node in nodes) {
                val colorExpr = colorExpressionForTag(node.colorTag)
                when (node.type) {
                    VisualComponentType.TEXT -> {
                        val colorArg = if (colorExpr != null) ", color = $colorExpr" else ""
                        appendLine(
                            "        Text(text = \"${escape(node.label)}\", style = MaterialTheme.typography.titleMedium$colorArg, modifier = Modifier.padding(${node.paddingDp}.dp))"
                        )
                    }
                    VisualComponentType.CARD -> {
                        val containerArg = if (colorExpr != null) ", colors = CardDefaults.cardColors(containerColor = $colorExpr.copy(alpha = 0.14f))" else ""
                        appendLine("        Card(modifier = Modifier.fillMaxWidth()$containerArg) {")
                        appendLine("            Column(modifier = Modifier.padding(${node.paddingDp}.dp)) {")
                        appendLine("                Text(\"${escape(node.label)}\", style = MaterialTheme.typography.titleSmall)")
                        if (node.secondaryText.isNotBlank()) {
                            appendLine("                Text(\"${escape(node.secondaryText)}\", style = MaterialTheme.typography.bodySmall)")
                        }
                        appendLine("            }")
                        appendLine("        }")
                    }
                    VisualComponentType.BUTTON -> {
                        val btnColors = if (colorExpr != null) ", colors = ButtonDefaults.buttonColors(containerColor = $colorExpr)" else ""
                        appendLine(
                            "        Button(onClick = { }, modifier = Modifier.fillMaxWidth().padding(${node.paddingDp}.dp)$btnColors) { Text(\"${escape(node.label)}\") }"
                        )
                    }
                    VisualComponentType.COUNTER -> {
                        appendLine("        Card(modifier = Modifier.fillMaxWidth()) {")
                        appendLine("            Row(")
                        appendLine("                modifier = Modifier.fillMaxWidth().padding(${node.paddingDp}.dp),")
                        appendLine("                horizontalArrangement = Arrangement.SpaceBetween,")
                        appendLine("                verticalAlignment = Alignment.CenterVertically")
                        appendLine("            ) {")
                        appendLine("                Text(\"${escape(node.label)}: \$count\", style = MaterialTheme.typography.titleSmall)")
                        appendLine("                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {")
                        appendLine("                    OutlinedButton(onClick = { if (count > 0) count-- }) { Text(\"-\") }")
                        appendLine("                    Button(onClick = { count++ }) { Text(\"+\") }")
                        appendLine("                }")
                        appendLine("            }")
                        appendLine("        }")
                    }
                    VisualComponentType.TEXT_FIELD -> appendLine(
                        "        OutlinedTextField(value = inputText, onValueChange = { inputText = it }, label = { Text(\"${escape(node.label)}\") }, modifier = Modifier.fillMaxWidth().padding(vertical = ${(node.paddingDp / 2).coerceAtLeast(2)}.dp))"
                    )
                    VisualComponentType.SWITCH_CONTROL -> {
                        appendLine("        Row(modifier = Modifier.fillMaxWidth().padding(${node.paddingDp}.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {")
                        appendLine("            Text(\"${escape(node.label)}\")")
                        appendLine("            Switch(checked = switchEnabled, onCheckedChange = { switchEnabled = it })")
                        appendLine("        }")
                    }
                    VisualComponentType.SPACER -> appendLine("        Spacer(modifier = Modifier.height(${node.paddingDp}.dp))")
                    VisualComponentType.COLUMN -> {
                        appendLine("        Column(modifier = Modifier.fillMaxWidth().padding(${node.paddingDp}.dp)) {")
                        appendLine("            Text(\"${escape(node.label)}\")")
                        appendLine("        }")
                    }
                    VisualComponentType.ROW -> {
                        appendLine("        Row(modifier = Modifier.fillMaxWidth().padding(${node.paddingDp}.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {")
                        appendLine("            Text(\"${escape(node.label)}\")")
                        appendLine("        }")
                    }
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
                VisualComponentType.TEXT, VisualComponentType.CARD, VisualComponentType.COUNTER -> appendLine(
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

    private fun colorExpressionForTag(tag: String): String? = when (tag) {
        "Copper" -> "Color(0xFF8C583A)"
        "Cyan" -> "Color(0xFF00E5FF)"
        "Purple" -> "Color(0xFF9E7CFF)"
        "Emerald" -> "Color(0xFF35D49A)"
        "Amber" -> "Color(0xFFFFBF5A)"
        "Rose" -> "Color(0xFFFF5577)"
        else -> null
    }

    private fun escape(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")
    private fun escapeXml(s: String): String = s.replace("&", "&amp;").replace("<", "&lt;").replace("\"", "&quot;")
}
