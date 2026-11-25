package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        assertEquals("LDROID", context.getString(R.string.app_name))
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
        composeTestRule.onNodeWithText("LDROID IDE").assertIsDisplayed()

        // Open Developer Lab -> APK/AAB Analyzer and return
        composeTestRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("lab_apk_analyzer"))
        composeTestRule.onNodeWithTag("lab_apk_analyzer").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pick_apk_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lab_back_button").performClick()
        composeTestRule.waitForIdle()

        // Navigate across primary tabs
        composeTestRule.onNodeWithTag("nav_projects").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("new_project_btn").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_editor").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("toggle_explorer_btn").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_ai_studio").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_prompt_input").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("view_roadmap_audit_btn"))
        composeTestRule.onNodeWithTag("view_roadmap_audit_btn").assertIsDisplayed()
    }
}
