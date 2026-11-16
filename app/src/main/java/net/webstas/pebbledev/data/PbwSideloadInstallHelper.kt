package net.webstas.pebbledev.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object PbwSideloadInstallHelper {

    /**
     * Launches an Android Intent to sideload a downloaded `.pbw` bundle into the
     * installed Pebble / Rebble / Cobble / Gadgetbridge companion app.
     */
    fun createSideloadIntent(context: Context, pbwFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pbwFile
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/octet-stream")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun sideloadPbwToWatch(context: Context, pbwFile: File): Result<String> {
        return try {
            val intent = createSideloadIntent(context, pbwFile)
            val chooser = Intent.createChooser(intent, "Sideload ${pbwFile.name} to Pebble").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Result.success("Opened Pebble companion installer for ${pbwFile.name}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
