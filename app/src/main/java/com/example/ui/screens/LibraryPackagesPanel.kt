package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GradlePackageItem
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

@Composable
fun LibraryPackagesPanel(
    uiState: LdroidUiState,
    viewModel: LdroidViewModel,
    onExtractSdkZipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    var pkgQuery by remember { mutableStateOf("") }
    val filteredPackages = remember(uiState.gradlePackages, pkgQuery) {
        val q = pkgQuery.trim().lowercase()
        if (q.isEmpty()) uiState.gradlePackages
        else uiState.gradlePackages.filter {
            it.title.lowercase().contains(q) ||
                it.groupArtifact.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 4A. Inbuilt Termux Linux Core
        Surface(
            color = tokens.surface2,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, tokens.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.Terminal,
                            contentDescription = "Termux Core",
                            tint = tokens.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Inbuilt Termux Linux Core",
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.initializeTermuxCore() },
                        modifier = Modifier.height(34.dp).testTag("init_termux_core_btn")
                    ) {
                        Text("Initialize", fontSize = 10.sp)
                    }
                }

                uiState.termuxCoreItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.name} (${item.category})",
                                color = tokens.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = JetBrainsMonoFontFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.relativePath} • ${item.detail}",
                                color = tokens.textMuted,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = if (item.isReady) "Ready" else "Pending",
                            tint = if (item.isReady) tokens.success else tokens.warning,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // 4B. Offline Build Toolchains & SDKs (6 Compiler Binaries + SDK ZIP Extraction)
        Surface(
            color = tokens.surface2,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, tokens.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.Build,
                            contentDescription = "Offline Build Toolchains",
                            tint = tokens.primaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Offline Toolchains & SDKs",
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    StatusPill("ARM64 SDK", tokens.success)
                }

                Text(
                    "Termux ARM64 SDK + AndroidIDE/AIDE SDK ZIP extraction & 6 compiler binaries",
                    color = tokens.textMuted,
                    fontSize = 10.sp
                )

                Button(
                    onClick = onExtractSdkZipClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.primaryAccent,
                        contentColor = tokens.onPrimaryAccent
                    ),
                    modifier = Modifier.fillMaxWidth().height(40.dp).testTag("extract_sdk_zip_btn")
                ) {
                    Icon(Icons.Filled.Archive, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Extract AndroidIDE / AIDE SDK ZIP", fontSize = 11.sp)
                }

                uiState.compilerBinaries.forEach { bin ->
                    Surface(
                        color = tokens.surface1,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, tokens.border)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bin.name,
                                    color = tokens.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = JetBrainsMonoFontFamily
                                )
                                Text(
                                    text = "${bin.version} • ${bin.detail}",
                                    color = tokens.textMuted,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            StatusPill(bin.arch, tokens.primaryAccent)
                        }
                    }
                }
            }
        }

        // 4C. Searchable Gradle & App Dependencies with One-Tap Injection
        Surface(
            color = tokens.surface2,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, tokens.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Gradle & App Dependencies",
                    color = tokens.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = pkgQuery,
                    onValueChange = { pkgQuery = it },
                    placeholder = { Text("Search packages (Compose, Retrofit, Room)...", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search packages", modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("gradle_package_search_input")
                )

                filteredPackages.take(6).forEach { pkg ->
                    GradlePackageRow(
                        pkg = pkg,
                        onInject = { viewModel.injectGradlePackage(pkg) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GradlePackageRow(
    pkg: GradlePackageItem,
    onInject: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    Surface(
        color = tokens.surface1,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, tokens.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                Text(
                    text = "${pkg.title} (${pkg.version})",
                    color = tokens.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = pkg.coordinate,
                    color = tokens.primaryAccent,
                    fontSize = 10.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(
                onClick = onInject,
                modifier = Modifier.height(34.dp).testTag("inject_pkg_${pkg.title.replace(" ", "_").lowercase()}")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Inject ${pkg.title}", modifier = Modifier.size(13.dp))
                Spacer(Modifier.size(3.dp))
                Text("Inject", fontSize = 10.sp)
            }
        }
    }
}
