package com.sumit.muzixx

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.sumit.muzixx.data.fetchLocalSongs
import com.sumit.muzixx.ui.components.MiniPlayer
import com.sumit.muzixx.ui.components.MuzixBottomBar
import com.sumit.muzixx.ui.components.UpdateDialog
import com.sumit.muzixx.ui.screens.FullPlayerScreen
import com.sumit.muzixx.ui.screens.HomeScreen
import com.sumit.muzixx.ui.screens.LibraryScreen
import com.sumit.muzixx.ui.screens.ProfileScreen
import com.sumit.muzixx.ui.screens.SearchScreen
import com.sumit.muzixx.ui.theme.MuzixXTheme
import com.sumit.muzixx.viewmodel.MusicViewModel
import com.sumit.muzixx.data.network.UpdateChecker
import org.schabi.newpipe.extractor.NewPipe
import okhttp3.OkHttpClient
import com.sumit.muzixx.data.network.MuzixDownloader
import com.sumit.muzixx.ui.screens.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    private val musicViewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val baseHttpClient = OkHttpClient.Builder().build()
                NewPipe.init(MuzixDownloader.init(baseHttpClient))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        enableEdgeToEdge()
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.BLACK

        setContent {
            MuzixXTheme {
                val context = LocalContext.current
                var currentScreen by remember { mutableStateOf("Home") }
                var showFullPlayer by remember { mutableStateOf(false) }

                val selectedSong = musicViewModel.selectedSong
                val isFullScreenView = currentScreen == "Profile" || currentScreen == "Settings"

                BackHandler(enabled = true) {
                    if (showFullPlayer) {
                        showFullPlayer = false
                    } else if (currentScreen != "Home") {
                        currentScreen = "Home"
                    } else {
                        finish()
                    }
                }

                UpdateDialog()

                val loadLocalTracks = {
                    val trackList = fetchLocalSongs(context)
                    musicViewModel.loadLocalSongsWithLoadingState(trackList)
                }

                val storagePermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) loadLocalTracks()
                }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) {}

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        musicViewModel.initSettings(context)
                        musicViewModel.initMediaController(context)
                        musicViewModel.initStorage(context)
                        musicViewModel.initStatsManager(context)
                        musicViewModel.loadJioSaavnHomeContent()
                    }

                    snapshotFlow { musicViewModel.settings.checkUpdatesOnStart }
                        .first()
                        .let { shouldCheck ->
                            if (shouldCheck) {
                                UpdateChecker.check(context, isManualCheck = false)
                            }
                        }

                    val requiredStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }

                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, requiredStoragePermission
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        loadLocalTracks()
                    } else {
                        storagePermissionLauncher.launch(requiredStoragePermission)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        bottomBar = {
                            if (!isFullScreenView) {
                                MuzixBottomBar(
                                    currentScreen = currentScreen,
                                    onTabSelected = { currentScreen = it }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                "Home" -> HomeScreen(
                                    viewModel = musicViewModel,
                                    context = context,
                                    onProfileClick = { currentScreen = "Profile" },
                                    onMiniPlayerClick = { showFullPlayer = true },
                                    onSettingsClick = { currentScreen = "Settings" }
                                )
                                "Search" -> SearchScreen(viewModel = musicViewModel)
                                "Library" -> LibraryScreen(viewModel = musicViewModel)
                                "Profile" -> ProfileScreen(
                                    viewModel = musicViewModel,
                                    onBackClick = { currentScreen = "Home" },
                                    modifier = Modifier.fillMaxSize()
                                )
                                "Settings" -> SettingsScreen(
                                    viewModel = musicViewModel,
                                    onBackClick = { currentScreen = "Home" }
                                )
                            }
                        }
                    }

                    if (!isFullScreenView && selectedSong != null) {
                        MiniPlayer(
                            song = selectedSong,
                            isPlaying = musicViewModel.isPlaying,
                            onPlayPause = { musicViewModel.togglePlayPause() },
                            onMiniPlayerClick = { showFullPlayer = true },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 90.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = showFullPlayer,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        ) {
                            FullPlayerScreen(
                                viewModel = musicViewModel,
                                onDismiss = { showFullPlayer = false }
                            )
                        }
                    }
                }
            }
        }
    }
}