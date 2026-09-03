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
     * Checks if a URL is an HTML webpage rather than a direct downloadable stream
     */
    fun isWebPageUrl(url: String): Boolean {
        val lower = url.trim().lowercase()
        return lower.contains("youtube.com") ||
                lower.contains("youtu.be") ||
                lower.contains("facebook.com") ||
                lower.contains("fb.watch") ||
                lower.contains("fb.com") ||
                lower.contains("instagram.com") ||
                lower.contains("tiktok.com") ||
                lower.contains("twitter.com") ||
                lower.contains("x.com") ||
                lower.startsWith("https://t.me/") ||
                lower.startsWith("http://t.me/")
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

        // 2. Direct Web URLs or CDN links (mp4, webm, mkv, mp3, cloud storage)
        if (platform == SupportedPlatform.WEB_DIRECT ||
            trimmed.endsWith(".mp4", ignoreCase = true) ||
            trimmed.endsWith(".webm", ignoreCase = true) ||
            trimmed.endsWith(".mkv", ignoreCase = true) ||
            trimmed.endsWith(".mp3", ignoreCase = true) ||
            trimmed.contains("commondatastorage.googleapis.com") ||
            trimmed.contains("googleusercontent.com") ||
            trimmed.contains(".cdninstagram.com") ||
            trimmed.contains(".fbcdn.net")
        ) {
            return@withContext trimmed
        }

        // 3. For TikTok: Try TikWM first (fastest and cleanest no-watermark MP4 stream)
        if (platform == SupportedPlatform.TIKTOK) {
            val tikWmStream = tryTikWMExtractor(trimmed)
            if (!tikWmStream.isNullOrBlank()) {
                return@withContext tikWmStream
            }
        }

        // 4. For YouTube & YouTube Shorts: Try Piped API first, then Invidious instances
        if (platform == SupportedPlatform.YOUTUBE) {
            val videoId = extractYouTubeId(trimmed)
            if (videoId.isNotBlank()) {
                // First try Piped API (high reliability for YouTube Shorts & Videos)
                val pipedStream = tryPipedStream(videoId, quality)
                if (!pipedStream.isNullOrBlank()) {
                    return@withContext pipedStream
                }

                // Next try Invidious public streaming instances
                val invidiousStream = tryInvidiousStream(videoId, quality)
                if (!invidiousStream.isNullOrBlank()) {
                    return@withContext invidiousStream
                }
            }
        }

        // 5. Try Cobalt multi-endpoint video stream extractors (supports YouTube, FB, Insta, TikTok, Twitter/X)
        val cobaltStream = tryCobaltExtractor(trimmed, quality)
        if (!cobaltStream.isNullOrBlank()) {
            return@withContext cobaltStream
        }

        // 6. If it's a webpage URL and could not be resolved, throw explicit error
        if (isWebPageUrl(trimmed)) {
            throw IllegalStateException(
                "অনলাইন সার্ভার এই ভিডিওর সরাসরি স্ট্রিমিং লিঙ্ক তৈরি করতে পারেনি। অনুগ্রহ করে কিছুক্ষণ পর আবার চেষ্টা করুন অথবা ভিডিওর সরাসরি mp4 লিঙ্ক দিন।"
            )
        }

        // Fallback
        return@withContext trimmed
    }

    private fun tryPipedStream(videoId: String, quality: VideoQuality): String? {
        val instances = listOf(
            "https://pipedapi.kavin.rocks",
            "https://api.piped.privacydev.net",
            "https://pipedapi.tokhmi.xyz",
            "https://piped-api.garudalinux.org",
            "https://api.piped.projectsegfau.lt",
            "https://pipedapi.leptons.xyz"
        )

        for (instance in instances) {
            try {
                val request = Request.Builder()
                    .url("$instance/streams/$videoId")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val bodyStr = response.body?.string() ?: return@use
                    val json = JSONObject(bodyStr)

                    if (quality == VideoQuality.AUDIO_MP3) {
                        val audioStreams = json.optJSONArray("audioStreams")
                        if (audioStreams != null && audioStreams.length() > 0) {
                            for (j in 0 until audioStreams.length()) {
                                val aObj = audioStreams.getJSONObject(j)
                                val aUrl = aObj.optString("url")
                                if (aUrl.isNotBlank() && aUrl.startsWith("http")) {
                                    return aUrl
                                }
                            }
                        }
                    }

                    val videoStreams = json.optJSONArray("videoStreams") ?: return@use
                    var fallbackUrl: String? = null
                    val targetRes = quality.resolution

                    for (i in 0 until videoStreams.length()) {
                        val stream = videoStreams.getJSONObject(i)
                        val streamUrl = stream.optString("url")
                        val res = stream.optString("quality")
                        val format = stream.optString("format")
                        val videoOnly = stream.optBoolean("videoOnly", false)

                        if (streamUrl.isNotBlank() && streamUrl.startsWith("http")) {
                            if (!videoOnly && (format.contains("MPEG", true) || format.contains("MP4", true))) {
                                if (fallbackUrl == null) fallbackUrl = streamUrl
                                if (res.contains(targetRes)) {
                                    return streamUrl
                                }
                            } else if (fallbackUrl == null) {
                                fallbackUrl = streamUrl
                            }
                        }
                    }
                    if (fallbackUrl != null) return fallbackUrl
                }
            } catch (e: Exception) {
                Log.d("MultiPlatformResolver", "Piped instance $instance failed: ${e.message}")
            }
        }
        return null
    }

    private fun tryInvidiousStream(videoId: String, quality: VideoQuality): String? {
        val instances = listOf(
            "https://inv.nadeko.net",
            "https://invidious.nerdvpn.de",
            "https://invidious.private.coffee",
            "https://iv.melmac.space",
            "https://invidious.jing.rocks",
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

                    if (quality == VideoQuality.AUDIO_MP3) {
                        val adaptiveFormats = json.optJSONArray("adaptiveFormats")
                        if (adaptiveFormats != null) {
                            for (j in 0 until adaptiveFormats.length()) {
                                val aObj = adaptiveFormats.getJSONObject(j)
                                val type = aObj.optString("type")
                                val aUrl = aObj.optString("url")
                                if (type.contains("audio") && aUrl.isNotBlank()) {
                                    return aUrl
                                }
                            }
                        }
                    }

                    val formatStreams = json.optJSONArray("formatStreams") ?: return@use

                    var chosenUrl: String? = null
                    val targetRes = quality.resolution

                    for (i in 0 until formatStreams.length()) {
                        val stream = formatStreams.getJSONObject(i)
                        val streamUrl = stream.optString("url")
                        val resolution = stream.optString("resolution")
                        val container = stream.optString("container")

                        if (streamUrl.isNotBlank() && streamUrl.startsWith("http")) {
                            if (chosenUrl == null) chosenUrl = streamUrl
                            if (resolution.contains(targetRes) && container.equals("mp4", ignoreCase = true)) {
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

    private fun tryTikWMExtractor(url: String): String? {
        try {
            val encoded = java.net.URLEncoder.encode(url, "UTF-8")
            val request = Request.Builder()
                .url("https://www.tikwm.com/api/?url=$encoded")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyStr = response.body?.string() ?: return null
                val json = JSONObject(bodyStr)
                if (json.optInt("code") == 0) {
                    val data = json.optJSONObject("data")
                    val playUrl = data?.optString("play") ?: ""
                    if (playUrl.startsWith("http")) {
                        return playUrl
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("MultiPlatformResolver", "TikWM extractor failed: ${e.message}")
        }
        return null
    }

    private fun tryCobaltExtractor(url: String, quality: VideoQuality): String? {
        val endpoints = listOf(
            "https://cobalt-backend.canine.tools/api/json",
            "https://api.wuk.sh/api/json",
            "https://co.wuk.sh/api/json",
            "https://cobalt.api.kwiatekm.tokyo/api/json",
            "https://api.cobalt.tools/api/json"
        )

        val jsonBody = JSONObject().apply {
            put("url", url)
            put("vQuality", if (quality.resolution == "mp3") "720" else quality.resolution)
            if (quality == VideoQuality.AUDIO_MP3) {
                put("isAudioOnly", true)
                put("aFormat", "mp3")
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

    /**
     * Extracts YouTube video ID using comprehensive regex patterns
     */
    fun extractYouTubeId(url: String): String {
        val patterns = listOf(
            Regex("""youtu\.be/([a-zA-Z0-9_-]{11})"""),
            Regex("""shorts/([a-zA-Z0-9_-]{11})"""),
            Regex("""v=([a-zA-Z0-9_-]{11})"""),
            Regex("""embed/([a-zA-Z0-9_-]{11})"""),
            Regex("""/v/([a-zA-Z0-9_-]{11})""")
        )
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) return match.groupValues[1]
        }
        return ""
    }

    /**
     * Fetches real video title using YouTube oEmbed
     */
    suspend fun fetchYouTubeVideoTitle(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val encoded = java.net.URLEncoder.encode(url, "UTF-8")
            val oembedUrl = "https://www.youtube.com/oembed?url=$encoded&format=json"
            val request = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    val title = json.optString("title")
                    if (title.isNotBlank()) {
                        return@withContext title.replace(Regex("""[\\/:*?"<>|]"""), "_").take(60)
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return@withContext null
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
