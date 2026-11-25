package com.example.data.workspace

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.FileOpHistoryEntity
import com.example.data.local.LdroidDao
import com.example.data.local.ProjectEntity
import com.example.data.local.WorkspaceSnapshotEntity
import com.example.model.FileChangeSummary
import com.example.model.FileDiffLine
import com.example.model.FilePreviewKind
import com.example.model.LineEnding
import com.example.model.OpenEditorTab
import com.example.model.ProjectTemplate
import com.example.model.TextEncodingOption
import com.example.model.WorkspaceFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ZipImportSummary(
    val project: ProjectEntity,
    val importedFiles: Int,
    val skippedEntries: Int,
    val detectedType: String,
    val warnings: List<String>
)

data class ZipEntryInfo(
    val name: String,
    val compressedBytes: Long,
    val uncompressedBytes: Long,
    val isDirectory: Boolean
)

class WorkspaceFileManager(
    private val context: Context,
    private val dao: LdroidDao
) {
    private val workspacesRoot: File
        get() = File(context.filesDir, "ldroid_workspaces").apply { mkdirs() }

    private val baselinesRoot: File
        get() = File(context.filesDir, "ldroid_baselines").apply { mkdirs() }

    companion object {
        const val MAX_EDITABLE_BYTES = 512 * 1024L // 512 KB safe full-edit threshold
        const val LARGE_FILE_PREVIEW_BYTES = 128 * 1024 // 128 KB streaming chunk
        const val MAX_ZIP_ENTRIES = 2000
        const val MAX_ZIP_UNCOMPRESSED_BYTES = 64L * 1024L * 1024L // 64 MB Zip Bomb guard
    }

    suspend fun ensureStarterWorkspaceIfEmpty(): ProjectEntity? = withContext(Dispatchers.IO) {
        val existing = workspacesRoot.listFiles()?.filter { it.isDirectory }.orEmpty()
        if (existing.isEmpty()) {
            createProjectFromTemplate(
                name = "NeonCounter",
                packageName = "com.ldroid.sample.neoncounter",
                template = ProjectTemplate.COMPOSE_M3_APP
            )
        } else {
            null
        }
    }

    suspend fun createProjectFromTemplate(
        name: String,
        packageName: String,
        template: ProjectTemplate
    ): ProjectEntity = withContext(Dispatchers.IO) {
        val cleanName = sanitizeName(name).ifBlank { "NewProject" }
        val cleanPkg = packageName.trim().ifBlank { "com.ldroid.app.${cleanName.lowercase()}" }
        val id = UUID.randomUUID().toString().take(8)
        val projectDir = File(workspacesRoot, "${cleanName}_$id").apply { mkdirs() }
        val now = System.currentTimeMillis()

        when (template) {
            ProjectTemplate.COMPOSE_M3_APP -> writeComposeTemplate(projectDir, cleanName, cleanPkg)
            ProjectTemplate.MULTI_MODULE_KOTLIN -> writeMultiModuleTemplate(projectDir, cleanName, cleanPkg)
            ProjectTemplate.WEB_PREVIEW_SANDBOX -> writeWebSandboxTemplate(projectDir, cleanName)
        }

        val defaultFile = when (template) {
            ProjectTemplate.WEB_PREVIEW_SANDBOX -> "index.html"
            else -> "app/src/main/java/${cleanPkg.replace('.', '/')}/MainActivity.kt"
        }

        val entity = ProjectEntity(
            id = id,
            name = cleanName,
            packageName = cleanPkg,
            projectType = template.projectType,
            rootPath = projectDir.absolutePath,
            createdAtMs = now,
            updatedAtMs = now,
            lastOpenedFile = defaultFile
        )
        dao.upsertProject(entity)
        saveBaselineSnapshot(entity)
        logOp(id, "CREATE_PROJECT", cleanName, "Template: ${template.title}")
        entity
    }

    suspend fun listFiles(project: ProjectEntity, searchQuery: String = ""): List<WorkspaceFileItem> =
        withContext(Dispatchers.IO) {
            val root = File(project.rootPath)
            if (!root.exists()) return@withContext emptyList()
            val query = searchQuery.trim().lowercase()
            root.walkTopDown()
                .filter { it != root && !it.name.startsWith(".git") }
                .map { file ->
                    val rel = file.relativeTo(root).path.replace(File.separatorChar, '/')
                    WorkspaceFileItem(
                        relativePath = rel,
                        name = file.name,
                        isDirectory = file.isDirectory,
                        sizeBytes = if (file.isDirectory) 0L else file.length(),
                        lastModified = file.lastModified(),
                        extension = file.extension.lowercase()
                    )
                }
                .filter { item ->
                    query.isEmpty() || item.relativePath.lowercase().contains(query)
                }
                .sortedWith(compareBy<WorkspaceFileItem> { !it.isDirectory }.thenBy { it.relativePath })
                .toList()
        }

    suspend fun openFileTab(project: ProjectEntity, relativePath: String): Result<OpenEditorTab> =
        withContext(Dispatchers.IO) {
            runCatching {
                val target = resolveSafe(project, relativePath)
                require(target.exists() && target.isFile) { "File not found: $relativePath" }
                val ext = target.extension.lowercase()
                val previewKind = classifyPreviewKind(ext)
                val totalBytes = target.length()

                if (previewKind != FilePreviewKind.CODE_TEXT) {
                    return@runCatching OpenEditorTab(
                        relativePath = relativePath,
                        content = "",
                        savedContent = "",
                        totalBytes = totalBytes,
                        previewMode = previewKind
                    )
                }

                val isLarge = totalBytes > MAX_EDITABLE_BYTES
                val rawBytes = if (isLarge) {
                    FileInputStream(target).use { fis ->
                        val buf = ByteArray(LARGE_FILE_PREVIEW_BYTES)
                        val read = fis.read(buf)
                        if (read > 0) buf.copyOf(read) else ByteArray(0)
                    }
                } else {
                    target.readBytes()
                }

                val encoding = detectEncoding(rawBytes)
                val decoded = String(rawBytes, Charset.forName(encoding.charsetName))
                val lineEnding = if (decoded.contains("\r\n")) LineEnding.CRLF else LineEnding.LF
                val normalized = decoded.replace("\r\n", "\n")

                dao.touchProject(project.id, System.currentTimeMillis(), relativePath)
                OpenEditorTab(
                    relativePath = relativePath,
                    content = normalized,
                    savedContent = normalized,
                    encoding = encoding,
                    lineEnding = lineEnding,
                    isLargeFilePreview = isLarge,
                    totalBytes = totalBytes,
                    previewMode = FilePreviewKind.CODE_TEXT
                )
            }
        }

    suspend fun saveFileTab(project: ProjectEntity, tab: OpenEditorTab): Result<OpenEditorTab> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(!tab.isLargeFilePreview) {
                    "Large files (>512 KB) open in streaming read-only preview to prevent memory corruption."
                }
                require(tab.previewMode == FilePreviewKind.CODE_TEXT) {
                    "Binary/preview-only file cannot be saved as text."
                }
                val target = resolveSafe(project, tab.relativePath)
                target.parentFile?.mkdirs()

                val outputText = if (tab.lineEnding == LineEnding.CRLF) {
                    tab.content.replace("\n", "\r\n")
                } else {
                    tab.content
                }
                val bytes = outputText.toByteArray(Charset.forName(tab.encoding.charsetName))

                // Atomic temp-file write + rename
                val tempFile = File(target.parentFile, ".${target.name}.ldroid_tmp")
                FileOutputStream(tempFile).use { fos ->
                    fos.write(bytes)
                    fos.fd.sync()
                }
                if (target.exists() && !target.delete()) {
                    tempFile.copyTo(target, overwrite = true)
                    tempFile.delete()
                } else if (!tempFile.renameTo(target)) {
                    tempFile.copyTo(target, overwrite = true)
                    tempFile.delete()
                }

                val now = System.currentTimeMillis()
                dao.touchProject(project.id, now, tab.relativePath)
                logOp(project.id, "SAVE", tab.relativePath, "${bytes.size} bytes (${tab.encoding.charsetName})")
                tab.copy(savedContent = tab.content, totalBytes = bytes.size.toLong())
            }
        }

    suspend fun createFileOrFolder(
        project: ProjectEntity,
        relativePath: String,
        isDirectory: Boolean,
        initialContent: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanRel = relativePath.trim().trimStart('/')
            require(cleanRel.isNotBlank()) { "Path cannot be empty" }
            val target = resolveSafe(project, cleanRel)
            require(!target.exists()) { "Already exists: $cleanRel" }
            if (isDirectory) {
                require(target.mkdirs()) { "Failed to create directory: $cleanRel" }
                logOp(project.id, "MKDIR", cleanRel, "Created folder")
            } else {
                target.parentFile?.mkdirs()
                target.writeText(initialContent, Charsets.UTF_8)
                logOp(project.id, "NEW_FILE", cleanRel, "${initialContent.length} chars")
            }
            dao.touchProject(project.id, System.currentTimeMillis(), cleanRel)
        }
    }

    suspend fun renameFile(project: ProjectEntity, oldRel: String, newName: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val src = resolveSafe(project, oldRel)
                require(src.exists()) { "Source missing: $oldRel" }
                val cleanNewName = sanitizeFileName(newName)
                require(cleanNewName.isNotBlank()) { "Invalid file name" }
                val dest = File(src.parentFile, cleanNewName)
                val destRel = dest.relativeTo(File(project.rootPath)).path.replace(File.separatorChar, '/')
                resolveSafe(project, destRel)
                require(!dest.exists()) { "Target already exists: $destRel" }
                require(src.renameTo(dest)) { "Rename failed" }
                logOp(project.id, "RENAME", oldRel, "-> $destRel")
                destRel
            }
        }

    suspend fun copyFile(project: ProjectEntity, srcRel: String, destRel: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val src = resolveSafe(project, srcRel)
                val dst = resolveSafe(project, destRel)
                require(src.exists()) { "Source missing: $srcRel" }
                require(!dst.exists()) { "Destination already exists: $destRel" }
                dst.parentFile?.mkdirs()
                if (src.isDirectory) {
                    src.copyRecursively(dst, overwrite = false)
                } else {
                    src.copyTo(dst, overwrite = false)
                }
                logOp(project.id, "COPY", srcRel, "-> $destRel")
            }
        }

    suspend fun deletePaths(project: ProjectEntity, relativePaths: List<String>): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                var count = 0
                for (rel in relativePaths) {
                    val target = resolveSafe(project, rel)
                    if (target.exists() && target.deleteRecursively()) {
                        count++
                        logOp(project.id, "DELETE", rel, "Deleted")
                    }
                }
                dao.touchProject(project.id, System.currentTimeMillis(), null)
                count
            }
        }

    suspend fun importFilesFromUris(
        project: ProjectEntity,
        uris: List<Uri>
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val imported = mutableListOf<String>()
            for (uri in uris) {
                val fileName = resolveDisplayName(uri) ?: "imported_${System.currentTimeMillis()}"
                val cleanName = sanitizeFileName(fileName)
                val rel = "imported/$cleanName"
                val dst = resolveSafe(project, rel)
                dst.parentFile?.mkdirs()
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dst).use { output ->
                        input.copyTo(output)
                    }
                } ?: error("Unable to read selected URI: $uri")
                imported.add(rel)
                logOp(project.id, "IMPORT_FILE", rel, "${dst.length()} bytes via SAF")
            }
            dao.touchProject(project.id, System.currentTimeMillis(), imported.firstOrNull())
            imported
        }
    }

    suspend fun importZipProjectFromUri(uri: Uri): Result<ZipImportSummary> =
        withContext(Dispatchers.IO) {
            runCatching {
                val rawName = resolveDisplayName(uri)?.substringBeforeLast('.') ?: "ImportedProject"
                val cleanName = sanitizeName(rawName).ifBlank { "ImportedProject" }
                val id = UUID.randomUUID().toString().take(8)
                val projectDir = File(workspacesRoot, "${cleanName}_$id").apply { mkdirs() }
                val canonicalRoot = projectDir.canonicalPath

                var entryCount = 0
                var totalUncompressed = 0L
                var skipped = 0
                val warnings = mutableListOf<String>()

                context.contentResolver.openInputStream(uri)?.use { rawIn ->
                    ZipInputStream(BufferedInputStream(rawIn)).use { zis ->
                        val buffer = ByteArray(8192)
                        var entry: ZipEntry? = zis.nextEntry
                        while (entry != null) {
                            entryCount++
                            if (entryCount > MAX_ZIP_ENTRIES) {
                                warnings.add("Stopped after $MAX_ZIP_ENTRIES entries (safety limit).")
                                break
                            }
                            val entryName = entry.name.replace('\\', '/')
                            val outFile = File(projectDir, entryName)
                            if (!outFile.canonicalPath.startsWith(canonicalRoot + File.separator) &&
                                outFile.canonicalPath != canonicalRoot
                            ) {
                                skipped++
                                warnings.add("Blocked unsafe path traversal entry: $entryName")
                                entry = zis.nextEntry
                                continue
                            }

                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { fos ->
                                    var len = zis.read(buffer)
                                    while (len > 0) {
                                        totalUncompressed += len
                                        if (totalUncompressed > MAX_ZIP_UNCOMPRESSED_BYTES) {
                                            error("ZIP archive exceeds 64 MB uncompressed safety limit.")
                                        }
                                        fos.write(buffer, 0, len)
                                        len = zis.read(buffer)
                                    }
                                }
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                } ?: error("Cannot open selected ZIP URI")

                val detectedType = detectProjectType(projectDir)
                val firstReadable = projectDir.walkTopDown()
                    .firstOrNull { it.isFile && classifyPreviewKind(it.extension.lowercase()) == FilePreviewKind.CODE_TEXT }
                    ?.relativeTo(projectDir)?.path?.replace(File.separatorChar, '/')

                val now = System.currentTimeMillis()
                val entity = ProjectEntity(
                    id = id,
                    name = cleanName,
                    packageName = "com.ldroid.imported.${cleanName.lowercase()}",
                    projectType = detectedType,
                    rootPath = projectDir.absolutePath,
                    createdAtMs = now,
                    updatedAtMs = now,
                    lastOpenedFile = firstReadable
                )
                dao.upsertProject(entity)
                saveBaselineSnapshot(entity)
                logOp(id, "IMPORT_ZIP", cleanName, "$entryCount entries ($detectedType)")
                ZipImportSummary(
                    project = entity,
                    importedFiles = entryCount - skipped,
                    skippedEntries = skipped,
                    detectedType = detectedType,
                    warnings = warnings
                )
            }
        }

    suspend fun exportProjectToZip(project: ProjectEntity, destUri: Uri): Result<Long> =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = File(project.rootPath)
                require(root.exists()) { "Project folder missing" }
                var fileCount = 0L
                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    ZipOutputStream(out).use { zos ->
                        root.walkTopDown().filter { it.isFile }.forEach { file ->
                            val rel = file.relativeTo(root).path.replace(File.separatorChar, '/')
                            zos.putNextEntry(ZipEntry(rel))
                            file.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                            fileCount++
                        }
                    }
                } ?: error("Cannot write to export destination URI")
                logOp(project.id, "EXPORT_ZIP", project.name, "$fileCount files exported")
                fileCount
            }
        }

    suspend fun inspectZipEntries(project: ProjectEntity, relativePath: String): Result<List<ZipEntryInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = resolveSafe(project, relativePath)
                require(file.exists() && file.isFile) { "Archive not found" }
                val list = mutableListOf<ZipEntryInfo>()
                ZipInputStream(BufferedInputStream(FileInputStream(file))).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null && list.size < 500) {
                        list.add(
                            ZipEntryInfo(
                                name = entry.name,
                                compressedBytes = entry.compressedSize.coerceAtLeast(0L),
                                uncompressedBytes = entry.size.coerceAtLeast(0L),
                                isDirectory = entry.isDirectory
                            )
                        )
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                list
            }
        }

    suspend fun deleteProject(project: ProjectEntity): Unit = withContext(Dispatchers.IO) {
        File(project.rootPath).deleteRecursively()
        File(baselinesRoot, project.id).deleteRecursively()
        dao.deleteHistoryForProject(project.id)
        dao.deleteProject(project.id)
    }

    suspend fun saveBaselineSnapshot(project: ProjectEntity): WorkspaceSnapshotEntity =
        withContext(Dispatchers.IO) {
            val root = File(project.rootPath)
            val baselineDir = File(baselinesRoot, project.id).apply {
                deleteRecursively()
                mkdirs()
            }
            var count = 0
            var totalBytes = 0L
            if (root.exists()) {
                root.walkTopDown().filter { it.isFile }.forEach { src ->
                    val rel = src.relativeTo(root).path
                    val dst = File(baselineDir, rel)
                    dst.parentFile?.mkdirs()
                    src.copyTo(dst, overwrite = true)
                    count++
                    totalBytes += src.length()
                }
            }
            val snap = WorkspaceSnapshotEntity(
                id = UUID.randomUUID().toString().take(8),
                projectId = project.id,
                label = "Snapshot #$count files",
                fileCount = count,
                totalBytes = totalBytes,
                createdAtMs = System.currentTimeMillis()
            )
            dao.insertSnapshot(snap)
            snap
        }

    suspend fun computeWorkspaceChanges(project: ProjectEntity): List<FileChangeSummary> =
        withContext(Dispatchers.IO) {
            val root = File(project.rootPath)
            val baseline = File(baselinesRoot, project.id)
            if (!root.exists()) return@withContext emptyList()

            val currentMap = root.walkTopDown().filter { it.isFile }
                .associateBy { it.relativeTo(root).path.replace(File.separatorChar, '/') }
            val baseMap = if (baseline.exists()) {
                baseline.walkTopDown().filter { it.isFile }
                    .associateBy { it.relativeTo(baseline).path.replace(File.separatorChar, '/') }
            } else emptyMap()

            val allKeys = (currentMap.keys + baseMap.keys).sorted()
            val results = mutableListOf<FileChangeSummary>()
            for (rel in allKeys) {
                val cur = currentMap[rel]
                val base = baseMap[rel]
                when {
                    cur != null && base == null -> {
                        val lines = safeReadLines(cur).mapIndexed { idx, s -> FileDiffLine('+', null, idx + 1, s) }
                        results.add(FileChangeSummary(rel, "ADDED", lines.take(120)))
                    }
                    cur == null && base != null -> {
                        val lines = safeReadLines(base).mapIndexed { idx, s -> FileDiffLine('-', idx + 1, null, s) }
                        results.add(FileChangeSummary(rel, "DELETED", lines.take(120)))
                    }
                    cur != null && base != null -> {
                        val oldText = base.readText()
                        val newText = cur.readText()
                        if (oldText != newText) {
                            results.add(FileChangeSummary(rel, "MODIFIED", computeLineDiff(oldText, newText)))
                        }
                    }
                }
            }
            results
        }

    fun computeLineDiff(oldText: String, newText: String): List<FileDiffLine> {
        val oldLines = oldText.lines()
        val newLines = newText.lines()
        val diffs = mutableListOf<FileDiffLine>()
        val maxLen = maxOf(oldLines.size, newLines.size)
        for (i in 0 until maxLen) {
            val o = oldLines.getOrNull(i)
            val n = newLines.getOrNull(i)
            when {
                o == n && o != null -> {
                    if (diffs.size < 160) diffs.add(FileDiffLine(' ', i + 1, i + 1, o))
                }
                else -> {
                    if (o != null) diffs.add(FileDiffLine('-', i + 1, null, o))
                    if (n != null) diffs.add(FileDiffLine('+', null, i + 1, n))
                }
            }
            if (diffs.size >= 200) break
        }
        return diffs
    }

    fun resolveSafe(project: ProjectEntity, relativePath: String): File {
        val root = File(project.rootPath)
        val candidate = File(root, relativePath)
        val canonicalRoot = root.canonicalPath
        val canonicalCandidate = candidate.canonicalPath
        require(
            canonicalCandidate == canonicalRoot ||
                canonicalCandidate.startsWith(canonicalRoot + File.separator)
        ) {
            "Blocked path traversal outside workspace: $relativePath"
        }
        return candidate
    }

    private fun safeReadLines(file: File): List<String> =
        if (file.length() > MAX_EDITABLE_BYTES) listOf("[Large file: ${file.length()} bytes]")
        else file.readLines()

    private fun classifyPreviewKind(ext: String): FilePreviewKind = when (ext) {
        "png", "jpg", "jpeg", "webp", "gif", "bmp" -> FilePreviewKind.IMAGE
        "zip", "jar", "apk", "aab" -> FilePreviewKind.ZIP_ARCHIVE
        "pdf" -> FilePreviewKind.PDF_BINARY
        "so", "dex", "bin", "class", "o", "ttf", "otf", "mp3", "mp4" -> FilePreviewKind.UNSUPPORTED_BINARY
        else -> FilePreviewKind.CODE_TEXT
    }

    private fun detectEncoding(bytes: ByteArray): TextEncodingOption {
        if (bytes.size >= 2) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            if ((b0 == 0xFE && b1 == 0xFF) || (b0 == 0xFF && b1 == 0xFE)) {
                return TextEncodingOption.UTF_16
            }
        }
        return TextEncodingOption.UTF_8
    }

    private fun detectProjectType(root: File): String {
        val files = root.walkTopDown().filter { it.isFile }.map { it.name.lowercase() }.toSet()
        return when {
            "build.gradle.kts" in files || "build.gradle" in files -> "ANDROID_GRADLE"
            "androidmanifest.xml" in files -> "ANDROID_SOURCE"
            "index.html" in files -> "WEB_SANDBOX"
            files.any { it.endsWith(".kt") || it.endsWith(".java") } -> "KOTLIN_JAVA"
            else -> "GENERAL_WORKSPACE"
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return cursor.getString(idx)
                }
            }
        return uri.lastPathSegment
    }

    private suspend fun logOp(projectId: String, op: String, target: String, details: String) {
        dao.insertHistory(
            FileOpHistoryEntity(
                projectId = projectId,
                operation = op,
                targetPath = target,
                details = details,
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    private fun sanitizeName(raw: String): String =
        raw.trim().replace(Regex("""[^A-Za-z0-9_-]"""), "")

    private fun sanitizeFileName(raw: String): String =
        raw.trim().replace(Regex("""[\\/:*?"<>|]"""), "_")

    private fun writeComposeTemplate(root: File, appName: String, pkg: String) {
        val pkgPath = pkg.replace('.', '/')
        writeRel(root, "settings.gradle.kts", """
            pluginManagement {
                repositories { google(); mavenCentral(); gradlePluginPortal() }
            }
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories { google(); mavenCentral() }
            }
            rootProject.name = "$appName"
            include(":app")
        """.trimIndent())

        writeRel(root, "build.gradle.kts", """
            plugins {
                id("com.android.application") version "8.9.1" apply false
                id("org.jetbrains.kotlin.android") version "2.1.20" apply false
                id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
            }
        """.trimIndent())

        writeRel(root, "app/build.gradle.kts", """
            plugins {
                id("com.android.application")
                id("org.jetbrains.kotlin.android")
                id("org.jetbrains.kotlin.plugin.compose")
            }

            android {
                namespace = "$pkg"
                compileSdk = 36

                defaultConfig {
                    applicationId = "$pkg"
                    minSdk = 26
                    targetSdk = 36
                    versionCode = 1
                    versionName = "1.0.0"
                }

                buildFeatures { compose = true }
            }

            dependencies {
                implementation(platform("androidx.compose:compose-bom:2025.04.00"))
                implementation("androidx.activity:activity-compose:1.10.1")
                implementation("androidx.compose.material3:material3")
                implementation("androidx.compose.ui:ui-tooling-preview")
                testImplementation("junit:junit:4.13.2")
            }
        """.trimIndent())

        writeRel(root, "app/src/main/AndroidManifest.xml", """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application
                    android:allowBackup="true"
                    android:label="@string/app_name">
                    <activity
                        android:name=".MainActivity"
                        android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
        """.trimIndent())

        writeRel(root, "app/src/main/res/values/strings.xml", """
            <resources>
                <string name="app_name">$appName</string>
            </resources>
        """.trimIndent())

        writeRel(root, "app/src/main/java/$pkgPath/MainActivity.kt", """
            package $pkg

            import android.os.Bundle
            import androidx.activity.ComponentActivity
            import androidx.activity.compose.setContent
            import androidx.compose.foundation.layout.*
            import androidx.compose.material3.*
            import androidx.compose.runtime.*
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            class MainActivity : ComponentActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    setContent {
                        MaterialTheme {
                            NeonCounterScreen()
                        }
                    }
                }
            }

            @Composable
            fun NeonCounterScreen() {
                var count by remember { mutableIntStateOf(0) }
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(text = "$appName Studio", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Active Build Counter: ${'$'}count", modifier = Modifier.padding(16.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { count++ }) {
                        Text("Increment Counter")
                    }
                }
            }
        """.trimIndent())

        writeRel(root, "app/src/test/java/$pkgPath/MainUnitTest.kt", """
            package $pkg

            import org.junit.Assert.assertEquals
            import org.junit.Test

            class MainUnitTest {
                @Test
                fun verifyArithmetic() {
                    assertEquals(4, 2 + 2)
                }
            }
        """.trimIndent())

        writeRel(root, ".gitignore", """
            .gradle/
            build/
            local.properties
            *.jks
            *.keystore
            .env
        """.trimIndent())

        writeRel(root, ".github/workflows/android_ci.yml", """
            name: Android CI
            on:
              push:
                branches: [ "main" ]
            jobs:
              build:
                runs-on: ubuntu-latest
                steps:
                  - uses: actions/checkout@v4
                  - uses: actions/setup-java@v4
                    with:
                      distribution: 'temurin'
                      java-version: '17'
                  - uses: gradle/actions/setup-gradle@v4
                  - run: ./gradlew lint testDebugUnitTest assembleDebug
        """.trimIndent())
    }

    private fun writeMultiModuleTemplate(root: File, appName: String, pkg: String) {
        writeComposeTemplate(root, appName, pkg)
        writeRel(root, "settings.gradle.kts", """
            rootProject.name = "$appName"
            include(":app")
            include(":core:model")
        """.trimIndent())
        writeRel(root, "gradle/libs.versions.toml", """
            [versions]
            agp = "8.9.1"
            kotlin = "2.1.20"
            composeBom = "2025.04.00"

            [libraries]
            compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
        """.trimIndent())
        writeRel(root, "core/model/build.gradle.kts", """
            plugins {
                id("org.jetbrains.kotlin.jvm")
            }
        """.trimIndent())
        writeRel(root, "core/model/src/main/kotlin/BuildMetadata.kt", """
            package $pkg.core.model

            data class BuildMetadata(
                val moduleName: String = ":core:model",
                val version: String = "1.0.0"
            )
        """.trimIndent())
    }

    private fun writeWebSandboxTemplate(root: File, appName: String) {
        writeRel(root, "index.html", """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <title>$appName Preview</title>
              <link rel="stylesheet" href="styles.css" />
            </head>
            <body>
              <main class="card">
                <h1>$appName Workspace</h1>
                <p>Ready for live HTML/CSS/JSON editing in LDROID.</p>
                <button id="pingBtn">Run Action</button>
              </main>
              <script src="app.js"></script>
            </body>
            </html>
        """.trimIndent())
        writeRel(root, "styles.css", """
            body {
              background: #070B14;
              color: #F4F7FF;
              font-family: sans-serif;
              padding: 24px;
            }
            .card {
              background: #111B2D;
              border: 1px solid #26354D;
              border-radius: 16px;
              padding: 20px;
            }
        """.trimIndent())
        writeRel(root, "app.js", """
            document.getElementById("pingBtn")?.addEventListener("click", () => {
              console.log("LDROID sandbox action triggered");
            });
        """.trimIndent())
        writeRel(root, "manifest.json", """
            {
              "name": "$appName",
              "version": "1.0.0",
              "offlineFirst": true
            }
        """.trimIndent())
    }

    private fun writeRel(root: File, rel: String, content: String) {
        val f = File(root, rel)
        f.parentFile?.mkdirs()
        f.writeText(content + "\n", Charsets.UTF_8)
    }
}
