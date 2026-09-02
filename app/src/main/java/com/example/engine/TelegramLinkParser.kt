package com.example.engine

import java.net.URI

enum class LinkType {
    PRIVATE_CHANNEL,
    PUBLIC_CHANNEL,
    BOT_FILE,
    TELEGRAM_WEB,
    DIRECT_VIDEO
}

data class ParsedLink(
    val originalUrl: String,
    val linkType: LinkType,
    val channelIdentifier: String,
    val messageId: String,
    val suggestedTitle: String,
    val isPrivate: Boolean,
    val infoMessage: String
)

object TelegramLinkParser {

    fun parse(rawInput: String): ParsedLink {
        val trimmed = rawInput.trim()

        // 1. Private Telegram link: https://t.me/c/1234567890/123 or t.me/c/1234567890/123
        val privateRegex = Regex("""(?:https?://)?t\.me/c/(\d+)/(\d+)""")
        val privateMatch = privateRegex.find(trimmed)
        if (privateMatch != null) {
            val channelId = privateMatch.groupValues[1]
            val msgId = privateMatch.groupValues[2]
            return ParsedLink(
                originalUrl = trimmed,
                linkType = LinkType.PRIVATE_CHANNEL,
                channelIdentifier = "-100$channelId",
                messageId = msgId,
                suggestedTitle = "Telegram_Private_${channelId}_msg_$msgId.mp4",
                isPrivate = true,
                infoMessage = "Private Telegram Channel Link (ID: -100$channelId, Msg: #$msgId)"
            )
        }

        // 2. Telegram Web links: https://web.telegram.org/a/#-1001234567890_42 or web.telegram.org/k/#@channel/42
        val webRegex = Regex("""(?:https?://)?web\.telegram\.org/[ak]/#(?:@|c/|-100)?([a-zA-Z0-9_-]+)[/_](\d+)""")
        val webMatch = webRegex.find(trimmed)
        if (webMatch != null) {
            val channel = webMatch.groupValues[1]
            val msgId = webMatch.groupValues[2]
            val isPriv = channel.all { it.isDigit() }
            return ParsedLink(
                originalUrl = trimmed,
                linkType = LinkType.TELEGRAM_WEB,
                channelIdentifier = channel,
                messageId = msgId,
                suggestedTitle = "Telegram_Web_${channel}_msg_$msgId.mp4",
                isPrivate = isPriv,
                infoMessage = "Telegram Web Client Link ($channel, Msg: #$msgId)"
            )
        }

        // 3. Telegram Bot File API link: https://api.telegram.org/file/bot<token>/<path>
        if (trimmed.contains("api.telegram.org/file/bot")) {
            val fileName = trimmed.substringAfterLast("/").substringBefore("?").ifEmpty { "telegram_bot_video.mp4" }
            return ParsedLink(
                originalUrl = trimmed,
                linkType = LinkType.BOT_FILE,
                channelIdentifier = "Bot API Stream",
                messageId = "DirectFile",
                suggestedTitle = if (fileName.contains(".")) fileName else "$fileName.mp4",
                isPrivate = true,
                infoMessage = "Telegram Bot Direct Stream File"
            )
        }

        // 4. Public Telegram link: https://t.me/channel_name/123
        val publicRegex = Regex("""(?:https?://)?t\.me/([a-zA-Z0-9_]+)/(\d+)""")
        val publicMatch = publicRegex.find(trimmed)
        if (publicMatch != null) {
            val channelName = publicMatch.groupValues[1]
            val msgId = publicMatch.groupValues[2]
            return ParsedLink(
                originalUrl = trimmed,
                linkType = LinkType.PUBLIC_CHANNEL,
                channelIdentifier = "@$channelName",
                messageId = msgId,
                suggestedTitle = "TG_${channelName}_$msgId.mp4",
                isPrivate = false,
                infoMessage = "Telegram Public Channel (@$channelName, Msg: #$msgId)"
            )
        }

        // 5. Direct video stream or CDN URL
        val cleanUrl = trimmed.substringBefore("?")
        val extractedName = cleanUrl.substringAfterLast("/").ifEmpty { "video_${System.currentTimeMillis()}.mp4" }
        val finalTitle = if (extractedName.endsWith(".mp4", ignoreCase = true) ||
            extractedName.endsWith(".mkv", ignoreCase = true) ||
            extractedName.endsWith(".webm", ignoreCase = true) ||
            extractedName.endsWith(".mov", ignoreCase = true)
        ) {
            extractedName
        } else {
            "$extractedName.mp4"
        }

        return ParsedLink(
            originalUrl = trimmed,
            linkType = LinkType.DIRECT_VIDEO,
            channelIdentifier = "Direct Stream / Media URL",
            messageId = "Direct",
            suggestedTitle = finalTitle,
            isPrivate = false,
            infoMessage = "Direct Video Stream / Media URL"
        )
    }
}
