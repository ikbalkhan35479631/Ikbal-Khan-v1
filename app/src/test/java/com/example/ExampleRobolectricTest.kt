package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.LinkType
import com.example.engine.MultiPlatformVideoResolver
import com.example.engine.SupportedPlatform
import com.example.engine.TelegramLinkParser
import com.example.engine.TelegramVideoDownloaderEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("VIP Video Downloader", appName)
  }

  @Test
  fun `parse private telegram channel link`() {
    val privateLink = "https://t.me/c/1839201948/425"
    val parsed = TelegramLinkParser.parse(privateLink)
    assertEquals(LinkType.PRIVATE_CHANNEL, parsed.linkType)
    assertTrue(parsed.isPrivate)
    assertEquals("-1001839201948", parsed.channelIdentifier)
    assertEquals("425", parsed.messageId)
  }

  @Test
  fun `detect multi platforms accurately`() {
    assertEquals(SupportedPlatform.YOUTUBE, MultiPlatformVideoResolver.detectPlatform("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    assertEquals(SupportedPlatform.FACEBOOK, MultiPlatformVideoResolver.detectPlatform("https://www.facebook.com/reel/12345678"))
    assertEquals(SupportedPlatform.INSTAGRAM, MultiPlatformVideoResolver.detectPlatform("https://www.instagram.com/reel/Cx12345/"))
    assertEquals(SupportedPlatform.GOOGLE_DRIVE, MultiPlatformVideoResolver.detectPlatform("https://drive.google.com/file/d/1a2b3c4d/view"))
    assertEquals(SupportedPlatform.TELEGRAM, MultiPlatformVideoResolver.detectPlatform("https://t.me/c/12345/67"))
  }

  @Test
  fun `format bytes for small and big videos`() {
    val small = TelegramVideoDownloaderEngine.formatBytes(5_500_000L)
    val big = TelegramVideoDownloaderEngine.formatBytes(1_200_000_000L)
    assertTrue(small.contains("MB"))
    assertTrue(big.contains("GB"))
  }
}


