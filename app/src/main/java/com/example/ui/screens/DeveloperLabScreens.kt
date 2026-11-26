package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.visual.VisualBuilderCodegen
import com.example.model.DeveloperLabSection
import com.example.model.DiagnosticSeverity
import com.example.ui.components.DeviceFramePreviewCanvas
import com.example.ui.components.LdroidSurfaceCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatBytes
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

@Composable
fun DeveloperLabDetailScreen(
    section: DeveloperLabSection,
    uiState: LdroidUiState,
    viewModel: LdroidViewModel,
    onPickApkOrAab: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val tokens = LocalLdroidTokens.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
    ) {
        // Top header bar with 48dp touch target
        Surface(color = tokens.surface1, border = BorderStroke(1.dp, tokens.divider)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp).testTag("lab_back_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = tokens.textPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(section.title, color = tokens.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("${section.categoryTag} • ${section.subtitle}", color = tokens.textMuted, fontSize = 11.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (section) {
                DeveloperLabSection.DEVICE_LAB -> {
                    item {
                        LdroidSurfaceCard(accentBorder = true) {
                            Text("Device Profiles & Form Factors", color = tokens.textPrimary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            VisualBuilderCodegen.defaultDeviceProfiles.forEach { prof ->
                                val selected = uiState.selectedDeviceProfile.name == prof.name
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.selectDeviceProfile(prof) },
                                    label = { Text("${prof.name} (${prof.widthDp}x${prof.heightDp}dp, ${prof.dpi}dpi, API ${prof.apiLevel})") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Font Scale: ${String.format("%.2f", uiState.previewFontScale)}x", color = tokens.textMuted, fontSize = 12.sp)
                                OutlinedButton(onClick = { viewModel.togglePreviewOrientation() }) {
                                    Icon(Icons.Filled.ScreenRotation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text(if (uiState.previewLandscape) "Landscape" else "Portrait")
                                }
                            }
                            Slider(
                                value = uiState.previewFontScale,
                                onValueChange = { viewModel.setPreviewFontScale(it) },
                                valueRange = 0.85f..1.4f
                            )
                        }
                    }
                    item {
                        LdroidSurfaceCard {
                            Text("Emulator & Virtualization Feasibility", color = tokens.warning, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = uiState.toolchainProbe?.virtualizationNote
                                    ?: "Full AVD hardware virtualization is not exposed inside a standard Android app sandbox. Use Device Lab layout profiles here, or connect a host/cloud runner via GitHub Actions.",
                                color = tokens.textMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                DeveloperLabSection.LIVE_PREVIEW -> {
                    item {
                        var showVisualBuilderPreview by remember { mutableStateOf(false) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !showVisualBuilderPreview,
                                onClick = { showVisualBuilderPreview = false },
                                label = { Text("Code Live Preview") }
                            )
                            FilterChip(
                                selected = showVisualBuilderPreview,
                                onClick = { showVisualBuilderPreview = true },
                                label = { Text("Visual UI Live Preview") }
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        if (!showVisualBuilderPreview) {
                            val codePrev = uiState.codePreviewState
                            DeviceFramePreviewCanvas(
                                title = codePrev.lastGoodTitle,
                                nodes = codePrev.lastGoodNodes,
                                profile = uiState.selectedDeviceProfile,
                                landscape = uiState.previewLandscape,
                                fontScale = uiState.previewFontScale,
                                isStale = codePrev.isStale,
                                staleError = codePrev.errorMessage
                            )
                        } else {
                            DeviceFramePreviewCanvas(
                                title = "Visual Builder Canvas",
                                nodes = uiState.visualNodes,
                                profile = uiState.selectedDeviceProfile,
                                landscape = uiState.previewLandscape,
                                fontScale = uiState.previewFontScale,
                                isStale = false,
                                staleError = null
                            )
                        }
                    }
                }

                DeveloperLabSection.DEBUGGER_LOGCAT -> {
                    item {
                        var tagFilter by remember { mutableStateOf("") }
                        var level by remember { mutableStateOf("V") }
                        LdroidSurfaceCard {
                            Text("Process Logcat Filter (Current App PID)", color = tokens.textPrimary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = tagFilter,
                                onValueChange = { tagFilter = it },
                                label = { Text("Filter keyword or tag") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("V", "D", "I", "W", "E").forEach { lvl ->
                                    FilterChip(
                                        selected = level == lvl,
                                        onClick = { level = lvl },
                                        label = { Text(lvl) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.refreshLogcat(tagFilter, level) },
                                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent)
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("Read Logcat")
                                }
                                OutlinedButton(onClick = { viewModel.explainLogcatWithAi() }) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = tokens.secondaryAccent, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("AI Explain Logs")
                                }
                            }
                        }
                    }
                    items(uiState.logcatLines) { line ->
                        Surface(
                            color = tokens.terminalBackground,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = line,
                                color = if (line.contains(" E ") || line.contains("Error", true)) tokens.error else tokens.terminalStdout,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                DeveloperLabSection.PERFORMANCE_PROFILER -> {
                    val probe = uiState.toolchainProbe
                    item {
                        LdroidSurfaceCard(accentBorder = true) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Live Runtime & Memory Telemetry", color = tokens.textPrimary, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { viewModel.refreshDiagnosticsProbe() }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh telemetry", tint = tokens.primaryAccent)
                                }
                            }
                            if (probe != null) {
                                val heapRatio = if (probe.javaHeapMaxMb > 0) {
                                    (probe.javaHeapUsedMb.toFloat() / probe.javaHeapMaxMb.toFloat()).coerceIn(0f, 1f)
                                } else 0f
                                Text("JVM Heap: ${probe.javaHeapUsedMb} MB / ${probe.javaHeapMaxMb} MB", color = tokens.textPrimary, fontSize = 13.sp)
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(progress = { heapRatio }, modifier = Modifier.fillMaxWidth(), color = tokens.primaryAccent)
                                Spacer(Modifier.height(10.dp))
                                Text("Native Heap Allocated: ${probe.nativeHeapMb} MB", color = tokens.textMuted, fontSize = 12.sp)
                                Text("System RAM Available: ${probe.availRamMb} MB / ${probe.totalRamMb} MB", color = tokens.textMuted, fontSize = 12.sp)
                                Text("CPU Cores: ${probe.cpuCores} • ABIs: ${probe.supportedAbis.joinToString()}", color = tokens.textMuted, fontSize = 12.sp)
                                Text("Free Workspace Storage: ${probe.freeStorageMb} MB", color = tokens.textMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }

                DeveloperLabSection.TESTING_LAB -> {
                    item {
                        LdroidSurfaceCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("On-Device Static & Policy Audit", color = tokens.textPrimary, fontWeight = FontWeight.Bold)
                                    Text("Scans Kotlin/XML/JSON/Manifest & exposed secrets", color = tokens.textMuted, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { viewModel.runFullWorkspaceStaticAudit() },
                                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent)
                                ) {
                                    Text("Run Scan")
                                }
                            }
                        }
                    }
                    if (uiState.diagnostics.isEmpty()) {
                        item {
                            LdroidSurfaceCard {
                                StatusPill("All Checked Files Passed", tokens.success)
                                Spacer(Modifier.height(6.dp))
                                Text("0 syntax errors, 0 malformed XML/JSON files, and 0 exposed secret patterns found.", color = tokens.textMuted, fontSize = 12.sp)
                            }
                        }
                    } else {
                        items(uiState.diagnostics) { issue ->
                            val sevColor = when (issue.severity) {
                                DiagnosticSeverity.ERROR, DiagnosticSeverity.SECRET -> tokens.error
                                DiagnosticSeverity.WARNING -> tokens.warning
                                DiagnosticSeverity.INFO -> tokens.info
                            }
                            LdroidSurfaceCard(onClick = { viewModel.openFileInEditor(issue.filePath, jumpToEditorTab = true) }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatusPill(issue.severity.name, sevColor)
                                    Text("${issue.filePath}:${issue.line}", color = tokens.textMuted, fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(issue.message, color = tokens.textPrimary, fontSize = 13.sp)
                                Text(issue.source, color = tokens.textMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }

                DeveloperLabSection.APK_ANALYZER -> {
                    item {
                        LdroidSurfaceCard(accentBorder = true) {
                            Text("Real APK / AAB / ZIP Artifact Inspector", color = tokens.textPrimary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Inspects ZIP entries, DEX bytecode headers, PackageManager manifest & permissions, X.509 signing certificates, ABIs, and SHA-256 digest.",
                                color = tokens.textMuted,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onPickApkOrAab,
                                    modifier = Modifier.weight(1f).testTag("pick_apk_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent)
                                ) {
                                    Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("Pick APK/AAB")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.analyzeSelfApk() },
                                    modifier = Modifier.weight(1f).testTag("analyze_self_apk_btn")
                                ) {
                                    Text("Inspect App APK", maxLines = 1)
                                }
                            }
                            if (uiState.isApkAnalyzing) {
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }

                    uiState.apkReport?.let { report ->
                        item {
                            LdroidSurfaceCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(report.fileName, color = tokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    StatusPill(report.artifactType, tokens.primaryAccent)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("Size: ${formatBytes(report.fileSizeBytes)}", color = tokens.textMuted, fontSize = 12.sp)
                                Text("SHA-256: ${report.sha256}", color = tokens.textMuted, fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily)
                                report.packageName?.let {
                                    Text("Package: $it • v${report.versionName} (${report.versionCode})", color = tokens.textPrimary, fontSize = 12.sp)
                                }
                                if (report.minSdk != null || report.targetSdk != null) {
                                    Text("SDK: minSdk=${report.minSdk ?: "?"} • targetSdk=${report.targetSdk ?: "?"}", color = tokens.textMuted, fontSize = 12.sp)
                                }
                                Text(
                                    "DEX Files: ${report.dexCount} (Versions: ${report.dexVersions.ifEmpty { listOf("N/A") }.joinToString()})",
                                    color = tokens.textMuted,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "ABIs: ${report.abis.ifEmpty { listOf("Universal / Pure DEX") }.joinToString()}",
                                    color = tokens.textMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        item {
                            LdroidSurfaceCard {
                                Text("Size Breakdown", color = tokens.textPrimary, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                val total = report.fileSizeBytes.coerceAtLeast(1L)
                                report.sizeBuckets.forEach { bucket ->
                                    val ratio = (bucket.bytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(bucket.label, color = tokens.textPrimary, fontSize = 12.sp)
                                        Text("${formatBytes(bucket.bytes)} (${(ratio * 100).toInt()}%)", color = tokens.primaryAccent, fontSize = 12.sp, fontFamily = JetBrainsMonoFontFamily)
                                    }
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        color = tokens.secondaryAccent
                                    )
                                }
                            }
                        }

                        item {
                            LdroidSurfaceCard {
                                Text("Signing & Permissions (${report.permissions.size})", color = tokens.textPrimary, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                report.signingIssuer?.let {
                                    Text("Subject: $it", color = tokens.textMuted, fontSize = 11.sp)
                                }
                                report.signingCertSha256.forEach { sha ->
                                    Text("Cert SHA-256: $sha", color = tokens.success, fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily)
                                }
                                Spacer(Modifier.height(6.dp))
                                report.permissions.take(15).forEach { perm ->
                                    Text("• $perm", color = tokens.textMuted, fontSize = 11.sp, fontFamily = JetBrainsMonoFontFamily)
                                }
                                Spacer(Modifier.height(8.dp))
                                report.heuristicNotes.forEach { note ->
                                    Text(note, color = tokens.warning, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                DeveloperLabSection.BUILD_DIAGNOSTICS -> {
                    item {
                        val gradleFiles = uiState.workspaceFiles.filter {
                            it.name.endsWith(".gradle.kts") || it.name.endsWith(".gradle") || it.name == "libs.versions.toml"
                        }
                        LdroidSurfaceCard {
                            Text("Gradle & Multi-Module Scanner", color = tokens.textPrimary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Detected ${gradleFiles.size} build/catalog file(s) in '${uiState.activeProject?.name ?: "None"}'.",
                                color = tokens.textMuted,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            gradleFiles.forEach { gf ->
                                OutlinedButton(
                                    onClick = { viewModel.openFileInEditor(gf.relativePath, jumpToEditorTab = true) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                ) {
                                    Text("Inspect ${gf.relativePath}", color = tokens.primaryAccent)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Note: On-device phone sandboxes do not ship a full JDK/Gradle daemon; use the generated .github/workflows/android_ci.yml for full cloud builds & signed APK/AAB artifacts.",
                                color = tokens.info,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                DeveloperLabSection.SDK_ADB_DIAGNOSTICS -> {
                    val probe = uiState.toolchainProbe
                    item {
                        LdroidSurfaceCard {
                            Text("SDK, ADB & Shell Environment Probe", color = tokens.textPrimary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            if (probe != null) {
                                ProbeRow("Device", "${probe.deviceModel} (Android ${probe.androidVersion} / API ${probe.apiLevel})")
                                ProbeRow("Shell", probe.shellPath ?: "Unavailable")
                                ProbeRow("Toybox Utilities", if (probe.toyboxAvailable) "Available" else "Not found")
                                ProbeRow("ADB Binary on PATH", if (probe.adbBinaryFound) "Available" else "Not bundled on device")
                                ProbeRow("Gradle CLI on PATH", if (probe.gradleBinaryFound) "Available" else "Not bundled on device")
                                ProbeRow("JDK / javac on PATH", if (probe.javaCompilerFound) "Available" else "Not bundled on device")
                                Spacer(Modifier.height(8.dp))
                                Text(probe.virtualizationNote, color = tokens.warning, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProbeRow(label: String, value: String) {
    val tokens = LocalLdroidTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = tokens.textMuted, fontSize = 12.sp)
        Text(value, color = tokens.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
