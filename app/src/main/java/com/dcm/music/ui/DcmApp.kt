package com.dcm.music.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dcm.music.MainViewModel
import com.dcm.music.R
import com.dcm.music.ui.components.MiniPlayer
import com.dcm.music.ui.screens.HomeScreen
import com.dcm.music.ui.screens.LibraryScreen
import com.dcm.music.ui.screens.PlayerScreen
import com.dcm.music.ui.screens.SearchScreen
import com.dcm.music.ui.screens.SettingsScreen
import com.dcm.music.ui.screens.SoundScreen
import com.dcm.music.ui.theme.DcmBlack
import com.dcm.music.ui.theme.DcmGray
import com.dcm.music.ui.theme.DcmNeon
import com.dcm.music.ui.theme.DcmSurface

private data class Tab(val route: String, val label: String)

private val tabs = listOf(
    Tab("home", "Inicio"),
    Tab("search", "Buscar"),
    Tab("library", "Biblioteca"),
    Tab("sound", "Sonido"),
    Tab("settings", "Ajustes")
)

@Composable
fun DcmApp(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val audioPermission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        granted = result[audioPermission] == true
    }
    LaunchedEffect(granted) { if (granted) vm.refresh() }

    val songs by vm.songs.collectAsState()
    val loading by vm.loading.collectAsState()
    val ps by vm.playerState.collectAsState()
    var playerOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = playerOpen) { playerOpen = false }

    Box(Modifier.fillMaxSize().background(DcmBlack)) {
        if (!granted) {
            PermissionScreen {
                val perms = mutableListOf(audioPermission)
                if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
                launcher.launch(perms.toTypedArray())
            }
        } else {
            val nav = rememberNavController()
            val backEntry by nav.currentBackStackEntryAsState()
            val route = backEntry?.destination?.route

            fun go(r: String) {
                nav.navigate(r) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            Scaffold(
                containerColor = DcmBlack,
                bottomBar = {
                    Column(Modifier.background(DcmBlack)) {
                        if (ps.hasMedia) {
                            MiniPlayer(ps, vm::togglePlay, vm::next) { playerOpen = true }
                        }
                        NavigationBar(containerColor = DcmSurface) {
                            tabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = route == tab.route,
                                    onClick = { go(tab.route) },
                                    icon = { Icon(tabIcon(tab.route), tab.label) },
                                    label = { Text(tab.label, fontSize = 10.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = DcmNeon,
                                        indicatorColor = DcmNeon,
                                        unselectedIconColor = DcmGray,
                                        unselectedTextColor = DcmGray
                                    )
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
                    composable("home") {
                        HomeScreen(
                            songs = songs,
                            onPlay = vm::play,
                            onShuffleAll = { vm.playShuffled(songs) },
                            onOpenSearch = { go("search") },
                            onOpenLibrary = { go("library") }
                        )
                    }
                    composable("search") { SearchScreen(songs, ps.mediaId, vm::play) }
                    composable("library") { LibraryScreen(songs, ps.mediaId, vm::play) }
                    composable("sound") { SoundScreen() }
                    composable("settings") { SettingsScreen(songs.size, loading, vm::refresh) }
                }
            }
        }

        AnimatedVisibility(
            visible = playerOpen && ps.hasMedia,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            PlayerScreen(
                state = ps,
                onClose = { playerOpen = false },
                onToggle = vm::togglePlay,
                onNext = vm::next,
                onPrevious = vm::previous,
                onSeek = vm::seekTo,
                onShuffle = vm::toggleShuffle,
                onRepeat = vm::cycleRepeat
            )
        }
    }
}

@Composable
private fun tabIcon(route: String): Painter = when (route) {
    "home" -> rememberVectorPainter(Icons.Filled.Home)
    "search" -> rememberVectorPainter(Icons.Filled.Search)
    "library" -> painterResource(R.drawable.ic_music_note)
    "sound" -> painterResource(R.drawable.ic_equalizer)
    else -> rememberVectorPainter(Icons.Filled.Settings)
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text("DCM MUSIC", color = DcmNeon, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
        Spacer(Modifier.height(8.dp))
        Text("Tu música, tu universo.", color = DcmGray)
        Spacer(Modifier.height(32.dp))
        Text(
            "Para mostrar y reproducir la música de tu teléfono necesitamos acceso a tus archivos de audio.",
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(DcmNeon)
                .clickable(onClick = onRequest)
                .padding(horizontal = 32.dp, vertical = 14.dp)
        ) {
            Text("Permitir acceso", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
