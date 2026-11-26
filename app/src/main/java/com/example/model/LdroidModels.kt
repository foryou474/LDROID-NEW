package com.example.model

enum class PrimaryDestination(val label: String, val route: String) {
    HOME("Home", "home"),
    MAX_STUDIO("Max Studio", "max_studio"),
    GOOGLE("Google", "google")
}

enum class MaxStudioMode(val label: String) {
    CODE_EG("<> Code E/G"),
    AI_UI_SCREEN("✨ AI UI Screen"),
    TERMINAL(">_ Terminal")
}

data class TermuxCoreItem(
    val id: String,
    val name: String,
    val relativePath: String,
    val category: String,
    val detail: String,
    val isReady: Boolean
)

data class CompilerBinaryItem(
    val id: String,
    val name: String,
    val version: String,
    val arch: String,
    val detail: String,
    val isReady: Boolean
)

data class GradlePackageItem(
    val groupArtifact: String,
    val version: String,
    val title: String,
    val category: String,
    val description: String
) {
    val coordinate: String get() = "$groupArtifact:$version"
}

data class BrowserConsoleEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val level: String,
    val message: String,
    val source: String = "WebView",
    val timestamp: String
)

enum class DeveloperLabSection(
    val title: String,
    val subtitle: String,
    val categoryTag: String
) {
    DEVICE_LAB("Device Lab", "Profiles, resolution, DPI, orientation & device state", "Devices"),
    LIVE_PREVIEW("Live Preview", "Interactive Compose, XML & HTML real-time preview", "Preview"),
    DEBUGGER_LOGCAT("Debugger & Logcat", "Live process logs, filters & stack-trace explorer", "Logs"),
    PERFORMANCE_PROFILER("Performance Profiler", "JVM heap, native RAM, CPU cores & frame metrics", "Profiler"),
    TESTING_LAB("Android Testing Lab", "Static lint, XML/JSON/Kotlin validation & QA suite", "Testing"),
    APK_ANALYZER("APK/AAB Analyzer", "Inspect ZIP entries, DEX, Manifest, certs & ABIs", "Packages"),
    BUILD_DIAGNOSTICS("Build Diagnostics", "Gradle scripts, version catalog & module scanner", "Build"),
    SDK_ADB_DIAGNOSTICS("SDK & Shell Probe", "Toolchain binaries, ART/JVM & environment check", "Toolchain")
}

enum class ProjectTemplate(
    val title: String,
    val badge: String,
    val description: String,
    val projectType: String
) {
    COMPOSE_M3_APP(
        title = "Kotlin Jetpack Compose",
        badge = "Kotlin",
        description = "Material 3 Compose app with MainActivity.kt, state & unit tests",
        projectType = "ANDROID_COMPOSE"
    ),
    JAVA_XML_APP(
        title = "Java + XML",
        badge = "Android Java",
        description = "AppCompatActivity MainActivity.java paired with activity_main.xml",
        projectType = "ANDROID_JAVA_XML"
    ),
    FLUTTER_DART(
        title = "Flutter (Dart)",
        badge = "Dart / Flutter",
        description = "Cross-platform Material widget app with lib/main.dart & pubspec.yaml",
        projectType = "FLUTTER_DART"
    ),
    REACT_NATIVE_TSX(
        title = "React Native (TSX)",
        badge = "TSX / JS",
        description = "TypeScript mobile UI with App.tsx, hooks & package.json",
        projectType = "REACT_NATIVE_TSX"
    ),
    WEB_PREVIEW_SANDBOX(
        title = "HTML5 / CSS / JS Web App",
        badge = "HTML / CSS / JS",
        description = "Interactive HTML5, CSS3 & JavaScript web workspace",
        projectType = "WEB_SANDBOX"
    ),
    MULTI_MODULE_KOTLIN(
        title = "Multi-Module Kotlin",
        badge = "Multi-Module",
        description = "App + :core:model module with version catalog & CI workflow",
        projectType = "MULTI_MODULE"
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
    TEXT("Text"),
    BUTTON("Button"),
    COLUMN("Column"),
    ROW("Row"),
    CARD("Card"),
    COUNTER("Counter"),
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
    val useAccent: Boolean = false,
    val colorTag: String = "Default"
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
    val shortName: String,
    val defaultModel: String,
    val defaultFastModel: String,
    val defaultProModel: String,
    val defaultBaseUrl: String
) {
    GEMINI(
        displayName = "Google Gemini",
        shortName = "Google",
        defaultModel = "gemini-2.5-flash",
        defaultFastModel = "gemini-2.5-flash",
        defaultProModel = "gemini-2.5-pro",
        defaultBaseUrl = "https://generativelanguage.googleapis.com"
    ),
    OPENAI_COMPATIBLE(
        displayName = "OpenAI",
        shortName = "OpenAI",
        defaultModel = "gpt-4o-mini",
        defaultFastModel = "gpt-4o-mini",
        defaultProModel = "gpt-4o",
        defaultBaseUrl = "https://api.openai.com"
    ),
    ANTHROPIC(
        displayName = "Anthropic Claude",
        shortName = "Anthropic",
        defaultModel = "claude-3-5-haiku-latest",
        defaultFastModel = "claude-3-5-haiku-latest",
        defaultProModel = "claude-3-5-sonnet-latest",
        defaultBaseUrl = "https://api.anthropic.com"
    ),
    CUSTOM(
        displayName = "Custom API",
        shortName = "Custom",
        defaultModel = "openai/gpt-4o-mini",
        defaultFastModel = "openai/gpt-4o-mini",
        defaultProModel = "anthropic/claude-3.5-sonnet",
        defaultBaseUrl = "https://openrouter.ai/api/v1"
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

enum class BottomToolTab(val label: String) {
    TERMINAL("Terminal"),
    PROBLEMS("Problems"),
    OUTPUT("Output"),
    BUILD("Build"),
    LOGCAT("Logcat")
}

data class IdeLogEntry(
    val id: String,
    val timestamp: String,
    val source: String,
    val message: String,
    val isError: Boolean = false
)

data class FindInFilesMatch(
    val relativePath: String,
    val lineNumber: Int,
    val lineText: String
)

data class BuildRunState(
    val isRunning: Boolean = false,
    val statusLabel: String = "Idle",
    val progress: Float = 0f,
    val logs: List<String> = emptyList(),
    val succeeded: Boolean? = null
)
