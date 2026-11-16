package net.webstas.pebbledev.model

enum class FeedbackSource(val label: String) {
    GITHUB_ISSUE("GitHub Issue"),
    GITHUB_PR("GitHub PR"),
    WORDPRESS_COMMENT("WordPress Blog"),
    REBBLE_PORTAL("Rebble Portal")
}

enum class WorkflowStatus(val label: String) {
    QUEUED("Queued"),
    IN_PROGRESS("Building"),
    SUCCESS("Success"),
    FAILURE("Failed"),
    IDLE("Idle")
}

data class PebbleProject(
    val repoName: String,
    val displayName: String,
    val defaultBranch: String,
    val wpSlug: String,
    val description: String = "",
    val currentVersion: String = "1.0.0",
    val latestReleaseTag: String? = null,
    val latestPbwDownloadUrl: String? = null,
    val latestPbwFileName: String? = null,
    val openIssuesCount: Int = 0,
    val rebbleAppId: String? = null,
    val rebbleHearts: Int = 0,
    val lastWorkflowStatus: WorkflowStatus = WorkflowStatus.IDLE,
    val lastWorkflowRunUrl: String? = null,
    val manifestPath: String = "package.json",
    val rawManifestJson: String? = null,
    val manifestSha: String? = null
)

data class FeedbackThread(
    val id: String,
    val source: FeedbackSource,
    val projectRepo: String,
    val projectDisplayName: String,
    val title: String,
    val author: String,
    val body: String,
    val createdAtIso: String,
    val webUrl: String,
    val issueNumber: Int? = null,
    val wpPostId: Int? = null,
    val wpCommentId: Int? = null,
    val isUnread: Boolean = false,
    val state: String = "open",
    val commentsCount: Int = 0
)

data class FeedbackReply(
    val id: String,
    val author: String,
    val body: String,
    val createdAtIso: String
)

data class ReleaseDraft(
    val project: PebbleProject,
    val newVersion: String,
    val tagName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val commitManifestBump: Boolean = true,
    val triggerGithubWorkflow: Boolean = true,
    val workflowFileName: String = "release.yml",
    val createGithubRelease: Boolean = true,
    val publishToRebblePortal: Boolean = false
)

data class DevCredentials(
    val githubUsername: String = "BigWebstas",
    val githubToken: String = "",
    val rebbleToken: String = "",
    val wpBaseUrl: String = "https://webstas.net",
    val wpUsername: String = "",
    val wpAppPassword: String = "",
    val backgroundSyncEnabled: Boolean = true
)
