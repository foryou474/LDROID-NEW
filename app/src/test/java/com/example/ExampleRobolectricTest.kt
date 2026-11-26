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
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verifyAppIdentityAndPackage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("M.A.X. Studio", context.getString(R.string.app_name))
        assertEquals("com.ldroid.ide", context.packageName)
    }

    @Test
    fun themeJsonExportAndImportRoundTrip() {
        val json = LdroidThemes.exportToJson(LdroidThemes.RoyalSapphire)
        val parsed = LdroidThemes.validateAndParseThemeJson(json)
        assertTrue(parsed.isSuccess)
        assertEquals("Royal Sapphire", parsed.getOrThrow().name)
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
    fun navigatePrimaryTabsAndDeveloperLab() {
        composeTestRule.waitForIdle()
        // App starts on the clean empty Editor screen
        composeTestRule.onNodeWithTag("empty_editor_create_btn").assertIsDisplayed()

        // Navigate to Home and open Developer Lab -> APK/AAB Analyzer and return
        composeTestRule.onNodeWithTag("nav_home").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("M.A.X. STUDIO").assertIsDisplayed()

        composeTestRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("lab_apk_analyzer"))
        composeTestRule.onNodeWithTag("lab_apk_analyzer").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pick_apk_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lab_back_button").performClick()
        composeTestRule.waitForIdle()

        // Navigate across remaining primary tabs
        composeTestRule.onNodeWithTag("nav_projects").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("new_project_btn").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_editor").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("toolbar_files_btn").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_ai_studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_prompt_input").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("view_roadmap_audit_btn"))
        composeTestRule.onNodeWithTag("view_roadmap_audit_btn").assertIsDisplayed()
    }

    @Test
    fun editorToolbarEmptyStateAndTerminalModeWork() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_editor").performClick()
        composeTestRule.waitForIdle()

        // Top action bar
        composeTestRule.onNodeWithTag("editor_ai_pill_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("toolbar_files_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("toolbar_undo_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("toolbar_redo_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("toolbar_save_btn").assertIsDisplayed()

        // Code sub-mode bar & empty-editor CTAs
        composeTestRule.onNodeWithTag("submode_code").assertIsDisplayed()
        composeTestRule.onNodeWithTag("submode_visual_builder").assertIsDisplayed()
        composeTestRule.onNodeWithTag("submode_live_preview").assertIsDisplayed()
        composeTestRule.onNodeWithTag("code_header_create_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("code_header_import_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("empty_editor_create_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("empty_editor_new_file_btn").assertIsDisplayed()

        // Bottom primary mode switcher
        composeTestRule.onNodeWithTag("primary_mode_code").assertIsDisplayed()
        composeTestRule.onNodeWithTag("primary_mode_terminal").assertIsDisplayed()

        // Toggle Compact AI Assistant bar via top-left AI pill
        composeTestRule.onNodeWithTag("editor_ai_pill_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_compact_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_compact_close_btn").performClick()
        composeTestRule.waitForIdle()

        // Open Create modal from empty state and create a template file
        composeTestRule.onNodeWithTag("empty_editor_create_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("create_file_path_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_create_btn").performClick()
        composeTestRule.waitForIdle()

        // Switch to Terminal primary mode and cycle bottom tool tabs
        composeTestRule.onNodeWithTag("primary_mode_terminal").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("terminal_command_input").assertIsDisplayed()

        composeTestRule.onNodeWithText("Build").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Run Build Check").assertIsDisplayed()

        composeTestRule.onNodeWithText("Logcat").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Return to Code primary mode
        composeTestRule.onNodeWithTag("primary_mode_code").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("submode_code").assertIsDisplayed()
    }
}
