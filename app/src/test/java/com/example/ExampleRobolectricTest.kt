package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

        // Fixed 3-tab bottom bar visible on Home
        composeTestRule.onNodeWithTag("nav_home").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_max_studio").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_google").assertIsDisplayed()

        // Open 82% sidebar drawer and verify all 4 expandable accordion sections
        composeTestRule.onNodeWithTag("home_menu_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Guest Developer").assertIsDisplayed()
        composeTestRule.onNodeWithTag("drawer_section_settings").assertExists()
        composeTestRule.onNodeWithTag("drawer_section_dev_lab").assertExists()
        composeTestRule.onNodeWithTag("drawer_section_workspace_lab").assertExists()
        composeTestRule.onNodeWithTag("drawer_section_library").assertExists()
    }

    @Test
    fun verifyMaxStudioFullScreenHidesBottomNavAndSwitchesThreeModes() {
        composeTestRule.waitForIdle()

        // Open Max Studio from Home Open Editor CTA
        composeTestRule.onNodeWithTag("home_open_editor_btn").performClick()
        composeTestRule.waitForIdle()

        // Main bottom navigation bar must be hidden inside Max Studio
        composeTestRule.onNodeWithTag("nav_home").assertDoesNotExist()
        composeTestRule.onNodeWithTag("max_studio_back_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("max_studio_build_apk_btn").assertIsDisplayed()

        // Persistent bottom controls visible with default AI UI Screen active
        composeTestRule.onNodeWithTag("max_mode_code_eg").assertIsDisplayed()
        composeTestRule.onNodeWithTag("max_mode_ai_ui").assertIsDisplayed()
        composeTestRule.onNodeWithTag("max_mode_terminal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("universal_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("universal_ai_enhance_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("universal_run_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_ui_screen_list").assertIsDisplayed()

        // Switch to Code E/G
        composeTestRule.onNodeWithTag("max_mode_code_eg").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("code_eg_save_btn").assertIsDisplayed()

        // Switch to Terminal and verify Termux shell + extra keys
        composeTestRule.onNodeWithTag("max_mode_terminal").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("termux_terminal_surface").assertIsDisplayed()
        composeTestRule.onNodeWithTag("termux_key_ESC").assertIsDisplayed()

        // Back returns cleanly to Home and restores 3-tab bottom bar
        composeTestRule.onNodeWithTag("max_studio_back_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_home").assertIsDisplayed()
    }

    @Test
    fun verifyGoogleDeveloperBrowserBookmarksAndControls() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_google").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("browser_url_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("browser_mic_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("browser_dev_menu_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bookmark_github").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bookmark_android_docs").assertIsDisplayed()
    }
}
