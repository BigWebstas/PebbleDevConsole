package net.webstas.pebbledev.data

import net.webstas.pebbledev.model.PebbleProject
import org.json.JSONObject

object PebbleProjectCatalog {

    val defaultProjects: List<PebbleProject> = listOf(
        PebbleProject(
            repoName = "Pebble-SleepSense",
            displayName = "Pebble SleepSense",
            defaultBranch = "main",
            wpSlug = "pebble-sleepsense",
            description = "Smart sleep tracking, alarm & heart-rate analytics for Pebble watches"
        ),
        PebbleProject(
            repoName = "PebbleCardia",
            displayName = "Pebble Cardia",
            defaultBranch = "master",
            wpSlug = "pebble-cardia",
            description = "Continuous heart rate monitoring and zone alerts for Pebble"
        ),
        PebbleProject(
            repoName = "PebbleDNDQuietTime",
            displayName = "Pebble DND Quiet Time Sync",
            defaultBranch = "main",
            wpSlug = "pebble-dnd-quiet-time",
            description = "Two-way Do Not Disturb / Quiet Time synchronization with Android"
        ),
        PebbleProject(
            repoName = "PebbleDoesTheDogDie",
            displayName = "Pebble Does the Dog Die",
            defaultBranch = "main",
            wpSlug = "pebble-does-the-dog-die",
            description = "Quick media content trigger warnings on your wrist via DoesTheDogDie API"
        ),
        PebbleProject(
            repoName = "PebbleHourlyChime",
            displayName = "Pebble Hourly Chime",
            defaultBranch = "master",
            wpSlug = "pebble-hourly-chime",
            description = "Customizable hourly vibration chime and schedule for Pebble"
        ),
        PebbleProject(
            repoName = "PebbleIndex2SuperProd",
            displayName = "Pebble Index 2 SuperProd",
            defaultBranch = "main",
            wpSlug = "pebble-index-to-super-productivity",
            description = "Voice note & index capture on Pebble synced to Super Productivity"
        ),
        PebbleProject(
            repoName = "PebbleMovieTimes",
            displayName = "Pebble Movie Times",
            defaultBranch = "master",
            wpSlug = "pebble-movie-times",
            description = "Local theater showtimes and ratings for Pebble"
        ),
        PebbleProject(
            repoName = "PebbleRecorder",
            displayName = "PebbleRecorder",
            defaultBranch = "main",
            wpSlug = "pebble-recorder",
            description = "Microphone audio memo & transcription client for Pebble"
        ),
        PebbleProject(
            repoName = "PebbleSuperProductivity",
            displayName = "Pebble Super Productivity",
            defaultBranch = "main",
            wpSlug = "pebble-super-productivity",
            description = "Task list, active task timer, and Pomodoro controller for Super Productivity"
        ),
        PebbleProject(
            repoName = "PebbleSuperProductivityFace",
            displayName = "Pebble Super Productivity Face",
            defaultBranch = "main",
            wpSlug = "pebble-super-productivity-face",
            description = "Watchface displaying current Super Productivity task, time tracked, and progress"
        ),
        PebbleProject(
            repoName = "PebbleTopNyTimes",
            displayName = "Pebble NYT Top Stories",
            defaultBranch = "main",
            wpSlug = "pebble-top-nytimes",
            description = "New York Times top stories reader for Pebble"
        ),
        PebbleProject(
            repoName = "PebbleUTA",
            displayName = "PebbleUTA",
            defaultBranch = "main",
            wpSlug = "pebble-uta-transit",
            description = "Real-time Utah Transit Authority FrontRunner, TRAX & bus arrivals on Pebble"
        ),
        PebbleProject(
            repoName = "PebbleXDrip",
            displayName = "PebbleXDrip",
            defaultBranch = "master",
            wpSlug = "pebble-xdrip-quick-launch",
            description = "CGM blood glucose quick-launch app & graph viewer for xDrip+"
        ),
        PebbleProject(
            repoName = "xDrip-Pebble-E",
            displayName = "xDrip-Pebble Enhanced",
            defaultBranch = "master",
            wpSlug = "xdrip-pebble-watchface-and-health-sync",
            description = "Enhanced xDrip CGM watchface with Pebble Health step and sleep sync"
        )
    )

    fun isPebbleRepository(repoName: String, description: String = ""): Boolean {
        val lowerName = repoName.lowercase()
        val lowerDesc = description.lowercase()
        return lowerName.contains("pebble") ||
            lowerDesc.contains("pebble") ||
            defaultProjects.any { it.repoName.equals(repoName, ignoreCase = true) }
    }
}

object ManifestVersionHelper {

    /**
     * Extracts the version string from either a modern `package.json` or legacy `appinfo.json`.
     */
    fun extractVersion(manifestJson: String): String? {
        return try {
            val obj = JSONObject(manifestJson)
            when {
                obj.has("version") -> obj.optString("version").takeIf { it.isNotBlank() }
                obj.has("versionLabel") -> obj.optString("versionLabel").takeIf { it.isNotBlank() }
                obj.has("pebble") -> {
                    val pebbleObj = obj.getJSONObject("pebble")
                    pebbleObj.optString("versionLabel").takeIf { it.isNotBlank() }
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Updates the version string inside `package.json` or `appinfo.json` while preserving formatting.
     */
    fun updateVersionInManifest(manifestJson: String, newVersion: String): String {
        val cleanVersion = newVersion.removePrefix("v").trim()
        return try {
            val obj = JSONObject(manifestJson)
            if (obj.has("version")) {
                obj.put("version", cleanVersion)
            }
            if (obj.has("versionLabel")) {
                obj.put("versionLabel", cleanVersion)
            }
            if (obj.has("pebble") && obj.optJSONObject("pebble") != null) {
                val pebbleObj = obj.getJSONObject("pebble")
                if (pebbleObj.has("versionLabel")) {
                    pebbleObj.put("versionLabel", cleanVersion)
                }
            }
            obj.toString(2) + "\n"
        } catch (_: Exception) {
            manifestJson
        }
    }

    /**
     * Suggests the next patch version (e.g., 1.2.3 -> 1.2.4) for quick one-tap version bumps.
     */
    fun bumpPatchVersion(currentVersion: String): String {
        val cleaned = currentVersion.removePrefix("v").trim()
        val parts = cleaned.split(".")
        if (parts.size >= 3) {
            val patch = parts[2].filter { it.isDigit() }.toIntOrNull()
            if (patch != null) {
                return "${parts[0]}.${parts[1]}.${patch + 1}"
            }
        } else if (parts.size == 2) {
            val minor = parts[1].filter { it.isDigit() }.toIntOrNull()
            if (minor != null) {
                return "${parts[0]}.${minor + 1}"
            }
        }
        return "$cleaned.1"
    }

    fun bumpMinorVersion(currentVersion: String): String {
        val cleaned = currentVersion.removePrefix("v").trim()
        val parts = cleaned.split(".")
        if (parts.size >= 2) {
            val minor = parts[1].filter { it.isDigit() }.toIntOrNull()
            if (minor != null) {
                return "${parts[0]}.${minor + 1}.0"
            }
        }
        return "1.1.0"
    }
}
