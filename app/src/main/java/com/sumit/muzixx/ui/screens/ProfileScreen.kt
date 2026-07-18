package com.sumit.muzixx.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumit.muzixx.viewmodel.MusicViewModel
import com.sumit.muzixx.viewmodel.AuthViewModel
import com.sumit.muzixx.utils.glassEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MusicViewModel,
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser = authViewModel.currentUser
    var showAuthScreen by remember { mutableStateOf(false) }

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

        var selectedTabState by remember { mutableIntStateOf(0) }
        val tabTitles = remember { listOf("This Month", "This Year", "All-Time") }

        val totalPlaylistsCount = viewModel.playlists.size
        val totalSongsCount = viewModel.songs.size

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(text = "Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile Pic",
                            tint = accentColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column {
                        Text(
                            text = if (currentUser != null) currentUser.displayName ?: currentUserName else currentUserName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (currentUser != null) "@Cloud Synced Account" else "MuzixX Listener",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showAuthScreen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 1.dp)
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
                                selectedContentColor = accentColor,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            )
                        }
                    })

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ProfileStatCard(
                        title = "Playlists",
                        value = totalPlaylistsCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatCard(
                        title = "Songs",
                        value = totalSongsCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

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

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ProfileStatCard(
                            title = "Songs Heard",
                            value = activeSongsHeard.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatCard(
                            title = "Listen Time",
                            value = "${listenHours}h ${listenMinutes}m ${listenSeconds}s",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassEffect(shape = RoundedCornerShape(26.dp))
                        .padding(20.dp)
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Keep listening to build your listening stats and personalized music experience.\n\nNote: Restart app to see live updates if you recently connected your account.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ProfileStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassEffect(shape = RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}