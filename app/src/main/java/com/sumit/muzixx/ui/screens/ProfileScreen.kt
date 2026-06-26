package com.sumit.muzixx.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sumit.muzixx.viewmodel.MusicViewModel
import com.sumit.muzixx.viewmodel.AuthViewModel

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

        val activeSongsHeard = when (selectedTabState) {
            0 -> viewModel.stats.monthlySongsHeardState.intValue
            1 -> viewModel.stats.yearlySongsHeardState.intValue
            else -> viewModel.stats.totalSongsHeardState.intValue
        }

        val activeSeconds = when (selectedTabState) {
            0 -> viewModel.stats.monthlyPlaySecondsState.longValue
            1 -> viewModel.stats.yearlyPlaySecondsState.longValue
            else -> viewModel.stats.totalPlaySecondsState.longValue
        }

        val listenHours = activeSeconds / 3600
        val listenMinutes = (activeSeconds % 3600) / 60
        val listenSeconds = activeSeconds % 60

        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            containerColor = Color.Black,
            topBar = {
                TopAppBar(
                    title = { Text(text = "Profile", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFE50914), Color(0xFFFF4D4D))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile Pic",
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column {
                        Text(
                            text = if (currentUser != null) currentUser.displayName ?: currentUserName else currentUserName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (currentUser != null) "@Cloud Synced Account" else "MuzixX Listener",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB3B3B3)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showAuthScreen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Account")
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF222222), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                SecondaryTabRow(
                    selectedTabIndex = selectedTabState,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    indicator = {},
                    divider = {},
                    tabs = {
                        tabTitles.forEachIndexed { index, title ->
                            val isSelected = selectedTabState == index
                            Tab(
                                selected = isSelected,
                                onClick = { selectedTabState = index },
                                selectedContentColor = accentColor,
                                unselectedContentColor = Color.Gray,
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            )
                        }
                    })

                Spacer(modifier = Modifier.height(24.dp))

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

                Spacer(modifier = Modifier.height(28.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF111111)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Your Music Journey",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Keep listening to build your listening stats and personalized music experience.\nNote: Restart app to See live Updates if you recently Connected the account",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB3B3B3),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
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
    Surface(
        modifier = modifier,
        color = Color(0xFF111111),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFB3B3B3)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}