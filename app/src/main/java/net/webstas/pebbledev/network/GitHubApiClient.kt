package net.webstas.pebbledev.network

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.webstas.pebbledev.data.ManifestVersionHelper
import net.webstas.pebbledev.data.PebbleProjectCatalog
import net.webstas.pebbledev.model.FeedbackReply
import net.webstas.pebbledev.model.FeedbackSource
import net.webstas.pebbledev.model.FeedbackThread
import net.webstas.pebbledev.model.PebbleProject
import net.webstas.pebbledev.model.WorkflowStatus
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class GitHubApiClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
) {
    private val jsonMediaType = "application/vnd.github+json".toMediaType()

    private fun Request.Builder.applyGitHubHeaders(token: String): Request.Builder {
        header("Accept", "application/vnd.github+json")
        header("User-Agent", "PebbleDevConsole-Android/1.0")
        header("X-GitHub-Api-Version", "2022-11-28")
        if (token.isNotBlank()) {
            header("Authorization", "Bearer ${token.trim()}")
        }
        return this
    }

    suspend fun fetchPebbleProjects(username: String, token: String): List<PebbleProject> =
        withContext(Dispatchers.IO) {
            val catalogByName = PebbleProjectCatalog.defaultProjects.associateBy { it.repoName }
            val result = mutableMapOf<String, PebbleProject>()
            catalogByName.forEach { (k, v) -> result[k] = v }

            val url = "https://api.github.com/users/$username/repos?per_page=100&sort=updated"
            val req = Request.Builder().url(url).applyGitHubHeaders(token).get().build()
            try {
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bodyStr = resp.body?.string().orEmpty()
                        val arr = JSONArray(bodyStr)
                        for (i in 0 until arr.length()) {
                            val repo = arr.getJSONObject(i)
                            val name = repo.optString("name")
                            val desc = repo.optString("description", "")
                            val branch = repo.optString("default_branch", "main")
                            val openIssues = repo.optInt("open_issues_count", 0)
                            if (PebbleProjectCatalog.isPebbleRepository(name, desc)) {
                                val existing = result[name]
                                result[name] = (existing ?: PebbleProject(
                                    repoName = name,
                                    displayName = name,
                                    defaultBranch = branch,
                                    wpSlug = name.lowercase()
                                )).copy(
                                    defaultBranch = branch,
                                    description = desc.ifBlank { existing?.description.orEmpty() },
                                    openIssuesCount = openIssues
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Keep catalog fallback when offline
            }

            result.values.sortedBy { it.displayName }
        }

    suspend fun enrichProjectReleaseAndManifest(
        username: String,
        project: PebbleProject,
        token: String
    ): PebbleProject = withContext(Dispatchers.IO) {
        var updated = project

        // 1. Check latest GitHub release & .pbw asset
        try {
            val relUrl = "https://api.github.com/repos/$username/${project.repoName}/releases?per_page=1"
            val req = Request.Builder().url(relUrl).applyGitHubHeaders(token).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val arr = JSONArray(resp.body?.string().orEmpty())
                    if (arr.length() > 0) {
                        val rel = arr.getJSONObject(0)
                        val tag = rel.optString("tag_name")
                        val assets = rel.optJSONArray("assets") ?: JSONArray()
                        var pbwUrl: String? = null
                        var pbwName: String? = null
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val aName = asset.optString("name")
                            if (aName.endsWith(".pbw", ignoreCase = true)) {
                                pbwName = aName
                                pbwUrl = asset.optString("browser_download_url")
                                break
                            }
                        }
                        updated = updated.copy(
                            latestReleaseTag = tag.ifBlank { null },
                            currentVersion = tag.removePrefix("v").ifBlank { updated.currentVersion },
                            latestPbwDownloadUrl = pbwUrl,
                            latestPbwFileName = pbwName
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }

        // 2. Check package.json or appinfo.json
        for (candidate in listOf("package.json", "appinfo.json")) {
            try {
                val contentUrl =
                    "https://api.github.com/repos/$username/${project.repoName}/contents/$candidate?ref=${updated.defaultBranch}"
                val req = Request.Builder().url(contentUrl).applyGitHubHeaders(token).get().build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val obj = JSONObject(resp.body?.string().orEmpty())
                        val sha = obj.optString("sha")
                        val b64 = obj.optString("content").replace("\n", "")
                        val decoded = String(Base64.decode(b64, Base64.DEFAULT), Charsets.UTF_8)
                        val ver = ManifestVersionHelper.extractVersion(decoded)
                        updated = updated.copy(
                            manifestPath = candidate,
                            rawManifestJson = decoded,
                            manifestSha = sha,
                            currentVersion = ver ?: updated.currentVersion
                        )
                        return@withContext updated
                    }
                }
            } catch (_: Exception) {
            }
        }

        // 3. Check latest Actions workflow run status
        try {
            val runsUrl = "https://api.github.com/repos/$username/${project.repoName}/actions/runs?per_page=1"
            val req = Request.Builder().url(runsUrl).applyGitHubHeaders(token).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val obj = JSONObject(resp.body?.string().orEmpty())
                    val runs = obj.optJSONArray("workflow_runs")
                    if (runs != null && runs.length() > 0) {
                        val run = runs.getJSONObject(0)
                        val status = run.optString("status")
                        val conclusion = run.optString("conclusion")
                        val htmlUrl = run.optString("html_url")
                        val wfStatus = when {
                            status == "in_progress" -> WorkflowStatus.IN_PROGRESS
                            status == "queued" -> WorkflowStatus.QUEUED
                            conclusion == "success" -> WorkflowStatus.SUCCESS
                            conclusion == "failure" -> WorkflowStatus.FAILURE
                            else -> WorkflowStatus.IDLE
                        }
                        updated = updated.copy(
                            lastWorkflowStatus = wfStatus,
                            lastWorkflowRunUrl = htmlUrl.ifBlank { null }
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }

        updated
    }

    suspend fun fetchOpenIssuesAndQuestions(
        username: String,
        projects: List<PebbleProject>,
        token: String
    ): List<FeedbackThread> = withContext(Dispatchers.IO) {
        val threads = mutableListOf<FeedbackThread>()
        val repoMap = projects.associateBy { it.repoName.lowercase() }

        // Use GitHub Search API to fetch issues across all of user's Pebble repositories in one call
        val q = "user:$username state:open"
        val url = "https://api.github.com/search/issues?q=${java.net.URLEncoder.encode(q, "UTF-8")}&sort=updated&per_page=50"
        val req = Request.Builder().url(url).applyGitHubHeaders(token).get().build()

        try {
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val root = JSONObject(resp.body?.string().orEmpty())
                    val items = root.optJSONArray("items") ?: JSONArray()
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val repoUrl = item.optString("repository_url")
                        val repoName = repoUrl.substringAfterLast("/")
                        val matchedProject = repoMap[repoName.lowercase()]
                        if (matchedProject == null && !PebbleProjectCatalog.isPebbleRepository(repoName)) {
                            continue
                        }
                        val isPr = item.has("pull_request")
                        val number = item.optInt("number")
                        val userObj = item.optJSONObject("user")
                        val author = userObj?.optString("login") ?: "github-user"
                        threads.add(
                            FeedbackThread(
                                id = "gh:${repoName}#$number",
                                source = if (isPr) FeedbackSource.GITHUB_PR else FeedbackSource.GITHUB_ISSUE,
                                projectRepo = repoName,
                                projectDisplayName = matchedProject?.displayName ?: repoName,
                                title = item.optString("title"),
                                author = author,
                                body = item.optString("body", ""),
                                createdAtIso = item.optString("created_at"),
                                webUrl = item.optString("html_url"),
                                issueNumber = number,
                                state = item.optString("state", "open"),
                                commentsCount = item.optInt("comments", 0)
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }
        threads
    }

    suspend fun fetchIssueComments(
        username: String,
        repoName: String,
        issueNumber: Int,
        token: String
    ): List<FeedbackReply> = withContext(Dispatchers.IO) {
        val list = mutableListOf<FeedbackReply>()
        val url = "https://api.github.com/repos/$username/$repoName/issues/$issueNumber/comments"
        val req = Request.Builder().url(url).applyGitHubHeaders(token).get().build()
        try {
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val arr = JSONArray(resp.body?.string().orEmpty())
                    for (i in 0 until arr.length()) {
                        val c = arr.getJSONObject(i)
                        val user = c.optJSONObject("user")?.optString("login") ?: "user"
                        list.add(
                            FeedbackReply(
                                id = c.optLong("id").toString(),
                                author = user,
                                body = c.optString("body"),
                                createdAtIso = c.optString("created_at")
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }
        list
    }

    suspend fun postIssueComment(
        username: String,
        repoName: String,
        issueNumber: Int,
        commentBody: String,
        token: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext Result.failure(IllegalStateException("GitHub Personal Access Token required in Settings"))
        }
        val url = "https://api.github.com/repos/$username/$repoName/issues/$issueNumber/comments"
        val payload = JSONObject().put("body", commentBody).toString()
        val req = Request.Builder()
            .url(url)
            .applyGitHubHeaders(token)
            .post(payload.toRequestBody(jsonMediaType))
            .build()
        try {
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(RuntimeException("GitHub API ${resp.code}: ${resp.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun closeIssue(
        username: String,
        repoName: String,
        issueNumber: Int,
        token: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext Result.failure(IllegalStateException("GitHub Personal Access Token required in Settings"))
        }
        val url = "https://api.github.com/repos/$username/$repoName/issues/$issueNumber"
        val payload = JSONObject().put("state", "closed").toString()
        val req = Request.Builder()
            .url(url)
            .applyGitHubHeaders(token)
            .patch(payload.toRequestBody(jsonMediaType))
            .build()
        try {
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(RuntimeException("GitHub API ${resp.code}: ${resp.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun commitManifestVersionBump(
        username: String,
        project: PebbleProject,
        newVersion: String,
        token: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext Result.failure(IllegalStateException("GitHub token required to commit version bump"))
        }
        val currentRaw = project.rawManifestJson ?: """{"name": "${project.repoName}", "version": "$newVersion"}"""
        val updatedContent = ManifestVersionHelper.updateVersionInManifest(currentRaw, newVersion)
        val encoded = Base64.encodeToString(updatedContent.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

        val payload = JSONObject().apply {
            put("message", "Bump version to $newVersion")
            put("content", encoded)
            put("branch", project.defaultBranch)
            if (!project.manifestSha.isNullOrBlank()) {
                put("sha", project.manifestSha)
            }
        }

        val url = "https://api.github.com/repos/$username/${project.repoName}/contents/${project.manifestPath}"
        val req = Request.Builder()
            .url(url)
            .applyGitHubHeaders(token)
            .put(payload.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    Result.success("Committed ${project.manifestPath} ($newVersion)")
                } else {
                    Result.failure(RuntimeException("Commit failed (${resp.code}): ${resp.body?.string()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun triggerWorkflowDispatch(
        username: String,
        repoName: String,
        branch: String,
        workflowIdOrFileName: String,
        versionInput: String,
        token: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext Result.failure(IllegalStateException("GitHub token required to trigger Actions workflow"))
        }
        val url =
            "https://api.github.com/repos/$username/$repoName/actions/workflows/$workflowIdOrFileName/dispatches"
        val payload = JSONObject().apply {
            put("ref", branch)
            put("inputs", JSONObject().put("version", versionInput))
        }
        val req = Request.Builder()
            .url(url)
            .applyGitHubHeaders(token)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        try {
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful || resp.code == 204) {
                    Result.success("Triggered workflow $workflowIdOrFileName on $branch")
                } else {
                    // Fallback: send repository_dispatch event if workflow_dispatch is not configured on that filename
                    triggerRepositoryDispatch(username, repoName, versionInput, token)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun triggerRepositoryDispatch(
        username: String,
        repoName: String,
        version: String,
        token: String
    ): Result<String> {
        val url = "https://api.github.com/repos/$username/$repoName/dispatches"
        val payload = JSONObject().apply {
            put("event_type", "pebble_release")
            put("client_payload", JSONObject().put("version", version))
        }
        val req = Request.Builder()
            .url(url)
            .applyGitHubHeaders(token)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        return httpClient.newCall(req).execute().use { resp ->
            if (resp.isSuccessful || resp.code == 204) {
                Result.success("Dispatched pebble_release event for v$version")
            } else {
                Result.failure(RuntimeException("Workflow dispatch returned HTTP ${resp.code}"))
            }
        }
    }

    suspend fun createGitHubRelease(
        username: String,
        repoName: String,
        branch: String,
        tagName: String,
        releaseTitle: String,
        releaseNotes: String,
        token: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext Result.failure(IllegalStateException("GitHub token required to publish release"))
        }
        val url = "https://api.github.com/repos/$username/$repoName/releases"
        val payload = JSONObject().apply {
            put("tag_name", tagName)
            put("target_commitish", branch)
            put("name", releaseTitle)
            put("body", releaseNotes)
            put("draft", false)
            put("prerelease", false)
            put("generate_release_notes", releaseNotes.isBlank())
        }
        val req = Request.Builder()
            .url(url)
            .applyGitHubHeaders(token)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        try {
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val obj = JSONObject(resp.body?.string().orEmpty())
                    Result.success(obj.optString("html_url", "https://github.com/$username/$repoName/releases/tag/$tagName"))
                } else {
                    Result.failure(RuntimeException("Release failed (${resp.code}): ${resp.body?.string()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadPbwFile(downloadUrl: String, destFile: File, token: String): Result<File> =
        withContext(Dispatchers.IO) {
            val reqBuilder = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "PebbleDevConsole-Android/1.0")
            if (token.isNotBlank() && downloadUrl.contains("github")) {
                reqBuilder.header("Authorization", "Bearer ${token.trim()}")
            }
            try {
                httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(RuntimeException("Download failed: HTTP ${resp.code}"))
                    }
                    val body = resp.body ?: return@withContext Result.failure(RuntimeException("Empty response body"))
                    destFile.parentFile?.mkdirs()
                    destFile.outputStream().use { out ->
                        body.byteStream().copyTo(out)
                    }
                    Result.success(destFile)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
