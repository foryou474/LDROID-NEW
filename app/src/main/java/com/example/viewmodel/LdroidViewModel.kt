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
import com.example.data.github.GitHubApiClient
import com.example.data.github.GitHubRepoStatus
import com.example.data.local.FileOpHistoryEntity
import com.example.data.local.LdroidDatabase
import com.example.data.local.ProjectEntity
import com.example.data.local.WorkspaceSnapshotEntity
import com.example.data.preferences.LdroidPreferences
import com.example.data.preferences.LdroidSettingsState
import com.example.data.security.SecureCredentialVault
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
import com.example.model.CodePreviewState
import com.example.model.DeveloperLabSection
import com.example.model.DeviceProfile
import com.example.model.DiagnosticIssue
import com.example.model.FileChangeSummary
import com.example.model.FilePreviewKind
import com.example.model.LineEnding
import com.example.model.OpenEditorTab
import com.example.model.PrimaryDestination
import com.example.model.ProjectTemplate
import com.example.model.TextEncodingOption
import com.example.model.VisualComponentType
import com.example.model.VisualNode
import com.example.model.WorkspaceFileItem
import com.example.ui.theme.LdroidThemeId
import com.example.ui.theme.LdroidThemes
import com.example.ui.theme.LdroidTokens
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class LdroidUiState(
    val currentTab: PrimaryDestination = PrimaryDestination.HOME,
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
    // Terminal & Developer Lab
    val activeTerminalSessionId: String? = null,
    val logcatLines: List<String> = emptyList(),
    val toolchainProbe: ToolchainProbeReport? = null,
    val apkReport: ApkAnalysisReport? = null,
    val isApkAnalyzing: Boolean = false,
    // AI Studio
    val aiMessages: List<AiChatMessage> = emptyList(),
    val isAiLoading: Boolean = false,
    // GitHub
    val gitHubStatus: GitHubRepoStatus? = null,
    val isGitHubLoading: Boolean = false,
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
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LdroidThemes.CyberpunkNeon)

    init {
        viewModelScope.launch {
            fileManager.ensureStarterWorkspaceIfEmpty()
            refreshDiagnosticsProbe()
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
    }

    private suspend fun onProjectActivated(project: ProjectEntity) {
        val termId = terminalManager.ensureDefaultSession(project.rootPath)
        terminalManager.updateWorkingDirectory(termId, project.rootPath)
        _uiState.update {
            it.copy(
                activeTerminalSessionId = termId,
                openTabs = emptyList(),
                activeTabPath = null,
                selectedFilePaths = emptySet()
            )
        }
        refreshWorkspaceFilesAndAnalysis(project)
        val initialFile = project.lastOpenedFile
            ?: _uiState.value.workspaceFiles.firstOrNull { !it.isDirectory }?.relativePath
        if (initialFile != null) {
            openFileInEditor(initialFile)
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

    fun createProject(name: String, packageName: String, template: ProjectTemplate) {
        if (name.isBlank()) {
            postMessage("Project name cannot be empty.")
            return
        }
        viewModelScope.launch {
            val created = fileManager.createProjectFromTemplate(name, packageName, template)
            preferences.setActiveProjectId(created.id)
            _uiState.update { it.copy(activeProject = created, currentTab = PrimaryDestination.EDITOR) }
            onProjectActivated(created)
            postMessage("Created ${created.name} (${template.title})")
        }
    }

    fun switchProject(project: ProjectEntity) {
        viewModelScope.launch {
            preferences.setActiveProjectId(project.id)
            _uiState.update { it.copy(activeProject = project, currentTab = PrimaryDestination.EDITOR) }
            onProjectActivated(project)
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
            fileManager.importFilesFromUris(project, uris)
                .onSuccess { imported ->
                    refreshWorkspaceFilesAndAnalysis(project)
                    imported.firstOrNull()?.let { openFileInEditor(it) }
                    postMessage("Imported ${imported.size} file(s) into imported/")
                }
                .onFailure { postMessage("Import failed: ${it.message}") }
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
                            currentTab = PrimaryDestination.PROJECTS
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

    fun createFileOrFolder(relativePath: String, isDirectory: Boolean) {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            fileManager.createFileOrFolder(project, relativePath, isDirectory)
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
                    refreshWorkspaceFilesAndAnalysis(project)
                    postMessage("Deleted $count item(s)")
                }
                .onFailure { postMessage("Delete failed: ${it.message}") }
        }
    }

    fun openFileInEditor(relativePath: String, jumpToEditorTab: Boolean = false) {
        val project = _uiState.value.activeProject ?: return
        val alreadyOpen = _uiState.value.openTabs.firstOrNull { it.relativePath == relativePath }
        if (alreadyOpen != null) {
            _uiState.update {
                it.copy(
                    activeTabPath = relativePath,
                    currentTab = if (jumpToEditorTab) PrimaryDestination.EDITOR else it.currentTab
                )
            }
            updateCodeLivePreview(alreadyOpen.relativePath, alreadyOpen.content)
            return
        }

        viewModelScope.launch {
            fileManager.openFileTab(project, relativePath)
                .onSuccess { tab ->
                    _uiState.update { state ->
                        state.copy(
                            openTabs = state.openTabs + tab,
                            activeTabPath = tab.relativePath,
                            currentTab = if (jumpToEditorTab) PrimaryDestination.EDITOR else state.currentTab
                        )
                    }
                    if (tab.previewMode == FilePreviewKind.ZIP_ARCHIVE) {
                        fileManager.inspectZipEntries(project, relativePath).onSuccess { entries ->
                            _uiState.update { it.copy(zipInspectionEntries = entries) }
                        }
                    } else if (tab.previewMode == FilePreviewKind.CODE_TEXT) {
                        updateCodeLivePreview(tab.relativePath, tab.content)
                    }
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

    private fun onTabContentMutated(tab: OpenEditorTab) {
        updateCodeLivePreview(tab.relativePath, tab.content)
        val fileIssues = StaticAnalyzer.analyzeFile(tab.relativePath, tab.content)
        _uiState.update { state ->
            val others = state.diagnostics.filterNot { it.filePath == tab.relativePath }
            state.copy(diagnostics = fileIssues + others)
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
            fileManager.saveFileTab(project, currentTab).onSuccess { saved ->
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
            fileManager.saveFileTab(project, tab)
                .onSuccess { saved ->
                    _uiState.update { state ->
                        state.copy(
                            openTabs = state.openTabs.map {
                                if (it.relativePath == saved.relativePath) saved else it
                            }
                        )
                    }
                    refreshWorkspaceFilesAndAnalysis(project)
                    postMessage("Saved ${saved.relativePath}")
                }
                .onFailure { postMessage("Save failed: ${it.message}") }
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

    // --- Visual Builder (M08 / M15) ---

    fun addVisualNode(type: VisualComponentType) {
        val node = VisualNode(
            id = UUID.randomUUID().toString().take(6),
            type = type,
            label = when (type) {
                VisualComponentType.TEXT -> "Heading Label"
                VisualComponentType.BUTTON -> "Primary Action"
                VisualComponentType.CARD -> "Feature Card"
                VisualComponentType.TEXT_FIELD -> "Input Field"
                VisualComponentType.SWITCH_CONTROL -> "Toggle Setting"
                else -> type.displayName
            },
            secondaryText = if (type == VisualComponentType.CARD) "Generated in Visual Builder" else "",
            useAccent = type == VisualComponentType.BUTTON
        )
        _uiState.update { it.copy(visualNodes = it.visualNodes + node) }
    }

    fun updateVisualNodeLabel(id: String, newLabel: String) {
        _uiState.update { state ->
            state.copy(
                visualNodes = state.visualNodes.map { if (it.id == id) it.copy(label = newLabel) else it }
            )
        }
    }

    fun removeVisualNode(id: String) {
        _uiState.update { state ->
            state.copy(visualNodes = state.visualNodes.filterNot { it.id == id })
        }
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
        val sessionId = _uiState.value.activeTerminalSessionId
            ?: terminalManager.ensureDefaultSession(_uiState.value.activeProject?.rootPath ?: getApplication<Application>().filesDir.absolutePath)
        _uiState.update { it.copy(activeTerminalSessionId = sessionId) }
        viewModelScope.launch {
            terminalManager.runCommand(sessionId, command)
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

    fun sendAiPrompt(prompt: String, attachActiveFile: Boolean) {
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
            val result: Result<AiGatewayResponse> = aiGateway.sendPrompt(
                provider = currentSettings.aiProvider,
                apiKey = key,
                model = currentSettings.aiModel,
                baseUrl = currentSettings.aiBaseUrl,
                userPrompt = cleanPrompt,
                activeFilePath = if (attachActiveFile) activeTab?.relativePath else null,
                activeFileContent = if (attachActiveFile) activeTab?.content else null,
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
                _uiState.update { it.copy(aiMessages = it.aiMessages + reply, isAiLoading = false) }
            }.onFailure { err ->
                val reply = AiChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = err.message ?: "AI request failed",
                    timestampMs = System.currentTimeMillis(),
                    isError = true
                )
                _uiState.update { it.copy(aiMessages = it.aiMessages + reply, isAiLoading = false) }
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
                _uiState.update { state ->
                    val updatedMessages = state.aiMessages.map { msg ->
                        if (msg.id == messageId) {
                            msg.copy(
                                proposedChanges = msg.proposedChanges.map { p ->
                                    if (p.relativePath == change.relativePath) p.copy(isApplied = true) else p
                                }
                            )
                        } else msg
                    }
                    val updatedTabs = state.openTabs.map { tab ->
                        if (tab.relativePath == change.relativePath) {
                            tab.copy(content = change.proposedContent, savedContent = change.proposedContent)
                        } else tab
                    }
                    state.copy(aiMessages = updatedMessages, openTabs = updatedTabs)
                }
                refreshWorkspaceFilesAndAnalysis(project)
                postMessage("Applied approved patch to ${change.relativePath}")
            }.onFailure {
                postMessage("Patch apply blocked: ${it.message}")
            }
        }
    }

    fun explainLogcatWithAi() {
        val sample = _uiState.value.logcatLines.takeLast(35).joinToString("\n")
        _uiState.update { it.copy(currentTab = PrimaryDestination.AI_STUDIO, activeLabSection = null) }
        sendAiPrompt("Analyze these Android Logcat entries and explain any warnings, errors, or actionable suggestions:\n\n$sample", attachActiveFile = false)
    }

    // --- GitHub Inspector (M18) ---

    fun inspectGitHubRepo(ownerRepo: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGitHubLoading = true) }
            val token = vault.readSecret(vault.githubTokenSlot)
            gitHubClient.fetchRepositoryStatus(ownerRepo, token)
                .onSuccess { status -> _uiState.update { it.copy(gitHubStatus = status, isGitHubLoading = false) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isGitHubLoading = false) }
                    postMessage("GitHub check failed: ${e.message}")
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

    fun saveAiProviderConfig(provider: AiProviderType, model: String, baseUrl: String, apiKey: String?, redactSecrets: Boolean) {
        viewModelScope.launch {
            preferences.updateAiConfig(provider, model, baseUrl, redactSecrets)
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

    fun updateEditorSettings(fontSize: Int, tabWidth: Int, autoSave: Boolean, lineNumbers: Boolean, wordWrap: Boolean) {
        viewModelScope.launch {
            preferences.updateEditorPrefs(fontSize, tabWidth, autoSave, lineNumbers, wordWrap)
        }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch { preferences.setReducedMotion(enabled) }
    }

    fun runFullWorkspaceStaticAudit() {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            refreshWorkspaceFilesAndAnalysis(project)
            postMessage("Static & secret scan complete: ${_uiState.value.diagnostics.size} finding(s)")
        }
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
}
