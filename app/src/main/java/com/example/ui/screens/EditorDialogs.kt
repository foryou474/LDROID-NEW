package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.LdroidSettingsState
import com.example.model.FindInFilesMatch
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import kotlin.math.roundToInt

data class EditorCommandItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val onRun: () -> Unit
)

@Composable
fun SinglePathDialog(
    title: String,
    initial: String,
    label: String = "Path or name",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label, fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(value.trim()) },
                enabled = value.isNotBlank()
            ) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun GoToLineDialog(
    maxLines: Int,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit
) {
    val tokens = LocalLdroidTokens.current
    var lineInput by remember { mutableStateOf("1") }
    val parsed = lineInput.toIntOrNull()
    val valid = parsed != null && parsed in 1..maxLines.coerceAtLeast(1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go to Line") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Enter line number (1 to ${maxLines.coerceAtLeast(1)})",
                    color = tokens.textMuted,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = lineInput,
                    onValueChange = { input -> lineInput = input.filter { it.isDigit() } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        if (valid && parsed != null) {
                            onJump(parsed)
                            onDismiss()
                        }
                    }),
                    modifier = Modifier.fillMaxWidth().testTag("goto_line_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsed != null) {
                        onJump(parsed)
                        onDismiss()
                    }
                },
                enabled = valid
            ) { Text("Jump") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun FindReplaceInFilesDialog(
    initialReplaceMode: Boolean,
    query: String,
    results: List<FindInFilesMatch>,
    onSearch: (String, Boolean) -> Unit,
    onReplaceAllInFiles: (String, String, Boolean) -> Unit,
    onJumpMatch: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    var searchText by remember { mutableStateOf(query) }
    var replaceText by remember { mutableStateOf("") }
    var matchCase by remember { mutableStateOf(false) }
    var showReplace by remember { mutableStateOf(initialReplaceMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (showReplace) "Replace in Files" else "Find in Files", fontSize = 16.sp)
                FilterChip(
                    selected = matchCase,
                    onClick = {
                        matchCase = !matchCase
                        onSearch(searchText, !matchCase)
                    },
                    label = { Text("Aa", fontSize = 11.sp) }
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Search across workspace...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("find_in_files_input")
                    )
                    IconButton(
                        onClick = { onSearch(searchText, !matchCase) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "Search Workspace", tint = tokens.primaryAccent)
                    }
                }
                if (showReplace) {
                    OutlinedTextField(
                        value = replaceText,
                        onValueChange = { replaceText = it },
                        placeholder = { Text("Replacement text...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("replace_in_files_input")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${results.size} match(es)", color = tokens.textMuted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = { showReplace = !showReplace }) {
                            Text(if (showReplace) "Hide Replace" else "Replace...", fontSize = 11.sp)
                        }
                        if (showReplace) {
                            Button(
                                onClick = { onReplaceAllInFiles(searchText, replaceText, !matchCase) },
                                enabled = searchText.isNotEmpty() && results.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryAccent, contentColor = tokens.onPrimaryAccent)
                            ) {
                                Text("Replace All", fontSize = 11.sp)
                            }
                        }
                    }
                }
                HorizontalDivider(color = tokens.divider)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (results.isEmpty()) {
                        item {
                            Text(
                                text = if (query.isBlank()) "Enter text and tap Search." else "No matches found.",
                                color = tokens.textMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    } else {
                        items(results) { match ->
                            Surface(
                                color = tokens.surface2,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, tokens.border),
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onJumpMatch(match.relativePath, match.lineNumber)
                                    onDismiss()
                                }
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        "${match.relativePath}:${match.lineNumber}",
                                        color = tokens.primaryAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        match.lineText,
                                        color = tokens.textPrimary,
                                        fontSize = 11.sp,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun CommandPaletteDialog(
    commands: List<EditorCommandItem>,
    onDismiss: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    var filter by remember { mutableStateOf("") }
    val filtered = remember(commands, filter) {
        if (filter.isBlank()) commands
        else commands.filter {
            it.title.contains(filter, ignoreCase = true) ||
                it.subtitle.contains(filter, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Command Palette", fontSize = 16.sp)
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Command Palette", modifier = Modifier.size(16.dp))
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    placeholder = { Text("Type a command...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("command_palette_input")
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered, key = { it.id }) { cmd ->
                        Surface(
                            color = if (cmd.enabled) tokens.surface2 else tokens.surface1,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, tokens.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = cmd.enabled) {
                                    onDismiss()
                                    cmd.onRun()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    cmd.icon,
                                    contentDescription = null,
                                    tint = if (cmd.enabled) tokens.primaryAccent else tokens.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        cmd.title,
                                        color = if (cmd.enabled) tokens.textPrimary else tokens.textMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(cmd.subtitle, color = tokens.textMuted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun QuickEditorSettingsDialog(
    settings: LdroidSettingsState,
    onApply: (fontSize: Int, tabWidth: Int, autoSave: Boolean, lineNumbers: Boolean, wordWrap: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    var fontSize by remember(settings.editorFontSizeSp) { mutableFloatStateOf(settings.editorFontSizeSp.toFloat()) }
    var tabWidth by remember(settings.tabWidth) { mutableIntStateOf(settings.tabWidth) }
    var autoSave by remember(settings.autoSaveEnabled) { mutableStateOf(settings.autoSaveEnabled) }
    var showLineNumbers by remember(settings.showLineNumbers) { mutableStateOf(settings.showLineNumbers) }
    var wordWrap by remember(settings.wordWrap) { mutableStateOf(settings.wordWrap) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editor Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Font Size: ${fontSize.roundToInt()}sp", color = tokens.textPrimary, fontSize = 12.sp)
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 10f..22f,
                    steps = 11
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tab Width", color = tokens.textPrimary, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 4).forEach { w ->
                            FilterChip(
                                selected = tabWidth == w,
                                onClick = { tabWidth = w },
                                label = { Text("$w spaces", fontSize = 11.sp) }
                            )
                        }
                    }
                }
                SettingToggleRow("Auto-Save on Edit", autoSave) { autoSave = it }
                SettingToggleRow("Line Numbers", showLineNumbers) { showLineNumbers = it }
                SettingToggleRow("Word Wrap", wordWrap) { wordWrap = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(fontSize.roundToInt(), tabWidth, autoSave, showLineNumbers, wordWrap)
                    onDismiss()
                }
            ) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val tokens = LocalLdroidTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = tokens.textPrimary, fontSize = 12.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun UnsavedChangesDialog(
    dirtyPaths: List<String>,
    onSaveAndProceed: () -> Unit,
    onDiscardAndProceed: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unsaved Changes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Save changes before closing ${dirtyPaths.size} modified file(s)?",
                    color = tokens.textPrimary,
                    fontSize = 13.sp
                )
                dirtyPaths.take(4).forEach { path ->
                    Text(
                        "• $path",
                        color = tokens.warning,
                        fontSize = 11.sp,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
                if (dirtyPaths.size > 4) {
                    Text("• +${dirtyPaths.size - 4} more...", color = tokens.textMuted, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = onSaveAndProceed) { Text("Save & Close") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                OutlinedButton(onClick = onDiscardAndProceed) { Text("Discard", color = tokens.error) }
            }
        }
    )
}

enum class FileScaffoldTemplate(val label: String, val defaultFileName: String) {
    EMPTY("Empty", "NewFile.kt"),
    COMPOSE_SCREEN("Compose Screen", "HomeFeatureScreen.kt"),
    JAVA_ACTIVITY("Java Activity", "FeatureActivity.java"),
    XML_LAYOUT("XML Layout", "activity_feature.xml"),
    FLUTTER_WIDGET("Flutter Widget", "feature_widget.dart"),
    REACT_NATIVE_TSX("React Native TSX", "FeatureCard.tsx"),
    HTML_PAGE("HTML5 Page", "page.html"),
    CSS_STYLES("CSS Stylesheet", "styles.css"),
    VIEW_MODEL("ViewModel", "FeatureViewModel.kt"),
    REPOSITORY("Repository", "FeatureRepository.kt")
}

@Composable
fun NewFileTemplateDialog(
    packageName: String,
    onDismiss: () -> Unit,
    onCreate: (relativePath: String, content: String) -> Unit
) {
    val tokens = LocalLdroidTokens.current
    val pkgPath = packageName.replace('.', '/')
    var selectedTemplate by remember { mutableStateOf(FileScaffoldTemplate.COMPOSE_SCREEN) }
    var pathInput by remember { mutableStateOf("app/src/main/java/$pkgPath/${selectedTemplate.defaultFileName}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New File & Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Choose scaffolding template:", color = tokens.textMuted, fontSize = 11.sp)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(FileScaffoldTemplate.entries) { tpl ->
                        FilterChip(
                            selected = selectedTemplate == tpl,
                            onClick = {
                                selectedTemplate = tpl
                                pathInput = when (tpl) {
                                    FileScaffoldTemplate.XML_LAYOUT -> "app/src/main/res/layout/${tpl.defaultFileName}"
                                    FileScaffoldTemplate.FLUTTER_WIDGET -> "lib/${tpl.defaultFileName}"
                                    FileScaffoldTemplate.REACT_NATIVE_TSX -> "src/${tpl.defaultFileName}"
                                    FileScaffoldTemplate.HTML_PAGE, FileScaffoldTemplate.CSS_STYLES -> tpl.defaultFileName
                                    else -> "app/src/main/java/$pkgPath/${tpl.defaultFileName}"
                                }
                            },
                            label = { Text(tpl.label, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = pathInput,
                    onValueChange = { pathInput = it },
                    label = { Text("Relative file path", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = pathInput.trim()
                    val generated = generateFileScaffoldContent(selectedTemplate, clean, packageName)
                    onCreate(clean, generated)
                },
                enabled = pathInput.isNotBlank()
            ) { Text("Create File") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

internal fun generateFileScaffoldContent(
    template: FileScaffoldTemplate,
    relativePath: String,
    packageName: String
): String {
    val className = relativePath.substringAfterLast('/').substringBeforeLast('.')
        .replace(Regex("""[^A-Za-z0-9_]"""), "")
        .ifBlank { "GeneratedComponent" }
    return when (template) {
        FileScaffoldTemplate.EMPTY -> ""
        FileScaffoldTemplate.COMPOSE_SCREEN -> """
            package $packageName

            import androidx.compose.foundation.layout.*
            import androidx.compose.material3.*
            import androidx.compose.runtime.*
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun $className(modifier: Modifier = Modifier) {
                Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
                    Text(text = "$className", style = MaterialTheme.typography.headlineSmall)
                }
            }
        """.trimIndent() + "\n"
        FileScaffoldTemplate.JAVA_ACTIVITY -> """
            package $packageName;

            import android.os.Bundle;
            import android.widget.TextView;
            import androidx.appcompat.app.AppCompatActivity;

            public class $className extends AppCompatActivity {
                @Override
                protected void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);
                    TextView label = new TextView(this);
                    label.setText("$className");
                    setContentView(label);
                }
            }
        """.trimIndent() + "\n"
        FileScaffoldTemplate.FLUTTER_WIDGET -> """
            import 'package:flutter/material.dart';

            class $className extends StatefulWidget {
              const $className({super.key});

              @override
              State<$className> createState() => _${className}State();
            }

            class _${className}State extends State<$className> {
              int _counter = 0;

              @override
              Widget build(BuildContext context) {
                return Column(
                  children: [
                    Text('Count: ${'$'}_counter'),
                    ElevatedButton(
                      onPressed: () => setState(() => _counter++),
                      child: const Text('Tap +1'),
                    ),
                  ],
                );
              }
            }
        """.trimIndent() + "\n"
        FileScaffoldTemplate.REACT_NATIVE_TSX -> """
            import React, { useState } from 'react';
            import { View, Text, TouchableOpacity } from 'react-native';

            export default function $className() {
              const [count, setCount] = useState<number>(0);
              return (
                <View>
                  <Text>$className: {count}</Text>
                  <TouchableOpacity onPress={() => setCount(c => c + 1)}>
                    <Text>Tap +1</Text>
                  </TouchableOpacity>
                </View>
              );
            }
        """.trimIndent() + "\n"
        FileScaffoldTemplate.HTML_PAGE -> """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8" />
              <title>$className</title>
              <link rel="stylesheet" href="styles.css" />
            </head>
            <body>
              <main class="card">
                <h1>$className</h1>
                <p>Built with M.A.X. Studio</p>
              </main>
            </body>
            </html>
        """.trimIndent() + "\n"
        FileScaffoldTemplate.CSS_STYLES -> """
            :root {
              --accent: #00e5ff;
              --bg: #070b14;
            }
            body {
              background: var(--bg);
              color: #e6f1ff;
              font-family: system-ui, sans-serif;
            }
            .card {
              border: 1px solid var(--accent);
              border-radius: 12px;
              padding: 16px;
            }
        """.trimIndent() + "\n"
        FileScaffoldTemplate.VIEW_MODEL -> """
            package $packageName

            import androidx.lifecycle.ViewModel
            import kotlinx.coroutines.flow.MutableStateFlow
            import kotlinx.coroutines.flow.StateFlow
            import kotlinx.coroutines.flow.asStateFlow

            class $className : ViewModel() {
                private val _status = MutableStateFlow("Ready")
                val status: StateFlow<String> = _status.asStateFlow()
            }
        """.trimIndent() + "\n"
        FileScaffoldTemplate.REPOSITORY -> """
            package $packageName

            interface $className {
                suspend fun refresh(): Result<Unit>
            }
        """.trimIndent() + "\n"
        FileScaffoldTemplate.XML_LAYOUT -> """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:orientation="vertical"
                android:padding="16dp">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="$className" />
            </LinearLayout>
        """.trimIndent() + "\n"
    }
}

@Composable
fun ActiveFileDiffDialog(
    relativePath: String,
    diffLines: List<com.example.model.FileDiffLine>,
    onDismiss: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unsaved Diff: ${relativePath.substringAfterLast('/')}", fontSize = 15.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (diffLines.none { it.marker == '+' || it.marker == '-' }) {
                    Text("No unsaved modifications compared to disk.", color = tokens.textMuted, fontSize = 12.sp)
                } else {
                    com.example.ui.components.DiffLinesBlock(diffLines.take(120))
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

