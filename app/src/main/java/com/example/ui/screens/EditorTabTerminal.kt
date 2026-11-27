package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.terminal.TerminalSession
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel
import java.io.File

/**
 * Tab 4 (>_): Terminal — On-device ARM64 Linux shell (aapt2, kotlinc, javac, d8/r8, zipalign, apksigner),
 * multi-session tabs (1: build-shell, 2: logcat), clickable ✨ Ask AI to Fix error pills,
 * and tactile extra keys (ESC, TAB, CTRL, ALT, { }, ↑, ↓).
 */
@Composable
fun EditorTabTerminal(
    uiState: LdroidUiState,
    sessions: List<TerminalSession>,
    viewModel: LdroidViewModel,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    val activeSession = sessions.firstOrNull { it.id == uiState.activeTerminalSessionId } ?: sessions.firstOrNull()
    val lines = activeSession?.lines.orEmpty()
    val listState = rememberLazyListState()
    val logcatListState = rememberLazyListState()
    val projectFolder = uiState.activeProject?.name?.ifBlank { null }
        ?: activeSession?.workingDir?.let { File(it).name }
        ?: "Your_Project"
    var historyCursor by remember(activeSession?.id) { mutableIntStateOf(-1) }
    var ctrlArmed by remember { mutableStateOf(false) }

    LaunchedEffect(lines.size, uiState.activeTerminalSubTab) {
        if (uiState.activeTerminalSubTab == 0 && lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.lastIndex)
        }
    }

    Surface(
        color = tokens.terminalBackground,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, tokens.border),
        modifier = modifier
            .fillMaxSize()
            .testTag("termux_terminal_surface")
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            // Session Sub-Tabs: 1: build-shell | 2: logcat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = uiState.activeTerminalSubTab == 0,
                        onClick = { viewModel.swapTerminalSubTab(0) },
                        label = { Text("1: build-shell", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily) },
                        modifier = Modifier.testTag("terminal_tab_shell")
                    )
                    FilterChip(
                        selected = uiState.activeTerminalSubTab == 1,
                        onClick = { viewModel.swapTerminalSubTab(1) },
                        label = { Text("2: logcat", fontSize = 10.sp, fontFamily = JetBrainsMonoFontFamily) },
                        modifier = Modifier.testTag("terminal_tab_logcat")
                    )
                }
                if (uiState.activeTerminalSubTab == 0) {
                    activeSession?.lastExitCode?.let { code ->
                        StatusPill("exit $code", if (code == 0) tokens.success else tokens.error)
                    }
                } else {
                    IconButton(onClick = { viewModel.refreshLogcat() }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Logcat", tint = tokens.primaryAccent, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Text(
                text = "u0_a1@ldroid:~/LDROID_Projects/$projectFolder $",
                color = tokens.primaryAccent,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(color = tokens.border.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))

            if (uiState.activeTerminalSubTab == 0) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(lines, key = { _, line -> line.id }) { _, line ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = line.text,
                                color = when {
                                    line.isError -> tokens.terminalStderr
                                    line.isPrompt -> tokens.terminalPrompt
                                    else -> tokens.terminalStdout
                                },
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp
                            )
                            val looksLikeError = line.isError || line.text.contains("ERROR", true) || line.text.contains("FAILED", true)
                            if (looksLikeError) {
                                Surface(
                                    color = tokens.error.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, tokens.error.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clickable { viewModel.askAiToFixTerminalLine(line.text) }
                                        .testTag("terminal_ask_ai_fix_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = tokens.error, modifier = Modifier.size(12.dp))
                                        Text("✨ Ask AI to Fix", color = tokens.error, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = logcatListState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    val logs = uiState.logcatLines.ifEmpty {
                        listOf("I/LDROID: Live ARM64 logcat stream ready. Tap Refresh to poll system ring buffer.")
                    }
                    itemsIndexed(logs) { _, entry ->
                        val isErr = entry.contains(" E/", true) || entry.contains("Exception", true)
                        Text(
                            text = entry,
                            color = if (isErr) tokens.error else tokens.terminalStdout,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            // Tactile Extra Keys Bar: ESC, TAB, CTRL, ALT, { }, ↑, ↓
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val extraKeys = listOf("ESC", "TAB", "CTRL", "ALT", "{ }", "↑", "↓")
                extraKeys.forEach { key ->
                    val highlighted = key == "CTRL" && ctrlArmed
                    Surface(
                        color = if (highlighted) tokens.primaryAccent else tokens.surface1,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (highlighted) tokens.primaryAccent else tokens.border),
                        modifier = Modifier
                            .clickable {
                                when (key) {
                                    "ESC" -> {
                                        ctrlArmed = false
                                        viewModel.cancelTerminalCommand()
                                        viewModel.updateUniversalInput("")
                                    }
                                    "TAB" -> {
                                        val cur = uiState.universalInputText
                                        viewModel.updateUniversalInput(if (cur.isEmpty()) "ls " else "$cur    ")
                                    }
                                    "CTRL" -> {
                                        ctrlArmed = !ctrlArmed
                                        if (ctrlArmed) viewModel.postMessage("CTRL armed")
                                    }
                                    "ALT" -> viewModel.runTerminalCommand("pkg list")
                                    "{ }" -> viewModel.updateUniversalInput(uiState.universalInputText + "{}")
                                    "↑" -> {
                                        val history = activeSession?.commandHistory.orEmpty()
                                        if (history.isNotEmpty()) {
                                            val nextIdx = if (historyCursor < 0) history.lastIndex else (historyCursor - 1).coerceAtLeast(0)
                                            historyCursor = nextIdx
                                            viewModel.updateUniversalInput(history[nextIdx])
                                        }
                                    }
                                    "↓" -> {
                                        val history = activeSession?.commandHistory.orEmpty()
                                        if (history.isNotEmpty() && historyCursor >= 0) {
                                            if (historyCursor >= history.lastIndex) {
                                                historyCursor = -1
                                                viewModel.updateUniversalInput("")
                                            } else {
                                                historyCursor++
                                                viewModel.updateUniversalInput(history[historyCursor])
                                            }
                                        }
                                    }
                                }
                            }
                            .testTag("termux_key_$key")
                    ) {
                        Text(
                            text = key,
                            color = if (highlighted) tokens.onPrimaryAccent else tokens.textPrimary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
