package com.example.ui

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.DownloadItem
import com.example.ui.theme.VipGold
import com.example.ui.theme.VipGoldLight
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

enum class PlayerAspectRatio(val label: String) {
    FIT("Fit"),
    STRETCH("Stretch"),
    SIXTEEN_NINE("16:9")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProVideoPlayerDialog(
    item: DownloadItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit
) {
    val context = LocalContext.current
    val videoFile = remember(item.filePath) { File(item.filePath) }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }

    var isControlsVisible by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var aspectRatioMode by remember { mutableStateOf(PlayerAspectRatio.FIT) }
    var isMuted by remember { mutableStateOf(false) }
    var doubleTapFeedback by remember { mutableStateOf<String?>(null) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    var videoWidth by remember { mutableIntStateOf(16) }
    var videoHeight by remember { mutableIntStateOf(9) }

    // Auto-hide controls after 4 seconds of inactivity
    LaunchedEffect(isControlsVisible, isPlaying, isScreenLocked) {
        if (isControlsVisible && isPlaying && !isScreenLocked) {
            delay(4000)
            isControlsVisible = false
        }
    }

    // Feedback popup dismiss
    LaunchedEffect(doubleTapFeedback) {
        if (doubleTapFeedback != null) {
            delay(700)
            doubleTapFeedback = null
        }
    }

    // Ticking progress update
    LaunchedEffect(isPlaying) {
        while (true) {
            if (isPlaying && !isSeeking) {
                mediaPlayer?.let { mp ->
                    try {
                        currentPositionMs = mp.currentPosition.toLong()
                        durationMs = mp.duration.toLong().coerceAtLeast(0L)
                    } catch (e: Exception) {
                        // ignored
                    }
                }
            }
            delay(400)
        }
    }

    DisposableEffect(videoFile) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (e: Exception) {
                // ignored
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (isScreenLocked) {
                                    // When screen is locked, tap momentarily reveals unlock button
                                    isControlsVisible = !isControlsVisible
                                } else {
                                    isControlsVisible = !isControlsVisible
                                }
                            }
                        )
                    }
            ) {
                // SurfaceView for Video Rendering
                val videoModifier = when (aspectRatioMode) {
                    PlayerAspectRatio.FIT -> {
                        val ratio = if (videoHeight > 0) videoWidth.toFloat() / videoHeight.toFloat() else 16f / 9f
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .aspectRatio(ratio)
                    }
                    PlayerAspectRatio.STRETCH -> Modifier.fillMaxSize()
                    PlayerAspectRatio.SIXTEEN_NINE -> Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                }

                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            holder.addCallback(object : SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: SurfaceHolder) {
                                    try {
                                        val mp = MediaPlayer().apply {
                                            setDisplay(holder)
                                            setDataSource(ctx, Uri.fromFile(videoFile))
                                            setOnPreparedListener { player ->
                                                videoWidth = player.videoWidth.coerceAtLeast(16)
                                                videoHeight = player.videoHeight.coerceAtLeast(9)
                                                durationMs = player.duration.toLong()
                                                player.start()
                                                isPlaying = true
                                            }
                                            setOnCompletionListener {
                                                isPlaying = false
                                                currentPositionMs = durationMs
                                            }
                                            prepareAsync()
                                        }
                                        mediaPlayer = mp
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }

                                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                                override fun surfaceDestroyed(holder: SurfaceHolder) {
                                    try {
                                        mediaPlayer?.setDisplay(null)
                                    } catch (e: Exception) {
                                        // ignored
                                    }
                                }
                            })
                        }
                    },
                    modifier = videoModifier
                )

                // Left & Right double-tap transparent zones (when not locked)
                if (!isScreenLocked) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left zone: Rewind 10s
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            mediaPlayer?.let { mp ->
                                                val target = (mp.currentPosition - 10000).coerceAtLeast(0)
                                                mp.seekTo(target)
                                                currentPositionMs = target.toLong()
                                                doubleTapFeedback = "⏪ -10s"
                                            }
                                        },
                                        onTap = { isControlsVisible = !isControlsVisible }
                                    )
                                }
                        )

                        // Center zone: Tap to toggle controls
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { isControlsVisible = !isControlsVisible }
                                    )
                                }
                        )

                        // Right zone: Forward 10s
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            mediaPlayer?.let { mp ->
                                                val target = (mp.currentPosition + 10000).coerceAtMost(mp.duration)
                                                mp.seekTo(target)
                                                currentPositionMs = target.toLong()
                                                doubleTapFeedback = "+10s ⏩"
                                            }
                                        },
                                        onTap = { isControlsVisible = !isControlsVisible }
                                    )
                                }
                        )
                    }
                }

                // Double-Tap Animated Indicator Popup
                if (doubleTapFeedback != null) {
                    Surface(
                        color = Color(0xCC000000),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = doubleTapFeedback ?: "",
                            color = VipGold,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                }

                // Screen Lock Floating Toggle (always accessible when controls visible)
                AnimatedVisibility(
                    visible = isControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp)
                ) {
                    Surface(
                        color = Color(0xAA1A1D24),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, VipGold)
                    ) {
                        IconButton(
                            onClick = {
                                isScreenLocked = !isScreenLocked
                                isControlsVisible = true
                            },
                            modifier = Modifier.testTag("player_lock_toggle")
                        ) {
                            Icon(
                                imageVector = if (isScreenLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = if (isScreenLocked) "Unlock" else "Lock Controls",
                                tint = VipGold
                            )
                        }
                    }
                }

                // Full Controls Overlay (Hidden when locked, except lock icon)
                AnimatedVisibility(
                    visible = isControlsVisible && !isScreenLocked,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Top gradient scrim
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xE6000000), Color.Transparent)
                                    )
                                )
                        )

                        // Bottom gradient scrim
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xF0000000))
                                    )
                                )
                        )

                        // Top Header Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 16.dp, vertical = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.testTag("player_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (videoWidth > 0 && videoHeight > 0) "${videoWidth}x${videoHeight} • Pro Player" else "Pro Video Player",
                                        color = VipGold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Aspect ratio toggle
                                Surface(
                                    color = Color(0x33FFFFFF),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clickable {
                                            aspectRatioMode = when (aspectRatioMode) {
                                                PlayerAspectRatio.FIT -> PlayerAspectRatio.SIXTEEN_NINE
                                                PlayerAspectRatio.SIXTEEN_NINE -> PlayerAspectRatio.STRETCH
                                                PlayerAspectRatio.STRETCH -> PlayerAspectRatio.FIT
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.AspectRatio,
                                            contentDescription = "Aspect Ratio",
                                            tint = VipGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = aspectRatioMode.label,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Playback speed button
                                Surface(
                                    color = Color(0x33FFFFFF),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .clickable { showSpeedMenu = true }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Speed,
                                            contentDescription = "Playback Speed",
                                            tint = VipGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${playbackSpeed}x",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Mute Toggle
                                IconButton(
                                    onClick = {
                                        isMuted = !isMuted
                                        val vol = if (isMuted) 0f else 1f
                                        mediaPlayer?.setVolume(vol, vol)
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                        contentDescription = "Mute Toggle",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        // Center Playback Controls (-10s, Play/Pause, +10s)
                        Row(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // -10s Button
                            Surface(
                                shape = CircleShape,
                                color = Color(0x66000000),
                                modifier = Modifier
                                    .size(54.dp)
                                    .clickable {
                                        mediaPlayer?.let { mp ->
                                            val target = (mp.currentPosition - 10000).coerceAtLeast(0)
                                            mp.seekTo(target)
                                            currentPositionMs = target.toLong()
                                            doubleTapFeedback = "⏪ -10s"
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FastRewind,
                                        contentDescription = "Rewind 10 Seconds",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(28.dp))

                            // Big Play / Pause Button
                            Surface(
                                shape = CircleShape,
                                color = VipGold,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clickable {
                                        mediaPlayer?.let { mp ->
                                            if (mp.isPlaying) {
                                                mp.pause()
                                                isPlaying = false
                                            } else {
                                                mp.start()
                                                isPlaying = true
                                            }
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.Black,
                                        modifier = Modifier.size(42.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(28.dp))

                            // +10s Button
                            Surface(
                                shape = CircleShape,
                                color = Color(0x66000000),
                                modifier = Modifier
                                    .size(54.dp)
                                    .clickable {
                                        mediaPlayer?.let { mp ->
                                            val target = (mp.currentPosition + 10000).coerceAtMost(mp.duration)
                                            mp.seekTo(target)
                                            currentPositionMs = target.toLong()
                                            doubleTapFeedback = "+10s ⏩"
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FastForward,
                                        contentDescription = "Forward 10 Seconds",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }

                        // Bottom Control Bar (Time, Slider, Actions)
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            // Timeline & Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatDuration(currentPositionMs),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = formatDuration(durationMs),
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Slider(
                                value = if (isSeeking) seekProgress else (if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f),
                                onValueChange = { progress ->
                                    isSeeking = true
                                    seekProgress = progress
                                    currentPositionMs = (progress * durationMs).toLong()
                                },
                                onValueChangeFinished = {
                                    isSeeking = false
                                    mediaPlayer?.let { mp ->
                                        val seekTarget = (seekProgress * durationMs).toInt()
                                        mp.seekTo(seekTarget)
                                        currentPositionMs = seekTarget.toLong()
                                    }
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = VipGold,
                                    activeTrackColor = VipGold,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Bottom Buttons: Export to Gallery & Share
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Save to Gallery Button
                                Surface(
                                    color = Color(0x33FFB300),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, VipGold.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clickable { onExport() }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.DownloadDone,
                                            contentDescription = "Save to Gallery",
                                            tint = VipGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "গ্যালারিতে সেভ করুন",
                                            color = VipGold,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Share Button
                                Surface(
                                    color = Color(0x33FFFFFF),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .clickable { onShare() }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = "Share",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "শেয়ার",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Speed Selector Dialog
                if (showSpeedMenu) {
                    Dialog(onDismissRequest = { showSpeedMenu = false }) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E222B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VipGold.copy(alpha = 0.4f)),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "প্লেব্যাক স্পিড নির্বাচন করুন",
                                    color = VipGold,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                                speeds.forEach { spd ->
                                    val isCurrent = (playbackSpeed == spd)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) VipGold else Color(0x22FFFFFF),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                playbackSpeed = spd
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    try {
                                                        mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(spd) ?: android.media.PlaybackParams().setSpeed(spd)
                                                    } catch (e: Exception) {
                                                        // ignored
                                                    }
                                                }
                                                showSpeedMenu = false
                                            }
                                    ) {
                                        Text(
                                            text = if (spd == 1.0f) "1.0x (স্বাভাবিক)" else "${spd}x",
                                            color = if (isCurrent) Color.Black else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
