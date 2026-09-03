package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DownloadStatus
import com.example.engine.PlatformAnalysis
import com.example.engine.SupportedPlatform
import com.example.engine.TelegramVideoDownloaderEngine
import com.example.engine.VideoQuality
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.GoogleEmerald
import com.example.ui.theme.InstagramPink
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.VipAmber
import com.example.ui.theme.VipCardElevated
import com.example.ui.theme.VipGold
import com.example.ui.theme.VipGoldBorder
import com.example.ui.theme.VipGoldLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.YouTubeRed

@Composable
fun DownloaderTab(
    viewModel: MainViewModel,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val urlInput by viewModel.urlInput.collectAsState()
    val platformAnalysis by viewModel.platformAnalysis.collectAsState()
    val selectedPlatform by viewModel.selectedPlatform.collectAsState()
    val selectedQuality by viewModel.selectedQuality.collectAsState()
    val customTitle by viewModel.customTitle.collectAsState()
    val multiThreaded by viewModel.multiThreaded.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val isBatchMode by viewModel.isBatchMode.collectAsState()
    val batchUrlsInput by viewModel.batchUrlsInput.collectAsState()
    val isResolving by viewModel.isResolving.collectAsState()
    val resolvingMessage by viewModel.resolvingMessage.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // VIP Luxury Hero Banner
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, VipGoldBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = VipCardElevated
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1E2130),
                                Color(0xFF2A281E)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
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
                            contentDescription = "VIP",
                            tint = Color.Black,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "VIP VIDEO DOWNLOADER",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = VipGold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(VipGold)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "👑 PRO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                        Text(
                            text = "YouTube • Telegram • Facebook • Instagram • Google • Web",
                            style = MaterialTheme.typography.bodySmall,
                            color = VipGoldLight,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "⚡ 10+ ভিডিও একসাথে হাই-স্পিড 4K/1080p তে ডাউনলোড করুন।",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // Mode Switcher: Single Video vs Batch 10+ Videos
        TabRow(
            selectedTabIndex = if (isBatchMode) 1 else 0,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = VipGold,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[if (isBatchMode) 1 else 0]),
                    color = VipGold
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .testTag("mode_tab_row")
        ) {
            Tab(
                selected = !isBatchMode,
                onClick = { viewModel.toggleBatchMode(false) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("একক ভিডিও (Single)", fontWeight = FontWeight.Bold)
                    }
                },
                selectedContentColor = VipGold,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Tab(
                selected = isBatchMode,
                onClick = { viewModel.toggleBatchMode(true) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = VipGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("১০+ একসাথে (10+ Batch)", fontWeight = FontWeight.Bold)
                    }
                },
                selectedContentColor = VipGold,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isBatchMode) {
            // ================= SINGLE VIDEO MODE =================

            // Platform Filter Row
            val platformScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(platformScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val platforms = listOf(
                    SupportedPlatform.ALL,
                    SupportedPlatform.TELEGRAM,
                    SupportedPlatform.YOUTUBE,
                    SupportedPlatform.FACEBOOK,
                    SupportedPlatform.INSTAGRAM,
                    SupportedPlatform.GOOGLE_DRIVE,
                    SupportedPlatform.WEB_DIRECT
                )

                platforms.forEach { plat ->
                    FilterChip(
                        selected = selectedPlatform == plat,
                        onClick = { viewModel.onPlatformFilterChanged(plat) },
                        label = {
                            Text(
                                plat.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (selectedPlatform == plat) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(plat.colorHex).copy(alpha = 0.25f),
                            selectedLabelColor = Color(plat.colorHex)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedPlatform == plat,
                            selectedBorderColor = Color(plat.colorHex),
                            selectedBorderWidth = 1.5.dp
                        )
                    )
                }
            }

            // Video Quality Selector (4K, 1080p, 720p, 480p, MP3)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "ভিডিও রেজোলিউশন / কোয়ালিটি সিলেক্ট করুন:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = VipGold
                )
                val qualityScroll = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(qualityScroll),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoQuality.values().forEach { quality ->
                        FilterChip(
                            selected = selectedQuality == quality,
                            onClick = { viewModel.onQualityChanged(quality) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (quality.isVip) {
                                        Text("👑 ", fontSize = 11.sp)
                                    }
                                    Text(
                                        quality.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedQuality == quality) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VipGold.copy(alpha = 0.2f),
                                selectedLabelColor = VipGold
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedQuality == quality,
                                selectedBorderColor = VipGold,
                                selectedBorderWidth = 1.5.dp
                            )
                        )
                    }
                }
            }

            // Input Card
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
                        text = "যেকোনো ভিডিও লিঙ্ক দিন (Paste Any Video Link)",
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
                                "YouTube, Telegram, Facebook, Instagram, Google বা সরাসরি লিঙ্ক পেস্ট করুন",
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Link",
                                tint = VipGold
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
                            focusedBorderColor = VipGold,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Paste & Guide Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.pasteFromClipboard(context) },
                            modifier = Modifier.testTag("paste_link_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
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
                                "Private Guide",
                                style = MaterialTheme.typography.labelMedium,
                                color = TelegramBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Platform Analysis Card (Live Recognition)
            platformAnalysis?.let { info ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(info.platform.colorHex).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(info.platform.colorHex).copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = info.platform.badgeLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(info.platform.colorHex)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = selectedQuality.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = VipGold
                            )
                        }

                        Text(
                            text = info.infoMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

            // Download Button (Single)
            Button(
                onClick = { viewModel.startDownload(context) },
                enabled = !isResolving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_download_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VipGold,
                    contentColor = Color.Black
                )
            ) {
                if (isResolving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = resolvingMessage,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download Now",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ভিডিও ডাউনলোড করুন (Download in ${selectedQuality.label})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // ================= BATCH MODE (10+ VIDEOS CONCURRENTLY) =================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VipGoldBorder, RoundedCornerShape(16.dp)),
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(VipGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = VipGold, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "১০+ ভিডিও একসাথে ডাউনলোড (Batch Multi-Downloader)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = VipGold
                            )
                            Text(
                                text = "প্রতি লাইনে একটি করে লিঙ্ক দিন। সবগুলো একই সাথে প্যারালালে ডাউনলোড হবে।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = batchUrlsInput,
                        onValueChange = { viewModel.onBatchUrlsChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("batch_urls_input"),
                        placeholder = {
                            Text(
                                "https://t.me/...\nhttps://youtube.com/...\nhttps://commondatastorage.googleapis.com/...\n(প্রতি লাইনে ১টি লিঙ্ক)",
                                fontSize = 12.sp
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VipGold,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    val linesCount = batchUrlsInput.lines().filter { it.isNotBlank() }.size

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "শনাক্ত ভিডিও: $linesCount টি",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (linesCount >= 10) SuccessGreen else VipGold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { viewModel.pasteFromClipboard(context) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paste", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.clearBatchInput() },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clear", fontSize = 12.sp)
                            }
                        }
                    }

                    // Quick Load 10 High Speed Links Button
                    FilledTonalButton(
                        onClick = { viewModel.load10SampleBatch() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("load_10_samples_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = VipGold.copy(alpha = 0.15f),
                            contentColor = VipGold
                        )
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🚀 ১০টি হাই-স্পিড ভিডিও লোড করুন (Load 10 Samples)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Batch Download Button
            val batchCount = batchUrlsInput.lines().filter { it.isNotBlank() }.size
            Button(
                onClick = { viewModel.startBatchDownload(context) },
                enabled = !isResolving && batchCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_batch_download_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VipGold,
                    contentColor = Color.Black
                )
            ) {
                if (isResolving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(resolvingMessage, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "⚡ এক ক্লিকে $batchCount টি ভিডিও একসাথে ডাউনলোড করুন",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Accelerator Toggle Card
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
                            .background(VipGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Speed",
                            tint = VipGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "10x Turbo Multi-Stream Accelerator",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "১০+ প্যারালাল কানেকশন ও বড় ভিডিও রেজিউম সক্রিয়",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Switch(
                    checked = multiThreaded,
                    onCheckedChange = { viewModel.toggleMultiThreaded(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = VipGold
                    ),
                    modifier = Modifier.testTag("accelerator_toggle")
                )
            }
        }

        // Active Download Live Controller Card
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
                                    .background(VipGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Downloading",
                                    tint = VipGold,
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
                                    text = if (activeItem.status == DownloadStatus.PAUSED) "স্থগিত (Paused)" else "চলমান (${activeItem.speedText}) [মোট ${activeDownloads.size}টি]",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (activeItem.status == DownloadStatus.PAUSED) WarningAmber else SuccessGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

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
                                    containerColor = VipGold,
                                    contentColor = Color.Black
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
                        color = if (activeItem.status == DownloadStatus.PAUSED) WarningAmber else VipGold,
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

        // Quick Multi-Platform Samples Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "সরাসরি টেস্ট করুন (Multi-Platform Demo Videos):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "বিভিন্ন প্ল্যাটফর্মের হাই-কোয়ালিটি ভিডিও এক ক্লিকে টেস্ট করুন:",
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
                    .background(Color(sample.platform.colorHex).copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = sample.sizeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(sample.platform.colorHex)
                )
            }
        }
    }
}
