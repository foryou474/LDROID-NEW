package com.example.data.analysis

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ApkSizeBucket(
    val label: String,
    val bytes: Long
)

data class ApkAnalysisReport(
    val fileName: String,
    val fileSizeBytes: Long,
    val sha256: String,
    val artifactType: String, // "APK", "AAB", or "ARCHIVE"
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
    val minSdk: Int?,
    val targetSdk: Int?,
    val dexCount: Int,
    val dexVersions: List<String>,
    val abis: List<String>,
    val permissions: List<String>,
    val signingCertSha256: List<String>,
    val signingIssuer: String?,
    val sizeBuckets: List<ApkSizeBucket>,
    val topEntries: List<Pair<String, Long>>,
    val heuristicNotes: List<String>
)

class ApkAnalyzer(private val context: Context) {

    suspend fun analyzeUri(uri: Uri, displayName: String): Result<ApkAnalysisReport> =
        withContext(Dispatchers.IO) {
            runCatching {
                val tempFile = File(context.cacheDir, "inspect_${System.currentTimeMillis()}_${displayName.replace('/', '_')}")
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                    } ?: error("Unable to open artifact URI")
                    analyzeFile(tempFile, displayName)
                } finally {
                    tempFile.delete()
                }
            }
        }

    suspend fun analyzeInstalledPackage(packageName: String): Result<ApkAnalysisReport> =
        withContext(Dispatchers.IO) {
            runCatching {
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                val apkFile = File(appInfo.sourceDir)
                require(apkFile.exists()) { "APK path not readable for $packageName" }
                analyzeFile(apkFile, "${packageName}.apk")
            }
        }

    fun analyzeFile(file: File, displayName: String = file.name): ApkAnalysisReport {
        val sha256 = sha256File(file)
        val ext = displayName.substringAfterLast('.', "").lowercase()
        val artifactType = when (ext) {
            "apk" -> "APK"
            "aab" -> "AAB"
            else -> "ZIP/ARCHIVE"
        }

        var dexBytes = 0L
        var resBytes = 0L
        var libBytes = 0L
        var assetBytes = 0L
        var metaBytes = 0L
        var otherBytes = 0L
        var dexCount = 0
        val dexVersions = mutableSetOf<String>()
        val abis = mutableSetOf<String>()
        val entriesList = mutableListOf<Pair<String, Long>>()

        ZipInputStream(file.inputStream().buffered()).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = entry.name
                    val size = if (entry.size > 0) entry.size else drainSize(zis)
                    entriesList.add(name to size)
                    when {
                        name.endsWith(".dex") -> {
                            dexBytes += size
                            dexCount++
                            if (entry.size > 0) {
                                val header = ByteArray(8)
                                val read = zis.read(header)
                                if (read >= 7 && header[0] == 'd'.code.toByte() &&
                                    header[1] == 'e'.code.toByte() && header[2] == 'x'.code.toByte()
                                ) {
                                    dexVersions.add(String(header, 4, 3, Charsets.US_ASCII))
                                }
                            }
                        }
                        name.startsWith("res/") || name == "resources.arsc" || name.endsWith(".pb") -> resBytes += size
                        name.startsWith("lib/") -> {
                            libBytes += size
                            val parts = name.split('/')
                            if (parts.size >= 3) abis.add(parts[1])
                        }
                        name.startsWith("assets/") -> assetBytes += size
                        name.startsWith("META-INF/") -> metaBytes += size
                        else -> otherBytes += size
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // Real PackageManager inspection for APKs
        val pkgInfo = readArchivePackageInfo(file)
        val permissions = pkgInfo?.requestedPermissions?.toList().orEmpty()
        val versionCode = pkgInfo?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode
            else @Suppress("DEPRECATION") it.versionCode.toLong()
        }
        val minSdk = pkgInfo?.applicationInfo?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) it.minSdkVersion else null
        }
        val targetSdk = pkgInfo?.applicationInfo?.targetSdkVersion

        val certInfo = extractSigningCerts(file, pkgInfo)

        val heuristics = buildList {
            if (permissions.any { it.contains("READ_EXTERNAL_STORAGE") || it.contains("MANAGE_EXTERNAL_STORAGE") }) {
                add("Heuristic: Requests broad external storage permission; check Google Play storage policy.")
            }
            if (permissions.any { it.contains("REQUEST_INSTALL_PACKAGES") }) {
                add("Heuristic: Requests package installation permission (sensitive Play policy permission).")
            }
            if (dexCount > 1) {
                add("Info: Multidex packaging detected ($dexCount DEX files).")
            }
            if (certInfo.first.isEmpty()) {
                add("Heuristic: No v1 JAR/APK signing certificate extracted in this context.")
            }
            add("Note: Heuristic checks highlight configuration traits only and are not a malware verdict.")
        }

        return ApkAnalysisReport(
            fileName = displayName,
            fileSizeBytes = file.length(),
            sha256 = sha256,
            artifactType = artifactType,
            packageName = pkgInfo?.packageName,
            versionName = pkgInfo?.versionName,
            versionCode = versionCode,
            minSdk = minSdk,
            targetSdk = targetSdk,
            dexCount = dexCount,
            dexVersions = dexVersions.sorted(),
            abis = abis.sorted(),
            permissions = permissions,
            signingCertSha256 = certInfo.first,
            signingIssuer = certInfo.second,
            sizeBuckets = listOf(
                ApkSizeBucket("DEX Bytecode", dexBytes),
                ApkSizeBucket("Resources", resBytes),
                ApkSizeBucket("Native Libs", libBytes),
                ApkSizeBucket("Assets", assetBytes),
                ApkSizeBucket("META-INF / Signatures", metaBytes),
                ApkSizeBucket("Other / Manifest", otherBytes)
            ).filter { it.bytes > 0L },
            topEntries = entriesList.sortedByDescending { it.second }.take(15),
            heuristicNotes = heuristics
        )
    }

    @Suppress("DEPRECATION")
    private fun readArchivePackageInfo(file: File): PackageInfo? {
        return runCatching {
            val flags = PackageManager.GET_PERMISSIONS or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES
                else PackageManager.GET_SIGNATURES
            context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun extractSigningCerts(file: File, pkgInfo: PackageInfo?): Pair<List<String>, String?> {
        val hashes = mutableListOf<String>()
        var issuer: String? = null
        val cf = CertificateFactory.getInstance("X.509")

        if (pkgInfo != null) {
            val sigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.signingInfo?.apkContentsSigners
            } else {
                pkgInfo.signatures
            }
            sigs?.forEach { sig ->
                val bytes = sig.toByteArray()
                hashes.add(sha256Bytes(bytes))
                if (issuer == null) {
                    runCatching {
                        val cert = cf.generateCertificate(bytes.inputStream()) as? X509Certificate
                        issuer = cert?.subjectX500Principal?.name
                    }
                }
            }
        }

        if (hashes.isEmpty()) {
            runCatching {
                JarFile(file).use { jar ->
                    val entry = jar.entries().asSequence().firstOrNull {
                        it.name.startsWith("META-INF/") &&
                            (it.name.endsWith(".RSA") || it.name.endsWith(".DSA") || it.name.endsWith(".EC"))
                    }
                    if (entry != null) {
                        val cert = cf.generateCertificates(jar.getInputStream(entry))
                            .firstOrNull() as? X509Certificate
                        if (cert != null) {
                            hashes.add(sha256Bytes(cert.encoded))
                            issuer = cert.subjectX500Principal.name
                        }
                    }
                }
            }
        }
        return hashes to issuer
    }

    private fun drainSize(zis: ZipInputStream): Long {
        var total = 0L
        val buf = ByteArray(4096)
        var n = zis.read(buf)
        while (n > 0) {
            total += n
            n = zis.read(buf)
        }
        return total
    }

    private fun sha256File(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            var n = input.read(buf)
            while (n > 0) {
                md.update(buf, 0, n)
                n = input.read(buf)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256Bytes(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString(":") { "%02X".format(it) }
    }
}
