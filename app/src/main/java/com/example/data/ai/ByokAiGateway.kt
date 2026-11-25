package com.example.data.ai

import com.example.data.security.SecureCredentialVault
import com.example.model.AiProposedFileChange
import com.example.model.AiProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiGatewayResponse(
    val text: String,
    val proposedChanges: List<AiProposedFileChange>
)

class ByokAiGateway {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun sendPrompt(
        provider: AiProviderType,
        apiKey: String,
        model: String,
        baseUrl: String,
        userPrompt: String,
        activeFilePath: String?,
        activeFileContent: String?,
        redactSecrets: Boolean,
        existingFileLookup: (String) -> String?
    ): Result<AiGatewayResponse> = withContext(Dispatchers.IO) {
        runCatching {
            require(apiKey.isNotBlank()) {
                "No BYOK API key configured for ${provider.displayName}. Add your own key in Settings -> Secrets Vault or AI Studio."
            }

            val sanitizedFile = activeFileContent?.let {
                val trimmed = it.take(12_000)
                if (redactSecrets) SecureCredentialVault.redactSecrets(trimmed) else trimmed
            }

            val systemInstruction = """
                You are an expert Android Kotlin & Jetpack Compose engineer inside LDROID IDE.
                Provide clear, accurate engineering answers.
                When proposing code edits, output each file inside exact delimiters so the user can review diffs before applying:
                <<<FILE: relative/path/to/File.kt>>>
                <full updated file content>
                <<<END>>>
                Never assume changes are applied automatically; the user reviews and approves every file patch.
            """.trimIndent()

            val fullUserPrompt = buildString {
                append(userPrompt.trim())
                if (!activeFilePath.isNullOrBlank() && sanitizedFile != null) {
                    append("\n\nActive file ($activeFilePath):\n```\n")
                    append(sanitizedFile)
                    append("\n```")
                }
            }

            val cleanBase = baseUrl.trim().trimEnd('/').ifBlank { provider.defaultBaseUrl }
            val cleanModel = model.trim().ifBlank { provider.defaultModel }

            val rawReply = when (provider) {
                AiProviderType.GEMINI -> callGemini(cleanBase, cleanModel, apiKey.trim(), systemInstruction, fullUserPrompt)
                AiProviderType.OPENAI_COMPATIBLE -> callOpenAi(cleanBase, cleanModel, apiKey.trim(), systemInstruction, fullUserPrompt)
                AiProviderType.ANTHROPIC -> callAnthropic(cleanBase, cleanModel, apiKey.trim(), systemInstruction, fullUserPrompt)
            }

            val proposals = parseProposedFiles(rawReply, existingFileLookup)
            AiGatewayResponse(text = rawReply, proposedChanges = proposals)
        }
    }

    private fun callGemini(
        baseUrl: String,
        model: String,
        apiKey: String,
        systemInstruction: String,
        userPrompt: String
    ): String {
        val url = "$baseUrl/v1beta/models/$model:generateContent?key=$apiKey"
        val body = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            })
            put("contents", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
            }))
        }
        val req = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(jsonMedia))
            .build()
        client.newCall(req).execute().use { resp ->
            val respText = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                error("Gemini HTTP ${resp.code}: ${extractErrorMessage(respText)}")
            }
            val json = JSONObject(respText)
            val candidates = json.optJSONArray("candidates") ?: error("Empty Gemini response")
            val parts = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
            return buildString {
                for (i in 0 until parts.length()) {
                    append(parts.getJSONObject(i).optString("text", ""))
                }
            }.ifBlank { "No text returned by model." }
        }
    }

    private fun callOpenAi(
        baseUrl: String,
        model: String,
        apiKey: String,
        systemInstruction: String,
        userPrompt: String
    ): String {
        val url = "$baseUrl/v1/chat/completions"
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", systemInstruction))
            .put(JSONObject().put("role", "user").put("content", userPrompt))
        val body = JSONObject().apply {
            put("model", model)
            put("messages", messages)
        }
        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()
        client.newCall(req).execute().use { resp ->
            val respText = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                error("OpenAI HTTP ${resp.code}: ${extractErrorMessage(respText)}")
            }
            val json = JSONObject(respText)
            return json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }

    private fun callAnthropic(
        baseUrl: String,
        model: String,
        apiKey: String,
        systemInstruction: String,
        userPrompt: String
    ): String {
        val url = "$baseUrl/v1/messages"
        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", 2048)
            put("system", systemInstruction)
            put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", userPrompt)))
        }
        val req = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()
        client.newCall(req).execute().use { resp ->
            val respText = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                error("Anthropic HTTP ${resp.code}: ${extractErrorMessage(respText)}")
            }
            val json = JSONObject(respText)
            val contentArr = json.getJSONArray("content")
            return buildString {
                for (i in 0 until contentArr.length()) {
                    val item = contentArr.getJSONObject(i)
                    if (item.optString("type") == "text") {
                        append(item.optString("text", ""))
                    }
                }
            }
        }
    }

    private fun extractErrorMessage(body: String): String {
        return runCatching {
            val obj = JSONObject(body)
            obj.optJSONObject("error")?.optString("message")
                ?: obj.optString("message")
                ?: body.take(240)
        }.getOrDefault(body.take(240))
    }

    fun parseProposedFiles(
        text: String,
        existingFileLookup: (String) -> String?
    ): List<AiProposedFileChange> {
        val regex = Regex("""<<<FILE:\s*([^\n>]+)>>>\s*\n([\s\S]*?)<<<END>>>""")
        return regex.findAll(text).map { match ->
            val path = match.groupValues[1].trim()
            val content = match.groupValues[2].trimEnd() + "\n"
            AiProposedFileChange(
                relativePath = path,
                originalContent = existingFileLookup(path),
                proposedContent = content,
                isApplied = false
            )
        }.toList()
    }
}
