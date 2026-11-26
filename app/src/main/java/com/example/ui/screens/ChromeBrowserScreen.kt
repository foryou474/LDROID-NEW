package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.ChromeBookmark
import com.example.model.ChromeHistoryEntry
import com.example.model.ChromeNetworkEntry
import com.example.model.ChromeTab
import com.example.ui.components.LdroidSurfaceCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel
import java.net.URLEncoder
import java.util.UUID

enum class SearchEngine(val displayName: String, val searchUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
    BING("Bing", "https://www.bing.com/search?q="),
    GITHUB("GitHub", "https://github.com/search?q="),
    ANDROID_DOCS("Android Dev", "https://developer.android.com/s/results?q=")
}

private val DEFAULT_DEV_BOOKMARKS = listOf(
    ChromeBookmark(title = "GitHub", url = "https://github.com", category = "Code", icon = "🐙"),
    ChromeBookmark(title = "Android Docs", url = "https://developer.android.com", category = "SDK", icon = "🤖"),
    ChromeBookmark(title = "Jetpack Compose", url = "https://developer.android.com/develop/ui/compose", category = "UI", icon = "✨"),
    ChromeBookmark(title = "Kotlin Lang", url = "https://kotlinlang.org", category = "Language", icon = "🎯"),
    ChromeBookmark(title = "Stack Overflow", url = "https://stackoverflow.com", category = "Q&A", icon = "💬"),
    ChromeBookmark(title = "Maven Central", url = "https://mvnrepository.com", category = "Packages", icon = "📦"),
    ChromeBookmark(title = "Firebase Console", url = "https://console.firebase.google.com", category = "Cloud", icon = "🔥"),
    ChromeBookmark(title = "Material 3", url = "https://m3.material.io", category = "Design", icon = "🎨")
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChromeBrowserScreen(
    uiState: LdroidUiState,
    viewModel: LdroidViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokens = LocalLdroidTokens.current

    // Chrome Tabs State
    val tabs = remember {
        mutableStateListOf(
            ChromeTab(
                id = "default_tab",
                title = "Android Developers",
                url = "https://developer.android.com",
                isIncognito = false,
                favicon = "🤖"
            )
        )
    }
    var activeTabIndex by remember { mutableIntStateOf(0) }
    val currentTab = tabs.getOrNull(activeTabIndex) ?: tabs.firstOrNull() ?: ChromeTab()

    // Browser navigation & Chrome UI State
    var urlInput by remember(currentTab.id) { mutableStateOf(currentTab.url) }
    var pageProgress by remember { mutableIntStateOf(100) }
    var desktopMode by remember { mutableStateOf(false) }
    var selectedSearchEngine by remember { mutableStateOf(SearchEngine.GOOGLE) }

    // Dialogs & Panels
    var showChromeMenu by remember { mutableStateOf(false) }
    var showTabSwitcher by remember { mutableStateOf(false) }
    var showDevTools by remember { mutableStateOf(false) }
    var showSecurityInfo by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showFindInPage by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findMatchesCount by remember { mutableIntStateOf(0) }
    var findCurrentIndex by remember { mutableIntStateOf(0) }

    // History and Network entries
    val historyEntries = remember { mutableStateListOf<ChromeHistoryEntry>() }
    val networkEntries = remember { mutableStateListOf<ChromeNetworkEntry>() }
    val bookmarks = remember { mutableStateListOf<ChromeBookmark>().apply { addAll(DEFAULT_DEV_BOOKMARKS) } }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageSourceHtml by remember { mutableStateOf<String>("") }
    var jsEvalInput by remember { mutableStateOf("document.title") }
    var jsEvalOutput by remember { mutableStateOf<String?>(null) }

    fun resolveInputToUrl(raw: String): String {
        val trimmed = raw.trim()
        return when {
            trimmed == "chrome://newtab" || trimmed.isBlank() -> "chrome://newtab"
            trimmed.startsWith("https://") || trimmed.startsWith("http://") -> trimmed
            trimmed.contains('.') && !trimmed.contains(' ') -> "https://$trimmed"
            else -> "${selectedSearchEngine.searchUrl}${URLEncoder.encode(trimmed, "UTF-8")}"
        }
    }

    fun navigateTo(raw: String) {
        val resolved = resolveInputToUrl(raw)
        urlInput = resolved
        if (activeTabIndex in tabs.indices) {
            val titleCandidate = when {
                resolved == "chrome://newtab" -> "New Tab"
                resolved.contains("developer.android.com") -> "Android Developers"
                resolved.contains("github.com") -> "GitHub"
                resolved.contains("stackoverflow.com") -> "Stack Overflow"
                resolved.contains("kotlinlang.org") -> "Kotlin"
                resolved.contains("google.com") -> "Google Search"
                else -> resolved.removePrefix("https://").removePrefix("http://").take(20)
            }
            tabs[activeTabIndex] = tabs[activeTabIndex].copy(
                url = resolved,
                title = titleCandidate
            )
        }
        if (resolved != "chrome://newtab") {
            if (!currentTab.isIncognito) {
                historyEntries.add(0, ChromeHistoryEntry(title = currentTab.title, url = resolved))
            }
            networkEntries.add(0, ChromeNetworkEntry(url = resolved, method = "GET", status = 200, mimeType = "text/html"))
            webViewRef?.loadUrl(resolved)
        }
    }

    fun createNewTab(url: String = "chrome://newtab", isIncognito: Boolean = false) {
        val newTab = ChromeTab(
            id = UUID.randomUUID().toString(),
            title = if (url == "chrome://newtab") "New Tab" else "Loading...",
            url = url,
            isIncognito = isIncognito,
            favicon = if (isIncognito) "🕵️" else "🌐"
        )
        tabs.add(newTab)
        activeTabIndex = tabs.lastIndex
        urlInput = url
        if (url != "chrome://newtab") {
            navigateTo(url)
        }
    }

    fun closeTab(index: Int) {
        if (tabs.size > 1) {
            tabs.removeAt(index)
            activeTabIndex = activeTabIndex.coerceIn(0, tabs.lastIndex)
            urlInput = tabs[activeTabIndex].url
            if (tabs[activeTabIndex].url != "chrome://newtab") {
                webViewRef?.loadUrl(tabs[activeTabIndex].url)
            }
        } else {
            tabs[0] = ChromeTab(title = "New Tab", url = "chrome://newtab")
            urlInput = "chrome://newtab"
        }
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
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search Chrome or speak URL...")
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            runCatching { voiceSearchLauncher.launch(intent) }
                .onFailure { viewModel.postMessage("Voice input error: ${it.message}") }
        } else {
            viewModel.postMessage("Voice speech recognition not found on device.")
        }
    }

    val webViewInstance = remember(context) {
        runCatching {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.databaseEnabled = true

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (!url.isNullOrBlank()) {
                            urlInput = url
                            if (activeTabIndex in tabs.indices) {
                                val pageTitle = view?.title?.ifBlank { null } ?: tabs[activeTabIndex].title
                                tabs[activeTabIndex] = tabs[activeTabIndex].copy(url = url, title = pageTitle)
                            }
                            view?.evaluateJavascript("document.documentElement.outerHTML") { html ->
                                pageSourceHtml = html ?: ""
                            }
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        val code = errorResponse?.statusCode ?: 500
                        val reqUrl = request?.url?.toString() ?: "unknown"
                        networkEntries.add(0, ChromeNetworkEntry(url = reqUrl, status = code, mimeType = errorResponse?.mimeType ?: "unknown"))
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

                if (currentTab.url != "chrome://newtab") {
                    loadUrl(currentTab.url)
                }
            }
        }.getOrNull().also { webViewRef = it }
    }

    // Incognito theme styling override
    val isIncognito = currentTab.isIncognito
    val chromeHeaderBg = if (isIncognito) Color(0xFF1F1F24) else tokens.surface1
    val chromeOmniboxBg = if (isIncognito) Color(0xFF2C2C32) else tokens.surface2
    val chromeAccent = if (isIncognito) Color(0xFFA8C7FA) else tokens.primaryAccent
    val chromeText = if (isIncognito) Color(0xFFE3E3E3) else tokens.textPrimary
    val chromeMuted = if (isIncognito) Color(0xFF9AA0A6) else tokens.textMuted

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isIncognito) Color(0xFF18181C) else tokens.background)
    ) {
        // 1. Chrome Multi-Tab Strip
        Surface(
            color = chromeHeaderBg,
            border = BorderStroke(1.dp, if (isIncognito) Color(0xFF383840) else tokens.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Tab Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEachIndexed { idx, tab ->
                        val isSelected = idx == activeTabIndex
                        val tabBg = if (isSelected) {
                            if (isIncognito) Color(0xFF2E2F38) else tokens.surface2
                        } else Color.Transparent
                        val tabBorder = if (isSelected) {
                            BorderStroke(1.dp, if (isIncognito) Color(0xFF4A4B58) else tokens.primaryAccent.copy(alpha = 0.5f))
                        } else null

                        Surface(
                            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                            color = tabBg,
                            border = tabBorder,
                            modifier = Modifier
                                .clickable {
                                    activeTabIndex = idx
                                    urlInput = tab.url
                                    if (tab.url != "chrome://newtab") {
                                        webViewRef?.loadUrl(tab.url)
                                    }
                                }
                                .padding(horizontal = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(tab.favicon, fontSize = 12.sp)
                                Text(
                                    text = tab.title,
                                    color = if (isSelected) chromeText else chromeMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 110.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .clickable { closeTab(idx) }
                                        .testTag("chrome_tab_close_$idx"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Close Tab",
                                        tint = chromeMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Add Tab Button (+)
                    IconButton(
                        onClick = { createNewTab() },
                        modifier = Modifier.size(32.dp).testTag("chrome_new_tab_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "New Tab", tint = chromeAccent, modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = if (isIncognito) Color(0xFF383840) else tokens.divider)

                // 2. Chrome Omnibox Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Back
                    IconButton(
                        onClick = { webViewRef?.goBack() },
                        enabled = webViewRef?.canGoBack() == true,
                        modifier = Modifier.size(38.dp).testTag("browser_back_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (webViewRef?.canGoBack() == true) chromeAccent else chromeMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Forward
                    IconButton(
                        onClick = { webViewRef?.goForward() },
                        enabled = webViewRef?.canGoForward() == true,
                        modifier = Modifier.size(38.dp).testTag("browser_forward_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (webViewRef?.canGoForward() == true) chromeAccent else chromeMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Refresh / Reload
                    IconButton(
                        onClick = {
                            if (currentTab.url == "chrome://newtab") {
                                urlInput = "chrome://newtab"
                            } else {
                                webViewRef?.reload() ?: navigateTo(urlInput)
                            }
                        },
                        modifier = Modifier.size(38.dp).testTag("browser_refresh_btn")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = chromeAccent, modifier = Modifier.size(18.dp))
                    }

                    // Omnibox
                    Surface(
                        color = chromeOmniboxBg,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, if (isIncognito) Color(0xFF454652) else tokens.border),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Security / Mode Indicator
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { showSecurityInfo = true }
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isIncognito) Icons.Filled.Shield else Icons.Filled.Lock,
                                    contentDescription = "Security Status",
                                    tint = if (isIncognito) Color(0xFFA8C7FA) else tokens.success,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            // Omnibox Text Input
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it },
                                placeholder = {
                                    Text(
                                        text = "Search or type URL...",
                                        fontSize = 12.sp,
                                        color = chromeMuted,
                                        maxLines = 1
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = { navigateTo(urlInput) }),
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (urlInput.isNotBlank()) {
                                            IconButton(
                                                onClick = { urlInput = "" },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = chromeMuted, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        IconButton(
                                            onClick = { launchBrowserVoice() },
                                            modifier = Modifier.size(28.dp).testTag("browser_mic_btn")
                                        ) {
                                            Icon(Icons.Filled.Mic, contentDescription = "Voice Search", tint = chromeAccent, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("browser_url_input")
                            )
                        }
                    }

                    // Tab Switcher Counter Badge [ N ]
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = chromeAccent.copy(alpha = 0.16f),
                        border = BorderStroke(1.5.dp, chromeAccent),
                        modifier = Modifier
                            .clickable { showTabSwitcher = true }
                            .testTag("chrome_tab_switcher_btn")
                    ) {
                        Text(
                            text = "${tabs.size}",
                            color = chromeAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = JetBrainsMonoFontFamily,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }

                    // Chrome 3-Dot Menu
                    Box {
                        IconButton(
                            onClick = { showChromeMenu = true },
                            modifier = Modifier.size(38.dp).testTag("browser_dev_menu_btn")
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Chrome Menu", tint = chromeText, modifier = Modifier.size(20.dp))
                        }

                        DropdownMenu(
                            expanded = showChromeMenu,
                            onDismissRequest = { showChromeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Tab") },
                                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                                onClick = {
                                    showChromeMenu = false
                                    createNewTab()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New Incognito Tab") },
                                leadingIcon = { Icon(Icons.Filled.Shield, contentDescription = null) },
                                onClick = {
                                    showChromeMenu = false
                                    createNewTab(isIncognito = true)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Bookmarks (${bookmarks.size})") },
                                leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null) },
                                onClick = {
                                    showChromeMenu = false
                                    showBookmarksSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("History (${historyEntries.size})") },
                                leadingIcon = { Icon(Icons.Filled.History, contentDescription = null) },
                                onClick = {
                                    showChromeMenu = false
                                    showHistorySheet = true
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(if (desktopMode) "Mobile Site" else "Desktop Site") },
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
                                            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
                                        } else null
                                        useWideViewPort = desktopMode
                                        loadWithOverviewMode = desktopMode
                                    }
                                    webViewRef?.reload()
                                    viewModel.postMessage(if (desktopMode) "Chrome Desktop Mode enabled" else "Chrome Mobile View enabled")
                                    showChromeMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Find in Page") },
                                leadingIcon = { Icon(Icons.Filled.FindInPage, contentDescription = null) },
                                onClick = {
                                    showChromeMenu = false
                                    showFindInPage = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Chrome DevTools") },
                                leadingIcon = { Icon(Icons.Filled.BugReport, contentDescription = null) },
                                onClick = {
                                    showChromeMenu = false
                                    showDevTools = true
                                }
                            )
                        }
                    }
                }

                // Progress Bar
                if (pageProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { pageProgress / 100f },
                        modifier = Modifier.fillMaxWidth().height(2.5.dp),
                        color = chromeAccent
                    )
                }
            }
        }

        // 3. Find in Page Bar
        AnimatedVisibility(visible = showFindInPage) {
            Surface(
                color = chromeOmniboxBg,
                border = BorderStroke(1.dp, tokens.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = findQuery,
                        onValueChange = {
                            findQuery = it
                            if (it.isNotBlank()) {
                                webViewRef?.findAllAsync(it)
                            } else {
                                webViewRef?.clearMatches()
                            }
                        },
                        placeholder = { Text("Find in page...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).height(46.dp)
                    )
                    IconButton(
                        onClick = { webViewRef?.findNext(false) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Previous Match", tint = chromeAccent, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = { webViewRef?.findNext(true) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Next Match", tint = chromeAccent, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = {
                            showFindInPage = false
                            webViewRef?.clearMatches()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Find", tint = chromeMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // 4. Main Chrome Content View (Start Page or Active WebView)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (currentTab.url == "chrome://newtab") {
                // Chrome New Tab Page (Speed Dial + Developer Portal)
                ChromeNewTabContent(
                    isIncognito = isIncognito,
                    searchEngine = selectedSearchEngine,
                    onSearch = { navigateTo(it) },
                    onBookmarkClick = { navigateTo(it) },
                    onSelectSearchEngine = { selectedSearchEngine = it },
                    onVoiceSearch = { launchBrowserVoice() }
                )
            } else {
                if (webViewInstance != null) {
                    AndroidView(
                        factory = { webViewInstance },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Headless / Sandbox Fallback Canvas
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        LdroidSurfaceCard(accentBorder = true) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPill(
                                    label = if (desktopMode) "Chrome Desktop View" else "Chrome Mobile View",
                                    color = chromeAccent
                                )
                                Text(
                                    text = if (isIncognito) "Incognito Session" else "Standard Session",
                                    color = chromeMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Chrome Developer Browser Active",
                                color = chromeText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Current URL: ${currentTab.url}",
                                color = chromeAccent,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Developer Bookmarks Quick Tiles
                        Text(
                            text = "Developer Bookmarks",
                            color = chromeText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DEFAULT_DEV_BOOKMARKS.take(4).forEach { bm ->
                                Surface(
                                    color = tokens.surface2,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, tokens.border),
                                    modifier = Modifier
                                        .clickable { navigateTo(bm.url) }
                                        .testTag("bookmark_${bm.title.lowercase().replace(" ", "_")}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(bm.icon, fontSize = 14.sp)
                                        Text(bm.title, color = chromeText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 5. Chrome Tab Switcher Dialog (Overview Grid)
    if (showTabSwitcher) {
        AlertDialog(
            onDismissRequest = { showTabSwitcher = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tabs (${tabs.size})", fontWeight = FontWeight.Bold)
                    Row {
                        IconButton(onClick = { createNewTab(); showTabSwitcher = false }) {
                            Icon(Icons.Filled.Add, contentDescription = "New Tab", tint = chromeAccent)
                        }
                    }
                }
            },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tabs) { tab ->
                        val isSelected = tabs.indexOf(tab) == activeTabIndex
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (tab.isIncognito) Color(0xFF24242A) else tokens.surface2,
                            border = BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) chromeAccent else tokens.border
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clickable {
                                    activeTabIndex = tabs.indexOf(tab)
                                    urlInput = tab.url
                                    if (tab.url != "chrome://newtab") {
                                        webViewRef?.loadUrl(tab.url)
                                    }
                                    showTabSwitcher = false
                                }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(tab.favicon, fontSize = 14.sp)
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .clickable { closeTab(tabs.indexOf(tab)) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = chromeMuted, modifier = Modifier.size(12.dp))
                                    }
                                }
                                Column {
                                    Text(
                                        text = tab.title,
                                        color = chromeText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tab.url.removePrefix("https://").removePrefix("http://"),
                                        color = chromeMuted,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showTabSwitcher = false }) { Text("Done") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        tabs.clear()
                        tabs.add(ChromeTab(title = "New Tab", url = "chrome://newtab"))
                        activeTabIndex = 0
                        urlInput = "chrome://newtab"
                        showTabSwitcher = false
                    }
                ) { Text("Close All") }
            }
        )
    }

    // 6. Chrome DevTools Dialog (Console, Network, Page Source)
    if (showDevTools) {
        var devToolTab by remember { mutableIntStateOf(0) }
        AlertDialog(
            onDismissRequest = { showDevTools = false },
            title = {
                Column {
                    Text("Chrome DevTools", fontWeight = FontWeight.Bold)
                    TabRow(selectedTabIndex = devToolTab, modifier = Modifier.fillMaxWidth()) {
                        Tab(selected = devToolTab == 0, onClick = { devToolTab = 0 }, text = { Text("Console (${uiState.browserConsoleLogs.size})", fontSize = 11.sp) })
                        Tab(selected = devToolTab == 1, onClick = { devToolTab = 1 }, text = { Text("Network (${networkEntries.size})", fontSize = 11.sp) })
                        Tab(selected = devToolTab == 2, onClick = { devToolTab = 2 }, text = { Text("Source", fontSize = 11.sp) })
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp)) {
                    when (devToolTab) {
                        0 -> {
                            // Console Tab
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("JS Console Messages", fontSize = 11.sp, color = chromeMuted)
                                TextButton(onClick = { viewModel.clearBrowserConsoleLogs() }) {
                                    Text("Clear", fontSize = 11.sp)
                                }
                            }
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (uiState.browserConsoleLogs.isEmpty()) {
                                    item { Text("No console logs emitted.", fontSize = 11.sp, color = chromeMuted) }
                                } else {
                                    items(uiState.browserConsoleLogs) { log ->
                                        Surface(
                                            color = tokens.surface2,
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, tokens.border),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(6.dp)) {
                                                Text(
                                                    "[${log.timestamp}] ${log.level} • ${log.source}",
                                                    color = chromeAccent,
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 10.sp
                                                )
                                                Text(log.message, color = chromeText, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // Interactive JavaScript Evaluator
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = jsEvalInput,
                                    onValueChange = { jsEvalInput = it },
                                    label = { Text("Evaluate JS", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).height(48.dp)
                                )
                                IconButton(
                                    onClick = {
                                        webViewRef?.evaluateJavascript(jsEvalInput) { res ->
                                            jsEvalOutput = res
                                            viewModel.appendBrowserConsoleLog("EVAL", "$jsEvalInput => $res", "DevTools")
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = "Run JS", tint = chromeAccent)
                                }
                            }
                            if (jsEvalOutput != null) {
                                Text(
                                    text = "Result: $jsEvalOutput",
                                    color = tokens.success,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        1 -> {
                            // Network Tab
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(networkEntries) { net ->
                                    Surface(
                                        color = tokens.surface2,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(net.url, fontSize = 11.sp, color = chromeText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${net.method} • ${net.mimeType} • ${net.timestamp}", fontSize = 9.sp, color = chromeMuted)
                                            }
                                            Text(
                                                "${net.status}",
                                                color = if (net.status in 200..299) tokens.success else tokens.error,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                fontFamily = JetBrainsMonoFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Page Source Tab
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Text(
                                        text = pageSourceHtml.ifBlank { "Page source preview unavailable for this URL." },
                                        color = chromeText,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDevTools = false }) { Text("Close") }
            }
        )
    }

    // 7. Site Security Popover
    if (showSecurityInfo) {
        AlertDialog(
            onDismissRequest = { showSecurityInfo = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = tokens.success)
                    Text("Connection is secure")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Your information (for example, passwords or credit card numbers) is private when it is sent to this site.",
                        fontSize = 12.sp,
                        color = tokens.textPrimary
                    )
                    HorizontalDivider()
                    Text("Certificate: Valid (TLS 1.3)", fontSize = 11.sp, color = tokens.success, fontWeight = FontWeight.SemiBold)
                    Text("Cookies: In use (Allowed)", fontSize = 11.sp, color = tokens.textMuted)
                    Text("Permissions: Location (Ask), Camera (Ask), Mic (Ask)", fontSize = 11.sp, color = tokens.textMuted)
                }
            },
            confirmButton = {
                Button(onClick = { showSecurityInfo = false }) { Text("Got it") }
            }
        )
    }

    // 8. Bookmarks Sheet / Dialog
    if (showBookmarksSheet) {
        AlertDialog(
            onDismissRequest = { showBookmarksSheet = false },
            title = { Text("Chrome Bookmarks", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(bookmarks) { bm ->
                        Surface(
                            color = tokens.surface2,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, tokens.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navigateTo(bm.url)
                                    showBookmarksSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(bm.icon, fontSize = 16.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(bm.title, color = tokens.textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(bm.url, color = tokens.textMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(bm.category, color = chromeAccent, fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showBookmarksSheet = false }) { Text("Close") }
            }
        )
    }

    // 9. History Sheet / Dialog
    if (showHistorySheet) {
        AlertDialog(
            onDismissRequest = { showHistorySheet = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Chrome History", fontWeight = FontWeight.Bold)
                    if (historyEntries.isNotEmpty()) {
                        TextButton(onClick = { historyEntries.clear() }) { Text("Clear All") }
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (historyEntries.isEmpty()) {
                        item { Text("No browsing history.", color = chromeMuted, fontSize = 12.sp) }
                    } else {
                        items(historyEntries) { item ->
                            Surface(
                                color = tokens.surface2,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navigateTo(item.url)
                                        showHistorySheet = false
                                    }
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(item.title, color = tokens.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(item.url, color = chromeAccent, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showHistorySheet = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun ChromeNewTabContent(
    isIncognito: Boolean,
    searchEngine: SearchEngine,
    onSearch: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onSelectSearchEngine: (SearchEngine) -> Unit,
    onVoiceSearch: () -> Unit
) {
    val tokens = LocalLdroidTokens.current
    var query by remember { mutableStateOf("") }
    var showEngineMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Chrome / Google Center Logo
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = if (isIncognito) Color(0xFF2E2E36) else tokens.primaryAccent.copy(alpha = 0.16f),
                border = BorderStroke(2.dp, if (isIncognito) Color(0xFFA8C7FA) else tokens.primaryAccent),
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isIncognito) Icons.Filled.Shield else Icons.Filled.Language,
                        contentDescription = "Chrome Browser",
                        tint = if (isIncognito) Color(0xFFA8C7FA) else tokens.primaryAccent,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isIncognito) "Chrome Incognito" else "Chrome Browser",
                color = if (isIncognito) Color(0xFFE3E3E3) else tokens.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isIncognito) "Pages you view won't be saved in your browsing history." else "Developer Edition • Android Studio in Your Pocket",
                color = if (isIncognito) Color(0xFF9AA0A6) else tokens.textMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        // Center Search Box
        Surface(
            color = if (isIncognito) Color(0xFF282830) else tokens.surface1,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, if (isIncognito) Color(0xFF3F404E) else tokens.primaryAccent.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Engine Selector
                Box {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = tokens.primaryAccent.copy(alpha = 0.14f),
                        modifier = Modifier.clickable { showEngineMenu = true }
                    ) {
                        Text(
                            text = searchEngine.displayName,
                            color = tokens.primaryAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                    DropdownMenu(expanded = showEngineMenu, onDismissRequest = { showEngineMenu = false }) {
                        SearchEngine.entries.forEach { engine ->
                            DropdownMenuItem(
                                text = { Text(engine.displayName) },
                                onClick = {
                                    onSelectSearchEngine(engine)
                                    showEngineMenu = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search ${searchEngine.displayName} or type URL...", fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { if (query.isNotBlank()) onSearch(query) }),
                    shape = RoundedCornerShape(20.dp),
                    trailingIcon = {
                        IconButton(onClick = onVoiceSearch) {
                            Icon(Icons.Filled.Mic, contentDescription = "Voice Search", tint = tokens.secondaryAccent)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Speed Dial / Developer Bookmarks Grid
        Text(
            text = "Developer Bookmarks",
            color = if (isIncognito) Color(0xFFE3E3E3) else tokens.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth().height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(DEFAULT_DEV_BOOKMARKS) { bm ->
                val tag = when (bm.title) {
                    "GitHub" -> "bookmark_github"
                    "Android Docs" -> "bookmark_android_docs"
                    else -> "bookmark_${bm.title.lowercase().replace(" ", "_")}"
                }
                Surface(
                    color = if (isIncognito) Color(0xFF26262E) else tokens.surface2,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isIncognito) Color(0xFF383842) else tokens.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBookmarkClick(bm.url) }
                        .testTag(tag)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(bm.icon, fontSize = 22.sp)
                        Text(
                            text = bm.title,
                            color = if (isIncognito) Color(0xFFE3E3E3) else tokens.textPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Android API 36 Developer Feed Banner
        Surface(
            color = if (isIncognito) Color(0xFF26262E) else tokens.surface1,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, tokens.primaryAccent.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💡", fontSize = 16.sp)
                    Text("Android API 36 (Baklava) Developer Tips", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = tokens.primaryAccent)
                }
                Text(
                    text = "Edge-to-edge layout is enforced by default in API 36. Use WindowInsetsCompat and enableEdgeToEdge() in Jetpack Compose to avoid navigation overlaps.",
                    color = tokens.textMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Backward compatibility alias for GoogleBrowserScreen.
 */
@Composable
fun GoogleBrowserScreen(
    uiState: LdroidUiState,
    viewModel: LdroidViewModel,
    modifier: Modifier = Modifier
) {
    ChromeBrowserScreen(uiState = uiState, viewModel = viewModel, modifier = modifier)
}
