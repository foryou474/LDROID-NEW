package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.build.BuildApkEngine
import com.example.model.BuildArtifactFormat
import com.example.model.BuildArtifactRecord
import com.example.model.BuildLanguageTarget
import com.example.model.BuildStepStatus
import com.example.ui.components.LdroidSurfaceCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatBytes
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

/**
 * Tab 7 (📦): Build & APK — Dedicated Release & Multi-Language Studio
 * - 9 Language Targets (Kotlin+XML, Compose, Java, Flutter, RN, HTML5, Python, Lua, C/C++ NDK)
 * - Debug APK vs Signed Release APK / AAB, +1 Version bumper, Custom .jks Keystore, arm64-v8a split, R8
 * - Live 5-step pipeline (aapt2 -> kotlinc -> d8/r8 -> zipalign -> apksigner)
 * - v1/v2/v3 Signature Verification Badge, Permission Audit, History Archive
 * - 1-tap Install APK, Share, and Local Wi-Fi QR Transfer
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorTabBuildApk(
    uiState: LdroidUiState,
    viewModel: LdroidViewModel,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    var showKeystoreDialog by remember { mutableStateOf(false) }
    val latestBuild = uiState.buildHistory.firstOrNull()

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("tab_build_apk"),
        contentPadding = PaddingValues(bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Multi-Language Auto-Compiler Selector (9 Languages)
        item {
            LdroidSurfaceCard(accentBorder = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Multi-Language Auto-Compiler", color = tokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    StatusPill(uiState.activeBuildLanguage.shortTag, tokens.primaryAccent)
                }
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BuildLanguageTarget.entries.forEach { target ->
                        FilterChip(
                            selected = uiState.activeBuildLanguage == target,
                            onClick = { viewModel.selectBuildLanguage(target) },
                            label = { Text(target.displayName, fontSize = 10.sp) },
                            modifier = Modifier.testTag("build_lang_${target.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Build Controls: Format, Version Bumper, Keystore, ABI Split & R8
        item {
            LdroidSurfaceCard {
                Text("Release & Signing Controls", color = tokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BuildArtifactFormat.entries.forEach { fmt ->
                        FilterChip(
                            selected = uiState.buildArtifactFormat == fmt,
                            onClick = { viewModel.selectBuildFormat(fmt) },
                            label = { Text(fmt.label, fontSize = 10.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Version ${uiState.buildVersionName} (code ${uiState.buildVersionCode})",
                            color = tokens.textPrimary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("Keystore: ${uiState.buildKeystoreAlias}", color = tokens.textMuted, fontSize = 10.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.bumpBuildVersionCode() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(36.dp).testTag("build_version_bump_btn")
                        ) { Text("+1 Ver", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = { showKeystoreDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(36.dp).testTag("build_keystore_btn")
                        ) {
                            Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(".jks", fontSize = 10.sp)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ABI Split (arm64-v8a)", color = tokens.textPrimary, fontSize = 12.sp)
                    Switch(checked = uiState.buildAbiArm64Only, onCheckedChange = { viewModel.toggleBuildAbiSplit() })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("R8 Shrinker & Resource Minifier", color = tokens.textPrimary, fontSize = 12.sp)
                    Switch(checked = uiState.buildR8ShrinkEnabled, onCheckedChange = { viewModel.toggleBuildR8() })
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.runBuildAndApkPipeline() },
                    enabled = !uiState.buildState.isRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("build_apk_compile_btn")
                ) {
                    Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (uiState.buildState.isRunning) uiState.buildState.statusLabel
                        else "Build ${uiState.buildArtifactFormat.label}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Live 5-Step Pipeline
        item {
            LdroidSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("5-Step ARM64 Pipeline", color = tokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    StatusPill(
                        uiState.buildState.statusLabel,
                        when (uiState.buildState.succeeded) {
                            true -> tokens.success
                            false -> tokens.error
                            null -> tokens.primaryAccent
                        }
                    )
                }
                if (uiState.buildState.isRunning || uiState.buildState.progress > 0f) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { uiState.buildState.progress.coerceIn(0f, 1f) },
                        color = tokens.primaryAccent,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.buildPipelineSteps.forEach { step ->
                        val stepColor = when (step.status) {
                            BuildStepStatus.SUCCESS -> tokens.success
                            BuildStepStatus.RUNNING -> tokens.primaryAccent
                            BuildStepStatus.FAILED -> tokens.error
                            BuildStepStatus.PENDING -> tokens.textMuted
                        }
                        Surface(
                            color = tokens.surface2,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, stepColor.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(step.title, color = tokens.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text(step.detail, color = tokens.textMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                StatusPill(step.status.name, stepColor)
                            }
                        }
                    }
                }
            }
        }

        // Latest Output Card + Install / Share / Local Wi-Fi QR Transfer
        latestBuild?.let { record ->
            item {
                BuildOutputArtifactCard(
                    record = record,
                    onInstall = { viewModel.installBuiltApk(record) },
                    onShare = { viewModel.shareBuiltApk(record) },
                    onWifiQr = { viewModel.toggleWifiQrTransfer(record) }
                )
            }
        }

        // Build History Archive
        if (uiState.buildHistory.size > 1) {
            item {
                LdroidSurfaceCard {
                    Text("Build History Archive (${uiState.buildHistory.size})", color = tokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.buildHistory.drop(1).forEach { rec ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "${rec.appName} v${rec.versionName} (${rec.format.label})",
                                        color = tokens.textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${rec.languageTarget.shortTag} • ${formatBytes(rec.sizeBytes)} • SHA256:${rec.sha256.take(10)}",
                                        color = tokens.textMuted,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.sp
                                    )
                                }
                                StatusPill("Verified", tokens.success)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showKeystoreDialog) {
        var alias by remember(uiState.buildKeystoreAlias) { mutableStateOf(uiState.buildKeystoreAlias) }
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showKeystoreDialog = false },
            title = { Text("Signing Keystore (.jks)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = alias,
                        onValueChange = { alias = it },
                        label = { Text("Key Alias") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Keystore Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setCustomKeystore(alias, password)
                    showKeystoreDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showKeystoreDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Local Wi-Fi QR Transfer Dialog
    uiState.wifiQrShareUrl?.let { url ->
        val matrix = remember(url) { BuildApkEngine.buildQrMatrix(url, 21) }
        AlertDialog(
            onDismissRequest = { viewModel.stopWifiQrTransfer() },
            title = { Text("📱 Local Wi-Fi QR Transfer") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Scan or open on a device connected to the same Wi-Fi network:",
                        color = tokens.textMuted,
                        fontSize = 11.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cellW = size.width / matrix.size
                            val cellH = size.height / matrix.size
                            for (r in matrix.indices) {
                                for (c in matrix[r].indices) {
                                    if (matrix[r][c]) {
                                        drawRect(
                                            color = Color.Black,
                                            topLeft = Offset(c * cellW, r * cellH),
                                            size = Size(cellW + 0.5f, cellH + 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        text = url,
                        color = tokens.primaryAccent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.stopWifiQrTransfer() }) { Text("Stop Server") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BuildOutputArtifactCard(
    record: BuildArtifactRecord,
    onInstall: () -> Unit,
    onShare: () -> Unit,
    onWifiQr: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    LdroidSurfaceCard(accentBorder = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = tokens.success, modifier = Modifier.size(18.dp))
                Text(
                    "${record.appName} v${record.versionName}",
                    color = tokens.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            StatusPill(formatBytes(record.sizeBytes), tokens.primaryAccent)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = tokens.success, modifier = Modifier.size(14.dp))
            Text(record.signatureSchemes, color = tokens.success, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        Text("Permission Audit:", color = tokens.textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            record.permissions.forEach { perm ->
                StatusPill(perm.substringAfterLast('.'), tokens.info)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = onInstall,
                colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.weight(1f).height(42.dp).testTag("build_install_apk_btn")
            ) {
                Icon(Icons.Filled.InstallMobile, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("📲 Install APK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onShare,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.weight(0.85f).height(42.dp).testTag("build_share_apk_btn")
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("📤 Share", fontSize = 10.sp)
            }
            OutlinedButton(
                onClick = onWifiQr,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.weight(0.95f).height(42.dp).testTag("build_wifi_qr_btn")
            ) {
                Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("📱 Wi-Fi QR", fontSize = 10.sp)
            }
        }
    }
}
