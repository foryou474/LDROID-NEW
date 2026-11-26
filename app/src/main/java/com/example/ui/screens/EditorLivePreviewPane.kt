package com.example.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.visual.VisualBuilderCodegen
import com.example.model.FilePreviewKind
import com.example.model.OpenEditorTab
import com.example.model.VisualComponentType
import com.example.model.VisualNode
import com.example.ui.components.DeviceFramePreviewCanvas
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.ui.platform.testTag

@Composable
fun EditorLivePreviewPane(
    activeTab: OpenEditorTab?,
    uiState: LdroidUiState,
    viewModel: LdroidViewModel,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    val path = activeTab?.relativePath?.lowercase().orEmpty()
    val content = activeTab?.content.orEmpty()
    var refreshNonce by remember { mutableIntStateOf(0) }
    var darkModeOverride by remember { mutableStateOf<Boolean?>(null) }
    var isWebLoading by remember { mutableStateOf(false) }
    var showDeviceMenu by remember { mutableStateOf(false) }

    val isHtml = path.endsWith(".html") || path.endsWith(".htm")
    val isCss = path.endsWith(".css")
    val isXml = path.endsWith(".xml")
    val isKotlin = path.endsWith(".kt")
    val isJava = path.endsWith(".java")
    val isDart = path.endsWith(".dart")
    val isReactNative = path.endsWith(".tsx") || path.endsWith(".ts") || path.endsWith(".jsx") || path.endsWith(".js")
    val isConfigPreview = path.endsWith(".gradle.kts") || path.endsWith(".kts") || path.endsWith(".gradle") || path.endsWith(".json")
    val isSupported = activeTab != null && activeTab.previewMode == FilePreviewKind.CODE_TEXT &&
        (isHtml || isCss || isXml || isKotlin || isJava || isDart || isReactNative || isConfigPreview)
    val effectiveDark = darkModeOverride ?: tokens.isDark

    val projectRoot = uiState.activeProject?.rootPath
    val siblingIndexHtml = remember(projectRoot, path, refreshNonce) {
        if (isCss && projectRoot != null) {
            runCatching {
                val candidate = java.io.File(projectRoot, "index.html")
                if (candidate.exists() && candidate.isFile) candidate.readText() else null
            }.getOrNull()
        } else null
    }
    val usesWebView = isHtml || (isCss && siblingIndexHtml != null)

    val statusText: String
    val statusColor: Color
    when {
        activeTab == null -> {
            statusText = "No File Open"
            statusColor = tokens.textMuted
        }
        !isSupported -> {
            statusText = "Preview Unavailable"
            statusColor = tokens.warning
        }
        usesWebView && isWebLoading -> {
            statusText = "Loading WebView"
            statusColor = tokens.info
        }
        (isKotlin || isJava || isDart || isReactNative || isCss || isConfigPreview) && uiState.codePreviewState.isStale -> {
            statusText = "Syntax Error (Stale)"
            statusColor = tokens.warning
        }
        else -> {
            statusText = when {
                isHtml -> "HTML / CSS Live"
                isCss -> "CSS Live"
                isXml -> "XML Layout Live"
                isDart -> "Flutter Live"
                isReactNative -> "React Native Live"
                isJava -> "Java + XML Live"
                isConfigPreview -> "Config Preview"
                else -> "Compose Live"
            }
            statusColor = tokens.success
        }
    }

    LaunchedEffect(path, usesWebView) {
        isWebLoading = usesWebView
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = tokens.surface1,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, tokens.border)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Compact single header: Device Profile dropdown + Status Pill + Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    OutlinedButton(
                        onClick = { showDeviceMenu = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp).testTag("preview_device_dropdown")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Smartphone,
                            contentDescription = null,
                            tint = tokens.primaryAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.size(5.dp))
                        Text(
                            text = uiState.selectedDeviceProfile.name,
                            color = tokens.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.size(2.dp))
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Select Device",
                            tint = tokens.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showDeviceMenu,
                        onDismissRequest = { showDeviceMenu = false }
                    ) {
                        VisualBuilderCodegen.defaultDeviceProfiles.forEach { profile ->
                            val isSelected = uiState.selectedDeviceProfile.name == profile.name
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${profile.name} (${profile.widthDp}x${profile.heightDp}dp)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Filled.Check else Icons.Filled.Smartphone,
                                        contentDescription = null,
                                        tint = if (isSelected) tokens.primaryAccent else tokens.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                onClick = {
                                    viewModel.selectDeviceProfile(profile)
                                    showDeviceMenu = false
                                }
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatusPill(label = statusText, color = statusColor)
                    IconButton(
                        onClick = {
                            refreshNonce++
                            viewModel.refreshActiveCodePreview()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Live Preview", tint = tokens.primaryAccent, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Helper Banner
            Surface(
                color = tokens.primaryAccent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, tokens.primaryAccent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.TouchApp, contentDescription = null, tint = tokens.primaryAccent, modifier = Modifier.size(15.dp))
                    Text(
                        text = "Interactive Preview: Tap elements inside the frame to test behavior",
                        color = tokens.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (usesWebView && isWebLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = tokens.primaryAccent)
            }

            when {
                activeTab == null -> {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Open a Kotlin, Java, XML, Flutter Dart, React Native TSX/JS, or HTML/CSS file to render a real-time preview.",
                            color = tokens.textMuted,
                            fontSize = 12.sp
                        )
                    }
                }
                !isSupported -> {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = tokens.surface2,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, tokens.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Live Preview not available for this file type",
                                    color = tokens.warning,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "M.A.X. Studio renders previews for .kt, .java, .xml, .dart, .tsx/.js, .html/.css, and .gradle/.json files. '${activeTab.relativePath}' is not a previewable source file.",
                                    color = tokens.textMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                usesWebView -> {
                    val renderedHtml = remember(content, siblingIndexHtml, projectRoot, path, refreshNonce) {
                        if (isHtml) {
                            buildInlinedHtml(content, projectRoot, activeTab.relativePath)
                        } else {
                            val base = siblingIndexHtml.orEmpty()
                            val withInlinedScripts = buildInlinedHtml(base, projectRoot, "index.html")
                            "$withInlinedScripts\n<style>\n$content\n</style>"
                        }
                    }
                    val renderKey = "$refreshNonce:${renderedHtml.hashCode()}:$effectiveDark"
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (effectiveDark) Color(0xFF0A0F1A) else Color.White),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isWebLoading = false
                                    }
                                }
                                tag = renderKey
                                isWebLoading = true
                                loadDataWithBaseURL(null, renderedHtml, "text/html", "UTF-8", null)
                            }
                        },
                        update = { webView ->
                            if (webView.tag != renderKey) {
                                webView.tag = renderKey
                                isWebLoading = true
                                webView.loadDataWithBaseURL(null, renderedHtml, "text/html", "UTF-8", null)
                            }
                        }
                    )
                }
                isXml -> {
                    val parsedNodes = remember(content, refreshNonce) { parseXmlLayoutNodes(content) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (parsedNodes.isEmpty()) {
                            Text(
                                text = "No renderable UI tags detected in XML. Add elements like <TextView android:text=\"Hello\" />, <Button />, <EditText />, or <Switch />.",
                                color = tokens.textMuted,
                                fontSize = 12.sp
                            )
                        } else {
                            DeviceFramePreviewCanvas(
                                title = activeTab.relativePath.substringAfterLast('/'),
                                nodes = parsedNodes,
                                profile = uiState.selectedDeviceProfile,
                                landscape = uiState.previewLandscape,
                                fontScale = uiState.previewFontScale,
                                isStale = false,
                                staleError = null,
                                resetNonce = refreshNonce,
                                darkModeOverride = effectiveDark
                            )
                        }
                    }
                }
                else -> {
                    val codePrev = uiState.codePreviewState
                    val linkedXmlNodes = remember(isJava, content, projectRoot, refreshNonce) {
                        if (isJava && projectRoot != null) {
                            resolveJavaLinkedXmlNodes(content, projectRoot)
                        } else emptyList()
                    }
                    val displayNodes = if (linkedXmlNodes.isNotEmpty()) linkedXmlNodes else codePrev.lastGoodNodes
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DeviceFramePreviewCanvas(
                            title = codePrev.lastGoodTitle,
                            nodes = displayNodes,
                            profile = uiState.selectedDeviceProfile,
                            landscape = uiState.previewLandscape,
                            fontScale = uiState.previewFontScale,
                            isStale = codePrev.isStale,
                            staleError = codePrev.errorMessage,
                            resetNonce = refreshNonce,
                            darkModeOverride = effectiveDark
                        )
                    }
                }
            }

            // Bottom Interactive Preview Controls: Reset State, Toggle Dark/Light Theme, Rotate Screen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { refreshNonce++ },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(40.dp).testTag("preview_reset_state_btn")
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp), tint = tokens.primaryAccent)
                    Spacer(Modifier.size(4.dp))
                    Text("Reset State", fontSize = 10.sp, maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = { darkModeOverride = !effectiveDark },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(40.dp).testTag("preview_toggle_theme_btn")
                ) {
                    Icon(
                        if (effectiveDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = tokens.secondaryAccent
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(if (effectiveDark) "Light Theme" else "Dark Theme", fontSize = 10.sp, maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = { viewModel.togglePreviewOrientation() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(40.dp).testTag("preview_rotate_screen_btn")
                ) {
                    Icon(Icons.Filled.ScreenRotation, contentDescription = null, modifier = Modifier.size(14.dp), tint = tokens.info)
                    Spacer(Modifier.size(4.dp))
                    Text(if (uiState.previewLandscape) "Portrait" else "Rotate Screen", fontSize = 10.sp, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

private fun parseXmlLayoutNodes(xml: String): List<VisualNode> {
    val nodes = mutableListOf<VisualNode>()
    val tagRegex = Regex("""<([A-Za-z0-9._]+)([^>]*?)>""")
    val textAttrRegex = Regex("""android:text\s*=\s*"([^"]*)"""")
    val hintAttrRegex = Regex("""android:hint\s*=\s*"([^"]*)"""")
    val titleAttrRegex = Regex("""app:title\s*=\s*"([^"]*)"""")

    val containerTags = setOf(
        "LinearLayout", "FrameLayout", "RelativeLayout", "ConstraintLayout",
        "CoordinatorLayout", "ScrollView", "NestedScrollView", "merge"
    )

    tagRegex.findAll(xml).forEach { match ->
        val simpleTag = match.groupValues[1].substringAfterLast('.')
        val attrs = match.groupValues[2]
        val explicitLabel = textAttrRegex.find(attrs)?.groupValues?.get(1)
            ?: hintAttrRegex.find(attrs)?.groupValues?.get(1)
            ?: titleAttrRegex.find(attrs)?.groupValues?.get(1)

        if (simpleTag in containerTags && explicitLabel == null) return@forEach

        val label = explicitLabel ?: simpleTag
        val compType = when {
            simpleTag.contains("TextView", ignoreCase = true) || simpleTag.contains("Toolbar", ignoreCase = true) -> VisualComponentType.TEXT
            simpleTag.contains("Button", ignoreCase = true) || simpleTag.contains("FloatingActionButton", ignoreCase = true) -> VisualComponentType.BUTTON
            simpleTag.contains("EditText", ignoreCase = true) || simpleTag.contains("TextInput", ignoreCase = true) -> VisualComponentType.TEXT_FIELD
            simpleTag.contains("Switch", ignoreCase = true) || simpleTag.contains("CheckBox", ignoreCase = true) || simpleTag.contains("RadioButton", ignoreCase = true) -> VisualComponentType.SWITCH_CONTROL
            simpleTag.contains("Card", ignoreCase = true) -> VisualComponentType.CARD
            else -> VisualComponentType.COLUMN
        }
        nodes.add(VisualNode(id = java.util.UUID.randomUUID().toString().take(6), type = compType, label = label))
    }
    return nodes
}

private fun buildInlinedHtml(html: String, projectRoot: String?, relativePath: String): String {
    if (projectRoot == null) return html
    val rootDir = java.io.File(projectRoot).canonicalFile
    val baseDir = java.io.File(rootDir, relativePath).parentFile ?: rootDir
    var result = html

    fun readSafeAsset(ref: String): String? {
        val clean = ref.trim().substringBefore('?').substringBefore('#')
        if (clean.startsWith("http://") || clean.startsWith("https://") || clean.startsWith("//")) return null
        val candidates = listOf(java.io.File(baseDir, clean), java.io.File(rootDir, clean))
        val target = candidates.firstOrNull { file ->
            runCatching {
                val canon = file.canonicalFile
                canon.path.startsWith(rootDir.path) && canon.exists() && canon.isFile
            }.getOrDefault(false)
        } ?: return null
        return runCatching { target.readText() }.getOrNull()
    }

    val linkRegex = Regex("""<link[^>]*href\s*=\s*["']([^"']+\.css)["'][^>]*>""", RegexOption.IGNORE_CASE)
    result = linkRegex.replace(result) { match ->
        val css = readSafeAsset(match.groupValues[1])
        if (css != null) "<style>\n$css\n</style>" else match.value
    }

    val scriptRegex = Regex("""<script[^>]*src\s*=\s*["']([^"']+\.js)["'][^>]*>\s*</script>""", RegexOption.IGNORE_CASE)
    result = scriptRegex.replace(result) { match ->
        val js = readSafeAsset(match.groupValues[1])
        if (js != null) "<script>\n$js\n</script>" else match.value
    }
    return result
}

private fun resolveJavaLinkedXmlNodes(javaContent: String, projectRoot: String): List<VisualNode> {
    val layoutName = Regex("""setContentView\s*\(\s*R\.layout\.([A-Za-z0-9_]+)\s*\)""")
        .find(javaContent)?.groupValues?.getOrNull(1) ?: return emptyList()
    val xmlFile = java.io.File(projectRoot, "app/src/main/res/layout/$layoutName.xml")
    if (!xmlFile.exists() || !xmlFile.isFile) return emptyList()
    val xml = runCatching { xmlFile.readText() }.getOrNull() ?: return emptyList()
    return parseXmlLayoutNodes(xml)
}

