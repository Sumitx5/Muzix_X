package com.sumit.muzixx

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import com.sumit.muzixx.ui.screens.*
import com.sumit.muzixx.ui.theme.MuzixXTheme
import com.sumit.muzixx.viewmodel.MusicViewModel
import com.sumit.muzixx.data.network.UpdateChecker
import org.schabi.newpipe.extractor.NewPipe
import okhttp3.OkHttpClient
import com.sumit.muzixx.data.network.MuzixDownloader
import com.sumit.muzixx.utils.YouTubeResolver
import com.sumit.muzixx.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    private val musicViewModel: MusicViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private var defaultCrashHandler: Thread.UncaughtExceptionHandler? = null

    private var isProcessingDeepLink = false

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupCrashHandler()
        unlockHighRefreshRate()

        intent?.data?.let { uri -> handleIncomingDeepLink(uri) }

        musicViewModel.initSettings(applicationContext)
        musicViewModel.initStatsManager(applicationContext)
        musicViewModel.initMediaController(applicationContext) {
            musicViewModel.initStorage(applicationContext, skipSongRestoration = isProcessingDeepLink)
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
                val isFullScreenView = currentScreen == "Profile" || currentScreen == "Settings" || currentScreen == "Integration" || currentScreen == "ListenTogether"

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
                    lifecycleScope.launch(Dispatchers.IO) {
                        val trackList = fetchLocalSongs(context)
                        withContext(Dispatchers.Main) {
                            musicViewModel.loadLocalSongsWithLoadingState(trackList)
                        }
                    }
                }

                val storagePermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted -> if (isGranted) loadLocalTracks() }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) {}

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    lifecycleScope.launch(Dispatchers.IO) {
                        delay(200.milliseconds)
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

                    if (ContextCompat.checkSelfPermission(context, requiredStoragePermission) == PackageManager.PERMISSION_GRANTED) {
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
                                AnimatedContent(
                                    targetState = currentScreen,
                                    transitionSpec = {
                                        val expressiveSpring = spring<androidx.compose.ui.unit.IntOffset>(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                        val fadeSpec = tween<Float>(durationMillis = 300)

                                        if (targetState == "Home" || initialState == "Profile" || initialState == "Settings") {
                                            (slideInHorizontally(animationSpec = expressiveSpring, initialOffsetX = { -it }) + fadeIn(fadeSpec)) togetherWith
                                                    (slideOutHorizontally(animationSpec = expressiveSpring, targetOffsetX = { it }) + fadeOut(fadeSpec))
                                        } else {
                                            (slideInHorizontally(animationSpec = expressiveSpring, initialOffsetX = { it }) + fadeIn(fadeSpec)) togetherWith
                                                    (slideOutHorizontally(animationSpec = expressiveSpring, targetOffsetX = { -it }) + fadeOut(fadeSpec))
                                        }
                                    },
                                    label = "CoreScreenNavigation"
                                ) { screen ->
                                    when (screen) {
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
                                            onBackClick = { currentScreen = "Home" },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        "ListenTogether" -> {}
                                    }
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
                                        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                            MiniPlayer(
                                                song = selectedSong,
                                                isPlaying = musicViewModel.isPlaying,
                                                onPlayPause = { musicViewModel.togglePlayPause() },
                                                onMiniPlayerClick = { showFullPlayer = true },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    ) {
                                        MuzixBottomBar(
                                            currentScreen = currentScreen,
                                            onTabSelected = { currentScreen = it }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showFullPlayer,
                        enter = slideInVertically(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                            initialOffsetY = { it }
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                            targetOffsetY = { it }
                        ) + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
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

    private fun unlockHighRefreshRate() {
        val window = this.window

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = window.decorView.display
            if (display != null) {
                val modes = display.supportedModes
                val currentMode = display.mode
                val highestRefreshRateMode = modes
                    .filter { it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight }
                    .maxByOrNull { it.refreshRate }

                val maxHz = modes.maxOfOrNull { it.refreshRate } ?: 120f
                val params = window.attributes

                if (highestRefreshRateMode != null) {
                    params.preferredDisplayModeId = highestRefreshRateMode.modeId
                }
                params.preferredRefreshRate = maxHz
                window.attributes = params

                android.util.Log.d("Muzix Performance", "Unlocked Peak Refresh Rate: ${maxHz}Hz")
            }
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        }
    }

    override fun onStop() {
        super.onStop()
        backupStatistics()
    }

    private fun setupCrashHandler() {
        defaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            backupStatistics()
            defaultCrashHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun backupStatistics() {
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
                android.util.Log.e("Muzix Stats", "Data backup exception recorded", e)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { uri -> handleIncomingDeepLink(uri) }
    }

    private fun handleIncomingDeepLink(uri: android.net.Uri) {
        if ((uri.scheme == "muzixx" || uri.scheme == "https") && uri.pathContainsShare()) {
            val payload = uri.getQueryParameter("p")
            if (!payload.isNullOrBlank()) {
                try {
                    val decodedBytes = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE)
                    val jsonString = String(decodedBytes, Charsets.UTF_8)
                    val jsonObject = org.json.JSONObject(jsonString)
                    val songId = jsonObject.getString("i")
                    val title = jsonObject.optString("t", "Unknown Track")
                    val artist = jsonObject.optString("a", "Unknown Artist")
                    val rawArt = jsonObject.optString("r", "")

                    if (songId.isNotBlank()) {
                        isProcessingDeepLink = true

                        lifecycleScope.launch(Dispatchers.IO) {
                            val sharedSong = YouTubeResolver.resolveDeepLinkSong(
                                songId = songId,
                                title = title,
                                artist = artist,
                                rawArt = rawArt
                            )

                            withContext(Dispatchers.Main) {
                                delay(300.milliseconds)
                                musicViewModel.playMusicCollection(listOf(sharedSong), 0)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun android.net.Uri.pathContainsShare(): Boolean = this.host == "share" || this.path?.contains("share") == true
    override fun onPause() { super.onPause(); musicViewModel.saveCurrentPlaybackPosition() }
    override fun onDestroy() {
        musicViewModel.saveCurrentPlaybackPosition()
        super.onDestroy()
    }
}