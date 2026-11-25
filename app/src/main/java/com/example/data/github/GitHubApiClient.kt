package com.example.data.github

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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

data class GitHubRepoStatus(
    val fullName: String,
    val defaultBranch: String,
    val isPrivate: Boolean,
    val stars: Int,
    val openIssues: Int,
    val pushedAt: String,
    val htmlUrl: String,
    val recentWorkflowRuns: List<GitHubWorkflowRunInfo>
)

class GitHubApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchRepositoryStatus(ownerRepo: String, token: String?): Result<GitHubRepoStatus> =
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

                GitHubRepoStatus(
                    fullName = repoJson.getString("full_name"),
                    defaultBranch = repoJson.optString("default_branch", "main"),
                    isPrivate = repoJson.optBoolean("private", false),
                    stars = repoJson.optInt("stargazers_count", 0),
                    openIssues = repoJson.optInt("open_issues_count", 0),
                    pushedAt = repoJson.optString("pushed_at", ""),
                    htmlUrl = repoJson.optString("html_url", "https://github.com/$cleaned"),
                    recentWorkflowRuns = runs
                )
            }
        }
}
