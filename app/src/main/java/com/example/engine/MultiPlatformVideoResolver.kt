package com.example.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit

enum class SupportedPlatform(
    val id: String,
    val displayName: String,
    val badgeLabel: String,
    val colorHex: Long
) {
    ALL("all", "All (সকল)", "VIP All Platforms", 0xFFFFD700),
    TELEGRAM("telegram", "Telegram", "TG Private & Public", 0xFF2AABEE),
    YOUTUBE("youtube", "YouTube", "YouTube 4K & Shorts", 0xFFFF0033),
    FACEBOOK("facebook", "Facebook", "Facebook HD & Reels", 0xFF1877F2),
    INSTAGRAM("instagram", "Instagram", "Insta Reels & Video", 0xFFE1306C),
    TIKTOK("tiktok", "TikTok", "TikTok No-Watermark", 0xFF00F2FE),
    TWITTER_X("twitter", "X (Twitter)", "Twitter/X Video", 0xFFE0E0E0),
    GOOGLE_DRIVE("google", "Google Drive", "Google Cloud Video", 0xFF34A853),
    WEB_DIRECT("web", "Web / Direct", "Direct Media Stream", 0xFFFFB300)
}

enum class VideoQuality(val label: String, val resolution: String, val isVip: Boolean = false) {
    RES_4K("4K Ultra HD", "2160", isVip = true),
    RES_1080P("1080p Full HD", "1080", isVip = true),
    RES_720P("720p HD", "720", isVip = false),
    RES_480P("480p SD", "480", isVip = false),
    AUDIO_MP3("MP3 Audio (320k)", "mp3", isVip = false)
}

data class PlatformAnalysis(
    val originalUrl: String,
    val platform: SupportedPlatform,
    val suggestedTitle: String,
    val isPrivateTelegram: Boolean = false,
    val messageId: String = "",
    val channelInfo: String = "",
    val infoMessage: String = ""
)

object MultiPlatformVideoResolver {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Identifies the platform from URL string
     */
    fun detectPlatform(url: String): SupportedPlatform {
        val lower = url.trim().lowercase()
        return when {
            lower.contains("t.me") || lower.contains("telegram.org") || lower.contains("telegram.me") ->
                SupportedPlatform.TELEGRAM

            lower.contains("youtube.com") || lower.contains("youtu.be") ->
                SupportedPlatform.YOUTUBE

            lower.contains("facebook.com") || lower.contains("fb.watch") || lower.contains("fb.com") ->
                SupportedPlatform.FACEBOOK

            lower.contains("instagram.com") || lower.contains("instagr.am") ->
                SupportedPlatform.INSTAGRAM

            lower.contains("tiktok.com") ->
                SupportedPlatform.TIKTOK

            lower.contains("twitter.com") || lower.contains("x.com") ->
                SupportedPlatform.TWITTER_X

            lower.contains("drive.google.com") ->
                SupportedPlatform.GOOGLE_DRIVE

            else ->
                SupportedPlatform.WEB_DIRECT
        }
    }

