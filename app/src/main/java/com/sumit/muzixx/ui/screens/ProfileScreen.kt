package com.sumit.muzixx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ========================= STATES =========================
    // 🎯 LINKED TO REPOSITORY PERSISTENCE DATA ENGINE
    val currentUserName = viewModel.settings.userName
    var showEditDialog by remember { mutableStateOf(false) }
    var tempNameInput by remember { mutableStateOf("") }

    var selectedTabState by remember { mutableIntStateOf(0) }
    val tabTitles = remember { listOf("This Month", "This Year", "All-Time") }

    // ========================= STATS DATA UNPACKING =========================
    val totalPlaylistsCount = remember(viewModel.playlists.size) { viewModel.playlists.size }
    val totalSongsCount = remember(viewModel.songs.size) { viewModel.songs.size }

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

    // ========================= UI FRAMEWORK =========================
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text(text = "Profile", color = Color.White) },
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
            // ========================= PROFILE HEADER =========================
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(110.dp)
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
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(58.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 🎯 Read from preferences state
            Text(
                text = currentUserName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "MuzixX Listener",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB3B3B3)
            )

            Spacer(modifier = Modifier.height(22.dp))

            // ========================= EDIT PROFILE BUTTON =========================
            Button(
                onClick = {
                    tempNameInput = currentUserName
                    showEditDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile")
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ========================= METRIC TAB SELECTOR =========================
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
                            selectedContentColor = Color(0xFFE50914),
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

            Spacer(modifier = Modifier.height(26.dp))

            // ========================= STATS VIEW GRID =========================
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

            // ========================= TIMELINE INFO BANNER =========================
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
                        text = "Keep listening to build your listening stats and personalized music experience.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB3B3B3),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // ========================= MODAL EDIT PROFILE DIALOG =========================
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = Color(0xFF121212),
            title = { Text(text = "Edit Profile", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = tempNameInput,
                    onValueChange = { tempNameInput = it },
                    singleLine = true,
                    label = { Text("User Name") },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE50914),
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFFE50914),
                        unfocusedLabelColor = Color.Gray
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempNameInput.isNotBlank()) {
                            // 🎯 FIXED: Trigger background preference save safely via the pipeline
                            viewModel.settings.updateUserName(tempNameInput.trim())
                        }
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(text = "Cancel", color = Color.Gray)
                }
            }
        )
    }
}

// ========================= PROFILE STAT CARD COMPONENT =========================
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
                color = Color(0xFFE50914),
                textAlign = TextAlign.Center
            )
        }
    }
}