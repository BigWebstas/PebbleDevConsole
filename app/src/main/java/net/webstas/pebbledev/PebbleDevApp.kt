package net.webstas.pebbledev

import android.app.Application
import net.webstas.pebbledev.data.DevPreferencesStore
import net.webstas.pebbledev.worker.FeedbackSyncWorker

class PebbleDevApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val creds = DevPreferencesStore(this).loadCredentials()
        FeedbackSyncWorker.schedulePeriodicSync(this, creds.backgroundSyncEnabled)
    }
}
