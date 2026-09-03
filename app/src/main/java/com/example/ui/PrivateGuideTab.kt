package com.example.ui

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.PrivateSettings
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TelegramAccent
import com.example.ui.theme.TelegramBlue
import com.example.ui.theme.WarningAmber

@Composable
fun PrivateGuideTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentSettings by viewModel.privateSettings.collectAsState()

    var botToken by remember(currentSettings.botToken) { mutableStateOf(currentSettings.botToken) }
    var sessionString by remember(currentSettings.sessionString) { mutableStateOf(currentSettings.sessionString) }
    var proxyUrl by remember(currentSettings.customProxyUrl) { mutableStateOf(currentSettings.customProxyUrl) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
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
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(WarningAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Private Channel",
                        tint = WarningAmber,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "প্রাইভেট চ্যানেল গাইড ও সেটিংস",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Telegram Private Video Downloader Guide & Config",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }
        }

        // How it works Card
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TelegramBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "প্রাইভেট ভিডিও কীভাবে কাজ করে?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                GuideStepItem(
                    stepNumber = "১",
                    title = "প্রাইভেট লিঙ্ক কপি করুন",
                    desc = "টেলিগ্রামের যেকোনো প্রাইভেট চ্যানেলের ভিডিও মেসেজে ট্যাপ করে 'Copy Link' করুন। লিঙ্কটি সাধারণত https://t.me/c/1234567890/42 এমন হয়।"
                )

                GuideStepItem(
                    stepNumber = "২",
                    title = "রেস্ট্রিক্টেড সেভিং (Restrict Saving)",
                    desc = "টেলিগ্রামের অনেক প্রাইভেট চ্যানেলে সেভ বা ফরোয়ার্ড বন্ধ থাকে। অ্যাপের ডাউনলোডার সরাসরি লিঙ্কটিকে হ্যান্ডেল করে স্ট্রিম বাফার করে।"
                )

                GuideStepItem(
                    stepNumber = "৩",
                    title = "বট দিয়ে প্রাইভেট চ্যানেল ডাউনলোডের সঠিক নিয়ম",
                    desc = "শুধুমাত্র Bot Token বসালেই প্রাইভেট চ্যানেলের ভিডিও পাওয়া যায় না! আপনার বটকে প্রথমে ঐ প্রাইভেট চ্যানেলে 'Administrator' হিসেবে অ্যাড করতে হবে। অন্যথায় টেলিগ্রাম বটকে চ্যানেলের মেসেজ পড়ার অনুমতি দেয় না।"
                )

                GuideStepItem(
                    stepNumber = "৪",
                    title = "সরাসরি ফাইল লিঙ্ক বা ফরওয়ার্ড লিঙ্ক",
                    desc = "চ্যানেলের পোস্ট থেকে বটের মাধ্যমে লিঙ্ক নিলে (যেমন https://api.telegram.org/file/bot... অথবা সরাসরি স্ট্রিমিং লিঙ্ক) ডাউনলোডার হাই-স্পিডে ডাউনলোড করে সরাসরি ফোনে সেভ করবে।"
                )
            }
        }

        // VIP Multi-Platform Guide Card
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = com.example.ui.theme.VipGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "👑 VIP মাল্টি-প্ল্যাটফর্ম ও ১০+ ব্যাচ গাইড",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.VipGold
                    )
                }

                GuideStepItem(
                    stepNumber = "A",
                    title = "YouTube, Facebook ও Instagram ডাউনলোড",
                    desc = "যেকোনো YouTube ভিডিও/Shorts, Facebook রিল অথবা Instagram পোস্টের লিঙ্ক কপি করে পেস্ট করুন। কোয়ালিটি ড্রপডাউন থেকে 4K, 1080p, 720p বা MP3 নির্বাচন করে ডাউনলোড করতে পারবেন।"
                )

                GuideStepItem(
                    stepNumber = "B",
                    title = "Google Drive ও ক্লাউড ভিডিও",
                    desc = "Google Drive বা Google ক্লাউড স্টোরেজের যেকোনো পাবলিক বা শেয়ার্ড ভিডিও সরাসরি হাই-স্পিড এক্সপোর্ট স্ট্রিমে ডাউনলোড করা যায়।"
                )

                GuideStepItem(
                    stepNumber = "C",
                    title = "একসাথে ১০+ ভিডিও ডাউনলোড (Batch Mode)",
                    desc = "'১০+ একসাথে (10+ Batch)' মোড সিলেক্ট করে একাধিক ভিডিও লিঙ্ক প্রতি লাইনে একটি করে পেস্ট করুন। এক ক্লিকে সবগুলো একসাথে প্যারালালে ডাউনলোড হবে।"
                )
            }
        }

        // Configuration Card
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = TelegramAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "অ্যাডভান্সড কনফিগারেশন (ঐচ্ছিক)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "সাধারণ ভিডিওর জন্য এটি খালি রাখলেও ডাউনলোড হবে। প্রাইভেট চ্যানেল বাইপাসের জন্য আপনার বট টোকেন দিতে পারেন:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                OutlinedTextField(
                    value = botToken,
                    onValueChange = { botToken = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bot_token_input"),
                    label = { Text("Telegram Bot Token (Optional)") },
                    placeholder = { Text("e.g. 1234567890:ABCdefGHIjklMNOpqrs") },
                    leadingIcon = {
                        Icon(Icons.Default.SmartToy, contentDescription = "Bot", tint = TelegramBlue)
                    },
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = sessionString,
                    onValueChange = { sessionString = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_string_input"),
                    label = { Text("Telegram String Session (Optional)") },
                    placeholder = { Text("Pyrogram / Telethon Session String") },
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = "Session", tint = TelegramBlue)
                    },
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = proxyUrl,
                    onValueChange = { proxyUrl = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("proxy_url_input"),
                    label = { Text("Custom Bridge / Proxy URL (Optional)") },
                    placeholder = { Text("https://my-tg-bridge.example.com") },
                    shape = RoundedCornerShape(10.dp)
                )

                Button(
                    onClick = {
                        viewModel.updatePrivateSettings(
                            PrivateSettings(
                                botToken = botToken.trim(),
                                sessionString = sessionString.trim(),
                                customProxyUrl = proxyUrl.trim(),
                                fastChunking = true
                            )
                        )
                        Toast.makeText(context, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_settings_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TelegramBlue)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Settings (সেটিংস সংরক্ষণ করুন)")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun GuideStepItem(
    stepNumber: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(TelegramBlue),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
