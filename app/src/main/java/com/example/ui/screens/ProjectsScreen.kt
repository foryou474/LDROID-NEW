package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProjectEntity
import com.example.data.local.WorkspaceSnapshotEntity
import com.example.model.ProjectTemplate
import com.example.ui.components.DiffLinesBlock
import com.example.ui.components.LdroidSurfaceCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.formatBytes
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

@Composable
fun ProjectsScreen(
    uiState: LdroidUiState,
    snapshots: List<WorkspaceSnapshotEntity>,
    viewModel: LdroidViewModel,
    onImportFilesClick: () -> Unit,
    onImportZipClick: () -> Unit,
    onExportZipClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRollbackConfirm by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectSearchQuery by remember { mutableStateOf("") }
    var repoInput by remember { mutableStateOf("octocat/Hello-World") }

    val filteredProjects = remember(uiState.projects, projectSearchQuery) {
        val q = projectSearchQuery.trim().lowercase()
        if (q.isEmpty()) uiState.projects
        else uiState.projects.filter {
            it.name.lowercase().contains(q) ||
                it.packageName.lowercase().contains(q) ||
                it.projectType.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Projects & Templates",
                subtitle = "Create workspaces, import code archives, or compare local changes",
                trailing = {
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.height(48.dp).testTag("new_project_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("New", fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                    }
                }
            )
        }

        // Import / Export Actions (compact padding & single-line text to prevent clipping)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onImportZipClick,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("projects_import_zip_btn"),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, tokens.border)
                ) {
                    Icon(Icons.Filled.FolderZip, contentDescription = null, tint = tokens.primaryAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Import ZIP", color = tokens.textPrimary, fontSize = 11.sp, maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = onImportFilesClick,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("projects_import_files_btn"),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, tokens.border)
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null, tint = tokens.secondaryAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Add Files", color = tokens.textPrimary, fontSize = 11.sp, maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = {
                        val activeName = uiState.activeProject?.name ?: "workspace"
                        onExportZipClick("${activeName}_backup.zip")
                    },
                    enabled = uiState.activeProject != null,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("projects_export_zip_btn"),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, tokens.border)
                ) {
                    Icon(Icons.Filled.Archive, contentDescription = null, tint = tokens.success, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Export ZIP", color = tokens.textPrimary, fontSize = 11.sp, maxLines = 1, softWrap = false)
                }
            }
        }

        // ZIP Import Summary banner
        uiState.lastZipImportSummary?.let { summary ->
            item {
                LdroidSurfaceCard(accentBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ZIP Import Summary: ${summary.project.name}", color = tokens.primaryAccent, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { viewModel.clearZipSummary() }) { Text("Dismiss") }
                    }
                    Text(
                        "Imported ${summary.importedFiles} file(s) • Detected: ${summary.detectedType} • Blocked entries: ${summary.skippedEntries}",
                        color = tokens.textPrimary,
                        fontSize = 12.sp
                    )
                    summary.warnings.forEach { w ->
                        Text("• $w", color = tokens.warning, fontSize = 11.sp)
                    }
                }
            }
        }

        // Project Search Filter
        if (uiState.projects.size > 1 || projectSearchQuery.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = projectSearchQuery,
                    onValueChange = { projectSearchQuery = it },
                    placeholder = { Text("Filter projects by name, package, or type...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = tokens.textMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("projects_search_input")
                )
            }
        }

        // Project List
        items(filteredProjects, key = { it.id }) { proj ->
            val isActive = uiState.activeProject?.id == proj.id
            LdroidSurfaceCard(
                accentBorder = isActive,
                onClick = { viewModel.switchProject(proj) },
                modifier = Modifier.fillMaxWidth().testTag("project_card_${proj.name}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(proj.name, color = tokens.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            if (isActive) StatusPill("Active", tokens.primaryAccent)
                        }
                        Text(proj.packageName, color = tokens.textMuted, fontSize = 12.sp, fontFamily = JetBrainsMonoFontFamily)
                        Text("Type: ${proj.projectType}", color = tokens.secondaryAccent, fontSize = 11.sp)
                    }
                    if (uiState.projects.size > 1) {
                        IconButton(
                            onClick = { projectToDelete = proj },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete ${proj.name}", tint = tokens.error)
                        }
                    }
                }
            }
        }

        // Workspace Diff & Snapshots
        item {
            SectionHeader(
                title = "Snapshots & Diff",
                subtitle = "Compare or restore saved workspace checkpoints",
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (uiState.workspaceChanges.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showRollbackConfirm = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("projects_rollback_all_btn")
                            ) {
                                Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(15.dp), tint = tokens.warning)
                                Spacer(Modifier.size(4.dp))
                                Text("Rollback", color = tokens.warning, fontSize = 11.sp, maxLines = 1, softWrap = false)
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.createWorkspaceSnapshot() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("Snapshot", fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            )
        }

        if (uiState.workspaceChanges.isEmpty()) {
            item {
                LdroidSurfaceCard {
                    StatusPill("Working Tree Clean", tokens.success)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "No modifications since last baseline snapshot. (${snapshots.size} saved snapshot(s))",
                        color = tokens.textMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(uiState.workspaceChanges) { change ->
                LdroidSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = change.relativePath,
                            color = tokens.textPrimary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatusPill(
                                change.status,
                                when (change.status) {
                                    "ADDED" -> tokens.success
                                    "DELETED" -> tokens.error
                                    else -> tokens.warning
                                }
                            )
                            IconButton(
                                onClick = { viewModel.restoreSnapshotFile(change.relativePath) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Filled.Restore, contentDescription = "Revert ${change.relativePath}", tint = tokens.primaryAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    DiffLinesBlock(change.diffLines.take(40))
                    if (change.diffLines.size > 40) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Showing first 40 of ${change.diffLines.size} diff lines",
                            color = tokens.textMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // GitHub Repository & Actions Inspector
        item {
            SectionHeader(
                title = "GitHub Inspector",
                subtitle = "Check repository details, recent commits & CI status"
            )
        }

        item {
            LdroidSurfaceCard {
                OutlinedTextField(
                    value = repoInput,
                    onValueChange = { repoInput = it },
                    label = { Text("GitHub Repository (owner/repo)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.inspectGitHubRepo(repoInput) },
                    enabled = !uiState.isGitHubLoading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.secondaryAccent, contentColor = tokens.onPrimaryAccent)
                ) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Fetch Repo & CI Runs")
                }
                if (uiState.isGitHubLoading) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                uiState.gitHubError?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Text(err, color = tokens.error, fontSize = 11.sp)
                }
                uiState.gitHubStatus?.let { gh ->
                    Spacer(Modifier.height(10.dp))
                    Text(gh.fullName, color = tokens.primaryAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Branch: ${gh.inspectedBranch} (default: ${gh.defaultBranch}) • Stars: ${gh.stars} • Forks: ${gh.forks} • Open issues: ${gh.openIssues}",
                        color = tokens.textMuted,
                        fontSize = 12.sp
                    )
                    if (gh.recentCommits.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Recent Commits (${gh.inspectedBranch})", color = tokens.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        gh.recentCommits.forEach { commit ->
                            Surface(
                                color = tokens.surface2,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(commit.sha, color = tokens.info, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(commit.message, color = tokens.textPrimary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    Text(commit.author, color = tokens.textMuted, fontSize = 10.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Recent CI Runs", color = tokens.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    if (gh.recentWorkflowRuns.isEmpty()) {
                        Text("No recent workflow runs returned.", color = tokens.textMuted, fontSize = 12.sp)
                    } else {
                        gh.recentWorkflowRuns.forEach { run ->
                            Surface(
                                color = tokens.surface2,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${run.name} (${run.branch})", color = tokens.textPrimary, fontSize = 12.sp)
                                    Text(run.conclusion ?: run.status, color = if (run.conclusion == "success") tokens.success else tokens.warning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRollbackConfirm) {
        AlertDialog(
            onDismissRequest = { showRollbackConfirm = false },
            title = { Text("Rollback Workspace?") },
            text = {
                Text("Restore all ${uiState.workspaceChanges.size} modified/added/deleted file(s) to the latest baseline snapshot?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreFullSnapshot()
                        showRollbackConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.warning, contentColor = tokens.onPrimaryAccent)
                ) { Text("Rollback All") }
            },
            dismissButton = {
                TextButton(onClick = { showRollbackConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, pkg, tpl ->
                viewModel.createProject(name, pkg, tpl)
                showCreateDialog = false
            }
        )
    }

    projectToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete '${target.name}'?") },
            text = { Text("This permanently deletes the local workspace files and snapshots for '${target.name}'.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(target)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, ProjectTemplate) -> Unit
) {
    val tokens = LocalLdroidTokens.current
    var name by remember { mutableStateOf("MaxPulseApp") }
    var pkg by remember { mutableStateOf("com.maxstudio.pulseapp") }
    var selectedTemplate by remember { mutableStateOf(ProjectTemplate.COMPOSE_M3_APP) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Workspace") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("create_project_name_input")
                )
                OutlinedTextField(
                    value = pkg,
                    onValueChange = { pkg = it },
                    label = { Text("Package / Bundle ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Starter Template",
                    color = tokens.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                ProjectTemplate.entries.forEach { tpl ->
                    val isSelected = selectedTemplate == tpl
                    Surface(
                        color = if (isSelected) tokens.primaryAccent.copy(alpha = 0.14f) else tokens.surface2,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isSelected) tokens.primaryAccent else tokens.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTemplate = tpl }
                            .testTag("template_${tpl.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tpl.title,
                                    color = if (isSelected) tokens.primaryAccent else tokens.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusPill(
                                    label = tpl.badge,
                                    color = if (isSelected) tokens.primaryAccent else tokens.secondaryAccent
                                )
                            }
                            Text(
                                text = tpl.description,
                                color = tokens.textMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, pkg, selectedTemplate) },
                modifier = Modifier.testTag("confirm_create_project_btn")
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
