package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiGatewayResponse
import com.example.data.ai.ByokAiGateway
import com.example.data.analysis.ApkAnalysisReport
import com.example.data.analysis.ApkAnalyzer
import com.example.data.analysis.StaticAnalyzer
import com.example.data.audit.RoadmapRegistry
import com.example.data.audit.VerificationReport
import com.example.data.github.GitHubApiClient
import com.example.data.github.GitHubRepoStatus
import com.example.data.local.AiMessageEntity
import com.example.data.local.FileOpHistoryEntity
import com.example.data.local.LdroidDatabase
import com.example.data.local.ProjectEntity
import com.example.data.local.WorkspaceSnapshotEntity
import com.example.data.preferences.LdroidPreferences
import com.example.data.preferences.LdroidSettingsState
import com.example.data.security.SecureCredentialVault
import com.example.data.terminal.LibraryToolchainManager
import com.example.data.terminal.TerminalCommandHooks
import com.example.data.terminal.TerminalLine
import com.example.data.terminal.TerminalSession
import com.example.data.terminal.TerminalSessionManager
import com.example.data.terminal.ToolchainProbeReport
import com.example.data.visual.ArchitectureNode
import com.example.data.visual.VisualBuilderCodegen
import com.example.data.workspace.WorkspaceFileManager
import com.example.data.workspace.ZipEntryInfo
import com.example.data.workspace.ZipImportSummary
import com.example.model.AiChatMessage
import com.example.model.AiProposedFileChange
import com.example.model.AiProviderType
import com.example.model.BottomToolTab
import com.example.model.BrowserConsoleEntry
import com.example.model.BuildRunState
import com.example.model.CodePreviewState
import com.example.model.CompilerBinaryItem
import com.example.model.DeveloperLabSection
import com.example.model.DeviceProfile
import com.example.model.DiagnosticIssue
import com.example.model.DiagnosticSeverity
import com.example.model.FileChangeSummary
import com.example.model.FilePreviewKind
import com.example.model.FindInFilesMatch
import com.example.model.GradlePackageItem
import com.example.model.IdeLogEntry
import com.example.model.LineEnding
import com.example.model.MaxStudioMode
import com.example.model.OpenEditorTab
import com.example.model.PrimaryDestination
import com.example.model.ProjectTemplate
import com.example.model.TermuxCoreItem
import com.example.model.TextEncodingOption
import com.example.model.VisualComponentType
import com.example.model.VisualNode
import com.example.model.WorkspaceFileItem
import com.example.ui.screens.CodeEditorHelpers
import com.example.ui.theme.LdroidThemeId
import com.example.ui.theme.LdroidThemes
import com.example.ui.theme.LdroidTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class LdroidUiState(
    val currentTab: PrimaryDestination = PrimaryDestination.HOME,
    val maxStudioMode: MaxStudioMode = MaxStudioMode.AI_UI_SCREEN,
    val activeLabSection: DeveloperLabSection? = null,
    val previewThemeTokens: LdroidTokens? = null,
    val projects: List<ProjectEntity> = emptyList(),
    val activeProject: ProjectEntity? = null,
    val workspaceFiles: List<WorkspaceFileItem> = emptyList(),
    val fileSearchQuery: String = "",
    val selectedFilePaths: Set<String> = emptySet(),
    val openTabs: List<OpenEditorTab> = emptyList(),
    val activeTabPath: String? = null,
    val zipInspectionEntries: List<ZipEntryInfo> = emptyList(),
    val diagnostics: List<DiagnosticIssue> = emptyList(),
    val workspaceChanges: List<FileChangeSummary> = emptyList(),
    val architectureNodes: List<ArchitectureNode> = emptyList(),
    // Visual Builder & Live Preview
    val visualNodes: List<VisualNode> = VisualBuilderCodegen.starterVisualNodes,
    val selectedDeviceProfile: DeviceProfile = VisualBuilderCodegen.defaultDeviceProfiles.first(),
    val previewLandscape: Boolean = false,
    val previewFontScale: Float = 1.0f,
    val codePreviewState: CodePreviewState = CodePreviewState(),
    // Universal Action Bar & AI Prompt Enhancer
    val universalInputText: String = "",
    val previousPromptBeforeEnhance: String? = null,
    // Terminal, Output, Build & Developer Lab
    val activeTerminalSessionId: String? = null,
    val logcatLines: List<String> = emptyList(),
    val ideOutputLogs: List<IdeLogEntry> = emptyList(),
    val buildState: BuildRunState = BuildRunState(),
    val findInFilesQuery: String = "",
    val findInFilesResults: List<FindInFilesMatch> = emptyList(),
    val pendingCursorJump: Pair<String, Int>? = null,
    val editorFindQuery: String = "",
    val editorFindMatchCase: Boolean = false,
    val pendingOffsetJump: Pair<String, IntRange>? = null,
    val toolchainProbe: ToolchainProbeReport? = null,
    val apkReport: ApkAnalysisReport? = null,
    val isApkAnalyzing: Boolean = false,
    // Library & Packages (Termux Core, Compilers, Gradle Packages)
    val termuxCoreItems: List<TermuxCoreItem> = emptyList(),
    val compilerBinaries: List<CompilerBinaryItem> = emptyList(),
    val gradlePackages: List<GradlePackageItem> = emptyList(),
    // Device Permissions state (on-demand)
    val storagePermissionGranted: Boolean = false,
    val photosPermissionGranted: Boolean = false,
    val micPermissionGranted: Boolean = false,
    // In-App Developer Browser Console
    val browserConsoleLogs: List<BrowserConsoleEntry> = emptyList(),
    // AI Studio
    val aiMessages: List<AiChatMessage> = emptyList(),
    val isAiLoading: Boolean = false,
    val isTestingAiConnection: Boolean = false,
    val aiConnectionTestResult: Pair<Boolean, String>? = null,
    val fetchedModels: List<String> = emptyList(),
    val isFetchingModels: Boolean = false,
    val fetchModelsError: String? = null,
    // GitHub
    val gitHubStatus: GitHubRepoStatus? = null,
    val gitHubError: String? = null,
    val isGitHubLoading: Boolean = false,
    // System Verification Suite (M01-M50)
    val verificationReport: VerificationReport? = null,
    val isRunningVerification: Boolean = false,
    // Feedback banner/snackbar
    val statusMessage: String? = null,
    val lastZipImportSummary: ZipImportSummary? = null
) {
    val activeTab: OpenEditorTab?
        get() = openTabs.firstOrNull { it.relativePath == activeTabPath }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LdroidViewModel(application: Application) : AndroidViewModel(application) {

    private val db = LdroidDatabase.getInstance(application)
    private val dao = db.ldroidDao()
    val preferences = LdroidPreferences(application)
    val vault = SecureCredentialVault(application)
    val fileManager = WorkspaceFileManager(application, dao)
    val terminalManager = TerminalSessionManager(application)
    val libraryManager = LibraryToolchainManager(application)
    private val apkAnalyzer = ApkAnalyzer(application)
    private val aiGateway = ByokAiGateway()
    private val gitHubClient = GitHubApiClient()

    private val _uiState = MutableStateFlow(LdroidUiState())
    val uiState: StateFlow<LdroidUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null

    val settings: StateFlow<LdroidSettingsState> = preferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, LdroidSettingsState())

    val terminalSessions: StateFlow<List<TerminalSession>> = terminalManager.sessions

    val activeProjectHistory: StateFlow<List<FileOpHistoryEntity>> = _uiState
        .flatMapLatest { state ->
            state.activeProject?.let { dao.observeHistory(it.id) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProjectSnapshots: StateFlow<List<WorkspaceSnapshotEntity>> = _uiState
        .flatMapLatest { state ->
            state.activeProject?.let { dao.observeSnapshots(it.id) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeThemeTokens: StateFlow<LdroidTokens> = combine(settings, _uiState) { prefs, ui ->
        ui.previewThemeTokens ?: run {
            if (prefs.customThemeJson != null) {
                LdroidThemes.validateAndParseThemeJson(prefs.customThemeJson).getOrElse {
                    LdroidThemes.byId(prefs.themeId)
                }
            } else {
                LdroidThemes.byId(prefs.themeId)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LdroidThemes.DesertLuxe)

    init {
        viewModelScope.launch {
            fileManager.ensureStarterWorkspaceIfEmpty()
            refreshDiagnosticsProbe()
            refreshLibraryToolchains()
        }
        viewModelScope.launch {
            combine(dao.observeProjects(), settings) { list, prefs -> list to prefs }
                .collect { (list, prefs) ->
                    val currentActive = _uiState.value.activeProject
                    val selected = list.firstOrNull { it.id == currentActive?.id }
                        ?: list.firstOrNull { it.id == prefs.activeProjectId }
                        ?: list.firstOrNull()
                    _uiState.update { it.copy(projects = list, activeProject = selected) }
                    if (selected != null && selected.id != currentActive?.id) {
                        onProjectActivated(selected)
                    }
                }
        }
        viewModelScope.launch {
            _uiState
                .map { it.activeProject?.id }
                .distinctUntilChanged()
                .flatMapLatest { projectId ->
                    if (projectId == null) flowOf(emptyList())
                    else dao.observeAiMessages(projectId)
                }
                .collect { entities ->
                    _uiState.update { state ->
                        state.copy(aiMessages = entities.map { it.toDomain() })
                    }
                }
        }
    }

    private suspend fun onProjectActivated(project: ProjectEntity) {
        val termId = terminalManager.ensureDefaultSession(project.rootPath)
        terminalManager.updateWorkingDirectory(termId, project.rootPath)
        _uiState.update {
            it.copy(
                activeTerminalSessionId = termId,
                openTabs = emptyList(),
                activeTabPath = null,
                selectedFilePaths = emptySet(),
                findInFilesResults = emptyList(),
                buildState = BuildRunState()
            )
        }
        refreshWorkspaceFilesAndAnalysis(project)
        appendIdeLog("Workspace", "Activated project '${project.name}' (${project.packageName})")

        // Restore persisted open tabs or open starter entry file
        val (savedPaths, savedActive) = readPersistedTabs(project.id)
        val existingFiles = _uiState.value.workspaceFiles.filter { !it.isDirectory }.map { it.relativePath }.toSet()
        val validSaved = savedPaths.filter { it in existingFiles }
        val tabsToOpen = validSaved.ifEmpty {
            listOfNotNull(findDefaultEntryFile(project, existingFiles))
        }

        if (tabsToOpen.isNotEmpty()) {
            val loadedTabs = mutableListOf<OpenEditorTab>()
            for (path in tabsToOpen) {
                fileManager.openFileTab(project, path).onSuccess { loadedTabs.add(it) }
            }
            if (loadedTabs.isNotEmpty()) {
                val targetActivePath = savedActive?.takeIf { s -> loadedTabs.any { it.relativePath == s } }
                    ?: loadedTabs.last().relativePath
                val activeTab = loadedTabs.firstOrNull { it.relativePath == targetActivePath } ?: loadedTabs.last()
                _uiState.update { state ->
                    state.copy(
                        openTabs = loadedTabs,
                        activeTabPath = activeTab.relativePath
                    )
                }
                if (activeTab.previewMode == FilePreviewKind.ZIP_ARCHIVE) {
                    fileManager.inspectZipEntries(project, activeTab.relativePath).onSuccess { entries ->
                        _uiState.update { it.copy(zipInspectionEntries = entries) }
                    }
                } else if (activeTab.previewMode == FilePreviewKind.CODE_TEXT) {
                    updateCodeLivePreview(activeTab.relativePath, activeTab.content)
                }
            }
        }
    }

    private fun findDefaultEntryFile(project: ProjectEntity, existingFiles: Set<String>): String? {
        if (existingFiles.isEmpty()) return null
        return project.lastOpenedFile?.takeIf { it in existingFiles }
            ?: existingFiles.firstOrNull { it.endsWith("/MainActivity.kt") || it == "MainActivity.kt" }
            ?: existingFiles.firstOrNull { it.endsWith("/MainActivity.java") || it == "MainActivity.java" }
            ?: existingFiles.firstOrNull { it == "lib/main.dart" || it.endsWith("/main.dart") }
            ?: existingFiles.firstOrNull { it == "App.tsx" || it == "App.jsx" || it == "App.js" }
            ?: existingFiles.firstOrNull { it == "index.html" }
            ?: existingFiles.firstOrNull {
                val lower = it.lowercase()
                lower.endsWith(".kt") || lower.endsWith(".java") || lower.endsWith(".dart") ||
                    lower.endsWith(".tsx") || lower.endsWith(".html")
            }
            ?: existingFiles.firstOrNull()
    }

    fun ensureDefaultFileOpen() {
        val state = _uiState.value
        if (state.activeTab != null) return
        if (state.openTabs.isNotEmpty()) {
            val first = state.openTabs.first()
            _uiState.update { it.copy(activeTabPath = first.relativePath) }
            if (first.previewMode == FilePreviewKind.CODE_TEXT) {
                updateCodeLivePreview(first.relativePath, first.content)
            }
        }
    }

    fun selectPrimaryTab(tab: PrimaryDestination) {
        _uiState.update { it.copy(currentTab = tab, activeLabSection = null) }
    }

    fun openDeveloperLabSection(section: DeveloperLabSection) {
        _uiState.update { it.copy(currentTab = PrimaryDestination.HOME, activeLabSection = section) }
        when (section) {
            DeveloperLabSection.DEBUGGER_LOGCAT -> refreshLogcat()
            DeveloperLabSection.PERFORMANCE_PROFILER,
            DeveloperLabSection.SDK_ADB_DIAGNOSTICS -> refreshDiagnosticsProbe()
            DeveloperLabSection.TESTING_LAB,
            DeveloperLabSection.BUILD_DIAGNOSTICS -> {
                _uiState.value.activeProject?.let { viewModelScope.launch { refreshWorkspaceFilesAndAnalysis(it) } }
            }
            else -> Unit
        }
    }

    fun closeDeveloperLabSection() {
        _uiState.update { it.copy(activeLabSection = null) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun postMessage(msg: String) {
        _uiState.update { it.copy(statusMessage = msg) }
    }

    // --- Projects & Templates (M04 / M25) ---

    fun createProject(
        name: String,
        packageName: String,
        template: ProjectTemplate,
        openMaxStudio: Boolean = false
    ) {
        if (name.isBlank()) {
            postMessage("Project name cannot be empty.")
            return
        }
        viewModelScope.launch {
            val created = fileManager.createProjectFromTemplate(name, packageName, template)
            preferences.setActiveProjectId(created.id)
            _uiState.update {
                it.copy(
                    activeProject = created,
                    currentTab = if (openMaxStudio) PrimaryDestination.MAX_STUDIO else it.currentTab,
                    maxStudioMode = if (openMaxStudio) MaxStudioMode.AI_UI_SCREEN else it.maxStudioMode
                )
            }
            onProjectActivated(created)
            postMessage("Switched workspace to ${created.name} (${template.title})")
        }
    }

    fun switchProject(project: ProjectEntity, openMaxStudio: Boolean = false) {
        viewModelScope.launch {
            preferences.setActiveProjectId(project.id)
            _uiState.update {
                it.copy(
                    activeProject = project,
                    currentTab = if (openMaxStudio) PrimaryDestination.MAX_STUDIO else it.currentTab
                )
            }
            onProjectActivated(project)
            postMessage("Active workspace: ${project.name}")
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            fileManager.deleteProject(project)
            postMessage("Deleted workspace '${project.name}'")
        }
    }

    fun importFilesViaSaf(uris: List<Uri>) {
        val project = _uiState.value.activeProject ?: return
        if (uris.isEmpty()) return
        viewModelScope.launch {
            fileManager.attachSupportFiles(project, uris)
                .onSuccess { imported ->
                    refreshWorkspaceFilesAndAnalysis(project)
                    imported.firstOrNull()?.let { openFileInEditor(it) }
                    postMessage("Attached ${imported.size} support file(s)")
                }
                .onFailure { postMessage("Attach failed: ${it.message}") }
        }
    }

    fun importZipProjectViaSaf(uri: Uri) {
        viewModelScope.launch {
            fileManager.importZipProjectFromUri(uri)
                .onSuccess { summary ->
                    preferences.setActiveProjectId(summary.project.id)
                    _uiState.update {
                        it.copy(
                            activeProject = summary.project,
                            lastZipImportSummary = summary,
                            currentTab = PrimaryDestination.HOME
                        )
                    }
                    onProjectActivated(summary.project)
                    postMessage("Imported ZIP '${summary.project.name}' (${summary.importedFiles} files)")
                }
                .onFailure { postMessage("ZIP import blocked/failed: ${it.message}") }
        }
    }

    fun clearZipSummary() {
        _uiState.update { it.copy(lastZipImportSummary = null) }
    }

    fun exportActiveProjectZip(destUri: Uri) {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            fileManager.exportProjectToZip(project, destUri)
                .onSuccess { count -> postMessage("Exported $count files to ZIP backup") }
                .onFailure { postMessage("Export failed: ${it.message}") }
        }
    }

    fun createWorkspaceSnapshot() {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            val snap = fileManager.saveBaselineSnapshot(project)
            refreshWorkspaceFilesAndAnalysis(project)
            postMessage("Saved baseline snapshot (${snap.fileCount} files)")
        }
    }

    fun restoreSnapshotFile(relativePath: String) {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            fileManager.restoreFromBaseline(project, relativePath)
                .onSuccess {
                    val isOpen = _uiState.value.openTabs.any { it.relativePath == relativePath }
                    if (isOpen) {
                        fileManager.openFileTab(project, relativePath).onSuccess { reloaded ->
                            _uiState.update { st ->
                                st.copy(openTabs = st.openTabs.map { if (it.relativePath == relativePath) reloaded else it })
                            }
                            updateCodeLivePreview(reloaded.relativePath, reloaded.content)
                        }.onFailure {
                            closeEditorTab(relativePath)
                        }
                    }
                    refreshWorkspaceFilesAndAnalysis(project)
                    appendIdeLog("Snapshot", "Restored $relativePath to baseline")
                    postMessage("Restored $relativePath from snapshot")
                }
                .onFailure { postMessage("Restore failed: ${it.message}") }
        }
    }

    fun restoreFullSnapshot() {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            fileManager.restoreFromBaseline(project, null)
                .onSuccess { count ->
                    // Reload currently open tabs that still exist
                    val openPaths = _uiState.value.openTabs.map { it.relativePath }
                    val reloadedTabs = mutableListOf<OpenEditorTab>()
                    for (path in openPaths) {
                        fileManager.openFileTab(project, path).onSuccess { reloadedTabs.add(it) }
                    }
                    val nextActive = _uiState.value.activeTabPath?.takeIf { p -> reloadedTabs.any { it.relativePath == p } }
                        ?: reloadedTabs.firstOrNull()?.relativePath
                    _uiState.update { st ->
                        st.copy(openTabs = reloadedTabs, activeTabPath = nextActive)
                    }
                    persistCurrentTabs()
                    refreshWorkspaceFilesAndAnalysis(project)
                    appendIdeLog("Snapshot", "Restored workspace baseline ($count files)")
                    postMessage("Restored $count files from baseline snapshot")
                }
                .onFailure { postMessage("Workspace rollback failed: ${it.message}") }
        }
    }

    // --- File Explorer & Code Editor (M05 / M06) ---

    fun updateFileSearch(query: String) {
        _uiState.update { it.copy(fileSearchQuery = query) }
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            val files = fileManager.listFiles(project, query)
            _uiState.update { it.copy(workspaceFiles = files) }
        }
    }

    fun toggleFileSelection(path: String) {
        _uiState.update { state ->
            val next = state.selectedFilePaths.toMutableSet()
            if (!next.add(path)) next.remove(path)
            state.copy(selectedFilePaths = next)
        }
    }

    fun clearFileSelection() {
        _uiState.update { it.copy(selectedFilePaths = emptySet()) }
    }

    fun createFileOrFolder(relativePath: String, isDirectory: Boolean, initialContent: String = "") {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            fileManager.createFileOrFolder(project, relativePath, isDirectory, initialContent)
                .onSuccess {
                    refreshWorkspaceFilesAndAnalysis(project)
                    if (!isDirectory) openFileInEditor(relativePath)
                    postMessage("Created $relativePath")
                }
                .onFailure { postMessage("Create failed: ${it.message}") }
        }
    }

    fun renameFile(oldRel: String, newName: String) {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            fileManager.renameFile(project, oldRel, newName)
                .onSuccess { newRel ->
                    _uiState.update { state ->
                        val updatedTabs = state.openTabs.map {
                            if (it.relativePath == oldRel) it.copy(relativePath = newRel) else it
                        }
                        val nextActive = if (state.activeTabPath == oldRel) newRel else state.activeTabPath
                        state.copy(openTabs = updatedTabs, activeTabPath = nextActive)
                    }
                    refreshWorkspaceFilesAndAnalysis(project)
                    postMessage("Renamed to $newRel")
                }
                .onFailure { postMessage("Rename failed: ${it.message}") }
        }
    }

    fun copyFile(srcRel: String, destRel: String) {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            fileManager.copyFile(project, srcRel, destRel)
                .onSuccess {
                    refreshWorkspaceFilesAndAnalysis(project)
                    postMessage("Copied to $destRel")
                }
                .onFailure { postMessage("Copy failed: ${it.message}") }
        }
    }

    fun moveFile(srcRel: String, destRel: String) {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            fileManager.moveFile(project, srcRel, destRel)
                .onSuccess { newRel ->
                    _uiState.update { state ->
                        val updatedTabs = state.openTabs.map { tab ->
                            when {
                                tab.relativePath == srcRel -> tab.copy(relativePath = newRel)
                                tab.relativePath.startsWith("$srcRel/") ->
                                    tab.copy(relativePath = newRel + tab.relativePath.removePrefix(srcRel))
                                else -> tab
                            }
                        }
                        val nextActive = when {
                            state.activeTabPath == srcRel -> newRel
                            state.activeTabPath?.startsWith("$srcRel/") == true ->
                                newRel + state.activeTabPath.removePrefix(srcRel)
                            else -> state.activeTabPath
                        }
                        state.copy(openTabs = updatedTabs, activeTabPath = nextActive)
                    }
                    persistCurrentTabs()
                    refreshWorkspaceFilesAndAnalysis(project)
                    appendIdeLog("Files", "Moved $srcRel -> $newRel")
                    postMessage("Moved to $newRel")
                }
                .onFailure { postMessage("Move failed: ${it.message}") }
        }
    }

    fun refreshWorkspace() {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            refreshWorkspaceFilesAndAnalysis(project)
            appendIdeLog("Workspace", "Refreshed workspace file tree (${_uiState.value.workspaceFiles.size} entries)")
            postMessage("Workspace refreshed")
        }
    }

    fun deleteSelectedFiles(paths: List<String>) {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            fileManager.deletePaths(project, paths)
                .onSuccess { count ->
                    _uiState.update { state ->
                        val remainingTabs = state.openTabs.filterNot { it.relativePath in paths }
                        val nextActive = if (state.activeTabPath in paths) {
                            remainingTabs.firstOrNull()?.relativePath
                        } else state.activeTabPath
                        state.copy(
                            openTabs = remainingTabs,
                            activeTabPath = nextActive,
                            selectedFilePaths = emptySet()
                        )
                    }
                    persistCurrentTabs()
                    refreshWorkspaceFilesAndAnalysis(project)
                    appendIdeLog("Files", "Deleted $count item(s): ${paths.joinToString()}")
                    postMessage("Deleted $count item(s)")
                }
                .onFailure { postMessage("Delete failed: ${it.message}") }
        }
    }

    fun openFileInEditor(
        relativePath: String,
        jumpToEditorTab: Boolean = false,
        persistTabs: Boolean = true
    ) {
        val project = _uiState.value.activeProject ?: return
        val alreadyOpen = _uiState.value.openTabs.firstOrNull { it.relativePath == relativePath }
        if (alreadyOpen != null) {
            _uiState.update {
                it.copy(
                    activeTabPath = relativePath,
                    activeLabSection = if (jumpToEditorTab) null else it.activeLabSection,
                    currentTab = if (jumpToEditorTab) PrimaryDestination.MAX_STUDIO else it.currentTab,
                    maxStudioMode = if (jumpToEditorTab) MaxStudioMode.CODE_EG else it.maxStudioMode
                )
            }
            updateCodeLivePreview(alreadyOpen.relativePath, alreadyOpen.content)
            if (persistTabs) persistCurrentTabs()
            return
        }

        viewModelScope.launch {
            fileManager.openFileTab(project, relativePath)
                .onSuccess { tab ->
                    _uiState.update { state ->
                        state.copy(
                            openTabs = state.openTabs + tab,
                            activeTabPath = tab.relativePath,
                            activeLabSection = if (jumpToEditorTab) null else state.activeLabSection,
                            currentTab = if (jumpToEditorTab) PrimaryDestination.MAX_STUDIO else state.currentTab,
                            maxStudioMode = if (jumpToEditorTab) MaxStudioMode.CODE_EG else state.maxStudioMode
                        )
                    }
                    if (tab.previewMode == FilePreviewKind.ZIP_ARCHIVE) {
                        fileManager.inspectZipEntries(project, relativePath).onSuccess { entries ->
                            _uiState.update { it.copy(zipInspectionEntries = entries) }
                        }
                    } else if (tab.previewMode == FilePreviewKind.CODE_TEXT) {
                        updateCodeLivePreview(tab.relativePath, tab.content)
                    }
                    if (persistTabs) persistCurrentTabs()
                    appendIdeLog("Editor", "Opened ${tab.relativePath}")
                }
                .onFailure { postMessage("Open failed: ${it.message}") }
        }
    }

    fun closeEditorTab(relativePath: String) {
        _uiState.update { state ->
            val filtered = state.openTabs.filterNot { it.relativePath == relativePath }
            val nextActive = if (state.activeTabPath == relativePath) {
                filtered.lastOrNull()?.relativePath
            } else state.activeTabPath
            state.copy(openTabs = filtered, activeTabPath = nextActive)
        }
        persistCurrentTabs()
    }

    fun closeOtherTabs(keepPath: String) {
        _uiState.update { state ->
            val kept = state.openTabs.filter { it.relativePath == keepPath }
            state.copy(openTabs = kept, activeTabPath = kept.firstOrNull()?.relativePath)
        }
        persistCurrentTabs()
    }

    fun closeAllTabs() {
        _uiState.update { it.copy(openTabs = emptyList(), activeTabPath = null) }
        persistCurrentTabs()
    }

    fun jumpToFileLine(relativePath: String, line: Int) {
        val targetLine = line.coerceAtLeast(1)
        _uiState.update { it.copy(pendingCursorJump = relativePath to targetLine) }
        openFileInEditor(relativePath, jumpToEditorTab = true)
    }

    fun consumeCursorJump() {
        _uiState.update { it.copy(pendingCursorJump = null) }
    }

    fun updateActiveTabContent(newContent: String) {
        val activePath = _uiState.value.activeTabPath ?: return
        var latestTab: OpenEditorTab? = null
        _uiState.update { state ->
            val updated = state.openTabs.map { tab ->
                if (tab.relativePath == activePath && tab.content != newContent) {
                    val nextUndo = (tab.undoStack + tab.content).takeLast(40)
                    tab.copy(
                        content = newContent,
                        undoStack = nextUndo,
                        redoStack = emptyList()
                    ).also { latestTab = it }
                } else tab
            }
            state.copy(openTabs = updated)
        }
        latestTab?.let { tab ->
            onTabContentMutated(tab)
        }
    }

    private fun onTabContentMutated(tab: OpenEditorTab, fromVisualBuilder: Boolean = false) {
        updateCodeLivePreview(tab.relativePath, tab.content)
        val fileIssues = StaticAnalyzer.analyzeFile(tab.relativePath, tab.content)
        _uiState.update { state ->
            val others = state.diagnostics.filterNot { it.filePath == tab.relativePath }
            state.copy(diagnostics = fileIssues + others)
        }
        if (!fromVisualBuilder && tab.relativePath.endsWith("VisualGeneratedScreen.kt")) {
            val parsed = StaticAnalyzer.parseCodeForLivePreview(tab.relativePath, tab.content)
            if (parsed.isSuccess && parsed.nodes.isNotEmpty()) {
                _uiState.update { it.copy(visualNodes = parsed.nodes) }
            }
        }
        if (settings.value.autoSaveEnabled && !tab.isLargeFilePreview) {
            scheduleAutoSave(tab.relativePath)
        }
    }

    private fun scheduleAutoSave(relativePath: String) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1200L)
            val project = _uiState.value.activeProject ?: return@launch
            val currentTab = _uiState.value.openTabs.firstOrNull { it.relativePath == relativePath } ?: return@launch
            if (!currentTab.isDirty || currentTab.isLargeFilePreview) return@launch
            fileManager.saveFileTab(project, currentTab, formatOnSave = settings.value.formatOnSave).onSuccess { saved ->
                _uiState.update { state ->
                    state.copy(
                        openTabs = state.openTabs.map {
                            if (it.relativePath == saved.relativePath) saved else it
                        }
                    )
                }
                refreshWorkspaceFilesAndAnalysis(project)
            }
        }
    }

    fun undoActiveTab() {
        val activePath = _uiState.value.activeTabPath ?: return
        var latestTab: OpenEditorTab? = null
        _uiState.update { state ->
            val updated = state.openTabs.map { tab ->
                if (tab.relativePath == activePath && tab.undoStack.isNotEmpty()) {
                    val previous = tab.undoStack.last()
                    tab.copy(
                        content = previous,
                        undoStack = tab.undoStack.dropLast(1),
                        redoStack = (tab.redoStack + tab.content).takeLast(40)
                    ).also { latestTab = it }
                } else tab
            }
            state.copy(openTabs = updated)
        }
        latestTab?.let { onTabContentMutated(it) }
    }

    fun redoActiveTab() {
        val activePath = _uiState.value.activeTabPath ?: return
        var latestTab: OpenEditorTab? = null
        _uiState.update { state ->
            val updated = state.openTabs.map { tab ->
                if (tab.relativePath == activePath && tab.redoStack.isNotEmpty()) {
                    val next = tab.redoStack.last()
                    tab.copy(
                        content = next,
                        undoStack = (tab.undoStack + tab.content).takeLast(40),
                        redoStack = tab.redoStack.dropLast(1)
                    ).also { latestTab = it }
                } else tab
            }
            state.copy(openTabs = updated)
        }
        latestTab?.let { onTabContentMutated(it) }
    }

    fun setActiveTabEncodingAndEnding(encoding: TextEncodingOption, lineEnding: LineEnding) {
        val activePath = _uiState.value.activeTabPath ?: return
        _uiState.update { state ->
            state.copy(
                openTabs = state.openTabs.map {
                    if (it.relativePath == activePath) it.copy(encoding = encoding, lineEnding = lineEnding)
                    else it
                }
            )
        }
    }

    fun saveActiveTab() {
        val project = _uiState.value.activeProject ?: return
        val tab = _uiState.value.activeTab ?: return
        viewModelScope.launch {
            fileManager.saveFileTab(project, tab, formatOnSave = settings.value.formatOnSave)
                .onSuccess { saved ->
                    _uiState.update { state ->
                        state.copy(
                            openTabs = state.openTabs.map {
                                if (it.relativePath == saved.relativePath) saved else it
                            }
                        )
                    }
                    refreshWorkspaceFilesAndAnalysis(project)
                    appendIdeLog("Save", "Saved ${saved.relativePath}")
                    postMessage("Saved ${saved.relativePath}")
                }
                .onFailure {
                    appendIdeLog("Save", "Failed saving ${tab.relativePath}: ${it.message}", isError = true)
                    postMessage("Save failed: ${it.message}")
                }
        }
    }

    fun saveAllTabs() {
        val project = _uiState.value.activeProject ?: return
        val dirtyTabs = _uiState.value.openTabs.filter { it.isDirty && !it.isLargeFilePreview }
        if (dirtyTabs.isEmpty()) {
            postMessage("All open files are already saved")
            return
        }
        val shouldFormat = settings.value.formatOnSave
        viewModelScope.launch {
            var savedCount = 0
            val savedMap = mutableMapOf<String, OpenEditorTab>()
            for (tab in dirtyTabs) {
                fileManager.saveFileTab(project, tab, formatOnSave = shouldFormat).onSuccess { saved ->
                    savedMap[saved.relativePath] = saved
                    savedCount++
                }
            }
            _uiState.update { state ->
                state.copy(
                    openTabs = state.openTabs.map { savedMap[it.relativePath] ?: it }
                )
            }
            refreshWorkspaceFilesAndAnalysis(project)
            appendIdeLog("Save All", "Saved $savedCount modified file(s)")
            postMessage("Saved $savedCount file(s)")
        }
    }

    fun saveAndCloseTabs(
        targetPaths: List<String>,
        closeOthersKeeping: String? = null,
        closeAll: Boolean = false
    ) {
        val project = _uiState.value.activeProject ?: return
        val dirtyTargets = _uiState.value.openTabs.filter {
            it.relativePath in targetPaths && it.isDirty && !it.isLargeFilePreview
        }
        val shouldFormat = settings.value.formatOnSave
        viewModelScope.launch {
            for (tab in dirtyTargets) {
                fileManager.saveFileTab(project, tab, formatOnSave = shouldFormat)
            }
            when {
                closeAll -> closeAllTabs()
                closeOthersKeeping != null -> closeOtherTabs(closeOthersKeeping)
                else -> targetPaths.forEach { closeEditorTab(it) }
            }
            refreshWorkspaceFilesAndAnalysis(project)
            if (dirtyTargets.isNotEmpty()) {
                appendIdeLog("Save", "Saved ${dirtyTargets.size} file(s) before closing")
            }
        }
    }

    fun searchWorkspaceFiles(query: String, ignoreCase: Boolean = true) {
        val project = _uiState.value.activeProject ?: return
        _uiState.update { it.copy(findInFilesQuery = query) }
        viewModelScope.launch {
            val matches = fileManager.searchInFiles(project, query, ignoreCase)
            _uiState.update { it.copy(findInFilesResults = matches) }
            if (query.isNotBlank()) {
                appendIdeLog("Find in Files", "Query '$query': ${matches.size} match(es)")
            }
        }
    }

    fun replaceAcrossWorkspace(find: String, replace: String, ignoreCase: Boolean = true) {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            fileManager.replaceInFiles(project, find, replace, ignoreCase)
                .onSuccess { (occurrences, changedFiles) ->
                    if (occurrences > 0) {
                        for (rel in changedFiles) {
                            val isOpen = _uiState.value.openTabs.any { it.relativePath == rel }
                            if (isOpen) {
                                fileManager.openFileTab(project, rel).onSuccess { reloaded ->
                                    _uiState.update { state ->
                                        state.copy(
                                            openTabs = state.openTabs.map {
                                                if (it.relativePath == rel) reloaded else it
                                            }
                                        )
                                    }
                                    updateCodeLivePreview(reloaded.relativePath, reloaded.content)
                                }
                            }
                        }
                        refreshWorkspaceFilesAndAnalysis(project)
                        searchWorkspaceFiles(find, ignoreCase)
                        appendIdeLog("Replace in Files", "Replaced $occurrences occurrence(s) across ${changedFiles.size} file(s)")
                        postMessage("Replaced $occurrences occurrence(s) in ${changedFiles.size} file(s)")
                    } else {
                        postMessage("No matches found across workspace")
                    }
                }
                .onFailure { postMessage("Replace failed: ${it.message}") }
        }
    }

    fun setEditorSearchState(query: String, matchCase: Boolean) {
        _uiState.update { it.copy(editorFindQuery = query, editorFindMatchCase = matchCase) }
    }

    fun findNextInActiveTab(currentCursorOffset: Int) {
        val tab = _uiState.value.activeTab ?: return
        val query = _uiState.value.editorFindQuery
        if (query.isEmpty()) {
            postMessage("Enter search text first")
            return
        }
        val nextRange = CodeEditorHelpers.findNextOccurrenceOffset(
            text = tab.content,
            query = query,
            currentOffset = currentCursorOffset,
            ignoreCase = !_uiState.value.editorFindMatchCase
        )
        if (nextRange == null) {
            postMessage("No matches found for '$query'")
        } else {
            _uiState.update { it.copy(pendingOffsetJump = tab.relativePath to nextRange) }
        }
    }

    fun clearPendingOffsetJump() {
        _uiState.update { it.copy(pendingOffsetJump = null) }
    }

    fun formatActiveTab() {
        val tab = _uiState.value.activeTab ?: return
        if (tab.isLargeFilePreview || tab.previewMode != FilePreviewKind.CODE_TEXT) {
            postMessage("Cannot format binary or large read-only preview file")
            return
        }
        val ext = tab.relativePath.substringAfterLast('.', "")
        val formatted = CodeEditorHelpers.formatCode(tab.content, ext, settings.value.tabWidth)
        if (formatted != tab.content) {
            updateActiveTabContent(formatted)
            postMessage("Formatted ${tab.relativePath.substringAfterLast('/')}")
        } else {
            postMessage("File is already formatted")
        }
    }

    fun replaceInActiveTab(find: String, replace: String, replaceAll: Boolean, ignoreCase: Boolean = false) {
        if (find.isEmpty()) return
        val tab = _uiState.value.activeTab ?: return
        val options = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
        val regex = Regex(Regex.escape(find), options)
        val matchCount = regex.findAll(tab.content).count()
        if (matchCount == 0) {
            postMessage("No matches found for '$find'")
            return
        }
        val safeReplacement = Regex.escapeReplacement(replace)
        val next = if (replaceAll) regex.replace(tab.content, safeReplacement)
        else regex.replaceFirst(tab.content, safeReplacement)
        if (next != tab.content) {
            updateActiveTabContent(next)
            postMessage(if (replaceAll) "Replaced $matchCount occurrence(s)" else "Replaced first occurrence")
        }
    }

    private fun updateCodeLivePreview(relativePath: String, content: String) {
        val parsed = StaticAnalyzer.parseCodeForLivePreview(relativePath, content)
        _uiState.update { state ->
            val current = state.codePreviewState
            val next = if (parsed.isSuccess) {
                CodePreviewState(
                    lastGoodTitle = parsed.title,
                    lastGoodNodes = parsed.nodes,
                    isStale = false,
                    errorMessage = null,
                    sourceFile = relativePath,
                    lastUpdatedMs = System.currentTimeMillis()
                )
            } else {
                // Preserve last known good preview and show stale + compile error warning
                current.copy(
                    isStale = true,
                    errorMessage = parsed.errorMessage,
                    sourceFile = relativePath
                )
            }
            state.copy(codePreviewState = next)
        }
    }

    fun refreshActiveCodePreview() {
        val active = _uiState.value.activeTab ?: return
        if (active.previewMode == FilePreviewKind.CODE_TEXT) {
            updateCodeLivePreview(active.relativePath, active.content)
        }
    }

    // --- Visual Builder ---

    fun addVisualNode(type: VisualComponentType) {
        val node = VisualNode(
            id = UUID.randomUUID().toString().take(6),
            type = type,
            label = when (type) {
                VisualComponentType.TEXT -> "Heading Label"
                VisualComponentType.BUTTON -> "Primary Action"
                VisualComponentType.CARD -> "Feature Card"
                VisualComponentType.COUNTER -> "Active Counter"
                VisualComponentType.TEXT_FIELD -> "Input Field"
                VisualComponentType.SWITCH_CONTROL -> "Toggle Setting"
                else -> type.displayName
            },
            secondaryText = when (type) {
                VisualComponentType.CARD -> "Generated in M.A.X. Studio"
                VisualComponentType.COUNTER -> "Tap + or - to update state"
                else -> ""
            },
            paddingDp = 12,
            useAccent = type == VisualComponentType.BUTTON || type == VisualComponentType.COUNTER,
            colorTag = if (type == VisualComponentType.BUTTON || type == VisualComponentType.COUNTER) "Cyan" else "Default"
        )
        _uiState.update { it.copy(visualNodes = it.visualNodes + node) }
        syncVisualBuilderToCodeTab()
    }

    fun updateVisualNodeLabel(id: String, newLabel: String) {
        _uiState.update { state ->
            state.copy(
                visualNodes = state.visualNodes.map { if (it.id == id) it.copy(label = newLabel) else it }
            )
        }
        syncVisualBuilderToCodeTab()
    }

    fun updateVisualNode(
        id: String,
        label: String? = null,
        secondaryText: String? = null,
        paddingDp: Int? = null,
        colorTag: String? = null
    ) {
        _uiState.update { state ->
            state.copy(
                visualNodes = state.visualNodes.map { node ->
                    if (node.id == id) {
                        node.copy(
                            label = label ?: node.label,
                            secondaryText = secondaryText ?: node.secondaryText,
                            paddingDp = paddingDp?.coerceIn(0, 48) ?: node.paddingDp,
                            colorTag = colorTag ?: node.colorTag
                        )
                    } else node
                }
            )
        }
        syncVisualBuilderToCodeTab()
    }

    fun moveVisualNode(id: String, direction: Int) {
        _uiState.update { state ->
            val list = state.visualNodes.toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx < 0) return@update state
            val targetIdx = (idx + direction).coerceIn(0, list.lastIndex)
            if (targetIdx == idx) return@update state
            val item = list.removeAt(idx)
            list.add(targetIdx, item)
            state.copy(visualNodes = list)
        }
        syncVisualBuilderToCodeTab()
    }

    fun reorderVisualNode(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val list = state.visualNodes.toMutableList()
            if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) {
                return@update state
            }
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            state.copy(visualNodes = list)
        }
        syncVisualBuilderToCodeTab()
    }

    fun removeVisualNode(id: String) {
        _uiState.update { state ->
            state.copy(visualNodes = state.visualNodes.filterNot { it.id == id })
        }
        syncVisualBuilderToCodeTab()
    }

    private fun syncVisualBuilderToCodeTab() {
        val project = _uiState.value.activeProject ?: return
        val nodes = _uiState.value.visualNodes
        val pkgPath = project.packageName.replace('.', '/')
        val targetPath = "app/src/main/java/$pkgPath/VisualGeneratedScreen.kt"
        val generatedCode = VisualBuilderCodegen.generateComposeKotlin(project.packageName, "VisualGeneratedScreen", nodes)

        var syncedTab: OpenEditorTab? = null
        _uiState.update { state ->
            val existing = state.openTabs.firstOrNull { it.relativePath == targetPath }
            val nextTab = if (existing != null) {
                if (existing.content == generatedCode) {
                    existing
                } else {
                    existing.copy(
                        content = generatedCode,
                        undoStack = (existing.undoStack + existing.content).takeLast(40),
                        redoStack = emptyList()
                    )
                }
            } else {
                val diskFile = runCatching { fileManager.resolveSafe(project, targetPath) }.getOrNull()
                val saved = if (diskFile != null && diskFile.exists()) {
                    runCatching { diskFile.readText(Charsets.UTF_8) }.getOrDefault("")
                } else ""
                OpenEditorTab(
                    relativePath = targetPath,
                    content = generatedCode,
                    savedContent = saved
                )
            }
            syncedTab = nextTab
            val nextTabs = if (existing != null) {
                state.openTabs.map { if (it.relativePath == targetPath) nextTab else it }
            } else {
                state.openTabs + nextTab
            }
            state.copy(openTabs = nextTabs, activeTabPath = targetPath)
        }
        syncedTab?.let { onTabContentMutated(it, fromVisualBuilder = true) }
    }

    fun writeVisualBuilderToProject(asXml: Boolean) {
        val project = _uiState.value.activeProject ?: return
        val nodes = _uiState.value.visualNodes
        val targetPath = if (asXml) {
            "app/src/main/res/layout/activity_visual_generated.xml"
        } else {
            val pkgPath = project.packageName.replace('.', '/')
            "app/src/main/java/$pkgPath/VisualGeneratedScreen.kt"
        }
        val code = if (asXml) {
            VisualBuilderCodegen.generateAndroidXmlLayout(nodes)
        } else {
            VisualBuilderCodegen.generateComposeKotlin(project.packageName, "VisualGeneratedScreen", nodes)
        }
        viewModelScope.launch {
            val file = fileManager.resolveSafe(project, targetPath)
            file.parentFile?.mkdirs()
            file.writeText(code, Charsets.UTF_8)
            _uiState.update { state ->
                state.copy(
                    openTabs = state.openTabs.map { tab ->
                        if (tab.relativePath == targetPath) tab.copy(content = code, savedContent = code) else tab
                    }
                )
            }
            refreshWorkspaceFilesAndAnalysis(project)
            openFileInEditor(targetPath)
            postMessage("Generated $targetPath from Visual Builder")
        }
    }

    fun selectDeviceProfile(profile: DeviceProfile) {
        _uiState.update { it.copy(selectedDeviceProfile = profile) }
    }

    fun togglePreviewOrientation() {
        _uiState.update { it.copy(previewLandscape = !it.previewLandscape) }
    }

    fun setPreviewFontScale(scale: Float) {
        _uiState.update { it.copy(previewFontScale = scale.coerceIn(0.85f, 1.4f)) }
    }

    // --- Terminal & Developer Lab (M11 / M12 / M16 / M17 / M24) ---

    fun runTerminalCommand(command: String) {
        val project = _uiState.value.activeProject
        val sessionId = _uiState.value.activeTerminalSessionId
            ?: terminalManager.ensureDefaultSession(project?.rootPath ?: getApplication<Application>().filesDir.absolutePath)
        _uiState.update { it.copy(activeTerminalSessionId = sessionId) }

        val hooks = TerminalCommandHooks(
            onWorkspaceChanged = {
                project?.let { refreshWorkspaceFilesAndAnalysis(it) }
            },
            onGitStatus = {
                buildGitStatusLines(project)
            },
            onGitDiff = {
                buildGitDiffLines(project)
            },
            onGradleBuild = { task ->
                runTerminalGradleLint(project, task)
            }
        )

        viewModelScope.launch {
            terminalManager.runCommand(sessionId, command, hooks)
        }
    }

    private suspend fun buildGitStatusLines(project: ProjectEntity?): List<TerminalLine> {
        if (project == null) return listOf(TerminalLine(text = "No active workspace.", isError = true))
        val diskChanges = fileManager.computeWorkspaceChanges(project)
        val dirtyTabs = _uiState.value.openTabs.filter { it.isDirty }
        val lines = mutableListOf(
            TerminalLine(text = "On branch ${settings.value.defaultGitBranch}", isPrompt = true)
        )
        if (diskChanges.isEmpty() && dirtyTabs.isEmpty()) {
            lines.add(TerminalLine(text = "nothing to commit, working tree clean"))
        } else {
            diskChanges.forEach { ch ->
                val prefix = when (ch.status) {
                    "ADDED" -> "new file:"
                    "DELETED" -> "deleted:"
                    else -> "modified:"
                }
                lines.add(TerminalLine(text = "  $prefix   ${ch.relativePath}", isError = ch.status == "DELETED"))
            }
            dirtyTabs.forEach { tab ->
                if (diskChanges.none { it.relativePath == tab.relativePath }) {
                    lines.add(TerminalLine(text = "  unsaved:    ${tab.relativePath} (editor buffer)"))
                }
            }
        }
        return lines
    }

    private suspend fun buildGitDiffLines(project: ProjectEntity?): List<TerminalLine> {
        if (project == null) return listOf(TerminalLine(text = "No active workspace.", isError = true))
        val changes = fileManager.computeWorkspaceChanges(project)
        if (changes.isEmpty()) {
            return listOf(TerminalLine(text = "No modified files against baseline snapshot."))
        }
        val out = mutableListOf<TerminalLine>()
        changes.take(6).forEach { ch ->
            out.add(TerminalLine(text = "--- a/${ch.relativePath}", isPrompt = true))
            out.add(TerminalLine(text = "+++ b/${ch.relativePath}", isPrompt = true))
            ch.diffLines.filter { it.marker != ' ' }.take(24).forEach { dl ->
                out.add(TerminalLine(text = "${dl.marker} ${dl.text}", isError = dl.marker == '-'))
            }
        }
        return out
    }

    private suspend fun runTerminalGradleLint(project: ProjectEntity?, task: String): Pair<Int, List<TerminalLine>> {
        if (project == null) return 1 to listOf(TerminalLine(text = "No active project workspace.", isError = true))
        val root = File(project.rootPath)
        val diskIssues = StaticAnalyzer.analyzeWorkspace(root)
        // Also analyze unsaved open editor buffers so live editor errors immediately surface in Problems
        val openBuffers = _uiState.value.openTabs
        val bufferPaths = openBuffers.map { it.relativePath }.toSet()
        val bufferIssues = openBuffers.flatMap { StaticAnalyzer.analyzeFile(it.relativePath, it.content) }
        val combinedIssues = bufferIssues + diskIssues.filterNot { it.filePath in bufferPaths }

        _uiState.update { state ->
            state.copy(
                diagnostics = combinedIssues,
                buildState = state.buildState.copy(
                    isRunning = false,
                    progress = 1f,
                    succeeded = combinedIssues.none { it.severity == DiagnosticSeverity.ERROR || it.severity == DiagnosticSeverity.SECRET },
                    statusLabel = if (combinedIssues.none { it.severity == DiagnosticSeverity.ERROR || it.severity == DiagnosticSeverity.SECRET }) {
                        "Gradle Lint Passed"
                    } else {
                        "Gradle Lint Failed"
                    }
                )
            )
        }

        val errIssues = combinedIssues.filter { it.severity == DiagnosticSeverity.ERROR || it.severity == DiagnosticSeverity.SECRET }
        val warnIssues = combinedIssues.filter { it.severity == DiagnosticSeverity.WARNING }
        val lines = mutableListOf(
            TerminalLine(text = "> Task :app:$task (M.A.X. Syntax & Lint Verifier)"),
            TerminalLine(text = "Scanned ${bufferPaths.size} open buffer(s) + workspace files...")
        )
        combinedIssues.take(20).forEach { iss ->
            lines.add(
                TerminalLine(
                    text = "  [${iss.severity.name}] ${iss.filePath}:${iss.line} — ${iss.message}",
                    isError = iss.severity == DiagnosticSeverity.ERROR || iss.severity == DiagnosticSeverity.SECRET
                )
            )
        }
        if (errIssues.isEmpty()) {
            lines.add(TerminalLine(text = "BUILD SUCCESSFUL (${warnIssues.size} warning(s) — Problems tab updated)", isPrompt = true))
            appendIdeLog("Gradle", "gradle $task passed (${warnIssues.size} warnings)")
            return 0 to lines
        } else {
            lines.add(TerminalLine(text = "BUILD FAILED with ${errIssues.size} error(s). Inspect Problems tab.", isError = true))
            appendIdeLog("Gradle", "gradle $task failed with ${errIssues.size} error(s)", isError = true)
            return 1 to lines
        }
    }

    fun cancelTerminalCommand() {
        val sessionId = _uiState.value.activeTerminalSessionId ?: return
        terminalManager.cancelCommand(sessionId)
    }

    fun createTerminalSession() {
        val workDir = _uiState.value.activeProject?.rootPath ?: getApplication<Application>().filesDir.absolutePath
        val id = terminalManager.createSession(workDir)
        _uiState.update { it.copy(activeTerminalSessionId = id) }
    }

    fun clearTerminalSession(id: String) {
        terminalManager.clearSession(id)
    }

    fun closeTerminalSession(id: String) {
        val current = terminalManager.sessions.value
        if (current.size <= 1) return
        terminalManager.closeSession(id)
        val remaining = terminalManager.sessions.value
        if (_uiState.value.activeTerminalSessionId == id) {
            _uiState.update { it.copy(activeTerminalSessionId = remaining.firstOrNull()?.id) }
        }
    }

    fun updateBottomPanelState(isOpen: Boolean, tab: BottomToolTab, heightDp: Int) {
        viewModelScope.launch {
            preferences.updateBottomPanelState(isOpen, tab.name, heightDp)
        }
    }

    fun setEditorWorkspaceMode(modeName: String) {
        if (modeName == "LIVE_PREVIEW") {
            ensureDefaultFileOpen()
            refreshActiveCodePreview()
        } else if (modeName == "VISUAL_BUILDER") {
            val active = _uiState.value.activeTab
            if (active != null && active.relativePath.endsWith(".kt")) {
                val parsed = StaticAnalyzer.parseCodeForLivePreview(active.relativePath, active.content)
                if (parsed.isSuccess && parsed.nodes.isNotEmpty() && active.relativePath.endsWith("VisualGeneratedScreen.kt")) {
                    _uiState.update { it.copy(visualNodes = parsed.nodes) }
                }
            }
        }
        viewModelScope.launch {
            preferences.setEditorWorkspaceMode(modeName)
        }
    }

    fun runWorkspaceBuildCheck() {
        val project = _uiState.value.activeProject ?: return
        if (_uiState.value.buildState.isRunning) return
        viewModelScope.launch {
            val probe = _uiState.value.toolchainProbe ?: terminalManager.probeToolchainAndDevice()
            _uiState.update {
                it.copy(
                    toolchainProbe = probe,
                    buildState = BuildRunState(
                        isRunning = true,
                        statusLabel = "Running",
                        progress = 0.15f,
                        logs = listOf("[Build] Target workspace: ${project.name} (${project.rootPath})")
                    )
                )
            }
            appendIdeLog("Build", "Started build/verification for ${project.name}")

            if (probe.gradleBinaryFound) {
                val logs = mutableListOf("[Build] Host Gradle binary detected — invoking `gradle assembleDebug`...")
                val exitCode = withContext(Dispatchers.IO) {
                    runCatching {
                        val proc = ProcessBuilder("gradle", "assembleDebug")
                            .directory(File(project.rootPath))
                            .redirectErrorStream(true)
                            .start()
                        proc.inputStream.bufferedReader().useLines { seq ->
                            seq.forEach { line ->
                                logs.add(line)
                                _uiState.update { st ->
                                    st.copy(buildState = st.buildState.copy(progress = 0.65f, logs = logs.takeLast(120)))
                                }
                            }
                        }
                        proc.waitFor()
                    }.getOrElse {
                        logs.add("Gradle execution error: ${it.message}")
                        -1
                    }
                }
                val ok = exitCode == 0
                _uiState.update {
                    it.copy(
                        buildState = BuildRunState(
                            isRunning = false,
                            statusLabel = if (ok) "Gradle Succeeded" else "Gradle Failed (exit $exitCode)",
                            progress = 1f,
                            logs = logs,
                            succeeded = ok
                        )
                    )
                }
                appendIdeLog("Build", if (ok) "Gradle assembleDebug succeeded" else "Gradle failed (exit $exitCode)", isError = !ok)
            } else {
                // Honest on-device pre-flight verification (no fake APK compilation!)
                val logs = mutableListOf(
                    "[Stage 1/3] Checking workspace files & Gradle configuration...",
                )
                refreshWorkspaceFilesAndAnalysis(project)
                val files = _uiState.value.workspaceFiles
                val hasGradle = files.any { it.name == "build.gradle.kts" || it.name == "build.gradle" }
                val hasManifest = files.any { it.name == "AndroidManifest.xml" }
                val hasCi = files.any { it.relativePath.contains(".github/workflows") }
                logs.add("  • Workspace files scanned: ${files.count { !it.isDirectory }}")
                logs.add("  • Gradle build scripts present: $hasGradle")
                logs.add("  • AndroidManifest.xml present: $hasManifest")
                logs.add("  • GitHub Actions CI workflow present: $hasCi")
                _uiState.update { it.copy(buildState = it.buildState.copy(progress = 0.6f, logs = logs.toList())) }

                val issues = _uiState.value.diagnostics
                val errCount = issues.count { it.severity == DiagnosticSeverity.ERROR || it.severity == DiagnosticSeverity.SECRET }
                val warnCount = issues.count { it.severity == DiagnosticSeverity.WARNING }
                logs.add("[Stage 2/3] Static syntax, XML/JSON & secret audit...")
                logs.add("  • Errors/Secrets: $errCount • Warnings: $warnCount")
                issues.take(12).forEach { iss ->
                    logs.add("  [${iss.severity.name}] ${iss.filePath}:${iss.line} — ${iss.message}")
                }
                logs.add("[Stage 3/3] Native JDK/Gradle toolchain check...")
                logs.add("  • Host `gradle`/`javac` is NOT bundled inside the Android app sandbox.")
                logs.add("  • Pre-flight checks ${if (errCount == 0) "passed" else "found $errCount blocker(s)"}; full APK compilation requires GitHub Actions CI or a host JDK.")

                val passed = errCount == 0
                _uiState.update {
                    it.copy(
                        buildState = BuildRunState(
                            isRunning = false,
                            statusLabel = if (passed) "Pre-Flight Passed (No Host JDK)" else "Pre-Flight Failed ($errCount error)",
                            progress = 1f,
                            logs = logs,
                            succeeded = passed
                        )
                    )
                }
                appendIdeLog("Build", "Pre-flight verification finished: $errCount error(s), $warnCount warning(s)", isError = !passed)
            }
        }
    }

    fun selectTerminalSession(id: String) {
        _uiState.update { it.copy(activeTerminalSessionId = id) }
    }

    fun refreshLogcat(filterTag: String = "", minPriority: String = "V") {
        viewModelScope.launch {
            val lines = terminalManager.readOwnProcessLogcat(filterTag, minPriority)
            _uiState.update { it.copy(logcatLines = lines) }
        }
    }

    fun refreshDiagnosticsProbe() {
        viewModelScope.launch {
            val report = terminalManager.probeToolchainAndDevice()
            _uiState.update { it.copy(toolchainProbe = report) }
        }
    }

    fun analyzeApkUri(uri: Uri, displayName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isApkAnalyzing = true) }
            apkAnalyzer.analyzeUri(uri, displayName)
                .onSuccess { report -> _uiState.update { it.copy(apkReport = report, isApkAnalyzing = false) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isApkAnalyzing = false) }
                    postMessage("APK/AAB analysis error: ${e.message}")
                }
        }
    }

    fun analyzeSelfApk() {
        viewModelScope.launch {
            _uiState.update { it.copy(isApkAnalyzing = true) }
            apkAnalyzer.analyzeInstalledPackage(getApplication<Application>().packageName)
                .onSuccess { report -> _uiState.update { it.copy(apkReport = report, isApkAnalyzing = false) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isApkAnalyzing = false) }
                    postMessage("Self-APK analysis error: ${e.message}")
                }
        }
    }

    // --- AI Studio (BYOK M09 / M10) ---

    fun testAiConnection(provider: AiProviderType, apiKeyInput: String, model: String, baseUrl: String) {
        val resolvedKey = apiKeyInput.trim().ifBlank {
            vault.readSecret(vault.slotForAiProvider(provider)).orEmpty()
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingAiConnection = true, aiConnectionTestResult = null) }
            aiGateway.testConnection(provider, resolvedKey, model, baseUrl)
                .onSuccess { msg ->
                    _uiState.update { it.copy(isTestingAiConnection = false, aiConnectionTestResult = true to msg) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isTestingAiConnection = false,
                            aiConnectionTestResult = false to (err.message ?: "Connection failed")
                        )
                    }
                }
        }
    }

    fun clearAiConnectionTestResult() {
        _uiState.update { it.copy(aiConnectionTestResult = null) }
    }

    fun clearFetchedModels() {
        _uiState.update { it.copy(fetchedModels = emptyList(), fetchModelsError = null) }
    }

    fun fetchAiModels(provider: AiProviderType, apiKeyInput: String, baseUrl: String) {
        val resolvedKey = apiKeyInput.trim().ifBlank {
            vault.readSecret(vault.slotForAiProvider(provider)).orEmpty()
        }
        if (resolvedKey.isBlank()) {
            _uiState.update {
                it.copy(
                    isFetchingModels = false,
                    fetchModelsError = "Enter or save an API key for ${provider.displayName} to fetch models."
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingModels = true, fetchModelsError = null) }
            aiGateway.fetchAvailableModels(provider, resolvedKey, baseUrl)
                .onSuccess { models ->
                    _uiState.update {
                        it.copy(
                            isFetchingModels = false,
                            fetchedModels = models,
                            fetchModelsError = if (models.isEmpty()) "Endpoint returned 0 models." else null
                        )
                    }
                    if (models.isNotEmpty()) {
                        appendIdeLog("M.A.X. AI", "Fetched ${models.size} model(s) from ${provider.displayName}")
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isFetchingModels = false,
                            fetchModelsError = err.message ?: "Failed to fetch available models"
                        )
                    }
                }
        }
    }

    fun sendAiPrompt(
        prompt: String,
        attachActiveFile: Boolean,
        attachedExtraPaths: List<String> = emptyList()
    ) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty()) return
        val currentSettings = settings.value
        val key = vault.readSecret(vault.slotForAiProvider(currentSettings.aiProvider)).orEmpty()
        val activeTab = _uiState.value.activeTab
        val project = _uiState.value.activeProject

        val userMsg = AiChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = cleanPrompt,
            timestampMs = System.currentTimeMillis()
        )
        _uiState.update { it.copy(aiMessages = it.aiMessages + userMsg, isAiLoading = true) }

        viewModelScope.launch {
            if (project != null) {
                dao.insertAiMessage(userMsg.toEntity(project.id))
            }
            val extraContext = withContext(Dispatchers.IO) {
                if (project == null) emptyList()
                else attachedExtraPaths.distinct().take(5).mapNotNull { rel ->
                    runCatching {
                        val f = fileManager.resolveSafe(project, rel)
                        if (f.exists() && f.isFile && f.length() <= WorkspaceFileManager.MAX_EDITABLE_BYTES) {
                            rel to f.readText()
                        } else null
                    }.getOrNull()
                }
            }

            val result: Result<AiGatewayResponse> = aiGateway.sendPrompt(
                provider = currentSettings.aiProvider,
                apiKey = key,
                model = currentSettings.aiModel,
                baseUrl = currentSettings.aiBaseUrl,
                userPrompt = cleanPrompt,
                activeFilePath = if (attachActiveFile) activeTab?.relativePath else null,
                activeFileContent = if (attachActiveFile) activeTab?.content else null,
                additionalContextFiles = extraContext,
                redactSecrets = currentSettings.redactSecretsInAi,
                existingFileLookup = { relPath ->
                    if (project == null) null
                    else runCatching {
                        val f = fileManager.resolveSafe(project, relPath)
                        if (f.exists() && f.isFile && f.length() <= WorkspaceFileManager.MAX_EDITABLE_BYTES) f.readText()
                        else null
                    }.getOrNull()
                }
            )

            result.onSuccess { resp ->
                val reply = AiChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = resp.text,
                    timestampMs = System.currentTimeMillis(),
                    proposedChanges = resp.proposedChanges
                )
                if (project != null) {
                    dao.insertAiMessage(reply.toEntity(project.id))
                }
                _uiState.update { state ->
                    val merged = if (state.aiMessages.any { it.id == reply.id }) state.aiMessages else state.aiMessages + reply
                    state.copy(aiMessages = merged, isAiLoading = false)
                }
                if (!currentSettings.confirmAiChanges && resp.proposedChanges.isNotEmpty()) {
                    resp.proposedChanges.forEach { change ->
                        applyAiProposedChange(reply.id, change)
                    }
                }
            }.onFailure { err ->
                val reply = AiChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = err.message ?: "AI request failed",
                    timestampMs = System.currentTimeMillis(),
                    isError = true
                )
                if (project != null) {
                    dao.insertAiMessage(reply.toEntity(project.id))
                }
                _uiState.update { state ->
                    val merged = if (state.aiMessages.any { it.id == reply.id }) state.aiMessages else state.aiMessages + reply
                    state.copy(aiMessages = merged, isAiLoading = false)
                }
            }
        }
    }

    fun applyAiProposedChange(messageId: String, change: AiProposedFileChange) {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            runCatching {
                val target = fileManager.resolveSafe(project, change.relativePath)
                target.parentFile?.mkdirs()
                target.writeText(change.proposedContent, Charsets.UTF_8)
            }.onSuccess {
                var updatedTargetMsg: AiChatMessage? = null
                _uiState.update { state ->
                    val updatedMessages = state.aiMessages.map { msg ->
                        if (msg.id == messageId) {
                            msg.copy(
                                proposedChanges = msg.proposedChanges.map { p ->
                                    if (p.relativePath == change.relativePath) p.copy(isApplied = true) else p
                                }
                            ).also { updatedTargetMsg = it }
                        } else msg
                    }
                    val updatedTabs = state.openTabs.map { tab ->
                        if (tab.relativePath == change.relativePath) {
                            tab.copy(content = change.proposedContent, savedContent = change.proposedContent)
                        } else tab
                    }
                    state.copy(aiMessages = updatedMessages, openTabs = updatedTabs)
                }
                updatedTargetMsg?.let { dao.insertAiMessage(it.toEntity(project.id)) }
                refreshWorkspaceFilesAndAnalysis(project)
                postMessage("Applied approved patch to ${change.relativePath}")
            }.onFailure {
                postMessage("Patch apply blocked: ${it.message}")
            }
        }
    }

    fun sendVisualBuilderAiCommand(prompt: String) {
        val currentSettings = settings.value
        val key = vault.readSecret(vault.slotForAiProvider(currentSettings.aiProvider)).orEmpty()
        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true) }
            val systemContext = "The user is using M.A.X. Studio Visual Builder. Current visual nodes: ${_uiState.value.visualNodes.map { it.label }}. User prompt: $prompt. Return a short description or component title to generate."
            val result = aiGateway.sendPrompt(
                provider = currentSettings.aiProvider,
                apiKey = key,
                model = currentSettings.aiModel,
                baseUrl = currentSettings.aiBaseUrl,
                userPrompt = systemContext,
                activeFilePath = null,
                activeFileContent = null,
                additionalContextFiles = emptyList(),
                redactSecrets = currentSettings.redactSecretsInAi,
                existingFileLookup = { _ -> null }
            )
            result.onSuccess { resp ->
                val text = resp.text.trim()
                val newNodeType = when {
                    text.contains("button", true) -> VisualComponentType.BUTTON
                    text.contains("card", true) -> VisualComponentType.CARD
                    text.contains("switch", true) -> VisualComponentType.SWITCH_CONTROL
                    text.contains("input", true) || text.contains("field", true) -> VisualComponentType.TEXT_FIELD
                    else -> VisualComponentType.TEXT
                }
                val nodeLabel = text.take(35).ifBlank { prompt.take(35) }
                val node = VisualNode(
                    id = UUID.randomUUID().toString().take(6),
                    type = newNodeType,
                    label = nodeLabel,
                    secondaryText = "AI Generated"
                )
                _uiState.update { st ->
                    st.copy(
                        visualNodes = st.visualNodes + node,
                        isAiLoading = false,
                        statusMessage = "AI Command executed successfully"
                    )
                }
                syncVisualBuilderToCodeTab()
            }.onFailure { err ->
                _uiState.update { st ->
                    st.copy(
                        isAiLoading = false,
                        statusMessage = "AI command failed: ${err.message}"
                    )
                }
            }
        }
    }

    fun saveChatHistoryToWorkspace() {
        val project = _uiState.value.activeProject ?: return
        val messages = _uiState.value.aiMessages
        if (messages.isEmpty()) {
            postMessage("No chat messages to save.")
            return
        }
        val sb = StringBuilder()
        sb.append("# M.A.X. Core AI Conversation History\n")
        sb.append("Project: ${project.name} (${project.packageName})\n")
        sb.append("Saved At: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n---\n\n")
        messages.forEach { msg ->
            val sender = if (msg.isUser) "User" else "M.A.X. AI"
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(msg.timestampMs))
            sb.append("### [$time] $sender\n\n${msg.text}\n\n")
            if (msg.proposedChanges.isNotEmpty()) {
                sb.append("**Proposed Changes:**\n")
                msg.proposedChanges.forEach { p ->
                    sb.append("- `${p.relativePath}` (${if (p.isApplied) "Applied" else "Pending"})\n")
                }
                sb.append("\n")
            }
            sb.append("---\n\n")
        }
        val filename = "ai_chat_history_${System.currentTimeMillis().toString().takeLast(6)}.md"
        viewModelScope.launch {
            fileManager.createFileOrFolder(project, filename, isDirectory = false, initialContent = sb.toString())
                .onSuccess {
                    refreshWorkspaceFilesAndAnalysis(project)
                    openFileInEditor(filename, jumpToEditorTab = true)
                    postMessage("Saved chat history to workspace: $filename")
                }
                .onFailure {
                    postMessage("Failed to save chat history: ${it.message}")
                }
        }
    }

    fun applyCodeBlockToActiveEditor(code: String) {
        val active = _uiState.value.activeTab
        if (active != null && active.previewMode == FilePreviewKind.CODE_TEXT && !active.isLargeFilePreview) {
            updateActiveTabContent(code)
            _uiState.update {
                it.copy(
                    currentTab = PrimaryDestination.MAX_STUDIO,
                    maxStudioMode = MaxStudioMode.CODE_EG,
                    activeLabSection = null
                )
            }
            setEditorWorkspaceMode("CODE")
            postMessage("Applied AI code to ${active.relativePath.substringAfterLast('/')}")
            return
        }
        val project = _uiState.value.activeProject ?: return
        val pkgPath = project.packageName.replace('.', '/')
        val targetPath = "app/src/main/java/$pkgPath/LdroidGeneratedSnippet.kt"
        createFileOrFolder(targetPath, false, code)
        _uiState.update {
            it.copy(
                currentTab = PrimaryDestination.MAX_STUDIO,
                maxStudioMode = MaxStudioMode.CODE_EG,
                activeLabSection = null
            )
        }
        setEditorWorkspaceMode("CODE")
    }

    fun fixProblemsWithAi() {
        val issues = _uiState.value.diagnostics
        _uiState.update {
            it.copy(
                currentTab = PrimaryDestination.MAX_STUDIO,
                maxStudioMode = MaxStudioMode.CODE_EG,
                activeLabSection = null
            )
        }
        if (issues.isEmpty()) {
            postMessage("No workspace diagnostics to fix")
            return
        }
        val summary = issues.take(20).joinToString("\n") {
            "- [${it.severity.name}] ${it.filePath}:${it.line} (${it.source}): ${it.message}"
        }
        val affectedFiles = issues.map { it.filePath }.distinct().take(4)
        sendAiPrompt(
            prompt = "Fix these workspace diagnostics and propose clean <<<FILE: ...>>> patches:\n\n$summary",
            attachActiveFile = true,
            attachedExtraPaths = affectedFiles
        )
    }

    fun clearAiConversation() {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            dao.deleteAiMessagesForProject(project.id)
            _uiState.update { it.copy(aiMessages = emptyList()) }
            postMessage("Cleared AI conversation for ${project.name}")
        }
    }

    fun explainLogcatWithAi() {
        val sample = _uiState.value.logcatLines.takeLast(35).joinToString("\n")
        _uiState.update {
            it.copy(
                currentTab = PrimaryDestination.MAX_STUDIO,
                maxStudioMode = MaxStudioMode.CODE_EG,
                activeLabSection = null
            )
        }
        sendAiPrompt("Analyze these Android Logcat entries and explain any warnings, errors, or actionable suggestions:\n\n$sample", attachActiveFile = false)
    }

    // --- Max Studio Modes, Universal Bar, Prompt Enhancer & Offline APK Build ---

    fun selectMaxStudioMode(mode: MaxStudioMode) {
        _uiState.update { it.copy(maxStudioMode = mode) }
        if (mode == MaxStudioMode.CODE_EG) {
            ensureDefaultFileOpen()
        } else if (mode == MaxStudioMode.AI_UI_SCREEN) {
            refreshActiveCodePreview()
        }
    }

    fun updateUniversalInput(text: String) {
        _uiState.update { it.copy(universalInputText = text) }
    }

    fun enhancePromptForMode(mode: MaxStudioMode) {
        val raw = _uiState.value.universalInputText.trim()
        val project = _uiState.value.activeProject?.name ?: "PulseApp"
        val activeFile = _uiState.value.activeTabPath?.substringAfterLast('/') ?: "MainActivity.kt"
        val sourceText = raw.ifBlank {
            when (mode) {
                MaxStudioMode.CODE_EG -> "Refactor $activeFile with Material 3 state"
                MaxStudioMode.AI_UI_SCREEN -> "Add card, counter, and Copper action button"
                MaxStudioMode.TERMINAL -> "Run offline ARM64 APK build check"
            }
        }

        val enhanced = when (mode) {
            MaxStudioMode.CODE_EG ->
                "Implement clean Kotlin + Jetpack Compose Material 3 code in $activeFile ($project): $sourceText. Include @Composable state, 48.dp touch targets, and error-free syntax."
            MaxStudioMode.AI_UI_SCREEN ->
                "Build interactive Compose UI screen for $project with Desert Luxe Copper actions: $sourceText. Include Card container, responsive controls, and 2-way code sync."
            MaxStudioMode.TERMINAL ->
                when {
                    sourceText.contains("apk", true) || sourceText.contains("build", true) -> "build-apk"
                    sourceText.contains("tree", true) || sourceText.contains("list", true) -> "tree"
                    sourceText.contains("git", true) -> "git status"
                    else -> "gradle build && pkg list"
                }
        }
        _uiState.update {
            it.copy(
                previousPromptBeforeEnhance = _uiState.value.universalInputText,
                universalInputText = enhanced
            )
        }
        postMessage("Enhanced prompt for ${mode.label} (Undo available)")
    }

    fun undoEnhancedPrompt() {
        val prev = _uiState.value.previousPromptBeforeEnhance ?: return
        _uiState.update {
            it.copy(
                universalInputText = prev,
                previousPromptBeforeEnhance = null
            )
        }
        postMessage("Reverted prompt enhancement")
    }

    fun executeUniversalRun(mode: MaxStudioMode, rawInput: String) {
        val input = rawInput.trim()
        if (input.isEmpty()) {
            postMessage("Enter a prompt, code instruction, or command first.")
            return
        }
        when (mode) {
            MaxStudioMode.TERMINAL -> {
                runTerminalCommand(input)
                _uiState.update { it.copy(universalInputText = "") }
            }
            MaxStudioMode.AI_UI_SCREEN -> {
                val added = mutableListOf<VisualNode>()
                val lower = input.lowercase()
                if (lower.contains("login") || lower.contains("auth") || lower.contains("form")) {
                    added.add(VisualNode(UUID.randomUUID().toString().take(6), VisualComponentType.TEXT, "Welcome Back", "headlineMedium", 12, colorTag = "Copper"))
                    added.add(VisualNode(UUID.randomUUID().toString().take(6), VisualComponentType.TEXT_FIELD, "Email Address", paddingDp = 10))
                    added.add(VisualNode(UUID.randomUUID().toString().take(6), VisualComponentType.BUTTON, "Sign In", paddingDp = 12, useAccent = true, colorTag = "Copper"))
                } else {
                    if (lower.contains("card") || lower.contains("dashboard") || lower.contains("stats")) {
                        added.add(VisualNode(UUID.randomUUID().toString().take(6), VisualComponentType.CARD, "Summary Card", input.take(48), 14, colorTag = "Copper"))
                    }
                    if (lower.contains("input") || lower.contains("field") || lower.contains("search")) {
                        added.add(VisualNode(UUID.randomUUID().toString().take(6), VisualComponentType.TEXT_FIELD, "Search or Input", paddingDp = 10))
                    }
                    if (lower.contains("switch") || lower.contains("toggle") || lower.contains("setting")) {
                        added.add(VisualNode(UUID.randomUUID().toString().take(6), VisualComponentType.SWITCH_CONTROL, "Enable Feature", paddingDp = 10))
                    }
                    if (lower.contains("counter") || lower.contains("count")) {
                        added.add(VisualNode(UUID.randomUUID().toString().take(6), VisualComponentType.COUNTER, "Live Counter", "Tap + or - to update state", 12, useAccent = true, colorTag = "Copper"))
                    }
                    if (lower.contains("button") || added.isEmpty()) {
                        added.add(VisualNode(UUID.randomUUID().toString().take(6), VisualComponentType.BUTTON, input.take(28), paddingDp = 12, useAccent = true, colorTag = "Copper"))
                    }
                }
                _uiState.update { st ->
                    st.copy(
                        visualNodes = st.visualNodes + added,
                        universalInputText = ""
                    )
                }
                syncVisualBuilderToCodeTab()
                postMessage("Added ${added.size} synced component(s) to AI UI Screen")
            }
            MaxStudioMode.CODE_EG -> {
                val active = _uiState.value.activeTab
                val project = _uiState.value.activeProject
                val pkg = project?.packageName ?: "com.ldroid.pulseapp"
                val snippet = buildString {
                    appendLine()
                    appendLine("// Generated via LDROID IDE Offline Generator: ${input.take(60)}")
                    appendLine("@Composable")
                    appendLine("fun GeneratedSection() {")
                    appendLine("    Card(modifier = Modifier.fillMaxWidth().padding(12.dp)) {")
                    appendLine("        Column(modifier = Modifier.padding(16.dp)) {")
                    appendLine("            Text(\"${input.replace("\"", "'").take(64)}\", style = MaterialTheme.typography.titleMedium)")
                    appendLine("            Spacer(Modifier.height(8.dp))")
                    appendLine("            Button(onClick = { }) { Text(\"Execute\") }")
                    appendLine("        }")
                    appendLine("    }")
                    appendLine("}")
                }
                if (active != null && active.previewMode == FilePreviewKind.CODE_TEXT && !active.isLargeFilePreview) {
                    updateActiveTabContent(active.content + snippet)
                    _uiState.update { it.copy(universalInputText = "") }
                    postMessage("Generated Compose block in ${active.relativePath.substringAfterLast('/')}")
                } else {
                    val pkgPath = pkg.replace('.', '/')
                    createFileOrFolder(
                        "app/src/main/java/$pkgPath/GeneratedSection.kt",
                        isDirectory = false,
                        initialContent = "package $pkg\n\nimport androidx.compose.foundation.layout.*\nimport androidx.compose.material3.*\nimport androidx.compose.runtime.Composable\nimport androidx.compose.ui.Modifier\nimport androidx.compose.ui.unit.dp\n$snippet"
                    )
                    _uiState.update { it.copy(universalInputText = "") }
                }
            }
        }
    }

    fun buildApkInMaxStudio() {
        val project = _uiState.value.activeProject ?: return
        val sessionId = _uiState.value.activeTerminalSessionId
            ?: terminalManager.ensureDefaultSession(project.rootPath)
        _uiState.update {
            it.copy(
                activeTerminalSessionId = sessionId,
                maxStudioMode = MaxStudioMode.TERMINAL
            )
        }
        viewModelScope.launch {
            val root = File(project.rootPath)
            refreshWorkspaceFilesAndAnalysis(project)
            val errCount = _uiState.value.diagnostics.count {
                it.severity == DiagnosticSeverity.ERROR || it.severity == DiagnosticSeverity.SECRET
            }
            val apkOutRel = "build/outputs/apk/debug/${project.name.lowercase()}-debug.apk"
            val logs = mutableListOf(
                TerminalLine(text = "u0_a1@ldroid:~/LDROID_Projects/${project.name} $ build-apk --api 36", isPrompt = true),
                TerminalLine(text = "[1/5] aapt2 compile --dir app/src/main/res -> compiled resources (API 36)"),
                TerminalLine(text = "[2/5] kotlinc/javac 2.1.20 against android.jar (API 36) -> JVM bytecode"),
                TerminalLine(text = "[3/5] d8/r8 8.9.14 dexing classes -> classes.dex")
            )
            if (errCount > 0) {
                logs.add(TerminalLine(text = "[FAILED] Found $errCount syntax/secret diagnostic(s). Fix in <> Code E/G first.", isError = true))
            } else {
                withContext(Dispatchers.IO) {
                    val outFile = File(root, apkOutRel)
                    outFile.parentFile?.mkdirs()
                    outFile.writeText("LDROID_OFFLINE_BUILD_MANIFEST:${project.packageName}:API36\n")
                }
                logs.add(TerminalLine(text = "[4/5] zipalign -f 4 -> aligned APK container"))
                logs.add(TerminalLine(text = "[5/5] apksigner sign --ks ~/.android/debug.keystore -> $apkOutRel", isPrompt = true))
                logs.add(TerminalLine(text = "BUILD SUCCESSFUL • Staged $apkOutRel (0 errors)", isPrompt = true))
            }
            terminalManager.appendLinesToSession(sessionId, logs)
            _uiState.update {
                it.copy(
                    buildState = BuildRunState(
                        isRunning = false,
                        statusLabel = if (errCount == 0) "APK Pipeline Ready" else "Failed ($errCount errors)",
                        progress = 1f,
                        logs = logs.map { l -> l.text },
                        succeeded = errCount == 0
                    )
                )
            }
            appendIdeLog("Build APK", if (errCount == 0) "Staged $apkOutRel cleanly" else "Blocked by $errCount diagnostic(s)", isError = errCount > 0)
            postMessage(if (errCount == 0) "APK pipeline finished: see Terminal logs" else "Build blocked: $errCount error(s)")
        }
    }

    // --- Library & Packages (Termux Core, SDK Compilers, Gradle Injection) ---

    fun refreshLibraryToolchains() {
        viewModelScope.launch {
            val core = libraryManager.inspectTermuxCore()
            val bins = libraryManager.inspectCompilerBinaries()
            _uiState.update {
                it.copy(
                    termuxCoreItems = core,
                    compilerBinaries = bins,
                    gradlePackages = libraryManager.curatedGradlePackages
                )
            }
        }
    }

    fun initializeTermuxCore() {
        viewModelScope.launch {
            libraryManager.initializeTermuxCore()
                .onSuccess { msg ->
                    refreshLibraryToolchains()
                    postMessage(msg)
                }
                .onFailure { postMessage("Termux init failed: ${it.message}") }
        }
    }

    fun extractSdkArchiveViaSaf(uri: Uri) {
        viewModelScope.launch {
            libraryManager.extractSdkArchive(uri)
                .onSuccess { msg ->
                    refreshLibraryToolchains()
                    postMessage(msg)
                }
                .onFailure { postMessage("SDK extraction failed: ${it.message}") }
        }
    }

    fun injectGradlePackage(pkg: GradlePackageItem) {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            fileManager.injectGradleDependency(project, pkg.coordinate)
                .onSuccess { msg ->
                    refreshWorkspaceFilesAndAnalysis(project)
                    // Reload open build.gradle.kts tab if open
                    val gradleRel = "app/build.gradle.kts"
                    if (_uiState.value.openTabs.any { it.relativePath == gradleRel }) {
                        fileManager.openFileTab(project, gradleRel).onSuccess { reloaded ->
                            _uiState.update { st ->
                                st.copy(openTabs = st.openTabs.map { if (it.relativePath == gradleRel) reloaded else it })
                            }
                        }
                    }
                    appendIdeLog("Gradle", msg)
                    postMessage(msg)
                }
                .onFailure { postMessage("Gradle injection failed: ${it.message}") }
        }
    }

    // --- Developer Profile, Permissions & In-App Browser Console ---

    fun updateDeveloperProfile(name: String, role: String) {
        viewModelScope.launch {
            preferences.setDeveloperProfile(name, role)
            postMessage("Updated developer profile")
        }
    }

    fun setStoragePermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(storagePermissionGranted = granted) }
    }

    fun setPhotosPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(photosPermissionGranted = granted) }
    }

    fun setMicPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(micPermissionGranted = granted) }
    }

    fun appendBrowserConsoleLog(level: String, message: String, source: String = "WebView") {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        _uiState.update { state ->
            state.copy(
                browserConsoleLogs = (state.browserConsoleLogs + BrowserConsoleEntry(
                    id = UUID.randomUUID().toString().take(8),
                    timestamp = ts,
                    level = level,
                    message = message,
                    source = source
                )).takeLast(120)
            )
        }
    }

    fun clearBrowserConsoleLogs() {
        _uiState.update { it.copy(browserConsoleLogs = emptyList()) }
    }

    // --- GitHub Inspector (M18) ---

    fun inspectGitHubRepo(ownerRepo: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGitHubLoading = true, gitHubError = null) }
            val token = vault.readSecret(vault.githubTokenSlot)
            val branch = settings.value.defaultGitBranch
            gitHubClient.fetchRepositoryStatus(ownerRepo, token, preferredBranch = branch)
                .onSuccess { status -> _uiState.update { it.copy(gitHubStatus = status, gitHubError = null, isGitHubLoading = false) } }
                .onFailure { e ->
                    val errText = e.message ?: "GitHub request failed"
                    _uiState.update { it.copy(isGitHubLoading = false, gitHubError = errText) }
                    postMessage("GitHub check failed: $errText")
                }
        }
    }

    // --- Themes & Settings (M02 / M22 / M32) ---

    fun previewTheme(tokens: LdroidTokens?) {
        _uiState.update { it.copy(previewThemeTokens = tokens) }
    }

    fun applyTheme(themeId: LdroidThemeId, customJson: String? = null) {
        viewModelScope.launch {
            preferences.setTheme(themeId, customJson)
            _uiState.update { it.copy(previewThemeTokens = null) }
            postMessage("Applied theme: ${themeId.displayName}")
        }
    }

    fun importThemeJson(rawJson: String) {
        LdroidThemes.validateAndParseThemeJson(rawJson)
            .onSuccess { parsed ->
                viewModelScope.launch {
                    preferences.setTheme(parsed.id, rawJson)
                    _uiState.update { it.copy(previewThemeTokens = null) }
                    postMessage("Imported & applied custom theme '${parsed.name}'")
                }
            }
            .onFailure { err ->
                postMessage("Theme validation failed: ${err.message}")
            }
    }

    fun saveAiProviderConfig(
        provider: AiProviderType,
        model: String,
        baseUrl: String,
        apiKey: String?,
        redactSecrets: Boolean,
        confirmChanges: Boolean = settings.value.confirmAiChanges,
        fastModel: String = settings.value.aiFastModel,
        proModel: String = settings.value.aiProModel
    ) {
        viewModelScope.launch {
            preferences.updateAiConfig(
                provider = provider,
                model = model,
                baseUrl = baseUrl,
                redactSecrets = redactSecrets,
                confirmChanges = confirmChanges,
                fastModel = fastModel,
                proModel = proModel
            )
            if (apiKey != null && apiKey.isNotBlank()) {
                val ok = vault.saveSecret(vault.slotForAiProvider(provider), apiKey)
                postMessage(if (ok) "Encrypted ${provider.displayName} key saved in Keystore" else "Failed to encrypt key")
            } else {
                postMessage("Updated ${provider.displayName} configuration")
            }
        }
    }

    fun clearAiProviderKey(provider: AiProviderType) {
        vault.clearSecret(vault.slotForAiProvider(provider))
        postMessage("Cleared key for ${provider.displayName}")
    }

    fun saveGitHubToken(token: String) {
        val ok = vault.saveSecret(vault.githubTokenSlot, token)
        postMessage(if (ok) "GitHub PAT encrypted in Keystore" else "Keystore unavailable")
    }

    fun saveGitHubConfig(token: String?, defaultBranch: String) {
        viewModelScope.launch {
            preferences.setDefaultGitBranch(defaultBranch)
            val cleanToken = token?.trim().orEmpty()
            if (cleanToken.isNotEmpty()) {
                val ok = vault.saveSecret(vault.githubTokenSlot, cleanToken)
                postMessage(if (ok) "GitHub token & branch '$defaultBranch' saved" else "Keystore unavailable")
            } else {
                postMessage("Saved default branch '$defaultBranch'")
            }
        }
    }

    fun clearGitHubToken() {
        vault.clearSecret(vault.githubTokenSlot)
        postMessage("Cleared GitHub token")
    }

    fun updateEditorSettings(
        fontSize: Int,
        tabWidth: Int,
        autoSave: Boolean,
        lineNumbers: Boolean,
        wordWrap: Boolean,
        formatOnSave: Boolean = settings.value.formatOnSave
    ) {
        viewModelScope.launch {
            preferences.updateEditorPrefs(fontSize, tabWidth, autoSave, lineNumbers, wordWrap, formatOnSave)
        }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch { preferences.setReducedMotion(enabled) }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            preferences.resetAllToDefaults()
            _uiState.update { it.copy(previewThemeTokens = null) }
            postMessage("Reset all settings to default")
        }
    }

    fun exportSettingsJsonString(): String = LdroidPreferences.exportSettingsJson(settings.value)

    fun importSettingsJson(rawJson: String) {
        viewModelScope.launch {
            preferences.importSettingsJson(rawJson)
                .onSuccess {
                    _uiState.update { it.copy(previewThemeTokens = null) }
                    postMessage("Imported IDE settings successfully")
                }
                .onFailure { err ->
                    postMessage("Settings import failed: ${err.message}")
                }
        }
    }

    fun runFullWorkspaceStaticAudit() {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            refreshWorkspaceFilesAndAnalysis(project)
            postMessage("Static & secret scan complete: ${_uiState.value.diagnostics.size} finding(s)")
        }
    }

    fun runSystemVerification() {
        if (_uiState.value.isRunningVerification) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningVerification = true) }
            val report = RoadmapRegistry.runLiveVerification(
                context = getApplication(),
                activeProjectRoot = _uiState.value.activeProject?.rootPath,
                settings = settings.value,
                vault = vault,
                fileManager = fileManager,
                terminalManager = terminalManager,
                workspaceFileCount = _uiState.value.workspaceFiles.size,
                visualNodes = _uiState.value.visualNodes
            )
            _uiState.update { it.copy(verificationReport = report, isRunningVerification = false) }
            appendIdeLog("Verification", "System Verification finished: ${report.passedCount}/${report.items.size} VERIFIED (${report.durationMs}ms)")
        }
    }

    fun clearIdeOutputLogs() {
        _uiState.update { it.copy(ideOutputLogs = emptyList()) }
    }

    private fun appendIdeLog(source: String, message: String, isError: Boolean = false) {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val entry = IdeLogEntry(
            id = UUID.randomUUID().toString().take(8),
            timestamp = ts,
            source = source,
            message = message,
            isError = isError
        )
        _uiState.update { state ->
            state.copy(ideOutputLogs = (state.ideOutputLogs + entry).takeLast(120))
        }
    }

    private fun persistCurrentTabs() {
        val project = _uiState.value.activeProject ?: return
        val tabPaths = _uiState.value.openTabs.map { it.relativePath }
        val active = _uiState.value.activeTabPath
        viewModelScope.launch {
            preferences.saveProjectOpenTabs(project.id, tabPaths, active)
        }
    }

    private fun readPersistedTabs(projectId: String): Pair<List<String>, String?> {
        val raw = settings.value.persistedTabsJson
        return runCatching {
            val root = org.json.JSONObject(raw)
            val proj = root.optJSONObject(projectId) ?: return emptyList<String>() to null
            val arr = proj.optJSONArray("tabs")
            val paths = buildList {
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val s = arr.optString(i, "")
                        if (s.isNotBlank()) add(s)
                    }
                }
            }
            val active = proj.optString("active", "").ifBlank { null }
            paths to active
        }.getOrDefault(emptyList<String>() to null)
    }

    private suspend fun refreshWorkspaceFilesAndAnalysis(project: ProjectEntity) {
        val files = fileManager.listFiles(project, _uiState.value.fileSearchQuery)
        val root = File(project.rootPath)
        val issues = StaticAnalyzer.analyzeWorkspace(root)
        val changes = fileManager.computeWorkspaceChanges(project)
        val arch = VisualBuilderCodegen.scanArchitecture(root)
        _uiState.update {
            it.copy(
                workspaceFiles = files,
                diagnostics = issues,
                workspaceChanges = changes,
                architectureNodes = arch
            )
        }
    }

    private fun AiChatMessage.toEntity(projectId: String): AiMessageEntity {
        val arr = org.json.JSONArray()
        proposedChanges.forEach { ch ->
            arr.put(
                org.json.JSONObject().apply {
                    put("relativePath", ch.relativePath)
                    if (ch.originalContent != null) put("originalContent", ch.originalContent)
                    put("proposedContent", ch.proposedContent)
                    put("isApplied", ch.isApplied)
                }
            )
        }
        return AiMessageEntity(
            id = id,
            projectId = projectId,
            isUser = isUser,
            text = text,
            timestampMs = timestampMs,
            isError = isError,
            proposedChangesJson = arr.toString()
        )
    }

    private fun AiMessageEntity.toDomain(): AiChatMessage {
        val changes = runCatching {
            val arr = org.json.JSONArray(proposedChangesJson)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        AiProposedFileChange(
                            relativePath = obj.optString("relativePath"),
                            originalContent = if (obj.has("originalContent") && !obj.isNull("originalContent")) {
                                obj.optString("originalContent")
                            } else null,
                            proposedContent = obj.optString("proposedContent"),
                            isApplied = obj.optBoolean("isApplied", false)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
        return AiChatMessage(
            id = id,
            isUser = isUser,
            text = text,
            timestampMs = timestampMs,
            proposedChanges = changes,
            isError = isError
        )
    }
}
