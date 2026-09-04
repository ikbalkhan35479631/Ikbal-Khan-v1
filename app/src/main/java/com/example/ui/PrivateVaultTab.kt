package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.DownloadItem
import com.example.engine.TelegramVideoDownloaderEngine
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.VipCardElevated
import com.example.ui.theme.VipGold
import com.example.ui.theme.VipGoldBorder
import com.example.ui.theme.VipGoldLight

@Composable
fun PrivateVaultTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val lockedList by viewModel.lockedDownloads.collectAsState()
    val hasPin = viewModel.hasVaultPin()

    var enteredPin by remember { mutableStateOf("") }
    var pinSetupConfirm by remember { mutableStateOf("") }
    var isConfirmStep by remember { mutableStateOf(false) }
    var tempSetupPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showTelegramSettings by remember { mutableStateOf(false) }

    if (!isUnlocked) {
        // Vault Lock Screen (Enter PIN or Setup PIN)
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lock Icon Badge
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(VipGold.copy(alpha = 0.15f))
                    .border(2.dp, VipGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Vault Locked",
                    tint = VipGold,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (!hasPin) {
                    if (isConfirmStep) "পিনটি পুনরায় দিয়ে নিশ্চিত করুন" else "৪-সংখ্যার নতুন ভল্ট পিন সেট করুন"
                } else {
                    "গোপন ভল্ট আনলক করুন"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VipGold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (!hasPin) {
                    "আপনার ব্যক্তিগত ভিডিও ও ফাইল নিরাপদে সুরক্ষিত রাখতে একটি ৪-সংখ্যার পাসওয়ার্ড নির্ধারণ করুন।"
                } else {
                    "লক করা ভিডিও দেখতে আপনার ৪-সংখ্যার গোপন পিন প্রদান করুন।"
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 4 Pin Dots Indicator
            val activePin = if (!hasPin && isConfirmStep) pinSetupConfirm else enteredPin
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < activePin.length
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) VipGold else Color.White.copy(alpha = 0.15f)
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (isFilled) VipGold else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                }
            }

            // Error notice
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 1-9 & 0 Numeric Keypad
            NumericKeypad(
                onDigitClick = { digit ->
                    errorMessage = null
                    if (!hasPin) {
                        if (!isConfirmStep) {
                            if (enteredPin.length < 4) {
                                val next = enteredPin + digit
                                enteredPin = next
                                if (next.length == 4) {
                                    tempSetupPin = next
                                    isConfirmStep = true
                                }
                            }
                        } else {
                            if (pinSetupConfirm.length < 4) {
                                val next = pinSetupConfirm + digit
                                pinSetupConfirm = next
                                if (next.length == 4) {
                                    if (next == tempSetupPin) {
                                        viewModel.setVaultPin(next)
                                        Toast.makeText(context, "পিন সফলভাবে সেট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        errorMessage = "দুটো পিন মেলেনি! আবার চেষ্টা করুন।"
                                        pinSetupConfirm = ""
                                        enteredPin = ""
                                        isConfirmStep = false
                                    }
                                }
                            }
                        }
                    } else {
                        if (enteredPin.length < 4) {
                            val next = enteredPin + digit
                            enteredPin = next
                            if (next.length == 4) {
                                val success = viewModel.verifyVaultPin(next)
                                if (!success) {
                                    errorMessage = "ভুল পিন! অনুগ্রহ করে সঠিক পিন দিন।"
                                    enteredPin = ""
                                }
                            }
                        }
                    }
                },
                onBackspace = {
                    errorMessage = null
                    if (!hasPin) {
                        if (isConfirmStep) {
                            if (pinSetupConfirm.isNotEmpty()) {
                                pinSetupConfirm = pinSetupConfirm.dropLast(1)
                            } else {
                                isConfirmStep = false
                                enteredPin = ""
                            }
                        } else {
                            if (enteredPin.isNotEmpty()) {
                                enteredPin = enteredPin.dropLast(1)
                            }
                        }
                    } else {
                        if (enteredPin.isNotEmpty()) {
                            enteredPin = enteredPin.dropLast(1)
                        }
                    }
                }
            )
        }
    } else {
        // Vault Unlocked: Show Private Media
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar with Lock button and Change PIN
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = VipCardElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VipGoldBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LockOpen,
                                    contentDescription = "Vault Unlocked",
                                    tint = VipGold,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "গোপন ভল্ট (সুরক্ষিত)",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VipGold
                                    )
                                    Text(
                                        text = "${lockedList.size}টি আইটেম সুরক্ষিত রয়েছে",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { showChangePinDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VipGold)
                                ) {
                                    Text("পিন পরিবর্তন", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.lockVault()
                                        enteredPin = ""
                                        Toast.makeText(context, "ভল্ট লক করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VipGold, contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = "Lock", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("এখনই লক করুন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (lockedList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Empty Vault",
                                tint = VipGold.copy(alpha = 0.5f),
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "ভল্টে কোনো গোপন ভিডিও নেই",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = VipGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "সংরক্ষিত ভিডিও তালিকা থেকে যেকোনো ভিডিওর পাশে লক (🔒) আইকনে চাপ দিলে তা এই ভল্টে গোপন হয়ে যাবে।",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            } else {
                items(lockedList, key = { it.id }) { item ->
                    LockedVideoCard(
                        item = item,
                        onPlay = { viewModel.playVideo(item) },
                        onOpenFile = { viewModel.openDownloadedFile(item, context) },
                        onUnlock = { viewModel.toggleLockVideo(item, false, context) },
                        onExport = { viewModel.exportToGallery(item, context) },
                        onDelete = { viewModel.deleteCompleted(item, context) }
                    )
                }
            }

            // Optional Telegram Settings accordion
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTelegramSettings = !showTelegramSettings },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = VipGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "টেলিগ্রাম প্রাইভেট চ্যানেল বট সেটিংস (ঐচ্ছিক)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = if (showTelegramSettings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }

                        if (showTelegramSettings) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val privateSettings by viewModel.privateSettings.collectAsState()
                            var botToken by remember { mutableStateOf(privateSettings.botToken) }

                            OutlinedTextField(
                                value = botToken,
                                onValueChange = { botToken = it },
                                label = { Text("বট টোকেন (Bot Token)") },
                                placeholder = { Text("123456:ABC-DEF...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    viewModel.updatePrivateSettings(
                                        privateSettings.copy(botToken = botToken.trim())
                                    )
                                    Toast.makeText(context, "বট টোকেন সংরক্ষণ করা হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VipGold, contentColor = Color.Black)
                            ) {
                                Text("সংরক্ষণ করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        var oldPinInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }
        var changePinError by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { showChangePinDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, VipGold),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "গোপন পিন পরিবর্তন করুন",
                        color = VipGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = oldPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) oldPinInput = it },
                        label = { Text("বর্তমান ৪-সংখ্যার পিন") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) newPinInput = it },
                        label = { Text("নতুন ৪-সংখ্যার পিন") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (changePinError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(changePinError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { showChangePinDialog = false },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("বাতিল")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (oldPinInput.length != 4 || newPinInput.length != 4) {
                                    changePinError = "উভয় পিনই অবশ্যই ৪-সংখ্যার হতে হবে।"
                                    return@Button
                                }
                                val ok = viewModel.changeVaultPin(oldPinInput, newPinInput)
                                if (ok) {
                                    Toast.makeText(context, "পিন সফলভাবে পরিবর্তন হয়েছে!", Toast.LENGTH_SHORT).show()
                                    showChangePinDialog = false
                                } else {
                                    changePinError = "বর্তমান পিনটি সঠিক নয়।"
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VipGold, contentColor = Color.Black)
                        ) {
                            Text("পরিবর্তন করুন", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NumericKeypad(
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val keypadRows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in keypadRows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (key in row) {
                    if (key.isEmpty()) {
                        Spacer(modifier = Modifier.size(68.dp))
                    } else if (key == "DEL") {
                        Surface(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .clickable { onBackspace() },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.08f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Backspace",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .clickable { onDigitClick(key) },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = key,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LockedVideoCard(
    item: DownloadItem,
    onPlay: () -> Unit,
    onOpenFile: () -> Unit,
    onUnlock: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val isVideo = item.mimeType.startsWith("video/") || item.title.endsWith(".mp4", true) || item.title.endsWith(".mkv", true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = VipCardElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, VipGold.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (isVideo) onPlay() else onOpenFile() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(VipGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isVideo) Icons.Default.Movie else Icons.Default.Folder,
                        contentDescription = "Play",
                        tint = VipGold,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = TelegramVideoDownloaderEngine.formatBytes(item.totalBytes),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VipGold
                        )
                        Text(
                            text = "• গোপন ভল্টে সুরক্ষিত",
                            fontSize = 11.sp,
                            color = VipGoldLight
                        )
                    }
                }
            }

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isVideo) {
                    FilledTonalButton(
                        onClick = onPlay,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("প্লে করুন", fontSize = 12.sp)
                    }
                } else {
                    FilledTonalButton(
                        onClick = onOpenFile,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ওপেন করুন", fontSize = 12.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Unlock button (restore to main list)
                    IconButton(onClick = onUnlock) {
                        Icon(
                            Icons.Default.LockOpen,
                            contentDescription = "Unlock back to general list",
                            tint = VipGold
                        )
                    }

                    // Save to Gallery
                    IconButton(onClick = onExport) {
                        Icon(
                            Icons.Default.DownloadDone,
                            contentDescription = "Save to Gallery",
                            tint = TelegramBlue
                        )
                    }

                    // Delete
                    IconButton(onClick = onDelete) {
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
