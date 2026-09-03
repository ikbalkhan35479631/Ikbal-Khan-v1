package com.example.engine

import android.content.Context
import android.content.SharedPreferences

data class PrivateSettings(
    val botToken: String = "",
    val sessionString: String = "",
    val customProxyUrl: String = "",
    val fastChunking: Boolean = true
)

class PrivateChannelHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tg_private_prefs", Context.MODE_PRIVATE)

    fun getSettings(): PrivateSettings {
        return PrivateSettings(
            botToken = prefs.getString("bot_token", "") ?: "",
            sessionString = prefs.getString("session_string", "") ?: "",
            customProxyUrl = prefs.getString("custom_proxy", "") ?: "",
            fastChunking = prefs.getBoolean("fast_chunking", true)
        )
    }

    fun saveSettings(settings: PrivateSettings) {
        prefs.edit()
            .putString("bot_token", settings.botToken)
            .putString("session_string", settings.sessionString)
            .putString("custom_proxy", settings.customProxyUrl)
            .putBoolean("fast_chunking", settings.fastChunking)
            .apply()
    }

    /**
     * Resolves a Telegram link into a downloadable video URL.
     * Supports Bot API endpoints, custom proxy/bridge, or direct stream resolution.
     */
    fun resolveDownloadUrl(parsedLink: ParsedLink): String {
        val settings = getSettings()

        // If it's already a direct bot stream or direct video URL:
        if (parsedLink.linkType == LinkType.BOT_FILE || parsedLink.linkType == LinkType.DIRECT_VIDEO) {
            return parsedLink.originalUrl
        }

        // If user configured a custom proxy or forwarder bridge URL:
        if (settings.customProxyUrl.isNotBlank()) {
            val bridge = settings.customProxyUrl.trimEnd('/')
            return "$bridge/download?url=${java.net.URLEncoder.encode(parsedLink.originalUrl, "UTF-8")}"
        }

        // Default or Fallback: return the original url
        return parsedLink.originalUrl
    }
}
