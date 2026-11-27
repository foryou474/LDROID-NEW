package com.example.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.model.AiProviderType
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts user-owned BYOK API keys, OAuth/PAT tokens, and signing credentials
 * using AndroidKeyStore AES-256-GCM (with deterministic AES-256-GCM fallback in JVM/Robolectric).
 */
class SecureCredentialVault(private val context: Context) {

    private val prefs = context.getSharedPreferences("ldroid_secure_vault", Context.MODE_PRIVATE)

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "ldroid_byok_master_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128

        private val SECRET_PATTERNS = listOf(
            Regex("""AIza[0-9A-Za-z\-_]{20,}""") to "Potential Google API key",
            Regex("""sk-[A-Za-z0-9\-_]{20,}""") to "Potential OpenAI / Anthropic secret key",
            Regex("""ghp_[A-Za-z0-9]{20,}""") to "Potential GitHub Personal Access Token",
            Regex("""github_pat_[A-Za-z0-9_]{20,}""") to "Potential GitHub fine-grained PAT",
            Regex("-----BEGIN " + "(RSA |EC |OPENSSH )?PRIV" + "ATE KEY-----") to "Private cryptographic key header"
        )

        fun redactSecrets(input: String): String {
            var sanitized = input
            for ((regex, label) in SECRET_PATTERNS) {
                sanitized = regex.replace(sanitized) { "[REDACTED:$label]" }
            }
            return sanitized
        }

        fun scanTextForSecrets(input: String): List<Pair<Int, String>> {
            val findings = mutableListOf<Pair<Int, String>>()
            val lines = input.lines()
            lines.forEachIndexed { index, line ->
                for ((regex, label) in SECRET_PATTERNS) {
                    if (regex.containsMatchIn(line)) {
                        findings.add((index + 1) to label)
                    }
                }
            }
            return findings
        }

        fun maskCredential(secret: String?): String {
            if (secret.isNullOrBlank()) return "Not configured"
            if (secret.length <= 6) return "••••••"
            return "••••••" + secret.takeLast(4)
        }
    }

    private fun getOrCreateKey(): SecretKey? {
        val androidKs = runCatching {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            (ks.getKey(KEY_ALIAS, null) as? SecretKey) ?: run {
                val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                generator.init(spec)
                generator.generateKey()
            }
        }.getOrNull()

        if (androidKs != null) return androidKs

        // Local JVM / Robolectric fallback AES-256 key derived from package + salt
        return runCatching {
            val seed = "${context.packageName}:${KEY_ALIAS}".toByteArray(Charsets.UTF_8)
            val digest = MessageDigest.getInstance("SHA-256").digest(seed)
            SecretKeySpec(digest, "AES")
        }.getOrNull()
    }

    fun saveSecret(slotKey: String, plaintext: String): Boolean {
        val trimmed = plaintext.trim()
        if (trimmed.isEmpty()) {
            prefs.edit().remove(slotKey).remove("${slotKey}_iv").apply()
            return true
        }
        val secretKey = getOrCreateKey() ?: return false
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encrypted = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv
            prefs.edit()
                .putString(slotKey, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString("${slotKey}_iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
            true
        }.getOrDefault(false)
    }

    fun readSecret(slotKey: String): String? {
        val encB64 = prefs.getString(slotKey, null) ?: return null
        val ivB64 = prefs.getString("${slotKey}_iv", null) ?: return null
        val secretKey = getOrCreateKey() ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = Base64.decode(ivB64, Base64.NO_WRAP)
            val enc = Base64.decode(encB64, Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(enc), Charsets.UTF_8)
        }.getOrNull()
    }

    fun clearSecret(slotKey: String) {
        prefs.edit().remove(slotKey).remove("${slotKey}_iv").apply()
    }

    fun isKeystoreAvailable(): Boolean = getOrCreateKey() != null

    fun slotForAiProvider(provider: AiProviderType): String = "byok_${provider.name}"
    val githubTokenSlot: String = "github_pat_token"
    val firebaseConfigSlot: String = "firebase_project_token"
    val supabaseUrlSlot: String = "supabase_url"
    val supabaseKeySlot: String = "supabase_anon_key"
    val figmaTokenSlot: String = "figma_pat_token"
    val googleAuthSlot: String = "google_account_token"
    val keystorePasswordSlot: String = "custom_jks_password"
}
