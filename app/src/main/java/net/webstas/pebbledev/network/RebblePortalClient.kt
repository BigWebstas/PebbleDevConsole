package net.webstas.pebbledev.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.webstas.pebbledev.model.PebbleProject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class RebblePortalClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
) {

    suspend fun enrichFromRebbleAppstore(project: PebbleProject): PebbleProject =
        withContext(Dispatchers.IO) {
            val query = URLEncoder.encode(project.displayName, "UTF-8")
            val url = "https://appstore-api.rebble.io/api/v1/apps/search?query=$query"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "PebbleDevConsole-Android/1.0")
                .get()
                .build()
            try {
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val root = JSONObject(resp.body?.string().orEmpty())
                        val data = root.optJSONArray("data")
                        if (data != null && data.length() > 0) {
                            val app = data.getJSONObject(0)
                            val appId = app.optString("id")
                            val hearts = app.optInt("hearts", 0)
                            return@withContext project.copy(
                                rebbleAppId = appId.ifBlank { null },
                                rebbleHearts = hearts
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            }
            project
        }

    suspend fun uploadPbwReleaseToRebble(
        appId: String,
        pbwFile: File,
        releaseNotes: String,
        rebbleToken: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (rebbleToken.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Rebble Developer Portal token required in Settings")
            )
        }
        if (appId.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Rebble App ID not set for this project")
            )
        }
        val url = "https://appstore-api.rebble.io/api/v1/Submit/release/$appId"
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "pbw",
                pbwFile.name,
                pbwFile.asRequestBody("application/octet-stream".toMediaType())
            )
            .addFormDataPart("release_notes", releaseNotes)
            .build()

        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${rebbleToken.trim()}")
            .header("User-Agent", "PebbleDevConsole-Android/1.0")
            .post(multipart)
            .build()

        try {
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    Result.success("Published ${pbwFile.name} to Rebble Appstore")
                } else {
                    Result.failure(RuntimeException("Rebble Portal returned HTTP ${resp.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
