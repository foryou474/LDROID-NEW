package com.example.data.sync

import com.example.data.analysis.StaticAnalyzer
import com.example.model.DiagnosticIssue
import com.example.model.DiagnosticSeverity
import com.example.model.EditorWorkspaceTab
import com.example.model.OpenEditorTab
import com.example.model.RecentActivityItem
import com.example.model.RecentActivityKind
import com.example.model.VisualComponentType
import com.example.model.VisualNode
import com.example.model.XmlHierarchyNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader

data class WorkspaceSyncSnapshot(
    val activeXmlPath: String = "app/src/main/res/layout/activity_main.xml",
    val xmlNodes: List<XmlHierarchyNode> = emptyList(),
    val selectedXmlNodeId: String? = null,
    val xmlErrors: List<DiagnosticIssue> = emptyList(),
    val allDiagnostics: List<DiagnosticIssue> = emptyList(),
    val errorCount: Int = 0,
    val warningCount: Int = 0,
    val liveSyncEnabled: Boolean = true,
    val wifiLivePreviewActive: Boolean = false,
    val recentActivities: List<RecentActivityItem> = emptyList(),
    val syncRevision: Long = 0L
)

/**
 * Central reactive synchronization engine unifying file buffers, XML view hierarchy AST,
 * real-time diagnostics (Smart Error Sentinel), Visual Builder nodes, and Recent Activities.
 */
class WorkspaceSyncEngine {

    private val _state = MutableStateFlow(WorkspaceSyncSnapshot())
    val state: StateFlow<WorkspaceSyncSnapshot> = _state.asStateFlow()

    fun toggleLiveSync(enabled: Boolean) {
        _state.update { it.copy(liveSyncEnabled = enabled) }
    }

    fun toggleWifiPreviewSync(enabled: Boolean) {
        _state.update { it.copy(wifiLivePreviewActive = enabled) }
    }

    fun selectXmlNode(nodeId: String?) {
        _state.update { it.copy(selectedXmlNodeId = nodeId) }
    }

    fun recordActivity(
        kind: RecentActivityKind,
        title: String,
        subtitle: String,
        relativePath: String? = null,
        targetLine: Int? = null,
        targetTab: EditorWorkspaceTab = EditorWorkspaceTab.CODE_EG
    ) {
        val entry = RecentActivityItem(
            kind = kind,
            title = title,
            subtitle = subtitle,
            relativePath = relativePath,
            targetLine = targetLine,
            targetTab = targetTab
        )
        _state.update { prev ->
            val deduped = prev.recentActivities.filterNot {
                it.kind == kind && it.relativePath == relativePath && it.title == title
            }
            prev.copy(recentActivities = (listOf(entry) + deduped).take(24))
        }
    }

    fun syncWorkspace(
        projectRoot: File?,
        openTabs: List<OpenEditorTab>,
        activeTabPath: String?
    ): List<DiagnosticIssue> {
        val diskIssues = if (projectRoot != null && projectRoot.exists()) {
            StaticAnalyzer.analyzeWorkspace(projectRoot)
        } else emptyList()

        // Overlay live unsaved buffer diagnostics over disk diagnostics for real-time Sentinel
        val dirtyPaths = openTabs.map { it.relativePath }.toSet()
        val liveTabIssues = openTabs.flatMap { tab ->
            StaticAnalyzer.analyzeFile(tab.relativePath, tab.content)
        }
        val mergedIssues = (diskIssues.filterNot { it.filePath in dirtyPaths } + liveTabIssues)
            .sortedWith(compareBy<DiagnosticIssue> { it.severity.ordinal }.thenBy { it.filePath }.thenBy { it.line })

        // Locate primary XML layout source
        val activeXmlTab = openTabs.firstOrNull {
            it.relativePath == activeTabPath && it.relativePath.endsWith(".xml", ignoreCase = true)
        } ?: openTabs.firstOrNull {
            it.relativePath.endsWith("activity_main.xml", ignoreCase = true)
        } ?: openTabs.firstOrNull {
            it.relativePath.endsWith(".xml", ignoreCase = true) && !it.relativePath.endsWith("AndroidManifest.xml")
        }

        val xmlPath = activeXmlTab?.relativePath ?: "app/src/main/res/layout/activity_main.xml"
        val xmlContent = activeXmlTab?.content ?: run {
            val diskXml = if (projectRoot != null) File(projectRoot, xmlPath) else null
            if (diskXml != null && diskXml.exists()) diskXml.readText() else defaultSampleActivityXml()
        }

        val parsedXml = parseXmlHierarchy(xmlContent)
        val xmlIssues = mergedIssues.filter { it.filePath == xmlPath }
        val errCount = mergedIssues.count { it.severity == DiagnosticSeverity.ERROR || it.severity == DiagnosticSeverity.SECRET }
        val warnCount = mergedIssues.count { it.severity == DiagnosticSeverity.WARNING }

        _state.update { prev ->
            prev.copy(
                activeXmlPath = xmlPath,
                xmlNodes = parsedXml,
                selectedXmlNodeId = prev.selectedXmlNodeId?.takeIf { id -> parsedXml.any { it.id == id } }
                    ?: parsedXml.lastOrNull()?.id,
                xmlErrors = xmlIssues,
                allDiagnostics = mergedIssues,
                errorCount = errCount,
                warningCount = warnCount,
                syncRevision = prev.syncRevision + 1
            )
        }
        return mergedIssues
    }

