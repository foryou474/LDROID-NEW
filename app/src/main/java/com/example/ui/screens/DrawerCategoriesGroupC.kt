package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.LdroidSettingsState
import com.example.model.DeveloperLabSection
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

/**
 * Group C of the Left Sidebar Navigation Drawer:
 * 10. Developer Lab (Experimental IDE flags, layout bounds inspector, FPS/memory profiler + 8 lab tools)
 * 11. Advanced Developer Settings (Target SDK/NDK/JDK, R8/ProGuard, .jks Keystore, Env Vars, System Audit)
 */
@Composable
fun DrawerCategoryDeveloperLabContent(
    uiState: LdroidUiState,
    settings: LdroidSettingsState,
    viewModel: LdroidViewModel,
    onCloseDrawer: () -> Unit,
    onOpenLabSection: (DeveloperLabSection) -> Unit
) {
    val tokens = LocalLdroidTokens.current
    val flags = settings.experimentalFlags
    val probe = uiState.toolchainProbe

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Experimental IDE Flags", color = tokens.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        DrawerSwitchRow(
            title = "Layout Bounds Inspector",
            subtitle = "Render wireframe dp margins in Live Preview",
            checked = flags.layoutBoundsInspector,
            testTag = "dev_flag_layout_bounds",
            onCheckedChange = {
                viewModel.updateExperimentalFlags(flags.copy(layoutBoundsInspector = it))
            }
        )
        DrawerSwitchRow(
            title = "FPS & Memory Profiler HUD",
            subtitle = "Heap ${probe?.javaHeapUsedMb ?: 39}/${probe?.javaHeapMaxMb ?: 256} MB • ${probe?.cpuCores ?: 8} ARM64 cores",
            checked = flags.fpsMemoryHud,
            testTag = "dev_flag_fps_hud",
            onCheckedChange = {
                viewModel.updateExperimentalFlags(flags.copy(fpsMemoryHud = it))
            }
        )
        DrawerSwitchRow(
            title = "AST Hot Reload & Predictive Back",
            subtitle = "Live Kotlin/XML tree diff without full restart",
            checked = flags.astHotReload,
            testTag = "dev_flag_hot_reload",
            onCheckedChange = {
                viewModel.updateExperimentalFlags(flags.copy(astHotReload = it))
            }
        )

        Text("8 Diagnostic Lab Modules", color = tokens.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        DeveloperLabSection.entries.forEach { section ->
            Surface(
                color = tokens.surface1,
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(1.dp, tokens.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenLabSection(section)
                        onCloseDrawer()
                    }
                    .testTag("drawer_lab_${section.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(iconForLabItem(section), contentDescription = section.title, tint = tokens.primaryAccent, modifier = Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(section.title, color = tokens.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(section.subtitle, color = tokens.textMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrawerCategoryAdvancedSettingsContent(
    uiState: LdroidUiState,
    settings: LdroidSettingsState,
    viewModel: LdroidViewModel,
    isMicGranted: Boolean,
    onToggleStorage: (Boolean) -> Unit,
    onTogglePhotos: (Boolean) -> Unit,
    onToggleMic: (Boolean) -> Unit
) {
    val tokens = LocalLdroidTokens.current
    var targetSdk by remember(settings.targetSdkVersion) { mutableIntStateOf(settings.targetSdkVersion) }
    var ndkVersion by remember(settings.ndkVersion) { mutableStateOf(settings.ndkVersion) }
    var jdkVersion by remember(settings.jdkVersion) { mutableStateOf(settings.jdkVersion) }
    var r8Enabled by remember(settings.r8MinifyEnabled) { mutableStateOf(settings.r8MinifyEnabled) }
    var proguardRules by remember(settings.proguardRulesText) { mutableStateOf(settings.proguardRulesText) }
    var envVarsJson by remember(settings.shellEnvVarsJson) { mutableStateOf(settings.shellEnvVarsJson) }
    var jksAlias by remember(uiState.buildKeystoreAlias) { mutableStateOf(uiState.buildKeystoreAlias) }
    var jksPass by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Toolchain & SDK Targets", color = tokens.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(34, 35, 36).forEach { api ->
                FilterChip(
                    selected = targetSdk == api,
                    onClick = { targetSdk = api },
                    label = { Text("API $api", fontSize = 10.sp) }
                )
            }
            listOf("17", "21").forEach { jdk ->
                FilterChip(
                    selected = jdkVersion == jdk,
                    onClick = { jdkVersion = jdk },
                    label = { Text("JDK $jdk", fontSize = 10.sp) }
                )
            }
        }

        OutlinedTextField(
            value = ndkVersion,
            onValueChange = { ndkVersion = it },
            label = { Text("NDK Version", fontSize = 10.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        DrawerSwitchRow(
            title = "R8 / ProGuard Shrinking",
            subtitle = "Minify & obfuscate release bytecode",
            checked = r8Enabled,
            testTag = "adv_r8_switch",
            onCheckedChange = { r8Enabled = it }
        )
        OutlinedTextField(
            value = proguardRules,
            onValueChange = { proguardRules = it },
            label = { Text("proguard-rules.pro", fontSize = 10.sp) },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = envVarsJson,
            onValueChange = { envVarsJson = it },
            label = { Text("Shell Env Variables (JSON)", fontSize = 10.sp) },
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                viewModel.updateAdvancedBuildSettings(targetSdk, ndkVersion, jdkVersion, r8Enabled, proguardRules, envVarsJson)
            },
            colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent),
            modifier = Modifier.fillMaxWidth().height(38.dp)
        ) { Text("Save SDK / R8 / Env Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) }

        // Custom .jks Keystore Manager
        Surface(
            color = tokens.surface1,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, tokens.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Custom .jks Keystore Manager", color = tokens.primaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = jksAlias,
                    onValueChange = { jksAlias = it },
                    label = { Text("Key Alias", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = jksPass,
                    onValueChange = { jksPass = it },
                    label = { Text("Keystore Password (Encrypted)", fontSize = 10.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        viewModel.setCustomKeystore(jksAlias, jksPass)
                        jksPass = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) { Text("Save Keystore Config", fontSize = 10.sp) }
            }
        }

        // Scoped Device Permissions
        Text("Scoped Device Permissions", color = tokens.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        DrawerSwitchRow(
            title = "Storage & Files (SAF Tree)",
            subtitle = "Zero broad storage permissions",
            checked = uiState.storagePermissionGranted,
            testTag = "perm_toggle_storage",
            onCheckedChange = onToggleStorage
        )
        DrawerSwitchRow(
            title = "Photos (Android Photo Picker)",
            subtitle = "Zero-permission media picker",
            checked = uiState.photosPermissionGranted,
            testTag = "perm_toggle_photos",
            onCheckedChange = onTogglePhotos
        )
        DrawerSwitchRow(
            title = "Microphone (Voice-to-Code)",
            subtitle = "Voice input for ChatGPT dock",
            checked = isMicGranted,
            testTag = "perm_toggle_mic",
            onCheckedChange = onToggleMic
        )

        // System Audit Logs & M01-M50 Suite
        Surface(
            color = tokens.surface1,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, tokens.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("System Audit Logs", color = tokens.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    uiState.verificationReport?.let { rep ->
                        StatusPill("${rep.passedCount}/${rep.items.size} OK", if (rep.failedCount == 0) tokens.success else tokens.warning)
                    }
                }
                OutlinedButton(
                    onClick = { viewModel.runSystemVerification() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp).testTag("adv_system_audit_btn")
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (uiState.isRunningVerification) "Auditing..." else "Run M01–M50 System Audit", fontSize = 10.sp)
                }
                uiState.ideOutputLogs.takeLast(4).forEach { log ->
                    Text(
                        "[${log.timestamp}] ${log.source}: ${log.message}",
                        color = if (log.isError) tokens.error else tokens.textMuted,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun iconForLabItem(section: DeveloperLabSection): ImageVector = when (section) {
    DeveloperLabSection.DEVICE_LAB -> Icons.Filled.Devices
    DeveloperLabSection.LIVE_PREVIEW -> Icons.Filled.Preview
    DeveloperLabSection.DEBUGGER_LOGCAT -> Icons.Filled.BugReport
    DeveloperLabSection.PERFORMANCE_PROFILER -> Icons.Filled.Speed
    DeveloperLabSection.TESTING_LAB -> Icons.Filled.Science
    DeveloperLabSection.APK_ANALYZER -> Icons.Filled.Inventory2
    DeveloperLabSection.BUILD_DIAGNOSTICS -> Icons.Filled.Build
    DeveloperLabSection.SDK_ADB_DIAGNOSTICS -> Icons.Filled.Terminal
}
