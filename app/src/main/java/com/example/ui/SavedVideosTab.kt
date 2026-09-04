package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.data.DownloadItem
import com.example.engine.MultiPlatformVideoResolver
import com.example.engine.TelegramVideoDownloaderEngine
import com.example.ui.theme.ApkGreen
import com.example.ui.theme.AudioPurple
import com.example.ui.theme.DocBlue
import com.example.ui.theme.PdfRed
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.VipGold
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.ZipOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedVideosTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val completedList by viewModel.completedDownloads.collectAsState()
    var filterType by remember { mutableStateOf("ALL") }

    val filteredList = when (filterType) {
        "VIDEO" -> completedList.filter {
            it.mimeType.startsWith("video/") || it.title.endsWith(".mp4", true) || it.title.endsWith(".mkv", true)
        }
        "FILES" -> completedList.filter {
            !it.mimeType.startsWith("video/") && !it.title.endsWith(".mp4", true) && !it.title.endsWith(".mkv", true)
        }
        else -> completedList
    }

    if (completedList.isEmpty()) {
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
                        imageVector = Icons.Default.Movie,
                        contentDescription = "No Saved Media",
                        tint = VipGold,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "কোনো সংরক্ষিত ভিডিও বা ফাইল নেই",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "ডাউনলোড শেষ হলে ভিডিও বা ফাইল এখানে দেখতে পাবেন এবং সরাসরি প্লে বা ওপেন করতে পারবেন।",
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "সংরক্ষিত ফাইল ও ভিডিও (${completedList.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = VipGold
                        )

                        Text(
                            text = "সব মুছে ফেলুন",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { viewModel.clearAllCompleted(context) }
                        )
                    }

                    // Filter chips: All, Videos, Files
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = filterType == "ALL",
                            onClick = { filterType = "ALL" },
                            label = { Text("সবগুলো (${completedList.size})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = filterType == "VIDEO",
                            onClick = { filterType = "VIDEO" },
                            label = {
                                val count = completedList.count { it.mimeType.startsWith("video/") || it.title.endsWith(".mp4", true) }
                                Text("ভিডিও ($count)", fontSize = 11.sp)
                            }
                        )
                        FilterChip(
                            selected = filterType == "FILES",
                            onClick = { filterType = "FILES" },
                            label = {
                                val count = completedList.count { !it.mimeType.startsWith("video/") && !it.title.endsWith(".mp4", true) }
                                Text("ফাইল ও ডক্স ($count)", fontSize = 11.sp)
                            }
                        )
                    }
                }
            }

            items(filteredList, key = { it.id }) { item ->
                SavedVideoCard(
                    item = item,
                    onPlay = { viewModel.playVideo(item) },
                    onOpenFile = { viewModel.openDownloadedFile(item, context) },
                    onShare = { viewModel.shareVideo(item, context) },
                    onExport = { viewModel.exportToGallery(item, context) },
                    onDelete = { viewModel.deleteCompleted(item, context) }
                )
            }
        }
    }
}

@Composable
fun SavedVideoCard(
    item: DownloadItem,
    onPlay: () -> Unit,
    onOpenFile: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = item.completedAt?.let {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(it))
    } ?: "Downloaded"

    val isVideo = item.mimeType.startsWith("video/") || item.title.endsWith(".mp4", true) || item.title.endsWith(".mkv", true)
    val isAudio = item.mimeType.startsWith("audio/") || item.title.endsWith(".mp3", true)
    val isPdf = item.mimeType == "application/pdf" || item.title.endsWith(".pdf", true)
    val isApk = item.title.endsWith(".apk", true)
    val isZip = item.title.endsWith(".zip", true) || item.title.endsWith(".rar", true)

    val itemIcon = when {
        isPdf -> Icons.Default.Description
        isAudio -> Icons.Default.MusicNote
        isApk || isZip -> Icons.Default.Folder
        isVideo -> Icons.Default.PlayArrow
        else -> Icons.Default.Description
    }

    val iconTint = when {
        isPdf -> PdfRed
        isAudio -> AudioPurple
        isApk -> ApkGreen
        isZip -> ZipOrange
        isVideo -> VipGold
        else -> DocBlue
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_video_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (isVideo) onPlay() else onOpenFile() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail / Play Icon Area
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = itemIcon,
                        contentDescription = if (isVideo) "Play Video" else "Open File",
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                val platform = MultiPlatformVideoResolver.detectPlatform(item.originalUrl)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

                        Text(
                            text = TelegramVideoDownloaderEngine.formatBytes(item.totalBytes),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = VipGold
                        )

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )

                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }

                    if (item.isPrivateChannel) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Private Post",
                                fontSize = 11.sp,
                                color = WarningAmber,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isVideo) {
                    FilledTonalButton(
                        onClick = onPlay,
                        modifier = Modifier.testTag("play_button_${item.id}"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ভিডিও প্লে করুন", fontSize = 12.sp)
                    }
                } else {
                    FilledTonalButton(
                        onClick = onOpenFile,
                        modifier = Modifier.testTag("open_file_button_${item.id}"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Open", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ফাইল ওপেন করুন", fontSize = 12.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onExport,
                        modifier = Modifier.testTag("export_button_${item.id}")
                    ) {
                        Icon(
                            Icons.Default.DownloadDone,
                            contentDescription = if (isVideo) "Save to Gallery" else "Save to Public Storage",
                            tint = TelegramBlue
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.testTag("share_button_${item.id}")
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_button_${item.id}")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