    fun parseXmlHierarchy(xmlContent: String): List<XmlHierarchyNode> {
        val nodes = mutableListOf<XmlHierarchyNode>()
        runCatching {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))
            var depth = 0
            var seq = 1
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val tag = parser.name ?: "View"
                        var viewId: String? = null
                        var textAttr: String? = null
                        var toolsTextAttr: String? = null
                        var colorAttr: String? = null
                        for (i in 0 until parser.attributeCount) {
                            val attrName = parser.getAttributeName(i)
                            val attrVal = parser.getAttributeValue(i)
                            when {
                                attrName == "id" || attrName.endsWith(":id") ->
                                    viewId = attrVal.substringAfterLast('/')
                                attrName == "text" -> {
                                    val prefix = parser.getAttributePrefix(i)
                                    if (prefix == "tools") toolsTextAttr = attrVal else textAttr = attrVal
                                }
                                attrName.contains("background", true) || attrName.contains("textColor", true) ->
                                    if (attrVal.startsWith("@color/") || attrVal.startsWith("#")) colorAttr = attrVal
                            }
                        }
                        nodes.add(
                            XmlHierarchyNode(
                                id = "xml_${seq++}",
                                tagName = tag.substringAfterLast('.'),
                                viewId = viewId,
                                lineNumber = parser.lineNumber.coerceAtLeast(1),
                                depth = depth,
                                textAttr = textAttr,
                                toolsTextAttr = toolsTextAttr,
                                colorAttr = colorAttr
                            )
                        )
                        depth++
                    }
                    XmlPullParser.END_TAG -> {
                        depth = (depth - 1).coerceAtLeast(0)
                    }
                }
                event = parser.next()
            }
        }
        return nodes
    }

    fun formatXmlCleanly(xmlContent: String): String {
        val trimmed = xmlContent.trim()
        if (trimmed.isEmpty()) return xmlContent
        val out = StringBuilder()
        var indent = 0
        val tokens = trimmed.replace(Regex(">\\s*<"), ">\n<").lines()
        for (rawLine in tokens) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("</")) {
                indent = (indent - 1).coerceAtLeast(0)
            }
            out.append("    ".repeat(indent)).append(line).append('\n')
            val isOpeningTag = line.startsWith("<") &&
                !line.startsWith("</") &&
                !line.startsWith("<?") &&
                !line.startsWith("<!--") &&
                !line.endsWith("/>") &&
                !line.contains("</")
            if (isOpeningTag) indent++
        }
        return out.toString()
    }

    fun visualNodesFromXml(xmlNodes: List<XmlHierarchyNode>): List<VisualNode> {
        if (xmlNodes.isEmpty()) return emptyList()
        return xmlNodes.mapIndexedNotNull { idx, node ->
            val lower = node.tagName.lowercase()
            val type = when {
                lower.contains("button") -> VisualComponentType.BUTTON
                lower.contains("card") -> VisualComponentType.CARD
                lower.contains("edittext") || lower.contains("textfield") -> VisualComponentType.TEXT_FIELD
                lower.contains("switch") -> VisualComponentType.SWITCH_CONTROL
                lower.contains("image") -> VisualComponentType.IMAGE
                lower.contains("textview") -> VisualComponentType.TEXT
                lower.contains("linearlayout") && idx > 0 -> VisualComponentType.COLUMN
                else -> null
            } ?: return@mapIndexedNotNull null
            VisualNode(
                id = node.id,
                type = type,
                label = node.toolsTextAttr ?: node.textAttr ?: node.viewId ?: node.tagName,
                secondaryText = node.viewId ?: "line ${node.lineNumber}",
                useAccent = type == VisualComponentType.BUTTON,
                colorTag = if (node.colorAttr != null) "Cyan" else "Default"
            )
        }
    }

    companion object {
        fun defaultSampleActivityXml(): String = """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:tools="http://schemas.android.com/tools"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:orientation="vertical"
                android:padding="16dp"
                android:background="#0D1117">

                <androidx.cardview.widget.CardView
                    android:id="@+id/heroCard"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginBottom="12dp">

                    <TextView
                        android:id="@+id/titleLabel"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/app_name"
                        tools:text="LDROID Workspace Card"
                        android:textColor="@color/white" />
                </androidx.cardview.widget.CardView>

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/actionBtn"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="Run Preview Action" />

            </LinearLayout>
        """.trimIndent() + "\n"
    }
}
