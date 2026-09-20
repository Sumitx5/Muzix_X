package com.sumit.muzixx.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sumit.muzixx.viewmodel.MusicViewModel
import com.sumit.muzixx.viewmodel.AuthViewModel
import com.sumit.muzixx.utils.glassEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MusicViewModel,
    authViewModel: AuthViewModel,
    onSettingsClick: () -> Unit,
    onIntegrationClick: () -> Unit,
    onListenTogetherClick: () -> Unit,
    onPermClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser = authViewModel.currentUser
    var showAuthScreen by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            viewModel.overwriteStatsFromCloud(
                totalHeard = authViewModel.firestoreTotalSongsHeard,
                monthlyHeard = authViewModel.firestoreMonthlySongsHeard,
                yearlyHeard = authViewModel.firestoreYearlySongsHeard,
                totalSec = authViewModel.firestoreTotalPlaySeconds,
                monthlySec = authViewModel.firestoreMonthlyPlaySeconds,
                yearlySec = authViewModel.firestoreYearlyPlaySeconds
            )
        }
    }

    if (showStatsDialog) {
        UserStatsDialog(
            viewModel = viewModel,
            onDismiss = { showStatsDialog = false }
        )
    }

    if (showAuthScreen) {
        AuthScreen(
            authViewModel = authViewModel,
            viewModel = viewModel,
            onAuthSuccess = { showAuthScreen = false },
            onBackClick = { showAuthScreen = false }
        )
    } else {
        val currentUserName = viewModel.settings.userName
        val accentColor = MaterialTheme.colorScheme.primary

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Profile & Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    windowInsets = WindowInsets.statusBars
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassEffect(RoundedCornerShape(24.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile Pic",
                                    tint = accentColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentUser != null) currentUser.displayName ?: currentUserName else currentUserName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val isSynced = if (currentUser != null) "Synced Account" else "Not Synced"
                                Text(
                                    text = "MuzixX Listener | $isSynced",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Button(
                            onClick = { showAuthScreen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Manage Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                val playtimeMonthly = viewModel.stats.monthlyPlaySecondsState.longValue
                val listenHours = playtimeMonthly / 3600
                val listenMinutes = (playtimeMonthly % 3600) / 60

                ProfileStatCard(
                    title = "Monthly Stats",
                    note = true,
                    stats = arrayOf(
                        "Songs Heard" to "${viewModel.stats.monthlySongsHeardState.intValue}",
                        "Total Time" to "${listenHours}h ${listenMinutes}m"
                    ),
                    onClick = { showStatsDialog = true }
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ItemCardSettings(
                        icon = Icons.Rounded.Cable,
                        title = "Integrations",
                        subtitle = "Get Cloud Playlists From Various apps",
                        onClick = { onIntegrationClick() }
                    )

                    ItemCardSettings(
                        icon = Icons.Rounded.Groups,
                        title = "Listen Together",
                        subtitle = "Stream synced audio with friends",
                        onClick = { onListenTogetherClick() },
                        badgeText = "Coming Soon"
                    )

                    ItemCardSettings(
                        icon = Icons.Rounded.Security,
                        title = "Permissions",
                        subtitle = "Manage Permission access",
                        onClick = { onPermClick() }
                    )

                    ItemCardSettings(
                        icon = Icons.Rounded.Update,
                        title = "Check for Updates",
                        subtitle = "Checks the Latest GitHub releases",
                        onClick = { viewModel.triggerUpdateCheck() }
                    )

                    ItemCardSettings(
                        icon = Icons.Rounded.Settings,
                        title = "Settings",
                        subtitle = "Playback, theme & audio quality",
                        onClick = { onSettingsClick() }
                    )

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserStatsDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    var selectedTabState by remember { mutableIntStateOf(0) }
    val tabTitles = remember { listOf("Monthly", "Yearly", "All-Time") }
    val accentColor = MaterialTheme.colorScheme.primary

    val totalPlaylistsCount = viewModel.playlists.size
    val totalSongsCount = viewModel.songs.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .glassEffect(shape = RoundedCornerShape(28.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Listening Statistics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SecondaryTabRow(
                    selectedTabIndex = selectedTabState,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    indicator = {},
                    divider = {},
                    tabs = {
                        tabTitles.forEachIndexed { index, title ->
                            val isSelected = selectedTabState == index
                            Tab(
                                selected = isSelected,
                                onClick = { selectedTabState = index },
                                selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                text = {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isSelected) accentColor.copy(0.2f) else Color.Transparent
                                            )
                                            .padding(horizontal = 6.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
                AnimatedContent(
                    targetState = selectedTabState,
                    transitionSpec = {
                        (fadeIn() + scaleIn(initialScale = 0.96f)).togetherWith(fadeOut())
                    },
                    label = "TabStatsTransition"
                ) { targetTab ->
                    val activeSongsHeard = when (targetTab) {
                        0 -> viewModel.stats.monthlySongsHeardState.intValue
                        1 -> viewModel.stats.yearlySongsHeardState.intValue
                        else -> viewModel.stats.totalSongsHeardState.intValue
                    }

                    val activeSeconds = when (targetTab) {
                        0 -> viewModel.stats.monthlyPlaySecondsState.longValue
                        1 -> viewModel.stats.yearlyPlaySecondsState.longValue
                        else -> viewModel.stats.totalPlaySecondsState.longValue
                    }

                    val listenHours = activeSeconds / 3600
                    val listenMinutes = (activeSeconds % 3600) / 60
                    val listenSeconds = activeSeconds % 60

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileStatCard(
                            title = "Songs Heard",
                            stats = arrayOf("" to "$activeSongsHeard"),
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatCard(
                            title = "Listen Time",
                            stats = arrayOf("" to "${listenHours}h ${listenMinutes}m ${listenSeconds}s"),
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileStatCard(
                        title = "Playlists",
                        stats = arrayOf("Saved" to "$totalPlaylistsCount"),
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatCard(
                        title = "Library",
                        stats = arrayOf("Tracks" to "$totalSongsCount"),
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassEffect(shape = RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Your Music Journey",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Keep listening to build your listening stats and personalized music experience.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStatCard(
    title: String,
    modifier: Modifier = Modifier,
    note: Boolean = false,
    onClick: () -> Unit,
    vararg stats: Pair<String, String>
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassEffect(shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            if (stats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stats.forEach { (name, value) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$name: ",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (note) {
                Text(
                    text = "Click to Open Stats",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ItemCardSettings(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassEffect(RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .glassEffect(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (badgeText != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}