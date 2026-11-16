package net.webstas.pebbledev.network

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.webstas.pebbledev.model.FeedbackSource
import net.webstas.pebbledev.model.FeedbackThread
import net.webstas.pebbledev.model.PebbleProject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WordPressApiClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun stripHtmlTags(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#8217;", "'")
            .replace("&#8220;", "\"")
            .replace("&#8221;", "\"")
            .trim()
    }

    suspend fun fetchPebbleBlogComments(
        baseUrl: String,
        projects: List<PebbleProject>
    ): List<FeedbackThread> = withContext(Dispatchers.IO) {
        val cleanBase = baseUrl.trim().removeSuffix("/")
        val commentsUrl = "$cleanBase/index.php/wp-json/wp/v2/comments?per_page=40&order=desc"
        val req = Request.Builder()
            .url(commentsUrl)
            .header("User-Agent", "PebbleDevConsole-Android/1.0")
            .get()
            .build()

        val threads = mutableListOf<FeedbackThread>()
        try {
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val arr = JSONArray(resp.body?.string().orEmpty())
                    for (i in 0 until arr.length()) {
                        val c = arr.getJSONObject(i)
                        val commentId = c.optInt("id")
                        val postId = c.optInt("post")
                        val authorName = c.optString("author_name", "Blog Reader")
                        val dateIso = c.optString("date_gmt", c.optString("date"))
                        val link = c.optString("link", cleanBase)
                        val rawHtml = c.optJSONObject("content")?.optString("rendered").orEmpty()
                        val text = stripHtmlTags(rawHtml)

                        // Match to one of our Pebble projects by URL slug or keyword
                        val matchedProject = projects.firstOrNull { proj ->
                            link.contains(proj.wpSlug, ignoreCase = true) ||
                                link.contains(proj.repoName, ignoreCase = true) ||
                                text.contains(proj.displayName, ignoreCase = true)
                        }

                        if (matchedProject != null || link.contains("pebble", ignoreCase = true)) {
                            val projRepo = matchedProject?.repoName ?: "Pebble"
                            val projName = matchedProject?.displayName ?: "Webstas.net Pebble Post"
                            threads.add(
                                FeedbackThread(
                                    id = "wp:$commentId",
                                    source = FeedbackSource.WORDPRESS_COMMENT,
                                    projectRepo = projRepo,
                                    projectDisplayName = projName,
                                    title = "Comment on $projName",
                                    author = authorName,
                                    body = text,
                                    createdAtIso = dateIso,
                                    webUrl = link,
                                    wpPostId = postId,
                                    wpCommentId = commentId
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        threads
    }

    suspend fun replyToBlogComment(
        baseUrl: String,
        postId: Int,
        parentCommentId: Int,
        replyText: String,
        wpUsername: String,
        wpAppPassword: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (wpUsername.isBlank() || wpAppPassword.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("WordPress username & Application Password required in Settings")
            )
        }
        val cleanBase = baseUrl.trim().removeSuffix("/")
        val url = "$cleanBase/index.php/wp-json/wp/v2/comments"
        val creds = "$wpUsername:$wpAppPassword"
        val basicAuth = "Basic " + Base64.encodeToString(creds.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

        val payload = JSONObject().apply {
            put("post", postId)
            put("parent", parentCommentId)
            put("content", replyText)
        }

        val req = Request.Builder()
            .url(url)
            .header("Authorization", basicAuth)
            .header("User-Agent", "PebbleDevConsole-Android/1.0")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(RuntimeException("WordPress HTTP ${resp.code}: ${resp.body?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
