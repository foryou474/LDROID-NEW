package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.components.LdroidSurfaceCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel
import java.net.URLEncoder

private data class DevBookmark(val label: String, val url: String)

private val DEV_BOOKMARKS = listOf(
    DevBookmark("GitHub", "https://github.com"),
    DevBookmark("Android Docs", "https://developer.android.com"),
    DevBookmark("Firebase", "https://console.firebase.google.com"),
    DevBookmark("Google AI Studio", "https://aistudio.google.com"),
    DevBookmark("StackOverflow", "https://stackoverflow.com")
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleBrowserScreen(
    uiState: LdroidUiState,
    viewModel: LdroidViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokens = LocalLdroidTokens.current

    var urlInput by remember { mutableStateOf("https://developer.android.com") }
    var currentUrl by remember { mutableStateOf("https://developer.android.com") }
    var pageProgress by remember { mutableIntStateOf(100) }
    var desktopMode by remember { mutableStateOf(false) }
    var showDevMenu by remember { mutableStateOf(false) }
    var showConsoleDialog by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun resolveInputToUrl(raw: String): String {
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("https://") || trimmed.startsWith("http://") -> trimmed
            trimmed.contains('.') && !trimmed.contains(' ') -> "https://$trimmed"
            else -> "https://www.google.com/search?q=${URLEncoder.encode(trimmed, "UTF-8")}"
        }
    }

    fun navigateTo(raw: String) {
        if (raw.isBlank()) return
        val resolved = resolveInputToUrl(raw)
        urlInput = resolved
        currentUrl = resolved
        webViewRef?.loadUrl(resolved)
    }

    val voiceSearchLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spoken.isNullOrEmpty()) {
                navigateTo(spoken)
            }
        }
    }

    fun launchBrowserVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search Android docs or URL...")
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            runCatching { voiceSearchLauncher.launch(intent) }
                .onFailure { viewModel.postMessage("Voice search unavailable: ${it.message}") }
        } else {
            viewModel.postMessage("Voice search requires a speech recognizer on device.")
        }
    }

    val webViewInstance = remember(context) {
        runCatching {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (!url.isNullOrBlank()) {
                            urlInput = url
                            currentUrl = url
                        }
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        pageProgress = newProgress
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let { msg ->
                            viewModel.appendBrowserConsoleLog(
                                level = msg.messageLevel().name,
                                message = msg.message(),
                                source = "${msg.sourceId()}:${msg.lineNumber()}"
                            )
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }
                }
                loadUrl(currentUrl)
            }
        }.getOrNull().also { webViewRef = it }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
    ) {
        // Smart URL / Search Bar + Controls + Developer Menu
        Surface(
            color = tokens.surface1,
            border = BorderStroke(1.dp, tokens.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { webViewRef?.goBack() },
                        modifier = Modifier.size(44.dp).testTag("browser_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = tokens.primaryAccent)
                    }
                    IconButton(
                        onClick = { webViewRef?.goForward() },
                        modifier = Modifier.size(44.dp).testTag("browser_forward_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = tokens.textMuted)
                    }
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        placeholder = { Text("Search Google or enter URL...", fontSize = 12.sp) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = tokens.primaryAccent, modifier = Modifier.size(16.dp))
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { launchBrowserVoice() },
                                modifier = Modifier.size(36.dp).testTag("browser_mic_btn")
                            ) {
                                Icon(Icons.Filled.Mic, contentDescription = "Voice Search", tint = tokens.secondaryAccent, modifier = Modifier.size(16.dp))
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { navigateTo(urlInput) }),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).testTag("browser_url_input")
                    )
                    IconButton(
                        onClick = { webViewRef?.reload() ?: navigateTo(urlInput) },
                        modifier = Modifier.size(44.dp).testTag("browser_refresh_btn")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = tokens.primaryAccent)
                    }
                    Box {
                        IconButton(
                            onClick = { showDevMenu = true },
                            modifier = Modifier.size(44.dp).testTag("browser_dev_menu_btn")
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Developer Browser Menu", tint = tokens.textPrimary)
                        }
                        DropdownMenu(
                            expanded = showDevMenu,
                            onDismissRequest = { showDevMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (desktopMode) "Switch to Mobile View" else "Switch to Desktop View") },
                                leadingIcon = {
                                    Icon(
                                        if (desktopMode) Icons.Filled.PhoneAndroid else Icons.Filled.Computer,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    desktopMode = !desktopMode
                                    webViewRef?.settings?.apply {
                                        userAgentString = if (desktopMode) {
                                            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/125.0 Safari/537.36"
                                        } else {
                                            null
                                        }
                                        useWideViewPort = desktopMode
                                        loadWithOverviewMode = desktopMode
                                    }
                                    webViewRef?.reload()
                                    viewModel.postMessage(if (desktopMode) "Desktop View enabled" else "Mobile View enabled")
                                    showDevMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Inspect Console Logs (${uiState.browserConsoleLogs.size})") },
                                leadingIcon = { Icon(Icons.Filled.BugReport, contentDescription = null) },
                                onClick = {
                                    showDevMenu = false
                                    showConsoleDialog = true
                                }
                            )
                        }
                    }
                }

                // Quick Developer Bookmarks
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DEV_BOOKMARKS.forEach { bm ->
                        val active = currentUrl.startsWith(bm.url)
                        Surface(
                            color = if (active) tokens.primaryAccent.copy(alpha = 0.16f) else tokens.surface2,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (active) tokens.primaryAccent else tokens.border),
                            modifier = Modifier
                                .clickable { navigateTo(bm.url) }
                                .testTag("bookmark_${bm.label.lowercase().replace(" ", "_")}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Bookmark,
                                    contentDescription = null,
                                    tint = if (active) tokens.primaryAccent else tokens.textMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = bm.label,
                                    color = if (active) tokens.primaryAccent else tokens.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        if (pageProgress in 1..99) {
            LinearProgressIndicator(
                progress = { pageProgress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = tokens.primaryAccent
            )
        }

        // WebView Canvas (with graceful fallback when sandbox/Robolectric lacks Chromium WebView)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (webViewInstance != null) {
                AndroidView(
                    factory = { webViewInstance },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LdroidSurfaceCard(accentBorder = true) {
                        StatusPill(
                            label = if (desktopMode) "Desktop View" else "Mobile View",
                            color = tokens.primaryAccent
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = "In-App Developer Browser Ready",
                            color = tokens.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Active URL: $currentUrl",
                            color = tokens.primaryAccent,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (showConsoleDialog) {
        AlertDialog(
            onDismissRequest = { showConsoleDialog = false },
            title = { Text("Browser Console Logs (${uiState.browserConsoleLogs.size})") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (uiState.browserConsoleLogs.isEmpty()) {
                        item {
                            Text("No console logs captured yet.", color = tokens.textMuted, fontSize = 12.sp)
                        }
                    } else {
                        items(uiState.browserConsoleLogs, key = { it.id }) { entry ->
                            Surface(
                                color = tokens.surface2,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, tokens.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        "[${entry.timestamp}] ${entry.level} • ${entry.source}",
                                        color = tokens.primaryAccent,
                                        fontSize = 10.sp,
                                        fontFamily = JetBrainsMonoFontFamily
                                    )
                                    Text(entry.message, color = tokens.textPrimary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showConsoleDialog = false }) { Text("Close") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearBrowserConsoleLogs() }) { Text("Clear Logs") }
            }
        )
    }
}
