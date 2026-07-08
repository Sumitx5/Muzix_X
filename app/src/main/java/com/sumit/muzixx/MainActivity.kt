package com.sumit.muzixx

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.material3.MaterialTheme
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
import com.sumit.muzixx.data.Song
import com.sumit.muzixx.ui.screens.IntegrationScreen
import com.sumit.muzixx.ui.screens.ListenTogether
import com.sumit.muzixx.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    private val musicViewModel: MusicViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private var defaultCrashHandler: Thread.UncaughtExceptionHandler? = null

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupCrashHandler()

        intent?.data?.let { uri ->
            handleIncomingDeepLink(uri)
        }

        musicViewModel.initSettings(applicationContext)
        musicViewModel.initStatsManager(applicationContext)
        musicViewModel.initMediaController(applicationContext){
            musicViewModel.initStorage(applicationContext)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val baseHttpClient = OkHttpClient.Builder().build()
                NewPipe.init(MuzixDownloader.init(baseHttpClient))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        enableEdgeToEdge()

        setContent {
            MuzixXTheme(viewModel = musicViewModel) {
                val context = LocalContext.current
                var currentScreen by remember { mutableStateOf("Home") }
                var showFullPlayer by remember { mutableStateOf(false) }

                val selectedSong = musicViewModel.selectedSong
                val isFullScreenView = currentScreen == "Profile" || currentScreen == "Settings"

                val view = androidx.compose.ui.platform.LocalView.current
                if (!view.isInEditMode) {
                    val isSystemInDark = androidx.compose.foundation.isSystemInDarkTheme()
                    val isLightMode = if (musicViewModel.isSettingsInitialized()) {
                        musicViewModel.settings.appTheme == "Match System" && !isSystemInDark
                    } else {
                        !isSystemInDark
                    }

                    DisposableEffect(isLightMode) {
                        val window = (context as android.app.Activity).window
                        val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)

                        window.statusBarColor = android.graphics.Color.TRANSPARENT
                        window.navigationBarColor = android.graphics.Color.TRANSPARENT

                        insetsController.isAppearanceLightStatusBars = isLightMode
                        insetsController.isAppearanceLightNavigationBars = isLightMode

                        onDispose {}
                    }
                }

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
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    top = innerPadding.calculateTopPadding(),
                                    bottom = if (isFullScreenView) 0.dp else innerPadding.calculateBottomPadding()
                                )
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                when (currentScreen) {
                                    "Home" -> HomeScreen(
                                        viewModel = musicViewModel,
                                        authViewModel = authViewModel,
                                        context = context,
                                        onProfileClick = { currentScreen = "Profile" },
                                        onSettingsClick = { currentScreen = "Settings" },
                                        onIntegrationClick = { currentScreen = "Integration" },
                                        onListenTogetherClick = { currentScreen = "ListenTogether" }
                                    )
                                    "Search" -> SearchScreen(viewModel = musicViewModel)
                                    "Library" -> LibraryScreen(viewModel = musicViewModel)
                                    "Profile" -> ProfileScreen(
                                        viewModel = musicViewModel,
                                        authViewModel = authViewModel,
                                        onBackClick = { currentScreen = "Home" },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    "Settings" -> SettingsScreen(
                                        viewModel = musicViewModel,
                                        onBackClick = { currentScreen = "Home" }
                                    )
                                    "Integration" -> IntegrationScreen(
                                        viewModel = musicViewModel,
                                        onBackClick = { currentScreen = "Home"},
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    "ListenTogether" -> ListenTogether(
                                        onBackClick = { currentScreen = "Home"},
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            if (!isFullScreenView) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Color.Transparent)
                                ) {
                                    if (selectedSong != null) {
                                        MiniPlayer(
                                            song = selectedSong,
                                            isPlaying = musicViewModel.isPlaying,
                                            onPlayPause = { musicViewModel.togglePlayPause() },
                                            onMiniPlayerClick = { showFullPlayer = true },
                                            modifier = Modifier
                                                .align(Alignment.CenterHorizontally)
                                                .padding(bottom = 8.dp)
                                        )
                                    }

                                    MuzixBottomBar(
                                        currentScreen = currentScreen,
                                        onTabSelected = { currentScreen = it }
                                    )
                                }
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = showFullPlayer,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent)
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

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                authViewModel.saveDataOnExitOrCrash(
                    playlistsCount = musicViewModel.playlists.size,
                    songsCount = musicViewModel.songs.size,
                    totalHeard = musicViewModel.stats.totalSongsHeardState.intValue,
                    monthlyHeard = musicViewModel.stats.monthlySongsHeardState.intValue,
                    yearlyHeard = musicViewModel.stats.yearlySongsHeardState.intValue,
                    totalSec = musicViewModel.stats.totalPlaySecondsState.longValue,
                    monthlySec = musicViewModel.stats.monthlyPlaySecondsState.longValue,
                    yearlySec = musicViewModel.stats.yearlyPlaySecondsState.longValue
                )
            } catch (e: Exception) {
                android.util.Log.e("MuzixXLifecycle", "Standard exit backup failed", e)
            }
        }
    }

    private fun setupCrashHandler() {
        defaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                android.util.Log.w("MuzixXCrash", "Critical exception intercepted! Backing up statistics absolutely...")

                authViewModel.saveDataOnExitOrCrash(
                    playlistsCount = musicViewModel.playlists.size,
                    songsCount = musicViewModel.songs.size,
                    totalHeard = musicViewModel.stats.totalSongsHeardState.intValue,
                    monthlyHeard = musicViewModel.stats.monthlySongsHeardState.intValue,
                    yearlyHeard = musicViewModel.stats.yearlySongsHeardState.intValue,
                    totalSec = musicViewModel.stats.totalPlaySecondsState.longValue,
                    monthlySec = musicViewModel.stats.monthlyPlaySecondsState.longValue,
                    yearlySec = musicViewModel.stats.yearlyPlaySecondsState.longValue
                )
            } catch (e: Exception) {
                android.util.Log.e("MuzixXCrash", "Failed to preserve engine stats during crash pipeline", e)
            }

            defaultCrashHandler?.uncaughtException(thread, throwable)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.data?.let { uri ->
            handleIncomingDeepLink(uri)
        }
    }

    private fun handleIncomingDeepLink(uri: android.net.Uri) {
        if ((uri.scheme == "muzixx" || uri.scheme == "https") && uri.pathContainsShare()) {
            val songId = uri.getQueryParameter("id")
            val title = uri.getQueryParameter("title") ?: "Unknown Track"
            val artist = uri.getQueryParameter("artist") ?: "Unknown Artist"
            val artUri = uri.getQueryParameter("art") ?: ""

            if (!songId.isNullOrBlank()) {
                val sharedSong = Song(
                    id = songId,
                    title = title,
                    artist = artist,
                    uri = "",
                    artUri = artUri,
                    duration = 0,
                    isStreaming = true,
                    folderName = "",
                    type = if (songId.startsWith("yt_")) "yt" else "saavn"
                )

                lifecycleScope.launch {
                    kotlinx.coroutines.delay(600.milliseconds)
                    musicViewModel.playMusicCollection(listOf(sharedSong), 0)
                }
            }
        }
    }

    private fun android.net.Uri.pathContainsShare(): Boolean {
        return this.host == "share" || this.path?.contains("share") == true
    }

    override fun onPause() {
        super.onPause()
        musicViewModel.saveCurrentPlaybackPosition()
    }

    override fun onDestroy() {
        musicViewModel.saveCurrentPlaybackPosition()
        super.onDestroy()
    }
}