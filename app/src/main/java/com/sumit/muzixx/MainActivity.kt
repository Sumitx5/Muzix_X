package com.sumit.muzixx

// imports
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sumit.muzixx.data.fetchLocalSongs
import com.sumit.muzixx.ui.theme.MuzixXTheme
import com.sumit.muzixx.ui.screens.FullPlayerScreen
import com.sumit.muzixx.ui.components.MiniPlayer
import com.sumit.muzixx.ui.components.MuzixBottomBar
import com.sumit.muzixx.viewmodel.MusicViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sumit.muzixx.ui.screens.LibraryScreen
import com.sumit.muzixx.ui.screens.SearchScreen // ADDED: Import for your updated search engine

class MainActivity : ComponentActivity() {
    private val musicViewModel: MusicViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MuzixXTheme {
                val context = LocalContext.current


                var currentScreen by remember { mutableStateOf("Library") }
                var showFullPlayer by remember { mutableStateOf(false) }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    musicViewModel.initMediaController(context)
                    musicViewModel.initStorage(context)

                    val trackList = fetchLocalSongs(context)
                    musicViewModel.loadSongs(trackList)

                    musicViewModel.initializeLocalSongsPlaylist()
                }

                Scaffold(
                    bottomBar = {
                        Column {
                            MiniPlayer(
                                song = musicViewModel.selectedSong,
                                isPlaying = musicViewModel.isPlaying,
                                onPlayPause = { musicViewModel.togglePlayPause() },
                                onMiniPlayerClick = { showFullPlayer = true }
                            )

                            MuzixBottomBar(
                                currentScreen = currentScreen,
                                onTabSelected = { selectedTab: String -> currentScreen = selectedTab }
                            )
                        }
                    }
                ) { innerPadding ->
                    when (currentScreen) {
                        "Home" -> {
                            Text("Home Screen Vibe Coming Soon!", modifier = Modifier.padding(innerPadding))
                        }
                        "Search" -> {
                            // 💡 FIXED: Replaced text placeholder with your actual functional SearchScreen layout
                            SearchScreen(
                                viewModel = musicViewModel,
                                context = context,
                                onAddToPlaylistClick = { song ->
                                    // Triggers your existing custom library playlist adder dialogue
                                    musicViewModel.selectedPlaylist = null
                                    currentScreen = "Library"
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        "Library" -> {
                            LibraryScreen(
                                viewModel = musicViewModel,
                                context = context,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }

                if (showFullPlayer) {
                    ModalBottomSheet(
                        onDismissRequest = { showFullPlayer = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        dragHandle = { BottomSheetDefaults.DragHandle() }
                    ) {

                        FullPlayerScreen(
                            viewModel = musicViewModel
                        )
                    }
                }
            }
        }
    }
}