package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens

val SUPPORTED_PROJECT_EXTENSIONS = setOf(
    "kt", "kts", "java", "xml", "json", "gradle", "dart",
    "tsx", "ts", "jsx", "js", "html", "htm", "css", "zip",
    "md", "txt", "properties", "toml", "yaml", "yml"
)

fun getUriDisplayName(context: Context, uri: Uri): String {
    if (uri.scheme == "content") {
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "file"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateProjectItemDialog(
    packageName: String,
    initialTab: Int = 0,
    onDismiss: () -> Unit,
    onCreateFile: (relativePath: String, content: String) -> Unit,
    onCreateFolder: (relativePath: String) -> Unit
) {
    val tokens = LocalLdroidTokens.current
    val pkgPath = packageName.replace('.', '/')
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) } // 0 = File, 1 = Folder

    // File tab state
    var selectedTemplate by remember { mutableStateOf(FileScaffoldTemplate.COMPOSE_SCREEN) }
    var filePathInput by remember { mutableStateOf("app/src/main/java/$pkgPath/${selectedTemplate.defaultFileName}") }

    // Folder tab state
    var folderPathInput by remember { mutableStateOf("app/src/main/java/$pkgPath/components") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (selectedTab == 0) Icons.Filled.Add else Icons.Filled.Folder,
                    contentDescription = null,
                    tint = tokens.primaryAccent
                )
                Text(
                    text = if (selectedTab == 0) "Create New File" else "Create New Folder",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tab switcher
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = tokens.surface2,
                    contentColor = tokens.primaryAccent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("📄 New File", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("📁 New Folder", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                if (selectedTab == 0) {
                    Text(
                        text = "Choose template / language:",
                        color = tokens.textMuted,
                        fontSize = 11.sp
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(FileScaffoldTemplate.entries) { tpl ->
                            FilterChip(
                                selected = selectedTemplate == tpl,
                                onClick = {
                                    selectedTemplate = tpl
                                    filePathInput = when (tpl) {
                                        FileScaffoldTemplate.XML_LAYOUT -> "app/src/main/res/layout/${tpl.defaultFileName}"
                                        FileScaffoldTemplate.FLUTTER_WIDGET -> "lib/${tpl.defaultFileName}"
                                        FileScaffoldTemplate.REACT_NATIVE_TSX -> "src/${tpl.defaultFileName}"
                                        FileScaffoldTemplate.HTML_PAGE, FileScaffoldTemplate.CSS_STYLES -> tpl.defaultFileName
                                        else -> "app/src/main/java/$pkgPath/${tpl.defaultFileName}"
                                    }
                                },
                                label = { Text(tpl.label, fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    OutlinedTextField(
                        value = filePathInput,
                        onValueChange = { filePathInput = it },
                        label = { Text("Relative File Path", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_file_path_input")
                    )

                    Text(
                        text = "Supported: .kt, .java, .xml, .json, .gradle.kts, .dart, .tsx, .html, .css",
                        color = tokens.textMuted,
                        fontSize = 10.sp,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                } else {
                    Text(
                        text = "Enter relative folder path to create:",
                        color = tokens.textMuted,
                        fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = folderPathInput,
                        onValueChange = { folderPathInput = it },
                        label = { Text("Folder Path", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_folder_path_input")
                    )
                    Text(
                        text = "Example: app/src/main/java/$pkgPath/models",
                        color = tokens.textMuted,
                        fontSize = 10.sp,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTab == 0) {
                        val clean = filePathInput.trim()
                        val content = generateFileScaffoldContent(selectedTemplate, clean, packageName)
                        onCreateFile(clean, content)
                    } else {
                        val clean = folderPathInput.trim()
                        onCreateFolder(clean)
                    }
                },
                enabled = if (selectedTab == 0) filePathInput.isNotBlank() else folderPathInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = tokens.primaryAccent,
                    contentColor = tokens.onPrimaryAccent
                ),
                modifier = Modifier.testTag("confirm_create_btn")
            ) {
                Text(if (selectedTab == 0) "Create File" else "Create Folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ImportProjectFilesDialog(
    onDismiss: () -> Unit,
    onPickFiles: () -> Unit,
    onPickFolder: () -> Unit
) {
    val tokens = LocalLdroidTokens.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.FileUpload,
                    contentDescription = null,
                    tint = tokens.primaryAccent
                )
                Text(
                    text = "Import Project Files",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select files or directories to import into active workspace:",
                    color = tokens.textMuted,
                    fontSize = 12.sp
                )

                Surface(
                    color = tokens.surface2,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, tokens.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickFiles() }
                        .testTag("import_files_choice_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.InsertDriveFile,
                            contentDescription = null,
                            tint = tokens.primaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Code Files or .ZIP Archive",
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.textPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Accepts .kt, .java, .xml, .json, .gradle, .dart, .tsx, .html, .css, .zip",
                                color = tokens.textMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Surface(
                    color = tokens.surface2,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, tokens.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickFolder() }
                        .testTag("import_folder_choice_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = null,
                            tint = tokens.secondaryAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Project Folder Tree",
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.textPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Imports directory structure via Android Document Tree",
                                color = tokens.textMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = tokens.border)

                Text(
                    text = "Supported file formats:\n.kt, .java, .xml, .json, .gradle, .dart, .tsx, .html, .css, .zip",
                    fontFamily = JetBrainsMonoFontFamily,
                    color = tokens.textMuted,
                    fontSize = 10.sp
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
