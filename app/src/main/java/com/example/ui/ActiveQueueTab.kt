package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DownloadItem
import com.example.data.DownloadStatus
import com.example.engine.TelegramVideoDownloaderEngine
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TelegramAccent
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.WarningAmber

@Composable
fun ActiveQueueTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeList by viewModel.activeDownloads.collectAsState()

    if (activeList.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = "No Downloads",
                        tint = TelegramBlue,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "কোনো ভিডিও ডাউনলোড হচ্ছে না",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "ডাউনলোডার ট্যাবে টেলিগ্রাম ভিডিওর লিঙ্ক দিয়ে ডাউনলোড শুরু করুন।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ডাউনলোড কিউ (${activeList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.pauseAll() },
                            modifier = Modifier.testTag("pause_all_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Pause All", fontSize = 12.sp)
                        }

                        FilledTonalButton(
                            onClick = { viewModel.resumeAll() },
                            modifier = Modifier.testTag("resume_all_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Resume All", fontSize = 12.sp)
                        }
                    }
                }
            }

            items(activeList, key = { it.id }) { item ->
                ActiveDownloadItemCard(
                    item = item,
                    onPause = { viewModel.pauseDownload(item.id) },
                    onResume = { viewModel.resumeDownload(item.id) },
                    onCancel = { viewModel.cancelDownload(item.id) }
                )
            }
        }
    }
}

@Composable
fun ActiveDownloadItemCard(
    item: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val progress = if (item.totalBytes > 0) {
        (item.downloadedBytes.toFloat() / item.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "DownloadProgress")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Title and Cancel button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (item.isPrivateChannel) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Private",
                                tint = WarningAmber,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Private Channel",
                                fontSize = 11.sp,
                                color = WarningAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Icon(
                                Icons.Default.Public,
                                contentDescription = "Public",
                                tint = TelegramAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Public / Stream",
                                fontSize = 11.sp,
                                color = TelegramAccent
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("cancel_download_${item.id}")
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel Download",
                        tint = ErrorRed
                    )
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .testTag("progress_bar_${item.id}"),
                color = if (item.status == DownloadStatus.PAUSED) WarningAmber else TelegramBlue,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Progress text row (Bytes + Speed + ETA)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val downloadedText = TelegramVideoDownloaderEngine.formatBytes(item.downloadedBytes)
                val totalText = if (item.totalBytes > 0) TelegramVideoDownloaderEngine.formatBytes(item.totalBytes) else "..."
                val percentText = "${(progress * 100).toInt()}%"

                Text(
                    text = "$downloadedText / $totalText ($percentText)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )

                if (item.status == DownloadStatus.DOWNLOADING) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = item.speedText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        if (item.etaSeconds > 0) {
                            Text(
                                text = "ETA: ${TelegramVideoDownloaderEngine.formatEta(item.etaSeconds)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else if (item.status == DownloadStatus.PAUSED) {
                    Text(
                        text = "Paused (বিরতি)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber
                    )
                }
            }

            // Controls (Pause / Resume)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (item.status == DownloadStatus.DOWNLOADING) {
                    OutlinedButton(
                        onClick = onPause,
                        modifier = Modifier.testTag("pause_button_${item.id}"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pause")
                    }
                } else {
                    FilledTonalButton(
                        onClick = onResume,
                        modifier = Modifier.testTag("resume_button_${item.id}"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = TelegramBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Resume")
                    }
                }
            }
        }
    }
}