    /**
     * Parses and analyzes the input URL with platform recognition
     */
    fun analyzeUrl(rawUrl: String): PlatformAnalysis {
        val trimmed = rawUrl.trim()
        val platform = detectPlatform(trimmed)

        return when (platform) {
            SupportedPlatform.TELEGRAM -> {
                val tgParsed = TelegramLinkParser.parse(trimmed)
                PlatformAnalysis(
                    originalUrl = trimmed,
                    platform = SupportedPlatform.TELEGRAM,
                    suggestedTitle = tgParsed.suggestedTitle,
                    isPrivateTelegram = tgParsed.isPrivate,
                    messageId = tgParsed.messageId,
                    channelInfo = tgParsed.channelIdentifier,
                    infoMessage = tgParsed.infoMessage
                )
            }

            SupportedPlatform.YOUTUBE -> {
                val videoId = extractYouTubeId(trimmed)
                val isShorts = trimmed.contains("/shorts/")
                val titlePrefix = if (isShorts) "YouTube_Shorts" else "YouTube_Video"
                val title = "${titlePrefix}_${videoId.ifEmpty { System.currentTimeMillis().toString() }}.mp4"
                PlatformAnalysis(
                    originalUrl = trimmed,
                    platform = SupportedPlatform.YOUTUBE,
                    suggestedTitle = title,
                    channelInfo = "YouTube",
                    infoMessage = if (isShorts) "YouTube Shorts 1080p Stream" else "YouTube High Quality Video Stream (ID: $videoId)"
                )
            }

            SupportedPlatform.FACEBOOK -> {
                val clean = trimmed.substringBefore("?")
                val id = clean.substringAfterLast("/").ifEmpty { System.currentTimeMillis().toString() }
                PlatformAnalysis(
                    originalUrl = trimmed,
                    platform = SupportedPlatform.FACEBOOK,
                    suggestedTitle = "Facebook_Video_$id.mp4",
                    channelInfo = "Facebook",
                    infoMessage = "Facebook HD Video / Reel"
                )
            }

            SupportedPlatform.INSTAGRAM -> {
                val clean = trimmed.substringBefore("?").removeSuffix("/")
                val shortcode = clean.substringAfterLast("/")
                val isReel = trimmed.contains("/reel/")
                PlatformAnalysis(
                    originalUrl = trimmed,
                    platform = SupportedPlatform.INSTAGRAM,
                    suggestedTitle = if (isReel) "Insta_Reel_$shortcode.mp4" else "Instagram_Post_$shortcode.mp4",
                    channelInfo = "Instagram",
                    infoMessage = if (isReel) "Instagram Reel HD Video" else "Instagram Video Post"
                )
            }

            SupportedPlatform.TIKTOK -> {
                val clean = trimmed.substringBefore("?").removeSuffix("/")
                val id = clean.substringAfterLast("/").ifEmpty { System.currentTimeMillis().toString() }
                PlatformAnalysis(
                    originalUrl = trimmed,
                    platform = SupportedPlatform.TIKTOK,
                    suggestedTitle = "TikTok_Video_$id.mp4",
                    channelInfo = "TikTok",
                    infoMessage = "TikTok HD Video Stream (No Watermark)"
                )
            }

            SupportedPlatform.TWITTER_X -> {
                val clean = trimmed.substringBefore("?").removeSuffix("/")
                val id = clean.substringAfterLast("/").ifEmpty { System.currentTimeMillis().toString() }
                PlatformAnalysis(
                    originalUrl = trimmed,
                    platform = SupportedPlatform.TWITTER_X,
                    suggestedTitle = "X_Video_$id.mp4",
                    channelInfo = "X / Twitter",
                    infoMessage = "X / Twitter Media Video Stream"
                )
            }

            SupportedPlatform.GOOGLE_DRIVE -> {
                val fileId = extractGoogleDriveId(trimmed)
                PlatformAnalysis(
                    originalUrl = trimmed,
                    platform = SupportedPlatform.GOOGLE_DRIVE,
                    suggestedTitle = "Google_Drive_Video_${fileId.take(8)}.mp4",
                    channelInfo = "Google Drive",
                    infoMessage = "Google Drive Cloud Video Stream (Direct Download)"
                )
            }

            SupportedPlatform.WEB_DIRECT, SupportedPlatform.ALL -> {
                val cleanUrl = trimmed.substringBefore("?")
                val rawName = cleanUrl.substringAfterLast("/").ifEmpty { "web_video_${System.currentTimeMillis()}" }
                val title = if (rawName.endsWith(".mp4", true) || rawName.endsWith(".mkv", true) || rawName.endsWith(".webm", true)) {
                    rawName
                } else {
                    "$rawName.mp4"
                }
                PlatformAnalysis(
                    originalUrl = trimmed,
                    platform = SupportedPlatform.WEB_DIRECT,
                    suggestedTitle = title,
                    channelInfo = "Web Direct Video",
                    infoMessage = "High Speed Direct Web Video Stream"
                )
            }
        }
    }

