package net.webstas.pebbledev.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.webstas.pebbledev.data.ManifestVersionHelper
import net.webstas.pebbledev.model.DevCredentials
import net.webstas.pebbledev.model.FeedbackSource
import net.webstas.pebbledev.model.FeedbackThread
import net.webstas.pebbledev.model.PebbleProject
import net.webstas.pebbledev.model.ReleaseDraft
import net.webstas.pebbledev.model.WorkflowStatus

private val PebbleOrange = Color(0xFFFF5722)
private val PebbleAmber = Color(0xFFFFB300)
private val PebbleTeal = Color(0xFF26A69A)
private val SlateBackground = Color(0xFF0F172A)
private val SlateSurface = Color(0xFF1E293B)
private val SlateCard = Color(0xFF26334D)

private val PebbleDarkColorScheme = darkColorScheme(
    primary = PebbleOrange,
    secondary = PebbleTeal,
    tertiary = PebbleAmber,
    background = SlateBackground,
    surface = SlateSurface,
    surfaceVariant = SlateCard,
    onPrimary = Color.White,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFE2E8F0)
)

enum class ConsoleTab(val title: String) {
    PROJECTS("Apps"),
    RELEASE("Release"),
    INBOX("Q&A Inbox"),
    SETTINGS("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PebbleDevConsoleApp(viewModel: PebbleDevViewModel) {
    val state by viewModel.uiState.collectAsState()
    var activeTab by rememberSaveable { mutableStateOf(ConsoleTab.PROJECTS) }

    MaterialTheme(colorScheme = PebbleDarkColorScheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Pebble Dev Console",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${state.projects.size} Pebble projects • ${state.feedbackThreads.size} open threads",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    },
                    actions = {
                        if (state.isLoadingProjects || state.isLoadingInbox || state.isReleasingOrBuilding) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(20.dp),
                                strokeWidth = 2.dp,
                                color = PebbleOrange
                            )
                        }
                        IconButton(onClick = { viewModel.refreshAll() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SlateSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(containerColor = SlateSurface) {
                    NavigationBarItem(
                        selected = activeTab == ConsoleTab.PROJECTS,
                        onClick = { activeTab = ConsoleTab.PROJECTS },
                        icon = { Icon(Icons.Default.Star, contentDescription = null) },
                        label = { Text(ConsoleTab.PROJECTS.title) }
                    )
                    NavigationBarItem(
                        selected = activeTab == ConsoleTab.RELEASE,
                        onClick = { activeTab = ConsoleTab.RELEASE },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                        label = { Text(ConsoleTab.RELEASE.title) }
                    )
                    NavigationBarItem(
                        selected = activeTab == ConsoleTab.INBOX,
                        onClick = { activeTab = ConsoleTab.INBOX },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (state.feedbackThreads.isNotEmpty()) {
                                        Badge(containerColor = PebbleOrange) {
                                            Text(state.feedbackThreads.size.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null)
                            }
                        },
                        label = { Text(ConsoleTab.INBOX.title) }
                    )
                    NavigationBarItem(
                        selected = activeTab == ConsoleTab.SETTINGS,
                        onClick = { activeTab = ConsoleTab.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(ConsoleTab.SETTINGS.title) }
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            ) {
                state.statusBanner?.let { msg ->
                    StatusBannerBar(message = msg, onDismiss = { viewModel.clearStatusBanner() })
                }

                when (activeTab) {
                    ConsoleTab.PROJECTS -> ProjectsDashboardTab(
                        projects = state.projects,
                        onSelectForRelease = { proj ->
                            viewModel.selectProject(proj)
                            activeTab = ConsoleTab.RELEASE
                        },
                        onSideloadPbw = { proj ->
                            viewModel.sideloadLatestPbwToWatch(proj)
                        }
                    )

                    ConsoleTab.RELEASE -> ReleaseStudioTab(
                        projects = state.projects,
                        selectedProject = state.selectedProject,
                        isReleasing = state.isReleasingOrBuilding,
                        onSelectProject = { viewModel.selectProject(it) },
                        onKickOffVersion = { draft -> viewModel.kickOffNewVersion(draft) },
                        onSideloadPbw = { proj -> viewModel.sideloadLatestPbwToWatch(proj) }
                    )

                    ConsoleTab.INBOX -> UnifiedInboxTab(
                        threads = state.feedbackThreads,
                        activeFilter = state.inboxFilterSource,
                        selectedThread = state.selectedThread,
                        selectedThreadReplies = state.selectedThreadReplies,
                        onFilterChange = { viewModel.setInboxFilter(it) },
                        onSelectThread = { viewModel.selectFeedbackThread(it) },
                        onSendReply = { thread, body, closeIssue ->
                            viewModel.sendReplyToThread(thread, body, closeIssue)
                        }
                    )

                    ConsoleTab.SETTINGS -> DeveloperSettingsTab(
                        credentials = state.credentials,
                        onSaveCredentials = { viewModel.saveCredentials(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBannerBar(message: String, onDismiss: () -> Unit) {
    Surface(
        color = PebbleOrange.copy(alpha = 0.2f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ProjectsDashboardTab(
    projects: List<PebbleProject>,
    onSelectForRelease: (PebbleProject) -> Unit,
    onSideloadPbw: (PebbleProject) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(projects, key = { it.repoName }) { project ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = project.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${project.repoName} (${project.defaultBranch})",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                        Surface(
                            color = PebbleOrange.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = project.latestReleaseTag ?: "v${project.currentVersion}",
                                style = MaterialTheme.typography.labelMedium,
                                color = PebbleOrange,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (project.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = project.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WorkflowBadge(project.lastWorkflowStatus)

                        if (project.openIssuesCount > 0) {
                            Text(
                                text = "${project.openIssuesCount} open issues",
                                style = MaterialTheme.typography.labelSmall,
                                color = PebbleAmber
                            )
                        }

                        if (project.rebbleHearts > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = PebbleOrange,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${project.rebbleHearts}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSelectForRelease(project) },
                            colors = ButtonDefaults.buttonColors(containerColor = PebbleOrange),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kick Off Version")
                        }

                        OutlinedButton(
                            onClick = {
                                if (!project.latestPbwDownloadUrl.isNullOrBlank()) {
                                    onSideloadPbw(project)
                                } else {
                                    val url = "https://github.com/BigWebstas/${project.repoName}/releases"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (project.latestPbwDownloadUrl != null) "Sideload .pbw" else "Releases")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkflowBadge(status: WorkflowStatus) {
    val dotColor = when (status) {
        WorkflowStatus.SUCCESS -> PebbleTeal
        WorkflowStatus.IN_PROGRESS, WorkflowStatus.QUEUED -> PebbleAmber
        WorkflowStatus.FAILURE -> Color(0xFFEF4444)
        WorkflowStatus.IDLE -> Color(0xFF94A3B8)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "CI: ${status.label}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ReleaseStudioTab(
    projects: List<PebbleProject>,
    selectedProject: PebbleProject,
    isReleasing: Boolean,
    onSelectProject: (PebbleProject) -> Unit,
    onKickOffVersion: (ReleaseDraft) -> Unit,
    onSideloadPbw: (PebbleProject) -> Unit
) {
    var newVersion by remember(selectedProject.repoName, selectedProject.currentVersion) {
        mutableStateOf(ManifestVersionHelper.bumpPatchVersion(selectedProject.currentVersion))
    }
    var releaseNotes by rememberSaveable(selectedProject.repoName) {
        mutableStateOf("- Bug fixes and performance improvements for Pebble watches\n- Updated SDK build")
    }
    var workflowFile by rememberSaveable(selectedProject.repoName) {
        mutableStateOf("release.yml")
    }
    var commitManifestBump by rememberSaveable { mutableStateOf(true) }
    var triggerWorkflow by rememberSaveable { mutableStateOf(true) }
    var createGhRelease by rememberSaveable { mutableStateOf(true) }
    var publishToRebble by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Select Pebble App",
            style = MaterialTheme.typography.labelLarge,
            color = PebbleOrange
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            projects.forEach { proj ->
                FilterChip(
                    selected = proj.repoName == selectedProject.repoName,
                    onClick = { onSelectProject(proj) },
                    label = { Text(proj.displayName) }
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${selectedProject.displayName} — Kick Off New Version",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Current version in ${selectedProject.manifestPath}: ${selectedProject.currentVersion} " +
                        "(Latest tag: ${selectedProject.latestReleaseTag ?: "none"})",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newVersion,
                        onValueChange = { newVersion = it.removePrefix("v") },
                        label = { Text("New Version") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            newVersion = ManifestVersionHelper.bumpPatchVersion(selectedProject.currentVersion)
                        }
                    ) {
                        Text("+Patch")
                    }
                    OutlinedButton(
                        onClick = {
                            newVersion = ManifestVersionHelper.bumpMinorVersion(selectedProject.currentVersion)
                        }
                    ) {
                        Text("+Minor")
                    }
                }

                OutlinedTextField(
                    value = workflowFile,
                    onValueChange = { workflowFile = it },
                    label = { Text("GitHub Actions Workflow File (or repository_dispatch)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = releaseNotes,
                    onValueChange = { releaseNotes = it },
                    label = { Text("Release Notes (GitHub Release & Webstas.net Blog Post)") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                ReleaseStepCheckbox(
                    checked = commitManifestBump,
                    onCheckedChange = { commitManifestBump = it },
                    title = "1. Commit ${selectedProject.manifestPath} version bump to ${selectedProject.defaultBranch}"
                )
                ReleaseStepCheckbox(
                    checked = triggerWorkflow,
                    onCheckedChange = { triggerWorkflow = it },
                    title = "2. Trigger GitHub Actions .pbw build workflow ($workflowFile)"
                )
                ReleaseStepCheckbox(
                    checked = createGhRelease,
                    onCheckedChange = { createGhRelease = it },
                    title = "3. Publish GitHub Release v$newVersion (syncs to webstas.net)"
                )
                ReleaseStepCheckbox(
                    checked = publishToRebble,
                    onCheckedChange = { publishToRebble = it },
                    title = "4. Upload built .pbw to Rebble Developer Portal"
                )

                Button(
                    onClick = {
                        val cleanVer = newVersion.trim().removePrefix("v")
                        val tag = "v$cleanVer"
                        onKickOffVersion(
                            ReleaseDraft(
                                project = selectedProject,
                                newVersion = cleanVer,
                                tagName = tag,
                                releaseTitle = "${selectedProject.displayName} $tag",
                                releaseNotes = releaseNotes,
                                commitManifestBump = commitManifestBump,
                                triggerGithubWorkflow = triggerWorkflow,
                                workflowFileName = workflowFile.trim().ifBlank { "release.yml" },
                                createGithubRelease = createGhRelease,
                                publishToRebblePortal = publishToRebble
                            )
                        )
                    },
                    enabled = !isReleasing && newVersion.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PebbleOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Build, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isReleasing) "Building & Releasing..."
                        else "Kick Off v${newVersion.removePrefix("v")} Release"
                    )
                }

                if (!selectedProject.latestPbwDownloadUrl.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = { onSideloadPbw(selectedProject) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sideload Latest ${selectedProject.latestPbwFileName ?: ".pbw"} to Watch")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseStepCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun UnifiedInboxTab(
    threads: List<FeedbackThread>,
    activeFilter: FeedbackSource?,
    selectedThread: FeedbackThread?,
    selectedThreadReplies: List<net.webstas.pebbledev.model.FeedbackReply>,
    onFilterChange: (FeedbackSource?) -> Unit,
    onSelectThread: (FeedbackThread?) -> Unit,
    onSendReply: (FeedbackThread, String, Boolean) -> Unit
) {
    val filtered = remember(threads, activeFilter) {
        if (activeFilter == null) threads else threads.filter { it.source == activeFilter }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeFilter == null,
                onClick = { onFilterChange(null) },
                label = { Text("All (${threads.size})") }
            )
            FeedbackSource.values().forEach { src ->
                val count = threads.count { it.source == src }
                FilterChip(
                    selected = activeFilter == src,
                    onClick = { onFilterChange(src) },
                    label = { Text("${src.label} ($count)") }
                )
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No open questions or comments matching this filter.\nTap Refresh to query GitHub & Webstas.net.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { thread ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectThread(thread) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = when (thread.source) {
                                        FeedbackSource.GITHUB_ISSUE -> PebbleOrange.copy(alpha = 0.2f)
                                        FeedbackSource.GITHUB_PR -> PebbleTeal.copy(alpha = 0.2f)
                                        FeedbackSource.WORDPRESS_COMMENT -> PebbleAmber.copy(alpha = 0.2f)
                                        FeedbackSource.REBBLE_PORTAL -> Color.Magenta.copy(alpha = 0.2f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${thread.source.label} • ${thread.projectDisplayName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = thread.createdAtIso.take(10),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = thread.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "@${thread.author}: ${thread.body}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedThread != null) {
        ReplyDialog(
            thread = selectedThread,
            replies = selectedThreadReplies,
            onDismiss = { onSelectThread(null) },
            onSendReply = { body, closeAfter ->
                onSendReply(selectedThread, body, closeAfter)
                onSelectThread(null)
            }
        )
    }
}

@Composable
private fun ReplyDialog(
    thread: FeedbackThread,
    replies: List<net.webstas.pebbledev.model.FeedbackReply>,
    onDismiss: () -> Unit,
    onSendReply: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var replyText by rememberSaveable(thread.id) { mutableStateOf("") }
    var closeAfterReply by rememberSaveable(thread.id) { mutableStateOf(false) }

    val cannedTemplates = listOf(
        "Thanks for reporting! I've pushed a fix and kicked off a new .pbw release.",
        "Could you share which Pebble model (Aplite/Basalt/Chalk/Diorite) and firmware you're on?",
        "This feature is now available in the latest release on GitHub & the Rebble Appstore!"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = thread.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${thread.projectDisplayName} • by @${thread.author}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PebbleOrange
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = SlateBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = thread.body.ifBlank { "(No description body)" },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                if (replies.isNotEmpty()) {
                    Text(
                        text = "Conversation (${replies.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = PebbleTeal
                    )
                    replies.forEach { r ->
                        Surface(
                            color = SlateSurface,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "@${r.author} (${r.createdAtIso.take(10)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(text = r.body, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Text(
                    text = "Quick Developer Snippets:",
                    style = MaterialTheme.typography.labelSmall
                )
                cannedTemplates.forEach { snippet ->
                    Surface(
                        color = SlateSurface,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                replyText = if (replyText.isBlank()) snippet else "$replyText\n$snippet"
                            }
                    ) {
                        Text(
                            text = snippet,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    label = { Text("Your Reply") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (thread.source == FeedbackSource.GITHUB_ISSUE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = closeAfterReply,
                            onCheckedChange = { closeAfterReply = it }
                        )
                        Text(
                            text = "Close GitHub issue after posting reply",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendReply(replyText, closeAfterReply) },
                enabled = replyText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PebbleOrange)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Send Reply")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        if (thread.webUrl.isNotBlank()) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(thread.webUrl)))
                        }
                    }
                ) {
                    Text("Open Web")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun DeveloperSettingsTab(
    credentials: DevCredentials,
    onSaveCredentials: (DevCredentials) -> Unit
) {
    var ghUser by remember(credentials) { mutableStateOf(credentials.githubUsername) }
    var ghToken by remember(credentials) { mutableStateOf(credentials.githubToken) }
    var rebbleToken by remember(credentials) { mutableStateOf(credentials.rebbleToken) }
    var wpUrl by remember(credentials) { mutableStateOf(credentials.wpBaseUrl) }
    var wpUser by remember(credentials) { mutableStateOf(credentials.wpUsername) }
    var wpPass by remember(credentials) { mutableStateOf(credentials.wpAppPassword) }
    var bgSync by remember(credentials) { mutableStateOf(credentials.backgroundSyncEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "GitHub Integration (Releases, Actions & Issues)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PebbleOrange
                )
                OutlinedTextField(
                    value = ghUser,
                    onValueChange = { ghUser = it },
                    label = { Text("GitHub Username / Owner") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ghToken,
                    onValueChange = { ghToken = it },
                    label = { Text("GitHub Personal Access Token (repo, workflow)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Rebble Developer Portal (dev-portal.rebble.io)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PebbleTeal
                )
                OutlinedTextField(
                    value = rebbleToken,
                    onValueChange = { rebbleToken = it },
                    label = { Text("Rebble OAuth Bearer Token") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "WordPress Blog Comments (webstas.net)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PebbleAmber
                )
                OutlinedTextField(
                    value = wpUrl,
                    onValueChange = { wpUrl = it },
                    label = { Text("WordPress Site URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = wpUser,
                    onValueChange = { wpUser = it },
                    label = { Text("WordPress Admin Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = wpPass,
                    onValueChange = { wpPass = it },
                    label = { Text("WordPress Application Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Background Question & Comment Notifications",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Polls GitHub Issues & Webstas.net blog comments every 15m via WorkManager",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = bgSync, onCheckedChange = { bgSync = it })
            }
        }

        Button(
            onClick = {
                onSaveCredentials(
                    DevCredentials(
                        githubUsername = ghUser,
                        githubToken = ghToken,
                        rebbleToken = rebbleToken,
                        wpBaseUrl = wpUrl,
                        wpUsername = wpUser,
                        wpAppPassword = wpPass,
                        backgroundSyncEnabled = bgSync
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = PebbleOrange),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Settings")
        }
    }
}
