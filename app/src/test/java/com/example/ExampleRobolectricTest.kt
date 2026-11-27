package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.analysis.ApkAnalyzer
import com.example.ui.theme.LdroidThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verifyAppIdentityAndPackage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("LDROID IDE", context.getString(R.string.app_name))
        assertEquals("com.ldroid.ide", context.packageName)
    }

    @Test
    fun themeJsonExportAndImportRoundTrip() {
        val json = LdroidThemes.exportToJson(LdroidThemes.DesertLuxe)
        val parsed = LdroidThemes.validateAndParseThemeJson(json)
        assertTrue(parsed.isSuccess)
        assertEquals("Desert Luxe", parsed.getOrThrow().name)
    }

    @Test
    fun verifyApkAnalyzerInspectsZipStructureAndSha256() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempApk = File(context.cacheDir, "sample_test.apk")
        ZipOutputStream(tempApk.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("classes.dex"))
            zos.write("dex\n035\u0000dummy".toByteArray())
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("lib/arm64-v8a/libnative.so"))
            zos.write(ByteArray(64))
            zos.closeEntry()
        }
        val report = ApkAnalyzer(context).analyzeFile(tempApk, "sample_test.apk")
        tempApk.delete()

        assertEquals("APK", report.artifactType)
        assertEquals(1, report.dexCount)
        assertTrue(report.abis.contains("arm64-v8a"))
        assertEquals(64, report.sha256.length)
    }

    @Test
    fun verifyHomeScreenThreeTabBarAndSidebarAccordions() {
        composeTestRule.waitForIdle()
        // Home screen top header & hero
        composeTestRule.onNodeWithText("LDROID IDE").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_guest_mode_pill").assertIsDisplayed()
        composeTestRule.onNodeWithText("Android Studio in Your Pocket").assertIsDisplayed()
        composeTestRule.onNodeWithTag("active_workspace_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("PulseApp").assertIsDisplayed()

        // Scroll Home list to verify My Projects + New button & Recent Activities section
        composeTestRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("home_projects_card"))
        composeTestRule.onNodeWithTag("home_new_project_btn").assertExists()

        // Fixed 3-tab bottom bar visible on Home: Home | Max Studio | Chrome (no Google tab)
        composeTestRule.onNodeWithTag("nav_home").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_max_studio").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_chrome").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_google").assertDoesNotExist()

        // Open sidebar drawer and verify locked 11-category accordion architecture (collapsed by default)
        composeTestRule.onNodeWithTag("home_menu_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Guest Developer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Offline Android Architect").assertIsDisplayed()

        // Pinned bottom linked accounts strip
        composeTestRule.onNodeWithTag("drawer_linked_accounts_strip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("linked_google_chip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("linked_github_chip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("linked_firebase_chip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("drawer_accounts_manage_btn").assertIsDisplayed()

        // All 11 accordion categories present and collapsed by default
        for (i in 1..11) {
            composeTestRule.onNodeWithTag("drawer_cat_$i").assertExists()
        }
        composeTestRule.onNodeWithText("AES-256-GCM BYOK Vault").assertDoesNotExist()

        // Expanding category 1 reveals its inner settings cleanly
        composeTestRule.onNodeWithTag("drawer_cat_1").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("AES-256-GCM BYOK Vault").assertIsDisplayed()
    }

    @Test
    fun verifyEditorWorkspaceHidesMainNavAndSwitchesAllSevenTabs() {
        composeTestRule.waitForIdle()

        // Open Editor Workspace from Home CTA
        composeTestRule.onNodeWithTag("home_open_editor_btn").performClick()
        composeTestRule.waitForIdle()

        // Main bottom navigation bar must be hidden; top header has Menu, Explorer, Quick Run, NO Build APK btn
        composeTestRule.onNodeWithTag("nav_home").assertDoesNotExist()
        composeTestRule.onNodeWithTag("max_studio_menu_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("max_studio_files_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("max_studio_quick_run_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("max_studio_build_apk_btn").assertDoesNotExist()

        // Single-row 7-icon bottom bar + ChatGPT-style universal AI dock
        composeTestRule.onNodeWithTag("max_mode_code_eg").assertIsDisplayed()
        composeTestRule.onNodeWithTag("max_mode_code_live").assertIsDisplayed()
        composeTestRule.onNodeWithTag("max_mode_ai_ui").assertIsDisplayed()
        composeTestRule.onNodeWithTag("max_mode_terminal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("max_mode_live_preview").assertIsDisplayed()
        composeTestRule.onNodeWithTag("max_mode_visual_builder").assertIsDisplayed()
        composeTestRule.onNodeWithTag("max_mode_build_apk").assertIsDisplayed()
        composeTestRule.onNodeWithTag("universal_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("universal_plus_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("universal_mic_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("universal_run_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_model_chips_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_studio_feed").assertIsDisplayed()

        // Switch to Tab 1: Code EG
        composeTestRule.onNodeWithTag("max_mode_code_eg").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("code_eg_save_btn").assertIsDisplayed()

        // Switch to Tab 2: Code Live Preview
        composeTestRule.onNodeWithTag("max_mode_code_live").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tab_code_live_preview").assertIsDisplayed()
        composeTestRule.onNodeWithTag("xml_clean_format_btn").assertIsDisplayed()

        // Switch to Tab 4: Terminal and verify ARM64 shell + extra keys
        composeTestRule.onNodeWithTag("max_mode_terminal").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("termux_terminal_surface").assertIsDisplayed()
        composeTestRule.onNodeWithTag("termux_key_ESC").assertIsDisplayed()

        // Switch to Tab 5: Live Preview
        composeTestRule.onNodeWithTag("max_mode_live_preview").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tab_live_preview").assertIsDisplayed()

        // Switch to Tab 6: Visual Builder
        composeTestRule.onNodeWithTag("max_mode_visual_builder").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_ui_screen_list").assertIsDisplayed()

        // Switch to Tab 7: Build & APK
        composeTestRule.onNodeWithTag("max_mode_build_apk").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tab_build_apk").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_build_apk")
            .performScrollToNode(hasTestTag("build_apk_compile_btn"))
        composeTestRule.onNodeWithTag("build_apk_compile_btn").assertExists()

        // System back returns cleanly to Home and restores 3-tab bottom bar
        composeTestRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_home").assertIsDisplayed()
    }

    @Test
    fun verifyChromeMobileBrowserSearchUrlShortcutsAndTabSwitcher() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_chrome").performClick()
        composeTestRule.waitForIdle()

        // Search / URL omnibox, shortcuts/bookmarks, and tab switcher
        composeTestRule.onNodeWithTag("browser_url_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("browser_mic_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("browser_dev_menu_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bookmark_github").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bookmark_android_docs").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chrome_shortcuts_grid").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("shortcut_github").performScrollTo().assertIsDisplayed()

        // Open tab switcher and verify multi-tab controls
        composeTestRule.onNodeWithTag("chrome_tab_switcher_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Tabs (1)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Done").performClick()
        composeTestRule.waitForIdle()
    }
}
