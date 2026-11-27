package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.LdroidSettingsState
import com.example.model.DiagnosticSeverity
import com.example.model.ProjectTemplate
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

/**
 * Group B of the Left Sidebar Navigation Drawer:
 * 4. Workspace Diagnostics (Relocated from Home)
 * 5. Quick Workspace (Relocated from Home)
 * 6. Workspace Lab (Project switcher, templates, ZIP export/backup, cache cleaner)
 * 7. Library & Packages (Gradle dependency manager & offline .jar/.aar importer)
 * 8. GitHub (PAT login, clone/pull, stage/commit/push, branch switcher, commits)
 * 9. Integrations (Firebase, Supabase, REST API client/tester, Figma UI importer)
 */
@Composable
fun DrawerCategoryDiagnosticsContent(
    uiState: LdroidUiState,
    viewModel: LdroidViewModel,
    onJumpToDiagnostic: (String, Int) -> Unit
) {
    val tokens = LocalLdroidTokens.current
    val issues = uiState.diagnostics
    val errorCount = issues.count { it.severity == DiagnosticSeverity.ERROR || it.severity == DiagnosticSeverity.SECRET }
    val warnCount = issues.count { it.severity == DiagnosticSeverity.WARNING }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill("$errorCount Errors", if (errorCount == 0) tokens.success else tokens.error)
                StatusPill("$warnCount Warn", tokens.warning)
            }
            if (errorCount > 0) {
                Button(
                    onClick = { viewModel.autoFixAllDiagnostics() },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(34.dp).testTag("diag_autofix_all_btn")
                ) {
                    Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("✨ Auto-Fix", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (issues.isEmpty()) {
            Surface(
                color = tokens.success.copy(alpha = 0.10f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, tokens.success.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "0 errors & 0 secrets detected across active workspace.",
                    color = tokens.success,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        } else {
            issues.take(8).forEach { issue ->
                val color = when (issue.severity) {
                    DiagnosticSeverity.ERROR, DiagnosticSeverity.SECRET -> tokens.error
                    DiagnosticSeverity.WARNING -> tokens.warning
                    DiagnosticSeverity.INFO -> tokens.info
                }
                Surface(
                    color = tokens.surface1,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = "${issue.filePath}:${issue.line}",
                            color = tokens.primaryAccent,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(issue.message, color = tokens.textPrimary, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.autoFixDiagnostic(issue) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) { Text("✨ Auto-Fix", fontSize = 10.sp) }
                            OutlinedButton(
                                onClick = { onJumpToDiagnostic(issue.filePath, issue.line) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) { Text("Jump to Line", fontSize = 10.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerCategoryQuickWorkspaceContent(
    uiState: LdroidUiState,
    settings: LdroidSettingsState,
    viewModel: LdroidViewModel,
    onOpenWorkspaceFile: (String) -> Unit
) {
    val tokens = LocalLdroidTokens.current
    val standardShortcuts = listOf(
        ".github/workflows/android_ci.yml",
        "app/build.gradle.kts",
        "app/src/main/AndroidManifest.xml",
        "app/src/main/res/values/colors.xml",
        "app/src/main/res/values/strings.xml"
    )
    val workspaceFiles = uiState.workspaceFiles.filter { !it.isDirectory }
    val allTargets = (settings.pinnedQuickFiles + standardShortcuts).distinct()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Instant Shortcuts & User-Pinned Files",
            color = tokens.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        allTargets.forEach { rel ->
            val matchedPath = workspaceFiles.firstOrNull {
                it.relativePath == rel || it.name == rel.substringAfterLast('/')
            }?.relativePath ?: rel
            val isPinned = rel in settings.pinnedQuickFiles || matchedPath in settings.pinnedQuickFiles
            Surface(
                color = tokens.surface1,
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(1.dp, tokens.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenWorkspaceFile(matchedPath) }
                    .testTag("quick_ws_${rel.substringAfterLast('/')}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = tokens.primaryAccent, modifier = Modifier.size(15.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                rel.substringAfterLast('/'),
                                color = tokens.textPrimary,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                matchedPath,
                                color = tokens.textMuted,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.togglePinnedQuickFile(matchedPath) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = if (isPinned) "Unpin $matchedPath" else "Pin $matchedPath",
                            tint = if (isPinned) tokens.primaryAccent else tokens.textMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerCategoryWorkspaceLabContent(
    uiState: LdroidUiState,
    viewModel: LdroidViewModel,
    onCloseDrawer: () -> Unit,
    onExportZipClick: (String) -> Unit
) {
    val tokens = LocalLdroidTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Project Switcher (${uiState.projects.size})", color = tokens.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        uiState.projects.forEach { proj ->
            val isActive = uiState.activeProject?.id == proj.id
            Surface(
                color = if (isActive) tokens.primaryAccent.copy(alpha = 0.14f) else tokens.surface1,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (isActive) tokens.primaryAccent else tokens.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.switchProject(proj, openMaxStudio = true)
                        onCloseDrawer()
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(proj.name, color = tokens.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(proj.packageName, color = tokens.textMuted, fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily)
                    }
                    if (isActive) StatusPill("Active", tokens.success)
                }
            }
        }

        Text("Starter Templates", color = tokens.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ProjectTemplate.entries.forEach { tpl ->
                FilterChip(
                    selected = false,
                    onClick = {
                        val suffix = (uiState.projects.size + 1)
                        val name = "${tpl.badge.replace(" ", "").replace("/", "")}App$suffix"
                        val pkg = "com.ldroid.${name.lowercase()}"
                        viewModel.createProject(name, pkg, tpl, openMaxStudio = true)
                        onCloseDrawer()
                    },
                    label = { Text("+ ${tpl.badge}", fontSize = 10.sp) }
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = {
                    val activeName = uiState.activeProject?.name ?: "ldroid_workspace"
                    onExportZipClick("${activeName}_backup.zip")
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.weight(1f).height(40.dp).testTag("drawer_export_zip_btn")
            ) {
                Icon(Icons.Filled.FolderZip, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("ZIP Backup", fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = { viewModel.cleanWorkspaceCaches() },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.weight(1f).height(40.dp).testTag("drawer_clean_cache_btn")
            ) {
                Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Clean Cache", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun DrawerCategoryLibraryPackagesContent(
    uiState: LdroidUiState,
    viewModel: LdroidViewModel,
    onExtractSdkZipClick: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    var jarName by remember { mutableStateOf("custom-offline-lib-1.0.aar") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LibraryPackagesPanel(
            uiState = uiState,
            viewModel = viewModel,
            onExtractSdkZipClick = onExtractSdkZipClick
        )

        // Offline .jar / .aar Importer
        Surface(
            color = tokens.surface1,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, tokens.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Offline .jar / .aar Importer", color = tokens.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = jarName,
                        onValueChange = { jarName = it },
                        placeholder = { Text("library.jar or sdk.aar", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { viewModel.importLocalJarOrAar(jarName) },
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent),
                        modifier = Modifier.height(42.dp).testTag("import_local_jar_btn")
                    ) { Text("Import", fontSize = 11.sp) }
                }
                if (uiState.installedLocalPackages.isNotEmpty()) {
                    Text(
                        "Installed: ${uiState.installedLocalPackages.joinToString()}",
                        color = tokens.success,
                        fontSize = 10.sp,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerCategoryGitHubContent(
    uiState: LdroidUiState,
    settings: LdroidSettingsState,
    viewModel: LdroidViewModel
) {
    val tokens = LocalLdroidTokens.current
    var handleInput by remember(settings.githubUsername) { mutableStateOf(settings.githubUsername) }
    var patInput by remember { mutableStateOf("") }
    var branchInput by remember(settings.defaultGitBranch) { mutableStateOf(settings.defaultGitBranch) }
    var ownerRepoInput by remember { mutableStateOf("octocat/Hello-World") }
    var commitMsgInput by remember { mutableStateOf("Update via LDROID IDE") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = handleInput,
                onValueChange = { handleInput = it },
                label = { Text("@username", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = branchInput,
                onValueChange = { branchInput = it },
                label = { Text("Branch", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(0.8f)
            )
        }
        OutlinedTextField(
            value = patInput,
            onValueChange = { patInput = it },
            label = { Text("GitHub PAT / OAuth Token (Encrypted)", fontSize = 10.sp) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("github_pat_input")
        )
        Button(
            onClick = {
                viewModel.saveGitHubConfig(patInput.takeIf { it.isNotBlank() }, branchInput)
                viewModel.updateLinkedAccounts(null, handleInput, null, null)
                patInput = ""
            },
            colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent),
            modifier = Modifier.fillMaxWidth().height(40.dp)
        ) { Text("Save GitHub Credentials", fontSize = 11.sp, fontWeight = FontWeight.Bold) }

        OutlinedTextField(
            value = ownerRepoInput,
            onValueChange = { ownerRepoInput = it },
            label = { Text("Repository (owner/repo)", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = commitMsgInput,
            onValueChange = { commitMsgInput = it },
            label = { Text("Commit Message", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = { viewModel.cloneOrPullGitHubRepo(ownerRepoInput) },
                enabled = !uiState.isGitHubLoading,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.weight(1f).height(38.dp)
            ) { Text("Clone/Pull", fontSize = 10.sp) }
            OutlinedButton(
                onClick = { viewModel.commitAndPushActiveFileToGitHub(ownerRepoInput, commitMsgInput) },
                enabled = !uiState.isGitHubLoading,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.weight(1f).height(38.dp)
            ) { Text("Commit & Push", fontSize = 10.sp) }
            OutlinedButton(
                onClick = { viewModel.inspectGitHubRepo(ownerRepoInput) },
                enabled = !uiState.isGitHubLoading,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.weight(0.9f).height(38.dp)
            ) { Text("Commits", fontSize = 10.sp) }
        }
        uiState.gitHubStatus?.let { st ->
            Surface(
                color = tokens.surface1,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, tokens.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("${st.fullName} (${st.inspectedBranch})", color = tokens.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    st.recentCommits.take(4).forEach { c ->
                        Text("${c.sha.take(7)} • ${c.message}", color = tokens.textMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrawerCategoryIntegrationsContent(
    uiState: LdroidUiState,
    settings: LdroidSettingsState,
    viewModel: LdroidViewModel,
    onCloseDrawer: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    var firebaseId by remember(settings.firebaseProjectId) { mutableStateOf(settings.firebaseProjectId) }
    var supabaseUrl by remember { mutableStateOf("") }
    var supabaseKey by remember { mutableStateOf("") }
    var figmaToken by remember { mutableStateOf("") }
    var figmaFrame by remember { mutableStateOf("CheckoutSummaryCard") }

    var restMethod by remember { mutableStateOf("GET") }
    var restUrl by remember { mutableStateOf("https://httpbin.org/get") }
    var restToken by remember { mutableStateOf("") }
    var restBody by remember { mutableStateOf("{\"ping\":\"ldroid\"}") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Cloud BaaS & Design Connectors", color = tokens.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = firebaseId,
            onValueChange = { firebaseId = it },
            label = { Text("Firebase Project ID", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = supabaseUrl,
                onValueChange = { supabaseUrl = it },
                label = { Text("Supabase URL", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = supabaseKey,
                onValueChange = { supabaseKey = it },
                label = { Text("Anon Key", fontSize = 10.sp) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.weight(0.9f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = figmaToken,
                onValueChange = { figmaToken = it },
                label = { Text("Figma Token", fontSize = 10.sp) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { viewModel.saveCloudIntegrations(firebaseId, supabaseUrl, supabaseKey, figmaToken) },
                colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent),
                modifier = Modifier.height(44.dp)
            ) { Text("Link", fontSize = 11.sp) }
        }

        // Figma UI Importer
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = figmaFrame,
                onValueChange = { figmaFrame = it },
                label = { Text("Figma Frame / Node", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = {
                    viewModel.importFigmaLayoutSample(figmaFrame)
                    onCloseDrawer()
                },
                modifier = Modifier.height(44.dp)
            ) { Text("Import UI", fontSize = 10.sp) }
        }

        // Built-In REST API Client / Tester
        Surface(
            color = tokens.surface1,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, tokens.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Built-In REST API Tester", color = tokens.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("GET", "POST", "PUT", "DELETE").forEach { m ->
                        FilterChip(
                            selected = restMethod == m,
                            onClick = { restMethod = m },
                            label = { Text(m, fontSize = 10.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = restUrl,
                    onValueChange = { restUrl = it },
                    label = { Text("Endpoint URL", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rest_tester_url_input")
                )
                if (restMethod != "GET") {
                    OutlinedTextField(
                        value = restBody,
                        onValueChange = { restBody = it },
                        label = { Text("JSON Payload", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Button(
                    onClick = { viewModel.runRestApiTest(restMethod, restUrl, restToken, restBody) },
                    enabled = !uiState.isRestTesting,
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent),
                    modifier = Modifier.fillMaxWidth().height(38.dp).testTag("rest_tester_send_btn")
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (uiState.isRestTesting) "Requesting..." else "Send $restMethod Request", fontSize = 11.sp)
                }
                uiState.restTestResult?.let { res ->
                    Surface(
                        color = tokens.terminalBackground,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "HTTP ${res.statusCode} • ${res.latencyMs}ms",
                                color = if (res.isError) tokens.error else tokens.success,
                                fontSize = 10.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                res.body.take(260),
                                color = tokens.terminalStdout,
                                fontSize = 9.sp,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}
