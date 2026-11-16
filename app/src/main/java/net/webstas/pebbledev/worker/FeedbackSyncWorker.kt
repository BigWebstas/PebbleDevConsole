package net.webstas.pebbledev.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import net.webstas.pebbledev.MainActivity
import net.webstas.pebbledev.data.DevPreferencesStore
import net.webstas.pebbledev.data.PebbleProjectCatalog
import net.webstas.pebbledev.network.GitHubApiClient
import net.webstas.pebbledev.network.WordPressApiClient
import java.util.concurrent.TimeUnit

class FeedbackSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = DevPreferencesStore(applicationContext)
        val creds = prefs.loadCredentials()
        if (!creds.backgroundSyncEnabled) {
            return Result.success()
        }

        val githubClient = GitHubApiClient()
        val wpClient = WordPressApiClient()
        val projects = PebbleProjectCatalog.defaultProjects

        val ghThreads = githubClient.fetchOpenIssuesAndQuestions(
            username = creds.githubUsername,
            projects = projects,
            token = creds.githubToken
        )
        val wpThreads = wpClient.fetchPebbleBlogComments(
            baseUrl = creds.wpBaseUrl,
            projects = projects
        )

        val allThreads = ghThreads + wpThreads
        val seenIds = prefs.getSeenFeedbackIds()
        val newThreads = allThreads.filter { it.id !in seenIds }

        if (seenIds.isNotEmpty() && newThreads.isNotEmpty()) {
            val first = newThreads.first()
            val title = if (newThreads.size == 1) {
                "New ${first.source.label} on ${first.projectDisplayName}"
            } else {
                "${newThreads.size} new Pebble developer questions/comments"
            }
            val body = "${first.author}: ${first.title.ifBlank { first.body }}"
            showNotification(applicationContext, title, body)
        }

        prefs.markFeedbackIdsSeen(allThreads.map { it.id })
        return Result.success()
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pebble Developer Questions & Releases",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new GitHub issues, PRs, and Webstas.net blog comments on Pebble apps"
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "pebble_dev_inbox_channel"
        private const val WORK_NAME = "pebble_dev_feedback_sync"
        private const val NOTIFICATION_ID = 4201

        fun schedulePeriodicSync(context: Context, enabled: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (!enabled) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<FeedbackSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            wm.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
