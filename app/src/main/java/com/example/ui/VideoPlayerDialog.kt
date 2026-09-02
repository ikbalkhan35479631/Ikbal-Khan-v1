package com.example.ui

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.DownloadItem
import com.example.engine.TelegramVideoDownloaderEngine
import com.example.ui.theme.TelegramBlue
import java.io.File

@Composable
fun VideoPlayerDialog(
    item: DownloadItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit
) {
    val context = LocalContext.current
    val videoFile = remember(item.filePath) { File(item.filePath) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top bar in player
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Size: ${TelegramVideoDownloaderEngine.formatBytes(item.totalBytes)}",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_player_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Player",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Video View Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF101010)),
                    contentAlignment = Alignment.Center
                ) {
                    if (videoFile.exists()) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("native_video_view"),
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setVideoURI(Uri.fromFile(videoFile))
                                    val mediaController = MediaController(ctx)
                                    mediaController.setAnchorView(this)
                                    setMediaController(mediaController)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = false
                                        start()
                                    }
                                }
                            }
                        )
                    } else {
                        Text(
                            text = "Video file not found on disk",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onShare,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("player_share_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share")
                    }

                    FilledTonalButton(
                        onClick = onExport,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("player_export_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF223447),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.DownloadDone, contentDescription = "Save to Gallery")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Gallery")
                    }
                }
            }
        }
    }
}
