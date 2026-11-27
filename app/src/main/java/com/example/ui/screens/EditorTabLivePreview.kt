package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PreviewNetworkProfile
import com.example.ui.components.DeviceFramePreviewCanvas
import com.example.ui.components.LdroidSurfaceCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LdroidThemes
import com.example.ui.theme.LocalLdroidTokens
import com.example.viewmodel.LdroidUiState
import com.example.viewmodel.LdroidViewModel

/**
 * Tab 5 (▶): Live Preview — Interactive 360×800 device frame preview with
 * dark/light mode, layout bounds, 1.0x & RTL scaling, pixel eyedropper,
 * WCAG contrast & 5G/3G/Offline simulators, animation speed (1x/0.5x),
 * design overlay compare, and live onClick() runtime logs.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorTabLivePreview(
    uiState: LdroidUiState,
    viewModel: LdroidViewModel,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLdroidTokens.current
    val previewNodes = uiState.codePreviewState.lastGoodNodes.ifEmpty { uiState.visualNodes }
    val contrast = LdroidThemes.contrastRatio(
        if (uiState.previewDarkMode) Color.White else Color(0xFF0E1726),
        if (uiState.previewDarkMode) tokens.deviceScreenBg else Color(0xFFF6F8FC)
    )
    val animDuration = if (uiState.previewSlowAnimation) 900 else 250
    val animatedOverlayAlpha by animateFloatAsState(
        targetValue = uiState.previewDesignOverlayAlpha,
        animationSpec = tween(durationMillis = animDuration),
        label = "design_overlay_alpha"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("tab_live_preview"),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Inspector Toolbar Row
        item {
            Surface(
                color = tokens.surface1,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, tokens.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill("360×800 Viewport", tokens.primaryAccent)
                        StatusPill(
                            "WCAG ${String.format("%.1f", contrast)}:1",
                            if (contrast >= 4.5) tokens.success else tokens.warning
                        )
                        IconButton(
                            onClick = { viewModel.togglePreviewOrientation() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Filled.RotateRight, contentDescription = "Rotate Preview", tint = tokens.primaryAccent)
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = !uiState.previewDarkMode,
                            onClick = { viewModel.togglePreviewDarkMode() },
                            leadingIcon = { Icon(Icons.Filled.Brightness6, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text(if (uiState.previewDarkMode) "Dark" else "Light", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = uiState.previewLayoutBounds,
                            onClick = { viewModel.togglePreviewLayoutBounds() },
                            leadingIcon = { Icon(Icons.Filled.GridOn, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("Bounds", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = uiState.previewRtl,
                            onClick = { viewModel.togglePreviewRtl() },
                            label = { Text(if (uiState.previewRtl) "RTL" else "LTR", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = uiState.previewFontScale > 1.05f,
                            onClick = {
                                val nextScale = if (uiState.previewFontScale > 1.05f) 1.0f else 1.25f
                                viewModel.setPreviewFontScale(nextScale)
                            },
                            label = { Text("${uiState.previewFontScale}x", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = uiState.previewSlowAnimation,
                            onClick = { viewModel.togglePreviewSlowAnimation() },
                            leadingIcon = { Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text(if (uiState.previewSlowAnimation) "0.5x" else "1x", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = uiState.previewEyedropperActive,
                            onClick = { viewModel.togglePreviewEyedropper() },
                            leadingIcon = { Icon(Icons.Filled.Colorize, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("Eyedropper", fontSize = 10.sp) }
                        )
                    }

                    // Network Profile Simulator (5G / 3G / Offline)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.NetworkCheck, contentDescription = null, tint = tokens.textMuted, modifier = Modifier.size(15.dp))
                        PreviewNetworkProfile.entries.forEach { net ->
                            FilterChip(
                                selected = uiState.selectedPreviewNetwork == net,
                                onClick = {
                                    viewModel.setPreviewNetworkProfile(net)
                                    viewModel.logPreviewInteraction("Network", "Simulated ${net.label} connectivity")
                                },
                                label = { Text(net.label, fontSize = 10.sp) }
                            )
                        }
                    }

                    // Design Overlay Compare Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Layers, contentDescription = null, tint = tokens.textMuted, modifier = Modifier.size(15.dp))
                        Text("Overlay", color = tokens.textMuted, fontSize = 10.sp)
                        Slider(
                            value = uiState.previewDesignOverlayAlpha,
                            onValueChange = { viewModel.setPreviewDesignOverlayAlpha(it) },
                            modifier = Modifier.weight(1f)
                        )
                        Text("${(uiState.previewDesignOverlayAlpha * 100).toInt()}%", color = tokens.primaryAccent, fontSize = 10.sp)
                    }

                    if (uiState.previewEyedropperActive) {
                        Surface(
                            color = tokens.surface2,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, tokens.primaryAccent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Eyedropper Active • Canvas #0D1117 • Card #161B22 • Accent #A8C7FA • WCAG ${String.format("%.2f", contrast)}:1",
                                color = tokens.primaryAccent,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Interactive 360x800 Device Frame Canvas with RTL / Dark-Light / Bounds / Design Overlay
        item {
            CompositionLocalProvider(
                LocalLayoutDirection provides if (uiState.previewRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (uiState.previewLayoutBounds) {
                                Modifier.border(1.dp, tokens.secondaryAccent.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                            } else Modifier
                        )
                ) {
                    DeviceFramePreviewCanvas(
                        title = uiState.activeProject?.name ?: "Your Project",
                        nodes = previewNodes,
                        profile = uiState.selectedDeviceProfile,
                        landscape = uiState.previewLandscape,
                        fontScale = uiState.previewFontScale,
                        isStale = uiState.codePreviewState.isStale,
                        staleError = uiState.codePreviewState.errorMessage,
                        darkModeOverride = uiState.previewDarkMode,
                        onNodeClick = { node ->
                            viewModel.logPreviewInteraction(
                                "${node.type.displayName} onClick()",
                                "Interacted with '${node.label}' (${uiState.selectedPreviewNetwork.label})"
                            )
                        }
                    )

                    if (animatedOverlayAlpha > 0.02f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .alpha(animatedOverlayAlpha * 0.55f)
                                .background(tokens.primaryAccent.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
                                .border(2.dp, tokens.warning, RoundedCornerShape(22.dp))
                        ) {
                            Text(
                                text = "FIGMA DESIGN OVERLAY (${(animatedOverlayAlpha * 100).toInt()}%)",
                                color = tokens.warning,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Live onClick() Event Logs
        item {
            LdroidSurfaceCard {
                Text(
                    "Live onClick() Event Logs (${uiState.previewRuntimeLogs.size})",
                    color = tokens.primaryAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                if (uiState.previewRuntimeLogs.isEmpty()) {
                    Text(
                        "Tap any widget inside the 360×800 device frame above to inspect live onClick() events.",
                        color = tokens.textMuted,
                        fontSize = 11.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        uiState.previewRuntimeLogs.take(6).forEach { evt ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {},
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "[${evt.timestamp}] ${evt.componentLabel}: ${evt.detail}",
                                    color = tokens.textPrimary,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
