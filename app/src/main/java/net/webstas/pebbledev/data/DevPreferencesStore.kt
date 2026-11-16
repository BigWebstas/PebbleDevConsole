package net.webstas.pebbledev.data

import android.content.Context
import android.content.SharedPreferences
import net.webstas.pebbledev.model.DevCredentials

class DevPreferencesStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pebble_dev_console_prefs", Context.MODE_PRIVATE)

    fun loadCredentials(): DevCredentials {
        return DevCredentials(
            githubUsername = prefs.getString(KEY_GH_USER, "BigWebstas") ?: "BigWebstas",
            githubToken = prefs.getString(KEY_GH_TOKEN, "") ?: "",
            rebbleToken = prefs.getString(KEY_REBBLE_TOKEN, "") ?: "",
            wpBaseUrl = prefs.getString(KEY_WP_URL, "https://webstas.net") ?: "https://webstas.net",
            wpUsername = prefs.getString(KEY_WP_USER, "") ?: "",
            wpAppPassword = prefs.getString(KEY_WP_PASS, "") ?: "",
            backgroundSyncEnabled = prefs.getBoolean(KEY_BG_SYNC, true)
        )
    }

    fun saveCredentials(creds: DevCredentials) {
        prefs.edit()
            .putString(KEY_GH_USER, creds.githubUsername.trim().ifBlank { "BigWebstas" })
            .putString(KEY_GH_TOKEN, creds.githubToken.trim())
            .putString(KEY_REBBLE_TOKEN, creds.rebbleToken.trim())
            .putString(KEY_WP_URL, creds.wpBaseUrl.trim().removeSuffix("/").ifBlank { "https://webstas.net" })
            .putString(KEY_WP_USER, creds.wpUsername.trim())
            .putString(KEY_WP_PASS, creds.wpAppPassword.trim())
            .putBoolean(KEY_BG_SYNC, creds.backgroundSyncEnabled)
            .apply()
    }

    fun getSeenFeedbackIds(): Set<String> {
        return prefs.getStringSet(KEY_SEEN_FEEDBACK_IDS, emptySet()) ?: emptySet()
    }

    fun markFeedbackIdsSeen(ids: Collection<String>) {
        val updated = getSeenFeedbackIds().toMutableSet()
        updated.addAll(ids)
        prefs.edit().putStringSet(KEY_SEEN_FEEDBACK_IDS, updated).apply()
    }

    companion object {
        private const val KEY_GH_USER = "gh_user"
        private const val KEY_GH_TOKEN = "gh_token"
        private const val KEY_REBBLE_TOKEN = "rebble_token"
        private const val KEY_WP_URL = "wp_url"
        private const val KEY_WP_USER = "wp_user"
        private const val KEY_WP_PASS = "wp_pass"
        private const val KEY_BG_SYNC = "bg_sync_enabled"
        private const val KEY_SEEN_FEEDBACK_IDS = "seen_feedback_ids"
    }
}
