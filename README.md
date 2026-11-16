# Pebble Dev Console (Android)

Native Android developer console (Kotlin + Jetpack Compose + Material 3) for managing Pebble smartwatch apps and watchfaces, kicking off new `.pbw` releases, sideloading builds to your watch, and responding to user questions and comments across GitHub and **webstas.net**.

## Key Features

1. **Pebble Apps Dashboard (`Apps` tab)**
   - Pre-loaded with all 14 `BigWebstas` Pebble repositories (`Pebble-SleepSense`, `PebbleCardia`, `PebbleDNDQuietTime`, `PebbleDoesTheDogDie`, `PebbleHourlyChime`, `PebbleIndex2SuperProd`, `PebbleMovieTimes`, `PebbleRecorder`, `PebbleSuperProductivity`, `PebbleSuperProductivityFace`, `PebbleTopNyTimes`, `PebbleUTA`, `PebbleXDrip`, `xDrip-Pebble-E`) plus automatic discovery of any new Pebble repositories under your GitHub user.
   - Displays current version (`package.json` / `appinfo.json`), latest GitHub release tag, GitHub Actions CI status, open issue count, and Rebble Appstore heart count.

2. **One-Tap Version Bump & Release Studio (`Release` tab)**
   - Reads and updates `version` / `versionLabel` in `package.json` or `appinfo.json` with `+Patch` and `+Minor` quick-bump buttons and commits directly to your default branch via the GitHub Contents API.
   - Triggers your GitHub Actions `.pbw` build workflow (`workflow_dispatch` or `repository_dispatch`).
   - Publishes the tagged GitHub Release with release notes (which automatically feeds `sync-blog-github.sh` for **webstas.net** release posts).
   - Optionally uploads the built `.pbw` directly to the Rebble Developer Portal (`dev-portal.rebble.io` / `appstore-api.rebble.io`).

3. **Sideload `.pbw` to Your Watch**
   - Downloads the latest `.pbw` release asset and launches an Android `FileProvider` `ACTION_VIEW` intent targeting the installed Pebble / Rebble / Cobble / Gadgetbridge companion app.

4. **Unified Q&A Inbox & Background Notifications (`Q&A Inbox` tab)**
   - Aggregates open GitHub Issues, Pull Requests, and **webstas.net** WordPress blog comments on your Pebble project posts into a single filterable inbox.
   - Includes quick developer response templates, full thread history, direct reply posting (to GitHub Issues/PRs and WordPress REST API `/wp-json/wp/v2/comments`), and optional one-tap issue closing.
   - Runs a periodic `WorkManager` background sync (`FeedbackSyncWorker`) every 15 minutes to notify you of new questions or comments.

## Building & Running

```bash
cd pebble-dev-android
./gradlew testDebugUnitTest assembleDebug
```

The built debug APK is output to:
`pebble-dev-android/app/build/outputs/apk/debug/app-debug.apk`