    /**
     * Resolves the actual direct downloadable stream URL for the platform
     */
    suspend fun resolveStreamUrl(
        url: String,
        platform: SupportedPlatform,
        quality: VideoQuality
    ): String = withContext(Dispatchers.IO) {
        val trimmed = url.trim()

        // 1. Google Drive direct stream
        if (platform == SupportedPlatform.GOOGLE_DRIVE) {
            val fileId = extractGoogleDriveId(trimmed)
            if (fileId.isNotBlank()) {
                return@withContext "https://drive.google.com/uc?export=download&id=$fileId"
            }
        }

        // 2. Direct Web URLs or CDN links (mp4, webm, mkv, cloud storage)
        if (platform == SupportedPlatform.WEB_DIRECT ||
            trimmed.endsWith(".mp4", ignoreCase = true) ||
            trimmed.endsWith(".webm", ignoreCase = true) ||
            trimmed.endsWith(".mkv", ignoreCase = true) ||
            trimmed.contains("commondatastorage.googleapis.com") ||
            trimmed.contains("googleusercontent.com") ||
            trimmed.contains(".cdninstagram.com") ||
            trimmed.contains(".fbcdn.net")
        ) {
            return@withContext trimmed
        }

        // 3. For YouTube: Try Invidious public streaming instances first for direct mp4 links
        if (platform == SupportedPlatform.YOUTUBE) {
            val videoId = extractYouTubeId(trimmed)
            if (videoId.isNotBlank()) {
                val invidiousStream = tryInvidiousStream(videoId, quality)
                if (!invidiousStream.isNullOrBlank()) {
                    return@withContext invidiousStream
                }
            }
        }

        // 4. Try public high-speed Cobalt video stream extractors
        val cobaltStream = tryCobaltExtractor(trimmed, quality)
        if (!cobaltStream.isNullOrBlank()) {
            return@withContext cobaltStream
        }

        // 5. If all online resolvers are unreachable, return original URL
        return@withContext trimmed
    }

    private fun tryInvidiousStream(videoId: String, quality: VideoQuality): String? {
        val instances = listOf(
            "https://inv.nadeko.net",
            "https://invidious.nerdvpn.de",
            "https://invidious.drgns.space",
            "https://yt.artemislena.eu"
        )

        for (instance in instances) {
            try {
                val request = Request.Builder()
                    .url("$instance/api/v1/videos/$videoId")
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val bodyStr = response.body?.string() ?: return@use
                    val json = JSONObject(bodyStr)
                    val formatStreams = json.optJSONArray("formatStreams") ?: return@use

                    // Find best matching stream
                    var chosenUrl: String? = null
                    val targetRes = quality.resolution

                    for (i in 0 until formatStreams.length()) {
                        val stream = formatStreams.getJSONObject(i)
                        val streamUrl = stream.optString("url")
                        val resolution = stream.optString("resolution")
                        val container = stream.optString("container")

                        if (streamUrl.isNotBlank()) {
                            if (chosenUrl == null) chosenUrl = streamUrl
                            if (resolution.contains(targetRes) && container == "mp4") {
                                return streamUrl
                            }
                        }
                    }
                    if (chosenUrl != null) return chosenUrl
                }
            } catch (e: Exception) {
                Log.d("MultiPlatformResolver", "Invidious instance $instance failed: ${e.message}")
            }
        }
        return null
    }

    private fun tryCobaltExtractor(url: String, quality: VideoQuality): String? {
        val endpoints = listOf(
            "https://api.cobalt.tools/api/json",
            "https://cobalt-backend.canine.tools/api/json"
        )

        val jsonBody = JSONObject().apply {
            put("url", url)
            put("vQuality", quality.resolution)
            if (quality == VideoQuality.AUDIO_MP3) {
                put("isAudioOnly", true)
            }
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())

        for (endpoint in endpoints) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "VIPVideoDownloader/2.0")
                    .post(requestBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val bodyStr = response.body?.string() ?: return@use
                    val resJson = JSONObject(bodyStr)
                    val streamUrl = resJson.optString("url")
                    if (streamUrl.isNotBlank() && streamUrl.startsWith("http")) {
                        return streamUrl
                    }
                }
            } catch (e: Exception) {
                Log.d("MultiPlatformResolver", "Cobalt endpoint $endpoint failed: ${e.message}")
            }
        }
        return null
    }

    fun extractYouTubeId(url: String): String {
        return try {
            val uri = URI(url)
            val host = uri.host ?: ""
            if (host.contains("youtu.be")) {
                uri.path.removePrefix("/").substringBefore("?")
            } else if (uri.path.contains("/shorts/")) {
                uri.path.substringAfter("/shorts/").substringBefore("?").substringBefore("/")
            } else {
                val query = uri.query ?: ""
                query.split("&").firstOrNull { it.startsWith("v=") }?.substringAfter("v=") ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun extractGoogleDriveId(url: String): String {
        return try {
            if (url.contains("/file/d/")) {
                url.substringAfter("/file/d/").substringBefore("/").substringBefore("?")
            } else if (url.contains("id=")) {
                url.substringAfter("id=").substringBefore("&")
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}
