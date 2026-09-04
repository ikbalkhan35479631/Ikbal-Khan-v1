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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
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
import com.example.engine.SupportedPlatform
import com.example.engine.TelegramVideoDownloaderEngine
import com.example.engine.VideoQuality
import com.example.ui.theme.ApkGreen
import com.example.ui.theme.AudioPurple
import com.example.ui.theme.DocBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PdfRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TelegramAccent
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.VipAmber
import com.example.ui.theme.VipCardElevated
import com.example.ui.theme.VipGold
import com.example.ui.theme.VipGoldBorder
import com.example.ui.theme.VipGoldLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.ZipOrange

@Composable
fun DownloaderTab(
    viewModel: MainViewModel,
    onOpenGuide: () -> Unit = {},
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
    val downloaderMode by viewModel.downloaderMode.collectAsState()
    val selectedFileFilter by viewModel.selectedFileFilter.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val networkLossNotice by viewModel.networkLossNotice.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Network Connectivity Indicator & Alert Banner
        if (!isOnline || networkLossNotice != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("network_offline_alert"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ErrorRed.copy(alpha = 0.18f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ErrorRed.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Offline",
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ইন্টারনেট সংযোগ বিচ্ছিন্ন! (Network Disconnected)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = networkLossNotice ?: "ডাউনলোড সাময়িকভাবে বিরতিতে রয়েছে। সংযোগ ফিরলে স্বয়ংক্রিয়ভাবে পুনরায় শুরু করতে পারবেন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 2. VIP Luxury Hero Banner with Real-Time Connectivity Badge
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
                                Color(0xFF161A29),
                                Color(0xFF262319),
                                Color(0xFF101B2E)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
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
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "TG & ALL-FILES DOWNLOADER",
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
                                            text = "👑 VIP",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                                Text(
                                    text = "Telegram • Files • Web • 100% Working",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VipGoldLight,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Network Live Pill Indicator
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isOnline) SuccessGreen.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isOnline) SuccessGreen else ErrorRed)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isOnline) "Online" else "Offline",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOnline) SuccessGreen else ErrorRed
                                )
                            }
                        }
                    }

                    // Bot-Free Public Channel Notice Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(TelegramBlue.copy(alpha = 0.12f))
                            .border(1.dp, TelegramBlue.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Public",
                                tint = TelegramAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "✨ পাবলিক টেলিগ্রাম পোস্ট ও ফাইল বট ছাড়াই সরাসরি ডাউনলোড হবে!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TelegramAccent
                            )
                        }
                    }
                }
            }
        }

        // 3. Primary Mode Switcher: Video Downloader vs All Files Downloader vs 10+ Batch
        val currentTabIndex = when {
            isBatchMode -> 2
            downloaderMode == DownloaderMode.ALL_FILES -> 1
            else -> 0
        }

        TabRow(
            selectedTabIndex = currentTabIndex,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = VipGold,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[currentTabIndex]),
                    color = VipGold
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .testTag("mode_tab_row")
        ) {
            Tab(
                selected = !isBatchMode && downloaderMode == DownloaderMode.VIDEO,
                onClick = {
                    viewModel.toggleBatchMode(false)
                    viewModel.setDownloaderMode(DownloaderMode.VIDEO)
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("ভিডিও ডাউনলোডার", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                selectedContentColor = VipGold,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Tab(
                selected = !isBatchMode && downloaderMode == DownloaderMode.ALL_FILES,
                onClick = {
                    viewModel.toggleBatchMode(false)
                    viewModel.setDownloaderMode(DownloaderMode.ALL_FILES)
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("ফাইলস ও ডক্স", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("১০+ ব্যাচ", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                selectedContentColor = VipGold,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isBatchMode) {
            // ================= SINGLE MODE (VIDEO OR ALL FILES) =================

            if (downloaderMode == DownloaderMode.ALL_FILES) {
                // All Files Filter Category Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ফাইলের ধরন নির্বাচন করুন (File Filter):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = VipGold
                    )
                    val fileCategories = listOf(
                        Triple("ALL", "সব ফাইল (All)", Color.White),
                        Triple("APK", "APK অ্যাপ", ApkGreen),
                        Triple("PDF", "PDF ডকুমেন্ট", PdfRed),
                        Triple("ZIP", "ZIP/RAR আর্কাইভ", ZipOrange),
                        Triple("AUDIO", "MP3 অডিও", AudioPurple),
                        Triple("DOC", "অন্যান্য ডক্স", DocBlue)
                    )
                    val fileScroll = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(fileScroll),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fileCategories.forEach { (key, label, badgeColor) ->
                            FilterChip(
                                selected = selectedFileFilter == key,
                                onClick = { viewModel.setSelectedFileFilter(key) },
                                label = {
                                    Text(
                                        label,
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedFileFilter == key) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = badgeColor.copy(alpha = 0.2f),
                                    selectedLabelColor = badgeColor
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedFileFilter == key,
                                    selectedBorderColor = badgeColor,
                                    selectedBorderWidth = 1.5.dp
                                )
                            )
                        }
                    }
                }
            } else {
                // Platform Filter Row for Videos
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
                        text = "ভিডিও রেজোলিউশন / কোয়ালিটি নির্বাচন করুন:",
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
                        text = if (downloaderMode == DownloaderMode.ALL_FILES) "যেকোনো ফাইল বা ডকুমেন্টের লিঙ্ক দিন" else "যেকোনো ভিডিও লিঙ্ক দিন (Paste Link)",
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
                                if (downloaderMode == DownloaderMode.ALL_FILES)
                                    "টেলিগ্রাম ফাইল, PDF, APK, ZIP বা সরাসরি ডাউনলোডেবল URL পেস্ট করুন"
                                else
                                    "Telegram, YouTube, Facebook, Instagram, Google বা সরাসরি লিঙ্ক পেস্ট করুন",
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (downloaderMode == DownloaderMode.ALL_FILES) Icons.Default.Folder else Icons.Default.Download,
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

                    // Paste Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
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
                    }

                    // Optional Custom Title
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { viewModel.onCustomTitleChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_title_input"),
                        label = { Text("কাস্টম ফাইলের নাম (ঐচ্ছিক)", fontSize = 12.sp) },
                        placeholder = { Text(if (downloaderMode == DownloaderMode.ALL_FILES) "my_document.pdf" else "my_video.mp4", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VipGold,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Primary Start Download Button
            Button(
                onClick = { viewModel.startDownload(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("download_button"),
                enabled = !isResolving && urlInput.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VipGold,
                    contentColor = Color.Black,
                    disabledContainerColor = VipGold.copy(alpha = 0.4f),
                    disabledContentColor = Color.Black.copy(alpha = 0.5f)
                )
            ) {
                if (isResolving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.Black,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = resolvingMessage.ifBlank { "প্রস্তুত করা হচ্ছে..." },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = if (downloaderMode == DownloaderMode.ALL_FILES) Icons.Default.Folder else Icons.Default.Download,
                        contentDescription = "Download",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (downloaderMode == DownloaderMode.ALL_FILES) "ফাইল ডাউনলোড শুরু করুন (Download File)" else "ভিডিও ডাউনলোড শুরু করুন (Download Video)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // ================= BATCH 10+ VIDEOS / FILES MODE =================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = VipGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("১০+ ফাইল/ভিডিও একসাথে ডাউনলোড", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.load10SampleBatch() },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VipGold.copy(alpha = 0.2f), contentColor = VipGold),
                            modifier = Modifier.testTag("load_10_samples_button")
                        ) {
                            Text("১০টি লিংক লোড করুন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = batchUrlsInput,
                        onValueChange = { viewModel.onBatchUrlsChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .testTag("batch_urls_input"),
                        placeholder = {
                            Text("প্রতি লাইনে ১টি করে লিঙ্ক দিন (YouTube, Telegram, Web, PDF ইত্যাদি):\nhttps://...\nhttps://...", fontSize = 12.sp)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.pasteFromClipboard(context) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste", fontSize = 12.sp)
                        }

                        if (batchUrlsInput.isNotBlank()) {
                            IconButton(onClick = { viewModel.clearBatchInput() }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                }
            }

            val batchCount = batchUrlsInput.lines().count { it.trim().isNotBlank() }
            Button(
                onClick = { viewModel.startBatchDownload(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_batch_button"),
                enabled = !isResolving && batchCount > 0,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VipGold, contentColor = Color.Black)
            ) {
                if (isResolving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(resolvingMessage, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "⚡ এক ক্লিকে $batchCount টি আইটেম একসাথে ডাউনলোড করুন",
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
                            text = "১০+ প্যারালাল কানেকশন ও অটো-রেজিউম সক্রিয়",
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

        // Quick 100% Tested Working Samples Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (downloaderMode == DownloaderMode.ALL_FILES)
                    "১০০% কার্যকরী টেস্ট ফাইলস (100% Tested Working Files):"
                else
                    "১০০% কার্যকরী টেস্ট ভিডিও (100% Tested Working Videos):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (downloaderMode == DownloaderMode.ALL_FILES)
                    "পিডিএফ, অডিও ও আর্কাইভ ফাইল এক ক্লিকে টেস্ট করুন:"
                else
                    "পাবলিক টেলিগ্রাম পোস্ট ও বিভিন্ন প্ল্যাটফর্মের ভিডিও এক ক্লিকে টেস্ট করুন:",
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
    val badgeColor = when {
        sample.badge.contains("TG Public") -> TelegramAccent
        sample.badge.contains("PDF") -> PdfRed
        sample.badge.contains("Audio") || sample.badge.contains("MP3") -> AudioPurple
        sample.badge.contains("ZIP") -> ZipOrange
        sample.badge.contains("Private") -> WarningAmber
        else -> VipGold
    }

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
                    .background(badgeColor.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = sample.badge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }
    }
}
