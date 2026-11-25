package com.example.model

enum class PrimaryDestination(val label: String, val route: String) {
    HOME("Home", "home"),
    PROJECTS("Projects", "projects"),
    EDITOR("Editor", "editor"),
    AI_STUDIO("AI Studio", "ai_studio"),
    SETTINGS("Settings", "settings")
}

enum class DeveloperLabSection(
    val title: String,
    val subtitle: String,
    val milestoneOwner: String
) {
    DEVICE_LAB("Device Lab", "Profiles, resolution, DPI, orientation & device state", "M15"),
    LIVE_PREVIEW("Live Preview", "Code Live Preview & Visual UI Live Preview", "M08 / M15"),
    DEBUGGER_LOGCAT("Debugger & Logcat", "Live process logs, filters & stack-trace explorer", "M16"),
    PERFORMANCE_PROFILER("Performance Profiler", "JVM heap, native RAM, CPU cores & frame metrics", "M24"),
    TESTING_LAB("Android Testing Lab", "Static lint, XML/JSON/Kotlin validation & QA suite", "M20 / M23"),
    APK_ANALYZER("APK/AAB Analyzer", "Real ZIP, DEX, Manifest, certs, size & ABI breakdown", "M17"),
    BUILD_DIAGNOSTICS("Build Diagnostics", "Gradle, version catalog & dependency graph scanner", "M14 / M42"),
    SDK_ADB_DIAGNOSTICS("SDK & ADB Diagnostics", "Toolchain, binaries, ART/JVM & virtualization probe", "M12")
}

enum class ProjectTemplate(
    val title: String,
    val badge: String,
    val description: String,
    val projectType: String
) {
    COMPOSE_M3_APP(
        title = "Compose Material 3 App",
        badge = "Android Kotlin",
        description = "Full Gradle Kotlin DSL app with MainActivity, Theme, strings.xml & unit tests",
        projectType = "ANDROID_COMPOSE"
    ),
    MULTI_MODULE_KOTLIN(
        title = "Multi-Module Android Workspace",
        badge = "M42 Multi-Module",
        description = "App + :core:model module with gradle/libs.versions.toml & CI workflow",
        projectType = "MULTI_MODULE"
    ),
    WEB_PREVIEW_SANDBOX(
        title = "Web & JSON/XML Sandbox",
        badge = "Markup / JS",
        description = "Lightweight HTML, CSS, JavaScript & JSON workspace for instant inspection",
        projectType = "WEB_SANDBOX"
    )
}

enum class LineEnding(val label: String, val separator: String) {
    LF("LF", "\n"),
    CRLF("CRLF", "\r\n")
}

enum class TextEncodingOption(val charsetName: String) {
    UTF_8("UTF-8"),
    UTF_16("UTF-16"),
    ISO_8859_1("ISO-8859-1")
}

data class WorkspaceFileItem(
    val relativePath: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val extension: String
)

data class OpenEditorTab(
    val relativePath: String,
    val content: String,
    val savedContent: String,
    val encoding: TextEncodingOption = TextEncodingOption.UTF_8,
    val lineEnding: LineEnding = LineEnding.LF,
    val isLargeFilePreview: Boolean = false,
    val totalBytes: Long = 0L,
    val undoStack: List<String> = emptyList(),
    val redoStack: List<String> = emptyList(),
    val previewMode: FilePreviewKind = FilePreviewKind.CODE_TEXT
) {
    val isDirty: Boolean get() = content != savedContent
}

enum class FilePreviewKind {
    CODE_TEXT,
    IMAGE,
    ZIP_ARCHIVE,
    PDF_BINARY,
    UNSUPPORTED_BINARY
}

enum class DiagnosticSeverity { ERROR, WARNING, INFO, SECRET }

data class DiagnosticIssue(
    val filePath: String,
    val line: Int,
    val severity: DiagnosticSeverity,
    val source: String,
    val message: String
)

enum class VisualComponentType(val displayName: String) {
    COLUMN("Column"),
    ROW("Row"),
    CARD("Card"),
    TEXT("Text"),
    BUTTON("Button"),
    TEXT_FIELD("OutlinedTextField"),
    SWITCH_CONTROL("Switch"),
    SPACER("Spacer")
}

data class VisualNode(
    val id: String,
    val type: VisualComponentType,
    val label: String,
    val secondaryText: String = "",
    val paddingDp: Int = 12,
    val useAccent: Boolean = false
)

data class DeviceProfile(
    val name: String,
    val widthDp: Int,
    val heightDp: Int,
    val dpi: Int,
    val apiLevel: Int,
    val category: String
)

data class CodePreviewState(
    val lastGoodTitle: String = "MainActivity",
    val lastGoodNodes: List<VisualNode> = emptyList(),
    val isStale: Boolean = false,
    val errorMessage: String? = null,
    val sourceFile: String = "",
    val lastUpdatedMs: Long = 0L
)

enum class AiProviderType(
    val displayName: String,
    val defaultModel: String,
    val defaultBaseUrl: String
) {
    GEMINI(
        displayName = "Google Gemini",
        defaultModel = "gemini-2.5-flash",
        defaultBaseUrl = "https://generativelanguage.googleapis.com"
    ),
    OPENAI_COMPATIBLE(
        displayName = "OpenAI / Compatible",
        defaultModel = "gpt-4o-mini",
        defaultBaseUrl = "https://api.openai.com"
    ),
    ANTHROPIC(
        displayName = "Anthropic Claude",
        defaultModel = "claude-3-5-sonnet-latest",
        defaultBaseUrl = "https://api.anthropic.com"
    )
}

data class AiProposedFileChange(
    val relativePath: String,
    val originalContent: String?,
    val proposedContent: String,
    val isApplied: Boolean = false
)

data class AiChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestampMs: Long,
    val proposedChanges: List<AiProposedFileChange> = emptyList(),
    val isError: Boolean = false
)

data class AiConversation(
    val id: String,
    val title: String,
    val timestampMs: Long,
    val messages: List<AiChatMessage> = emptyList()
)

data class FileDiffLine(
    val marker: Char, // '+', '-', ' '
    val lineNumberOld: Int?,
    val lineNumberNew: Int?,
    val text: String
)

data class FileChangeSummary(
    val relativePath: String,
    val status: String, // "MODIFIED", "ADDED", "DELETED"
    val diffLines: List<FileDiffLine>
)

data class MilestoneAuditItem(
    val id: String,
    val title: String,
    val status: String, // VERIFIED, IMPLEMENTED, BLOCKED
    val summary: String
)
