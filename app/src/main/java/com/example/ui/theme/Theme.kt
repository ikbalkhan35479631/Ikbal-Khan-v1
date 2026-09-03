package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = VipGold,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF332900),
    onPrimaryContainer = VipGoldLight,
    secondary = VipAmber,
    onSecondary = Color.Black,
    tertiary = TelegramAccent,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFE2E4EB),
    outline = DarkOutline
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF8A6D00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF3CD),
    onPrimaryContainer = Color(0xFF473700),
    secondary = Color(0xFFD48806),
    onSecondary = Color.White,
    tertiary = TelegramBlue,
    background = LightBackground,
    onBackground = Color(0xFF10141C),
    surface = LightSurface,
    onSurface = Color(0xFF10141C),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF3B4455),
    outline = LightOutline
  )


@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
