package net.webstas.pebbledev.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.webstas.pebbledev.data.DevPreferencesStore
import net.webstas.pebbledev.data.PbwSideloadInstallHelper
import net.webstas.pebbledev.data.PebbleProjectCatalog
import net.webstas.pebbledev.model.DevCredentials
import net.webstas.pebbledev.model.FeedbackReply
import net.webstas.pebbledev.model.FeedbackSource
import net.webstas.pebbledev.model.FeedbackThread
import net.webstas.pebbledev.model.PebbleProject
import net.webstas.pebbledev.model.ReleaseDraft
import net.webstas.pebbledev.network.GitHubApiClient
import net.webstas.pebbledev.network.RebblePortalClient
import net.webstas.pebbledev.network.WordPressApiClient
import net.webstas.pebbledev.worker.FeedbackSyncWorker
import java.io.File

data class PebbleDevUiState(
    val projects: List<PebbleProject> = PebbleProjectCatalog.defaultProjects,
    val selectedProject: PebbleProject = PebbleProjectCatalog.defaultProjects.first(),
    val feedbackThreads: List<FeedbackThread> = emptyList(),
    val selectedThread: FeedbackThread? = null,
    val selectedThreadReplies: List<FeedbackReply> = emptyList(),
    val credentials: DevCredentials = DevCredentials(),
    val isLoadingProjects: Boolean = false,
    val isLoadingInbox: Boolean = false,
    val isReleasingOrBuilding: Boolean = false,
    val statusBanner: String? = null,
    val inboxFilterSource: FeedbackSource? = null
)

class PebbleDevViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsStore = DevPreferencesStore(application)
    private val githubClient = GitHubApiClient()
    private val wpClient = WordPressApiClient()
    private val rebbleClient = RebblePortalClient()

    private val _uiState = MutableStateFlow(
        PebbleDevUiState(credentials = prefsStore.loadCredentials())
    )
    val uiState: StateFlow<PebbleDevUiState> = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    fun clearStatusBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }

    fun refreshAll() {
        refreshProjects()
        refreshInbox()
    }

    fun refreshProjects() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProjects = true) }
            val creds = _uiState.value.credentials
            val discovered = githubClient.fetchPebbleProjects(
                username = creds.githubUsername,
                token = creds.githubToken
            )
            // Enrich top projects concurrently
            val enriched = discovered.map { project ->
                async {
                    val withGh = githubClient.enrichProjectReleaseAndManifest(
                        username = creds.githubUsername,
                        project = project,
                        token = creds.githubToken
                    )
                    rebbleClient.enrichFromRebbleAppstore(withGh)
                }
            }.awaitAll()

            _uiState.update { state ->
                val currentSel = enriched.find { it.repoName == state.selectedProject.repoName }
                    ?: enriched.firstOrNull()
                    ?: state.selectedProject
                state.copy(
                    projects = enriched,
                    selectedProject = currentSel,
                    isLoadingProjects = false
                )
            }
        }
    }

    fun refreshInbox() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingInbox = true) }
            val state = _uiState.value
            val creds = state.credentials

            val ghDeferred = async {
                githubClient.fetchOpenIssuesAndQuestions(
                    username = creds.githubUsername,
                    projects = state.projects,
                    token = creds.githubToken
                )
            }
            val wpDeferred = async {
                wpClient.fetchPebbleBlogComments(
                    baseUrl = creds.wpBaseUrl,
                    projects = state.projects
                )
            }

            val combined = (ghDeferred.await() + wpDeferred.await())
                .sortedByDescending { it.createdAtIso }

            prefsStore.markFeedbackIdsSeen(combined.map { it.id })

            _uiState.update {
                it.copy(
                    feedbackThreads = combined,
                    isLoadingInbox = false
                )
            }
        }
    }

    fun selectProject(project: PebbleProject) {
        _uiState.update { it.copy(selectedProject = project) }
    }

    fun setInboxFilter(source: FeedbackSource?) {
        _uiState.update { it.copy(inboxFilterSource = source) }
    }

    fun selectFeedbackThread(thread: FeedbackThread?) {
        _uiState.update { it.copy(selectedThread = thread, selectedThreadReplies = emptyList()) }
        if (thread != null && thread.issueNumber != null &&
            (thread.source == FeedbackSource.GITHUB_ISSUE || thread.source == FeedbackSource.GITHUB_PR)
        ) {
            viewModelScope.launch {
                val creds = _uiState.value.credentials
                val replies = githubClient.fetchIssueComments(
                    username = creds.githubUsername,
                    repoName = thread.projectRepo,
                    issueNumber = thread.issueNumber,
                    token = creds.githubToken
                )
                _uiState.update { it.copy(selectedThreadReplies = replies) }
            }
        }
    }

    fun sendReplyToThread(thread: FeedbackThread, replyBody: String, closeAfterReply: Boolean = false) {
        if (replyBody.isBlank()) return
        viewModelScope.launch {
            val creds = _uiState.value.credentials
            when (thread.source) {
                FeedbackSource.GITHUB_ISSUE, FeedbackSource.GITHUB_PR -> {
                    val issueNum = thread.issueNumber ?: return@launch
                    val res = githubClient.postIssueComment(
                        username = creds.githubUsername,
                        repoName = thread.projectRepo,
                        issueNumber = issueNum,
                        commentBody = replyBody,
                        token = creds.githubToken
                    )
                    if (res.isSuccess && closeAfterReply) {
                        githubClient.closeIssue(
                            username = creds.githubUsername,
                            repoName = thread.projectRepo,
                            issueNumber = issueNum,
                            token = creds.githubToken
                        )
                    }
                    if (res.isSuccess) {
                        _uiState.update {
                            it.copy(statusBanner = "Posted reply to ${thread.projectRepo}#$issueNum")
                        }
                        selectFeedbackThread(thread)
                        refreshInbox()
                    } else {
                        _uiState.update {
                            it.copy(statusBanner = "Reply failed: ${res.exceptionOrNull()?.message}")
                        }
                    }
                }

                FeedbackSource.WORDPRESS_COMMENT -> {
                    val postId = thread.wpPostId ?: return@launch
                    val commentId = thread.wpCommentId ?: return@launch
                    val res = wpClient.replyToBlogComment(
                        baseUrl = creds.wpBaseUrl,
                        postId = postId,
                        parentCommentId = commentId,
                        replyText = replyBody,
                        wpUsername = creds.wpUsername,
                        wpAppPassword = creds.wpAppPassword
                    )
                    if (res.isSuccess) {
                        _uiState.update {
                            it.copy(statusBanner = "Posted comment reply on Webstas.net")
                        }
                        refreshInbox()
                    } else {
                        _uiState.update {
                            it.copy(statusBanner = "Blog reply failed: ${res.exceptionOrNull()?.message}")
                        }
                    }
                }

                FeedbackSource.REBBLE_PORTAL -> {
                    _uiState.update {
                        it.copy(statusBanner = "Opened Rebble Portal feedback response")
                    }
                }
            }
        }
    }

    fun kickOffNewVersion(draft: ReleaseDraft) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReleasingOrBuilding = true, statusBanner = "Starting release ${draft.tagName} for ${draft.project.displayName}...") }
            val creds = _uiState.value.credentials
            val stepsCompleted = mutableListOf<String>()
            val errors = mutableListOf<String>()

            // 1. Commit manifest version bump (package.json / appinfo.json)
            if (draft.commitManifestBump) {
                val commitRes = githubClient.commitManifestVersionBump(
                    username = creds.githubUsername,
                    project = draft.project,
                    newVersion = draft.newVersion,
                    token = creds.githubToken
                )
                commitRes.onSuccess { stepsCompleted.add(it) }
                    .onFailure { errors.add("Manifest bump: ${it.message}") }
            }

            // 2. Trigger GitHub Actions build / release workflow
            if (draft.triggerGithubWorkflow) {
                val wfRes = githubClient.triggerWorkflowDispatch(
                    username = creds.githubUsername,
                    repoName = draft.project.repoName,
                    branch = draft.project.defaultBranch,
                    workflowIdOrFileName = draft.workflowFileName,
                    versionInput = draft.newVersion,
                    token = creds.githubToken
                )
                wfRes.onSuccess { stepsCompleted.add(it) }
                    .onFailure { errors.add("Workflow: ${it.message}") }
            }

            // 3. Create GitHub Tag & Release (which also feeds sync-blog-github.sh for webstas.net)
            if (draft.createGithubRelease) {
                val relRes = githubClient.createGitHubRelease(
                    username = creds.githubUsername,
                    repoName = draft.project.repoName,
                    branch = draft.project.defaultBranch,
                    tagName = draft.tagName,
                    releaseTitle = draft.releaseTitle,
                    releaseNotes = draft.releaseNotes,
                    token = creds.githubToken
                )
                relRes.onSuccess { stepsCompleted.add("Created GitHub Release ${draft.tagName}") }
                    .onFailure { errors.add("Release: ${it.message}") }
            }

            // 4. Optional Rebble Appstore Portal upload if a .pbw is available
            if (draft.publishToRebblePortal && !draft.project.latestPbwDownloadUrl.isNullOrBlank()) {
                val cacheFile = File(
                    getApplication<Application>().cacheDir,
                    "pbw/${draft.project.repoName}-${draft.newVersion}.pbw"
                )
                val dl = githubClient.downloadPbwFile(
                    downloadUrl = draft.project.latestPbwDownloadUrl,
                    destFile = cacheFile,
                    token = creds.githubToken
                )
                if (dl.isSuccess) {
                    val rebbleRes = rebbleClient.uploadPbwReleaseToRebble(
                        appId = draft.project.rebbleAppId.orEmpty(),
                        pbwFile = cacheFile,
                        releaseNotes = draft.releaseNotes,
                        rebbleToken = creds.rebbleToken
                    )
                    rebbleRes.onSuccess { stepsCompleted.add(it) }
                        .onFailure { errors.add("Rebble: ${it.message}") }
                }
            }

            val summary = buildString {
                if (stepsCompleted.isNotEmpty()) {
                    append(stepsCompleted.joinToString(" • "))
                }
                if (errors.isNotEmpty()) {
                    if (isNotEmpty()) append(" | ")
                    append(errors.joinToString("; "))
                }
            }

            _uiState.update {
                it.copy(
                    isReleasingOrBuilding = false,
                    statusBanner = summary.ifBlank { "Release action completed" }
                )
            }
            refreshProjects()
        }
    }

    fun sideloadLatestPbwToWatch(project: PebbleProject) {
        val pbwUrl = project.latestPbwDownloadUrl
        if (pbwUrl.isNullOrBlank()) {
            _uiState.update {
                it.copy(statusBanner = "No .pbw release asset found yet for ${project.displayName}")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(statusBanner = "Downloading ${project.latestPbwFileName ?: "app.pbw"}...")
            }
            val app = getApplication<Application>()
            val fileName = project.latestPbwFileName ?: "${project.repoName}.pbw"
            val destFile = File(app.cacheDir, "pbw/$fileName")
            val dlRes = githubClient.downloadPbwFile(
                downloadUrl = pbwUrl,
                destFile = destFile,
                token = _uiState.value.credentials.githubToken
            )
            dlRes.onSuccess { file ->
                val sideloadRes = PbwSideloadInstallHelper.sideloadPbwToWatch(app, file)
                _uiState.update {
                    it.copy(
                        statusBanner = sideloadRes.getOrElse { err ->
                            "Sideload failed: ${err.message}"
                        }
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(statusBanner = "Failed downloading .pbw: ${err.message}")
                }
            }
        }
    }

    fun saveCredentials(newCredentials: DevCredentials) {
        prefsStore.saveCredentials(newCredentials)
        FeedbackSyncWorker.schedulePeriodicSync(
            getApplication(),
            newCredentials.backgroundSyncEnabled
        )
        _uiState.update {
            it.copy(
                credentials = newCredentials,
                statusBanner = "Saved developer credentials & sync settings"
            )
        }
        refreshAll()
    }
}
