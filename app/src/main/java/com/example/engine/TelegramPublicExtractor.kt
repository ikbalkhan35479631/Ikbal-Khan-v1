package com.example.engine

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class TelegramPublicMedia(
    val directStreamUrl: String,
    val title: String,
    val mimeType: String,
    val thumbnailUrl: String?,
    val isVideo: Boolean,
    val isDocument: Boolean
)

object TelegramPublicExtractor {
    private const val TAG = "TgPublicExtractor"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    /**
     * Resolves a public Telegram channel or post link without needing any bot or API token.
     * Uses Telegram's public embed widgets (e.g. https://t.me/<channel>/<msg_id>?embed=1)
     */
    suspend fun resolvePublicMedia(channel: String, messageId: String): TelegramPublicMedia? = withContext(Dispatchers.IO) {
        val cleanChannel = channel.trim().removePrefix("@").removePrefix("t.me/").removePrefix("https://t.me/")
        val cleanMsgId = messageId.trim()

        if (cleanChannel.isBlank() || cleanMsgId.isBlank() || cleanChannel.all { it.isDigit() }) {
            // Numeric-only channel identifier is private (-100xxxx), cannot be scraped via public embed
            return@withContext null
        }

        val embedUrl = "https://t.me/$cleanChannel/$cleanMsgId?embed=1"
        try {
            val request = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Embed fetch failed HTTP ${response.code} for $embedUrl")
                return@withContext null
            }

            val html = response.body?.string() ?: return@withContext null

            // 1. Extract Video Stream URL
            // Example: <video src="https://cdn4.telesco.pe/file/...mp4?token=..." ...>
            val videoRegex = Pattern.compile("""<video[^>]+src=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE)
            val videoMatcher = videoRegex.matcher(html)
            var videoUrl: String? = null
            if (videoMatcher.find()) {
                videoUrl = videoMatcher.group(1)?.replace("&amp;", "&")
            }

            // Fallback video match from telescope CDN
            if (videoUrl.isNullOrBlank()) {
                val cdnVideoRegex = Pattern.compile("""(https://cdn[0-9]*\.telesco\.pe/file/[a-zA-Z0-9_-]+\.mp4\?token=[^"'\s<>]+)""")
                val cdnMatcher = cdnVideoRegex.matcher(html)
                if (cdnMatcher.find()) {
                    videoUrl = cdnMatcher.group(1)?.replace("&amp;", "&")
                }
            }

            // 2. Extract Audio Stream URL
            var audioUrl: String? = null
            val audioRegex = Pattern.compile("""<audio[^>]+src=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE)
            val audioMatcher = audioRegex.matcher(html)
            if (audioMatcher.find()) {
                audioUrl = audioMatcher.group(1)?.replace("&amp;", "&")
            }

            // 3. Extract Document / File URL
            var docUrl: String? = null
            val docRegex = Pattern.compile("""<a[^>]+class=["'][^"']*tgme_widget_message_document_wrap[^"']*["'][^>]+href=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE)
            val docMatcher = docRegex.matcher(html)
            if (docMatcher.find()) {
                docUrl = docMatcher.group(1)?.replace("&amp;", "&")
            }

            // Fallback document title
            var docTitle: String? = null
            val docTitleRegex = Pattern.compile("""<div[^>]+class=["'][^"']*tgme_widget_message_document_title[^"']*["'][^>]*>([^<]+)</div>""", Pattern.CASE_INSENSITIVE)
            val docTitleMatcher = docTitleRegex.matcher(html)
            if (docTitleMatcher.find()) {
                docTitle = docTitleMatcher.group(1)?.trim()
            }

            // 4. Extract Caption / Text
            val textRegex = Pattern.compile("""<div[^>]+class=["'][^"']*tgme_widget_message_text[^"']*["'][^>]*>(.*?)</div>""", Pattern.DOTALL or Pattern.CASE_INSENSITIVE)
            val textMatcher = textRegex.matcher(html)
            var caption: String? = null
            if (textMatcher.find()) {
                val rawText = textMatcher.group(1) ?: ""
                caption = rawText
                    .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), " ")
                    .replace(Regex("<[^>]+>"), "")
                    .trim()
                    .replace("\n", " ")
                    .replace(Regex("\\s+"), " ")
                    .take(70)
            }

            // 5. Extract Thumbnail
            var thumbUrl: String? = null
            val thumbRegex = Pattern.compile("""background-image:\s*url\(['"]?(https://cdn[0-9]*\.telesco\.pe/file/[^'")]+)['"]?\)""")
            val thumbMatcher = thumbRegex.matcher(html)
            if (thumbMatcher.find()) {
                thumbUrl = thumbMatcher.group(1)
            }

            // Build final TelegramPublicMedia
            when {
                !videoUrl.isNullOrBlank() -> {
                    val cleanCaption = caption?.replace(Regex("[^a-zA-Z0-9_\\-\\s\u0980-\u09FF]"), "")?.trim()?.replace(" ", "_")
                    val title = if (!cleanCaption.isNullOrBlank()) {
                        "TG_${cleanChannel}_${cleanCaption.take(40)}.mp4"
                    } else {
                        "TG_${cleanChannel}_$cleanMsgId.mp4"
                    }
                    return@withContext TelegramPublicMedia(
                        directStreamUrl = videoUrl,
                        title = title,
                        mimeType = "video/mp4",
                        thumbnailUrl = thumbUrl,
                        isVideo = true,
                        isDocument = false
                    )
                }

                !audioUrl.isNullOrBlank() -> {
                    val title = if (!caption.isNullOrBlank()) "TG_${cleanChannel}_${cleanMsgId}.mp3" else "TG_Audio_${cleanChannel}_$cleanMsgId.mp3"
                    return@withContext TelegramPublicMedia(
                        directStreamUrl = audioUrl,
                        title = title,
                        mimeType = "audio/mpeg",
                        thumbnailUrl = thumbUrl,
                        isVideo = false,
                        isDocument = false
                    )
                }

                !docUrl.isNullOrBlank() -> {
                    val fileName = docTitle ?: "TG_${cleanChannel}_Doc_$cleanMsgId"
                    return@withContext TelegramPublicMedia(
                        directStreamUrl = docUrl,
                        title = fileName,
                        mimeType = "application/octet-stream",
                        thumbnailUrl = thumbUrl,
                        isVideo = false,
                        isDocument = true
                    )
                }

                else -> {
                    // Check if it's in public channel web page format (https://t.me/s/<channel>/<msg_id>)
                    return@withContext tryExtractFromPublicPage(cleanChannel, cleanMsgId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving Telegram public embed: ${e.message}", e)
            return@withContext null
        }
    }

    private fun tryExtractFromPublicPage(channel: String, messageId: String): TelegramPublicMedia? {
        try {
            val pageUrl = "https://t.me/s/$channel/$messageId"
            val request = Request.Builder()
                .url(pageUrl)
                .header("User-Agent", USER_AGENT)
                .build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null
            val html = response.body?.string() ?: return null

            val videoRegex = Pattern.compile("""<video[^>]+src=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE)
            val matcher = videoRegex.matcher(html)
            if (matcher.find()) {
                val vUrl = matcher.group(1)?.replace("&amp;", "&") ?: return null
                return TelegramPublicMedia(
                    directStreamUrl = vUrl,
                    title = "TG_${channel}_$messageId.mp4",
                    mimeType = "video/mp4",
                    thumbnailUrl = null,
                    isVideo = true,
                    isDocument = false
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "tryExtractFromPublicPage error: ${e.message}")
        }
        return null
    }
}
