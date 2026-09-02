package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.DownloadStatus
import com.example.engine.LinkType
import com.example.engine.TelegramVideoDownloaderEngine
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TelegramAccent
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.TelegramDarkBlue
import com.example.ui.theme.WarningAmber

@Composable
fun DownloaderTab(
    viewModel: MainViewModel,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val urlInput by viewModel.urlInput.collectAsState()
    val parsedLink by viewModel.parsedLink.collectAsState()
    val customTitle by viewModel.customTitle.collectAsState()
    val multiThreaded by viewModel.multiThreaded.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card / Banner
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TelegramBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Turbo",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Telegram Video Downloader",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "ছোট বা বড় (Small or Big) যেকোনো টেলিগ্রাম ভিডিও হাই-স্পিডে ডাউনলোড করুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // URL Input Section
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ভিডিও লিঙ্ক দিন (Paste Telegram Video Link)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { viewModel.onUrlChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("url_input_field"),
                    placeholder = {
                        Text(
                            "e.g. https://t.me/c/1234567890/42 or https://t.me/...",
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Link",
                            tint = TelegramBlue
                        )
                    },
                    trailingIcon = {
                        if (urlInput.isNotBlank()) {
                            IconButton(
                                onClick = { viewModel.clearInput() },
                                modifier = Modifier.testTag("clear_url_button")
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TelegramBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Paste button row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.pasteFromClipboard(context) },
                        modifier = Modifier.testTag("paste_link_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = "Paste",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Paste Link",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOpenGuide() }
                    ) {
                        Icon(
                            Icons.Default.HelpOutline,
                            contentDescription = "Help",
                            tint = TelegramBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Private Link Guide",
                            style = MaterialTheme.typography.labelMedium,
                            color = TelegramBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Link Analysis & Type Badge (if URL entered)
        parsedLink?.let { info ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (info.isPrivate) Color(0xFF1E2838) else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (icon, badgeText, badgeColor) = when (info.linkType) {
                            LinkType.PRIVATE_CHANNEL -> Triple(Icons.Default.Lock, "Private Channel Post", WarningAmber)
                            LinkType.PUBLIC_CHANNEL -> Triple(Icons.Default.Public, "Public Channel Post", TelegramAccent)
                            LinkType.BOT_FILE -> Triple(Icons.Default.SmartToy, "Telegram Bot Stream", SuccessGreen)
                            LinkType.TELEGRAM_WEB -> Triple(Icons.Default.Public, "Telegram Web Link", TelegramBlue)
                            LinkType.DIRECT_VIDEO -> Triple(Icons.Default.VideoLibrary, "Direct Media Stream", SuccessGreen)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeColor.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = badgeText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = info.channelIdentifier,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }

                    Text(
                        text = info.infoMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { viewModel.onCustomTitleChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("file_title_field"),
                        label = { Text("Save File As (ফাইলের নাম)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Large Video Accelerator Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(TelegramBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Speed",
                            tint = TelegramBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Big Video Multi-Stream Accelerator",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "বড় ভিডিওর জন্য রেজিউম ও হাই-স্পিড বাফারিং সক্রিয়",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Switch(
                    checked = multiThreaded,
                    onCheckedChange = { viewModel.toggleMultiThreaded(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TelegramBlue
                    ),
                    modifier = Modifier.testTag("accelerator_toggle")
                )
            }
        }

        // Primary Download Button
        Button(
            onClick = { viewModel.startDownload(context) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("start_download_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TelegramBlue,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download Now",
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "ভিডিও ডাউনলোড করুন (Download Video)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Active Download Live Controller Card with Pause & Resume buttons
        if (activeDownloads.isNotEmpty()) {
            val activeItem = activeDownloads.first()
            val progress = if (activeItem.totalBytes > 0) {
                (activeItem.downloadedBytes.toFloat() / activeItem.totalBytes.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            val animatedProgress by animateFloatAsState(targetValue = progress, label = "TabProgress")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_download_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(TelegramBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Downloading",
                                    tint = TelegramBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = activeItem.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (activeItem.status == DownloadStatus.PAUSED) "ডাউনলোড স্থগিত (Paused)" else "ডাউনলোড চলমান (${activeItem.speedText})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (activeItem.status == DownloadStatus.PAUSED) WarningAmber else SuccessGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Pause or Resume button right here on DownloaderTab!
                        if (activeItem.status == DownloadStatus.DOWNLOADING) {
                            OutlinedButton(
                                onClick = { viewModel.pauseDownload(activeItem.id) },
                                modifier = Modifier.testTag("tab_pause_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pause", fontSize = 12.sp)
                            }
                        } else {
                            FilledTonalButton(
                                onClick = { viewModel.resumeDownload(activeItem.id) },
                                modifier = Modifier.testTag("tab_resume_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = TelegramBlue,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume", fontSize = 12.sp)
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .testTag("tab_progress_bar"),
                        color = if (activeItem.status == DownloadStatus.PAUSED) WarningAmber else TelegramBlue,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val downloadedText = TelegramVideoDownloaderEngine.formatBytes(activeItem.downloadedBytes)
                        val totalText = if (activeItem.totalBytes > 0) TelegramVideoDownloaderEngine.formatBytes(activeItem.totalBytes) else "..."
                        val percentText = "${(progress * 100).toInt()}%"

                        Text(
                            text = "$downloadedText / $totalText ($percentText)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )

                        if (activeItem.etaSeconds > 0 && activeItem.status == DownloadStatus.DOWNLOADING) {
                            Text(
                                text = "ETA: ${TelegramVideoDownloaderEngine.formatEta(activeItem.etaSeconds)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Quick Test Samples Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "সরাসরি টেস্ট করুন (Quick Test Samples):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "ছোট ও বড় ভিডিও কেমন স্পিডে ডাউনলোড হয় তা তাৎক্ষণিক টেস্ট করুন:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            viewModel.sampleLinks.forEach { sample ->
                SampleItemCard(
                    sample = sample,
                    onClick = { viewModel.loadSample(sample) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SampleItemCard(
    sample: SampleLink,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("sample_${sample.badge.replace(" ", "_").lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sample.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = sample.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(TelegramBlue.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = sample.sizeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TelegramBlue
                )
            }
        }
    }
}
