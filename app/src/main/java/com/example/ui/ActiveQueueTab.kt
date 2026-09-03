package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
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
import com.example.engine.MultiPlatformVideoResolver
import com.example.engine.TelegramVideoDownloaderEngine
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TelegramAccent
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.VipGold
import com.example.ui.theme.VipGoldBorder
import com.example.ui.theme.VipGoldLight
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
                        tint = VipGold,
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
                    text = "ডাউনলোডার ট্যাবে YouTube, Telegram, Facebook, Instagram, Google বা সরাসরি লিঙ্ক দিয়ে ডাউনলোড শুরু করুন।",
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ডাউনলোড কিউ (${activeList.size}টি ভিডিও)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = VipGold
                                )
                                if (activeList.size >= 10) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(VipGold)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "10+ BATCH",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "হাই-স্পিড মাল্টি-স্ট্রিম অ্যাক্সিলারেটর সক্রিয়",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

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
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = VipGold,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("Resume All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
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

    val platform = MultiPlatformVideoResolver.detectPlatform(item.originalUrl)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VipGoldBorder.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
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
            // Title & Platform Badge & Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Platform Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(platform.colorHex).copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = platform.displayName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(platform.colorHex)
                            )
                        }

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
                                text = "Fast Stream",
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
                color = if (item.status == DownloadStatus.PAUSED) WarningAmber else VipGold,
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
                } else if (item.status == DownloadStatus.FAILED) {
                    Text(
                        text = "Failed (ব্যর্থ)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )
                }
            }

            // Error Message Display
            if (item.status == DownloadStatus.FAILED && !item.errorMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ErrorRed.copy(alpha = 0.12f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = item.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed,
                        lineHeight = 16.sp
                    )
                }
            }

            // Action Buttons (Pause / Resume)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (item.status) {
                    DownloadStatus.DOWNLOADING -> {
                        OutlinedButton(
                            onClick = onPause,
                            modifier = Modifier.testTag("pause_btn_${item.id}"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Pause,
                                contentDescription = "Pause",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause")
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        FilledTonalButton(
                            onClick = onResume,
                            modifier = Modifier.testTag("resume_btn_${item.id}"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = VipGold,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume", fontWeight = FontWeight.Bold)
                        }
                    }
                    DownloadStatus.FAILED -> {
                        FilledTonalButton(
                            onClick = onResume,
                            modifier = Modifier.testTag("retry_btn_${item.id}"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Retry",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("পুনরায় চেষ্টা করুন (Retry)")
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
