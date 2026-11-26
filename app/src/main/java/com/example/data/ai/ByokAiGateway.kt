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

    suspend fun fetchAvailableModels(
        provider: AiProviderType,
        apiKey: String,
        baseUrl: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanKey = apiKey.trim()
            require(cleanKey.isNotBlank()) { "Enter or save an API key for ${provider.displayName} first." }
            val cleanBase = baseUrl.trim().trimEnd('/').ifBlank { provider.defaultBaseUrl }

            when (provider) {
                AiProviderType.GEMINI -> {
                    val url = if (cleanBase.endsWith("/v1beta") || cleanBase.endsWith("/v1")) {
                        "$cleanBase/models?key=$cleanKey"
                    } else {
                        "$cleanBase/v1beta/models?key=$cleanKey"
                    }
                    val req = Request.Builder().url(url).get().build()
                    client.newCall(req).execute().use { resp ->
                        val body = resp.body?.string().orEmpty()
                        if (!resp.isSuccessful) {
                            error("${provider.displayName} HTTP ${resp.code}: ${extractErrorMessage(body)}")
                        }
                        ensureJsonResponse(body, provider)
                        val arr = JSONObject(body).optJSONArray("models") ?: JSONArray()
                        buildList {
                            for (i in 0 until arr.length()) {
                                val obj = arr.optJSONObject(i) ?: continue
                                val methods = obj.optJSONArray("supportedGenerationMethods")
                                val supportsGen = if (methods == null || methods.length() == 0) {
                                    true
                                } else {
                                    (0 until methods.length()).any { methods.optString(it) == "generateContent" }
                                }
                                if (supportsGen) {
                                    val id = obj.optString("name", "").removePrefix("models/").trim()
                                    if (id.isNotBlank()) add(id)
                                }
                            }
                        }.distinct()
                    }
                }
                AiProviderType.OPENAI_COMPATIBLE, AiProviderType.CUSTOM -> {
                    val primaryUrl = buildOpenAiModelsUrl(cleanBase, provider)
                    val fallbackUrl = if (primaryUrl.endsWith("/v1/models")) {
                        "$cleanBase/models"
                    } else {
                        "$cleanBase/v1/models"
                    }
                    val body = executeBearerGet(primaryUrl, cleanKey, provider)
                        ?: executeBearerGet(fallbackUrl, cleanKey, provider, throwOnFailure = true)!!
                    parseOpenAiStyleModels(body)
                }
                AiProviderType.ANTHROPIC -> {
                    val url = if (cleanBase.endsWith("/v1")) "$cleanBase/models" else "$cleanBase/v1/models"
                    val req = Request.Builder()
                        .url(url)
                        .addHeader("x-api-key", cleanKey)
                        .addHeader("anthropic-version", "2023-06-01")
                        .get()
                        .build()
                    client.newCall(req).execute().use { resp ->
                        val body = resp.body?.string().orEmpty()
                        if (!resp.isSuccessful) {
                            error("${provider.displayName} HTTP ${resp.code}: ${extractErrorMessage(body)}")
                        }
                        ensureJsonResponse(body, provider)
                        parseOpenAiStyleModels(body)
                    }
                }
            }
        }
    }

    private fun buildOpenAiModelsUrl(cleanBase: String, provider: AiProviderType): String {
        return when {
            cleanBase.endsWith("/models") -> cleanBase
            cleanBase.endsWith("/v1") || cleanBase.endsWith("/v1beta") || cleanBase.contains("/api/v1") ->
                "$cleanBase/models"
            provider == AiProviderType.CUSTOM -> "$cleanBase/models"
            else -> "$cleanBase/v1/models"
        }
    }

    private fun executeBearerGet(
        url: String,
        cleanKey: String,
        provider: AiProviderType,
        throwOnFailure: Boolean = false
    ): String? {
        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $cleanKey")
            .get()
            .build()
        return client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                if (!throwOnFailure && resp.code == 404) return null
                error("${provider.displayName} HTTP ${resp.code}: ${extractErrorMessage(body)}")
            }
            ensureJsonResponse(body, provider)
            body
        }
    }

    private fun parseOpenAiStyleModels(body: String): List<String> {
        val trimmed = body.trim()
        val arr = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            val obj = JSONObject(trimmed)
            obj.optJSONArray("data") ?: obj.optJSONArray("models") ?: JSONArray()
        }
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.opt(i)
                val id = when (item) {
                    is JSONObject -> item.optString("id").ifBlank { item.optString("name") }
                    is String -> item
                    else -> ""
                }.trim()
                if (id.isNotBlank()) add(id)
            }
        }.distinct()
    }

    suspend fun testConnection(
        provider: AiProviderType,
        apiKey: String,
        model: String,
        baseUrl: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanKey = apiKey.trim()
            require(cleanKey.isNotBlank()) { "Enter or save an API key for ${provider.displayName} first." }
            val cleanBase = baseUrl.trim().trimEnd('/').ifBlank { provider.defaultBaseUrl }
            val cleanModel = model.trim().ifBlank { provider.defaultModel }
            val startMs = System.currentTimeMillis()
            val req = when (provider) {
                AiProviderType.GEMINI -> Request.Builder()
                    .url("$cleanBase/v1beta/models/$cleanModel?key=$cleanKey")
                    .get()
                    .build()
                AiProviderType.OPENAI_COMPATIBLE, AiProviderType.CUSTOM -> Request.Builder()
                    .url(buildOpenAiModelsUrl(cleanBase, provider))
                    .addHeader("Authorization", "Bearer $cleanKey")
                    .get()
                    .build()
                AiProviderType.ANTHROPIC -> Request.Builder()
                    .url(if (cleanBase.endsWith("/v1")) "$cleanBase/models" else "$cleanBase/v1/models")
                    .addHeader("x-api-key", cleanKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .get()
                    .build()
            }
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    error("${provider.displayName} HTTP ${resp.code}: ${extractErrorMessage(body)}")
                }
                ensureJsonResponse(body, provider)
            }
            val elapsedMs = System.currentTimeMillis() - startMs
            "Connected to ${provider.displayName} ($cleanModel) in ${elapsedMs}ms"
        }
    }

    suspend fun sendPrompt(
        provider: AiProviderType,
        apiKey: String,
        model: String,
        baseUrl: String,
        userPrompt: String,
        activeFilePath: String?,
        activeFileContent: String?,
        additionalContextFiles: List<Pair<String, String>> = emptyList(),
        redactSecrets: Boolean,
        existingFileLookup: (String) -> String?
    ): Result<AiGatewayResponse> = withContext(Dispatchers.IO) {
        runCatching {
            require(apiKey.isNotBlank()) {
                "No BYOK API key configured for ${provider.displayName}. Add your own key in Settings."
            }

            val sanitizedFile = activeFileContent?.let {
                val trimmed = it.take(12_000)
                if (redactSecrets) SecureCredentialVault.redactSecrets(trimmed) else trimmed
            }

            val systemInstruction = """
                You are M.A.X. Core AI (Modular AI eXecutor), an expert Android Kotlin & Jetpack Compose engineer inside M.A.X. Studio.
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
                additionalContextFiles.take(5).forEach { (ctxPath, ctxContent) ->
                    if (ctxPath != activeFilePath) {
                        val trimmed = ctxContent.take(6_000)
                        val cleanCtx = if (redactSecrets) SecureCredentialVault.redactSecrets(trimmed) else trimmed
                        append("\n\nAttached context ($ctxPath):\n```\n")
                        append(cleanCtx)
                        append("\n```")
                    }
                }
            }

            val cleanBase = baseUrl.trim().trimEnd('/').ifBlank { provider.defaultBaseUrl }
            val cleanModel = model.trim().ifBlank { provider.defaultModel }

            val rawReply = when (provider) {
                AiProviderType.GEMINI -> callGemini(cleanBase, cleanModel, apiKey.trim(), systemInstruction, fullUserPrompt)
                AiProviderType.OPENAI_COMPATIBLE, AiProviderType.CUSTOM ->
                    callOpenAi(cleanBase, cleanModel, apiKey.trim(), systemInstruction, fullUserPrompt, provider)
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
            ensureJsonResponse(respText, AiProviderType.GEMINI)
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
        userPrompt: String,
        provider: AiProviderType
    ): String {
        val url = when {
            baseUrl.endsWith("/chat/completions") -> baseUrl
            baseUrl.endsWith("/v1") || baseUrl.contains("/api/v1") -> "$baseUrl/chat/completions"
            else -> "$baseUrl/v1/chat/completions"
        }
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
                error("${provider.displayName} HTTP ${resp.code}: ${extractErrorMessage(respText)}")
            }
            ensureJsonResponse(respText, provider)
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
        val url = if (baseUrl.endsWith("/v1")) "$baseUrl/messages" else "$baseUrl/v1/messages"
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
            ensureJsonResponse(respText, AiProviderType.ANTHROPIC)
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

    private fun ensureJsonResponse(respText: String, provider: AiProviderType) {
        val trimmed = respText.trimStart()
        if (trimmed.startsWith("<!DOCTYPE", ignoreCase = true) || trimmed.startsWith("<html", ignoreCase = true)) {
            error("API endpoint returned HTML instead of JSON. Verify Base URL (expected ${provider.defaultBaseUrl}) and Model ID in Settings.")
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
