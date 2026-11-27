package com.example.data.github

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GitHubWorkflowRunInfo(
    val id: Long,
    val name: String,
    val status: String,
    val conclusion: String?,
    val branch: String,
    val createdAt: String
)

data class GitHubCommitInfo(
    val sha: String,
    val message: String,
    val author: String
)

data class GitHubRepoStatus(
    val fullName: String,
    val defaultBranch: String,
    val inspectedBranch: String,
    val isPrivate: Boolean,
    val stars: Int,
    val forks: Int = 0,
    val openIssues: Int,
    val pushedAt: String,
    val htmlUrl: String,
    val recentWorkflowRuns: List<GitHubWorkflowRunInfo>,
    val recentCommits: List<GitHubCommitInfo> = emptyList()
)

class GitHubApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchRepositoryStatus(
        ownerRepo: String,
        token: String?,
        preferredBranch: String = "main"
    ): Result<GitHubRepoStatus> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleaned = ownerRepo.trim()
                    .removePrefix("https://github.com/")
                    .removeSuffix(".git")
                    .trim('/')
                require(cleaned.contains('/') && cleaned.split('/').size == 2) {
                    "Enter repository as owner/repo (e.g. square/okhttp)"
                }

                val repoReq = Request.Builder()
                    .url("https://api.github.com/repos/$cleaned")
                    .addHeader("Accept", "application/vnd.github+json")
                    .apply {
                        if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer ${token.trim()}")
                    }
                    .build()

                val repoJson = client.newCall(repoReq).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) error("GitHub API ${resp.code}: ${body.take(180)}")
                    JSONObject(body)
                }

                val repoDefaultBranch = repoJson.optString("default_branch", "main")
                val targetBranch = preferredBranch.trim().ifBlank { repoDefaultBranch }

                val runsReq = Request.Builder()
                    .url("https://api.github.com/repos/$cleaned/actions/runs?per_page=5")
                    .addHeader("Accept", "application/vnd.github+json")
                    .apply {
                        if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer ${token.trim()}")
                    }
                    .build()

                val runs = runCatching {
                    client.newCall(runsReq).execute().use { resp ->
                        if (!resp.isSuccessful) return@use emptyList()
                        val arr = JSONObject(resp.body?.string().orEmpty()).optJSONArray("workflow_runs")
                            ?: return@use emptyList()
                        buildList {
                            for (i in 0 until arr.length()) {
                                val r = arr.getJSONObject(i)
                                add(
                                    GitHubWorkflowRunInfo(
                                        id = r.optLong("id"),
                                        name = r.optString("name", "CI"),
                                        status = r.optString("status", "unknown"),
                                        conclusion = r.optString("conclusion", "").ifBlank { null },
                                        branch = r.optString("head_branch", "main"),
                                        createdAt = r.optString("created_at", "")
                                    )
                                )
                            }
                        }
                    }
                }.getOrDefault(emptyList())

                val commitsReq = Request.Builder()
                    .url("https://api.github.com/repos/$cleaned/commits?sha=$targetBranch&per_page=5")
                    .addHeader("Accept", "application/vnd.github+json")
                    .apply {
                        if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer ${token.trim()}")
                    }
                    .build()

                val commits = runCatching {
                    client.newCall(commitsReq).execute().use { resp ->
                        if (!resp.isSuccessful) return@use emptyList()
                        val arr = org.json.JSONArray(resp.body?.string().orEmpty())
                        buildList {
                            for (i in 0 until arr.length()) {
                                val c = arr.getJSONObject(i)
                                val commitObj = c.optJSONObject("commit")
                                add(
                                    GitHubCommitInfo(
                                        sha = c.optString("sha", "").take(7),
                                        message = commitObj?.optString("message", "")?.lineSequence()?.firstOrNull().orEmpty(),
                                        author = commitObj?.optJSONObject("author")?.optString("name", "unknown") ?: "unknown"
                                    )
                                )
                            }
                        }
                    }
                }.getOrDefault(emptyList())

                GitHubRepoStatus(
                    fullName = repoJson.getString("full_name"),
                    defaultBranch = repoDefaultBranch,
                    inspectedBranch = targetBranch,
                    isPrivate = repoJson.optBoolean("private", false),
                    stars = repoJson.optInt("stargazers_count", 0),
                    forks = repoJson.optInt("forks_count", 0),
                    openIssues = repoJson.optInt("open_issues_count", 0),
                    pushedAt = repoJson.optString("pushed_at", ""),
                    htmlUrl = repoJson.optString("html_url", "https://github.com/$cleaned"),
                    recentWorkflowRuns = runs,
                    recentCommits = commits
                )
            }
        }

    suspend fun fetchRepoTreePaths(
        ownerRepo: String,
        branch: String,
        token: String?
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val cleaned = ownerRepo.trim()
                .removePrefix("https://github.com/")
                .removeSuffix(".git")
                .trim('/')
            val req = Request.Builder()
                .url("https://api.github.com/repos/$cleaned/git/trees/$branch?recursive=1")
                .addHeader("Accept", "application/vnd.github+json")
                .apply {
                    if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer ${token.trim()}")
                }
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("GitHub HTTP ${resp.code}: ${body.take(140)}")
                val arr = JSONObject(body).optJSONArray("tree") ?: org.json.JSONArray()
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        if (obj.optString("type") == "blob") {
                            add(obj.optString("path"))
                        }
                    }
                }.take(40)
            }
        }
    }

    suspend fun downloadRawFile(
        ownerRepo: String,
        branch: String,
        path: String,
        token: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleaned = ownerRepo.trim()
                .removePrefix("https://github.com/")
                .removeSuffix(".git")
                .trim('/')
            val req = Request.Builder()
                .url("https://raw.githubusercontent.com/$cleaned/$branch/$path")
                .apply {
                    if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer ${token.trim()}")
                }
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("GitHub Raw HTTP ${resp.code}")
                body
            }
        }
    }

    suspend fun commitAndPushFile(
        ownerRepo: String,
        relativePath: String,
        content: String,
        commitMessage: String,
        branch: String,
        token: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(token.isNotBlank()) { "GitHub PAT token required to push commits." }
            val cleaned = ownerRepo.trim()
                .removePrefix("https://github.com/")
                .removeSuffix(".git")
                .trim('/')
            val contentsUrl = "https://api.github.com/repos/$cleaned/contents/$relativePath"
            // Query existing SHA if updating
            val existingSha = runCatching {
                val getReq = Request.Builder()
                    .url("$contentsUrl?ref=$branch")
                    .addHeader("Accept", "application/vnd.github+json")
                    .addHeader("Authorization", "Bearer ${token.trim()}")
                    .build()
                client.newCall(getReq).execute().use { r ->
                    if (r.isSuccessful) JSONObject(r.body?.string().orEmpty()).optString("sha", "").ifBlank { null }
                    else null
                }
            }.getOrNull()

            val encoded = android.util.Base64.encodeToString(content.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
            val payload = JSONObject().apply {
                put("message", commitMessage.ifBlank { "Update $relativePath via LDROID IDE" })
                put("content", encoded)
                put("branch", branch.ifBlank { "main" })
                if (existingSha != null) put("sha", existingSha)
            }
            val putReq = Request.Builder()
                .url(contentsUrl)
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("Authorization", "Bearer ${token.trim()}")
                .put(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(putReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("Push failed HTTP ${resp.code}: ${body.take(160)}")
                val commitSha = JSONObject(body).optJSONObject("commit")?.optString("sha")?.take(7) ?: "committed"
                "Pushed $relativePath ($commitSha) to $branch"
            }
        }
    }

    suspend fun executeRestRequest(
        method: String,
        url: String,
        bearerToken: String?,
        bodyJson: String?
    ): com.example.model.RestTestResult = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        val start = System.currentTimeMillis()
        runCatching {
            val upper = method.trim().uppercase()
            val reqBuilder = Request.Builder().url(cleanUrl)
            if (!bearerToken.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer ${bearerToken.trim()}")
            }
            if (upper == "POST" || upper == "PUT" || upper == "PATCH") {
                val media = "application/json; charset=utf-8".toMediaType()
                reqBuilder.method(upper, (bodyJson ?: "{}").toRequestBody(media))
            } else {
                reqBuilder.get()
            }
            client.newCall(reqBuilder.build()).execute().use { resp ->
                val elapsed = System.currentTimeMillis() - start
                val headersSummary = resp.headers.names().take(5).joinToString(" • ") { k -> "$k: ${resp.header(k)}" }
                val respBody = resp.body?.string().orEmpty().take(4_000)
                com.example.model.RestTestResult(
                    statusCode = resp.code,
                    latencyMs = elapsed,
                    headersSummary = headersSummary,
                    body = respBody,
                    isError = !resp.isSuccessful
                )
            }
        }.getOrElse { ex ->
            com.example.model.RestTestResult(
                statusCode = 0,
                latencyMs = System.currentTimeMillis() - start,
                headersSummary = "Connection error",
                body = ex.message ?: "Request failed",
                isError = true
            )
        }
    }
}
