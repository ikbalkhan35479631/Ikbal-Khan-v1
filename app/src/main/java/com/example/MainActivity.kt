package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ActiveQueueTab
import com.example.ui.DownloaderTab
import com.example.ui.MainViewModel
import com.example.ui.PrivateGuideTab
import com.example.ui.SavedVideosTab
import com.example.ui.VideoPlayerDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.VipAmber
import com.example.ui.theme.VipGold
import com.example.ui.theme.VipGoldLight
import com.example.ui.theme.WarningAmber

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleShareIntent(intent)

        setContent {
            MyApplicationTheme {
                MainScreen(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.onUrlChanged(sharedText)
                viewModel.onTabSelected(0)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val completedDownloads by viewModel.completedDownloads.collectAsState()
    val playingVideo by viewModel.playingVideo.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(VipGold, VipAmber)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "VIP TG & Files",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = VipGold
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(VipGold.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "👑 VIP",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = VipGold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                // Tab 0: Downloader
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    icon = {
                        Icon(Icons.Default.Download, contentDescription = "Downloader")
                    },
                    label = { Text("ডাউনলোডার", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VipGold,
                        selectedTextColor = VipGold,
                        indicatorColor = VipGold.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_downloader_tab")
                )

                // Tab 1: Active Downloads
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    icon = {
                        if (activeDownloads.isNotEmpty()) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = VipGold, contentColor = Color.Black) {
                                        Text("${activeDownloads.size}", fontWeight = FontWeight.Bold)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = "Active Downloads")
                            }
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = "Active Downloads")
                        }
                    },
                    label = { Text("চলমান", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VipGold,
                        selectedTextColor = VipGold,
                        indicatorColor = VipGold.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_active_tab")
                )

                // Tab 2: Saved Videos
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.onTabSelected(2) },
                    icon = {
                        if (completedDownloads.isNotEmpty()) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                        Text("${completedDownloads.size}")
                                    }
                                }
                            ) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = "Saved Videos")
                            }
                        } else {
                            Icon(Icons.Default.VideoLibrary, contentDescription = "Saved Videos")
                        }
                    },
                    label = { Text("সংরক্ষিত", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VipGold,
                        selectedTextColor = VipGold,
                        indicatorColor = VipGold.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_saved_tab")
                )

                // Tab 3: Private Guide & Settings
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.onTabSelected(3) },
                    icon = {
                        Icon(Icons.Default.Lock, contentDescription = "Private Guide")
                    },
                    label = { Text("প্রাইভেট গাইড", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VipGold,
                        selectedTextColor = VipGold,
                        indicatorColor = VipGold.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_guide_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DownloaderTab(
                    viewModel = viewModel,
                    onOpenGuide = { viewModel.onTabSelected(3) },
                    modifier = Modifier.fillMaxSize()
                )
                1 -> ActiveQueueTab(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
                2 -> SavedVideosTab(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
                3 -> PrivateGuideTab(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Video Player Dialog (shows when a video is being played)
    playingVideo?.let { item ->
        VideoPlayerDialog(
            item = item,
            onDismiss = { viewModel.closePlayer() },
            onShare = { viewModel.shareVideo(item, context) },
            onExport = { viewModel.exportToGallery(item, context) }
        )
    }
}
