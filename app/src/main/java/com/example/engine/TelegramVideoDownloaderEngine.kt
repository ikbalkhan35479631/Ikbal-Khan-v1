package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.MediaStore
import android.util.Log
import com.example.data.DownloadDao
import com.example.data.DownloadItem
import com.example.data.DownloadStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class TelegramVideoDownloaderEngine(
    private val context: Context,
    private val downloadDao: DownloadDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val activeJobs = ConcurrentHashMap<Long, Job>()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .dispatcher(okhttp3.Dispatcher().apply {
            maxRequests = 128
            maxRequestsPerHost = 64
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    init {
        try {
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "TelegramDownloader:DownloadWakeLock"
            )
        } catch (e: Exception) {
            Log.e("DownloaderEngine", "Could not create WakeLock", e)
        }
    }

    private fun acquireWakeLockIfNeeded() {
        try {
            if (activeJobs.isNotEmpty() && wakeLock?.isHeld == false) {
                wakeLock?.acquire(3 * 60 * 60 * 1000L) // 3 hours max
            }
        } catch (e: Exception) {
            Log.e("DownloaderEngine", "WakeLock acquire error", e)
        }
    }

    private fun releaseWakeLockIfEmpty() {
        try {
            if (activeJobs.isEmpty() && wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e("DownloaderEngine", "WakeLock release error", e)
        }
    }

    fun isDownloading(id: Long): Boolean {
        return activeJobs[id]?.isActive == true
    }

    fun startOrResumeDownload(id: Long) {
        if (activeJobs[id]?.isActive == true) return

        val job = scope.launch {
            val item = downloadDao.getById(id) ?: return@launch
            downloadDao.updateStatus(id, DownloadStatus.DOWNLOADING)
            acquireWakeLockIfNeeded()

            try {
                executeDownload(item)
            } catch (e: CancellationException) {
                // Paused or cancelled
                val current = downloadDao.getById(id)
                if (current?.status == DownloadStatus.DOWNLOADING) {
                    downloadDao.updateStatus(id, DownloadStatus.PAUSED)
                }
            } catch (e: Exception) {
                Log.e("DownloaderEngine", "Download failed for item $id", e)
                val current = downloadDao.getById(id)
                if (current != null) {
                    downloadDao.update(
                        current.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = e.message ?: "Network error"
                        )
                    )
                }
            } finally {
                activeJobs.remove(id)
                releaseWakeLockIfEmpty()
            }
        }
        activeJobs[id] = job
    }

    fun pauseDownload(id: Long) {
        val job = activeJobs.remove(id)
        if (job != null && job.isActive) {
            job.cancel()
        }
        scope.launch {
            downloadDao.updateStatus(id, DownloadStatus.PAUSED)
            releaseWakeLockIfEmpty()
        }
    }

    fun cancelDownload(id: Long) {
        val job = activeJobs.remove(id)
        if (job != null && job.isActive) {
            job.cancel()
        }
        scope.launch {
            val item = downloadDao.getById(id)
            if (item != null) {
                // Remove partial files
                try {
                    val targetFile = File(item.filePath)
                    if (targetFile.exists()) targetFile.delete()
                    val tempFile = File("${item.filePath}.part")
                    if (tempFile.exists()) tempFile.delete()
                } catch (e: Exception) {
                    Log.e("DownloaderEngine", "Error deleting file on cancel", e)
                }
                downloadDao.deleteById(id)
            }
            releaseWakeLockIfEmpty()
        }
    }

    fun pauseAll() {
        val ids = activeJobs.keys().toList()
        for (id in ids) {
            pauseDownload(id)
        }
    }

    private suspend fun executeDownload(item: DownloadItem) = withContext(Dispatchers.IO) {
        val destinationFile = File(item.filePath)
        val partFile = File("${item.filePath}.part")

        // Ensure parent directory exists
        destinationFile.parentFile?.mkdirs()

        // 1. Check if the URL is an invalid bot stream placeholder
        if (item.downloadUrl.contains("api.telegram.org/bot") && item.downloadUrl.contains("/getFileStream?")) {
            throw IllegalStateException(
                "Telegram Bot Token একা প্রাইভেট চ্যানেল ডাউনলোড করতে পারে না। টেলিগ্রামের নিয়মানুযায়ী বটটিকে অবশ্যই ঐ প্রাইভেট চ্যানেলে 'Administrator' হিসেবে যুক্ত করতে হবে অথবা সঠিক ফাইল স্ট্রিম লিঙ্ক দিতে হবে।"
            )
        }

        // 2. Check if the URL is an unconverted Telegram page URL (e.g. t.me/c/... or t.me/...)
        if (item.downloadUrl.startsWith("https://t.me/") || item.downloadUrl.startsWith("http://t.me/")) {
            throw IllegalStateException(
                "টেলিগ্রাম পেজ সরাসরি ভিডিও ফাইল নয়। টেলিগ্রামের প্রটেক্টেড ভিডিও ডাউনলোডের জন্য টেলিগ্রাম বট ফরওয়ার্ডার বা সরাসরি মিডিয়া স্ট্রিম লিঙ্ক ব্যবহার করুন। (প্রাইভেট গাইড ট্যাব দেখুন)"
            )
        }

        // 3. Check for unconverted web video page URLs and attempt auto-resolution
        var targetUrl = item.downloadUrl
        if (MultiPlatformVideoResolver.isWebPageUrl(targetUrl)) {
            try {
                val analysis = MultiPlatformVideoResolver.analyzeUrl(item.originalUrl)
                val resolved = MultiPlatformVideoResolver.resolveStreamUrl(
                    item.originalUrl,
                    analysis.platform,
                    VideoQuality.RES_720P
                )
                if (!MultiPlatformVideoResolver.isWebPageUrl(resolved)) {
                    targetUrl = resolved
                    downloadDao.update(item.copy(downloadUrl = resolved))
                } else {
                    throw IllegalStateException(
                        "ভিডিও স্ট্রিম লিঙ্ক সংগ্রহ করা যায়নি (অনলাইন স্ট্রিমিং সার্ভার ব্যস্ত)। অনুগ্রহ করে 'পুনরায় চেষ্টা করুন' চাপুন অথবা সরাসরি ভিডিও স্ট্রিম লিঙ্ক দিন।"
                    )
                }
            } catch (e: Exception) {
                throw IllegalStateException(
                    e.message ?: "ভিডিও স্ট্রিম লিঙ্ক সংগ্রহ করা যায়নি (অনলাইন স্ট্রিমিং সার্ভার ব্যস্ত)। অনুগ্রহ করে 'পুনরায় চেষ্টা করুন' চাপুন।"
                )
            }
        }

        var downloadedBytes = if (partFile.exists()) partFile.length() else 0L

        // Prepare request with Range header for resuming big files
        val requestBuilder = Request.Builder()
            .url(targetUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")

        if (downloadedBytes > 0) {
            requestBuilder.addHeader("Range", "bytes=$downloadedBytes-")
        }

        val request = requestBuilder.build()
        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful && response.code != 206) {
            // If Range was rejected (e.g. 416), try starting over from 0
            if (response.code == 416 && downloadedBytes > 0) {
                partFile.delete()
                downloadedBytes = 0L
                val freshRequest = Request.Builder()
                    .url(targetUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                val retryResponse = okHttpClient.newCall(freshRequest).execute()
                if (!retryResponse.isSuccessful) {
                    throw IllegalStateException("HTTP ${retryResponse.code}: ${retryResponse.message}")
                }
                processResponseBody(item.id, retryResponse, partFile, destinationFile, 0L)
                return@withContext
            }
            val errorBody = try { response.body?.string()?.take(200) } catch (e: Exception) { null }
            val errorDetail = if (!errorBody.isNullOrBlank() && errorBody.contains("description")) {
                try {
                    val json = JSONObject(errorBody)
                    json.optString("description", response.message)
                } catch (e: Exception) {
                    response.message
                }
            } else {
                response.message
            }
            throw IllegalStateException("HTTP ${response.code}: $errorDetail")
        }

        val contentType = response.header("Content-Type")?.lowercase() ?: ""
        if (contentType.contains("text/html") || contentType.contains("application/json")) {
            val preview = try { response.body?.string()?.take(300) } catch (e: Exception) { "" } ?: ""
            throw IllegalStateException(
                if (preview.contains("description")) {
                    try {
                        val json = JSONObject(preview)
                        "Telegram API Error: " + json.optString("description")
                    } catch (e: Exception) {
                        "সার্ভার থেকে ভিডিওর পরিবর্তে এরর পেজ এসেছে ($contentType)"
                    }
                } else {
                    "সার্ভার থেকে ভিডিওর পরিবর্তে ওয়েবপেজ এসেছে। স্ট্রিমিং সার্ভার লিঙ্ক জেনারেট করতে পারেনি। অনুগ্রহ করে 'পুনরায় চেষ্টা করুন' চাপুন।"
                }
            )
        }

        val isPartial = response.code == 206
        val startingOffset = if (isPartial) downloadedBytes else 0L
        if (!isPartial && downloadedBytes > 0) {
            // Server ignored Range header, reset
            partFile.delete()
        }

        processResponseBody(item.id, response, partFile, destinationFile, startingOffset)
    }

    private suspend fun processResponseBody(
        id: Long,
        response: okhttp3.Response,
        partFile: File,
        destinationFile: File,
        startingOffset: Long
    ) = withContext(Dispatchers.IO) {
        val body = response.body ?: throw IllegalStateException("Empty response body")
        val contentLength = body.contentLength()
        val totalBytes = if (contentLength > 0) startingOffset + contentLength else 0L

        val randomAccessFile = RandomAccessFile(partFile, "rw")
        randomAccessFile.seek(startingOffset)

        val inputStream = body.byteStream()
        val buffer = ByteArray(64 * 1024) // 64KB buffer for optimal disk/net throughput

        var currentDownloaded = startingOffset
        var lastDbUpdate = System.currentTimeMillis()
        var speedTimer = System.currentTimeMillis()
        var bytesSinceSpeedTimer = 0L
        var currentSpeedText = "0 KB/s"
        var currentEta = 0L

        try {
            while (isActive) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break

                randomAccessFile.write(buffer, 0, bytesRead)
                currentDownloaded += bytesRead
                bytesSinceSpeedTimer += bytesRead

                val now = System.currentTimeMillis()
                val elapsedSinceSpeed = now - speedTimer

                if (elapsedSinceSpeed >= 1000) {
                    val speedBytesPerSec = (bytesSinceSpeedTimer * 1000.0 / elapsedSinceSpeed).toLong()
                    currentSpeedText = formatSpeed(speedBytesPerSec)
                    currentEta = if (totalBytes > currentDownloaded && speedBytesPerSec > 0) {
                        (totalBytes - currentDownloaded) / speedBytesPerSec
                    } else {
                        0L
                    }
                    speedTimer = now
                    bytesSinceSpeedTimer = 0L
                }

                // Throttle DB updates to avoid excessive SQLite writes
                if (now - lastDbUpdate >= 400) {
                    downloadDao.updateProgress(id, currentDownloaded, totalBytes, currentSpeedText, currentEta)
                    lastDbUpdate = now
                }
            }

            if (!isActive) {
                throw CancellationException("Download paused by user")
            }

            // Download finished! Atomically rename partFile to destinationFile
            randomAccessFile.close()
            if (destinationFile.exists()) destinationFile.delete()
            val renameSuccess = partFile.renameTo(destinationFile)
            if (!renameSuccess) {
                partFile.copyTo(destinationFile, overwrite = true)
                partFile.delete()
            }

            // Update item to COMPLETED
            val finalItem = downloadDao.getById(id)
            if (finalItem != null) {
                val completed = finalItem.copy(
                    status = DownloadStatus.COMPLETED,
                    downloadedBytes = destinationFile.length(),
                    totalBytes = destinationFile.length(),
                    speedText = "Completed",
                    etaSeconds = 0L,
                    completedAt = System.currentTimeMillis(),
                    errorMessage = null
                )
                downloadDao.update(completed)

                // Automatically export to Android MediaStore Gallery (Movies/TelegramDownloads)
                exportToGallery(context, destinationFile, completed.title)
            }
        } finally {
            try {
                randomAccessFile.close()
            } catch (e: Exception) {
                // Ignore close exceptions
            }
            try {
                inputStream.close()
            } catch (e: Exception) {
                // Ignore close exceptions
            }
            body.close()
        }
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.1f KB", kb)
                else -> "$bytes B"
            }
        }

        fun formatSpeed(bytesPerSec: Long): String {
            if (bytesPerSec <= 0) return "0 KB/s"
            val kb = bytesPerSec / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format("%.2f MB/s", mb)
                else -> String.format("%.1f KB/s", kb)
            }
        }

        fun formatEta(seconds: Long): String {
            if (seconds <= 0) return "--"
            val mins = seconds / 60
            val remainingSecs = seconds % 60
            val hours = mins / 60
            val remainingMins = mins % 60
            return when {
                hours > 0 -> "${hours}h ${remainingMins}m"
                mins > 0 -> "${mins}m ${remainingSecs}s"
                else -> "${remainingSecs}s"
            }
        }

        fun scanFileInMediaStore(context: Context, file: File): Uri? {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/TelegramDownloads")
                        put(MediaStore.Video.Media.IS_PENDING, 0)
                    }
                    context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("DownloaderEngine", "MediaStore scan error", e)
                null
            }
        }

        fun exportToGallery(context: Context, sourceFile: File, title: String): Pair<Boolean, String> {
            return try {
                if (!sourceFile.exists() || sourceFile.length() == 0L) {
                    return Pair(false, "File does not exist or is empty (${sourceFile.length()} bytes)")
                }

                val safeTitle = if (title.isBlank()) "TG_Video_${System.currentTimeMillis()}" else title
                val cleanFileName = if (safeTitle.endsWith(".mp4", ignoreCase = true) || 
                    safeTitle.endsWith(".mkv", ignoreCase = true) || 
                    safeTitle.endsWith(".webm", ignoreCase = true)) {
                    safeTitle
                } else {
                    "$safeTitle.mp4"
                }

                val mime = when {
                    cleanFileName.endsWith(".mkv", ignoreCase = true) -> "video/x-matroska"
                    cleanFileName.endsWith(".webm", ignoreCase = true) -> "video/webm"
                    else -> "video/mp4"
                }

                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.TITLE, safeTitle)
                    put(MediaStore.Video.Media.DISPLAY_NAME, cleanFileName)
                    put(MediaStore.Video.Media.MIME_TYPE, mime)
                    put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/TelegramDownloads")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                }

                var uri: Uri? = null
                try {
                    uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                } catch (e: Exception) {
                    Log.w("DownloaderEngine", "MediaStore insert exception: ${e.message}")
                }

                if (uri != null) {
                    val bytesCopied = resolver.openOutputStream(uri)?.use { outputStream ->
                        sourceFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    } ?: 0L

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val finalValues = ContentValues().apply {
                            put(MediaStore.Video.Media.IS_PENDING, 0)
                        }
                        resolver.update(uri, finalValues, null, null)
                    }

                    // Trigger MediaScanner for Gallery refresh
                    try {
                        android.media.MediaScannerConnection.scanFile(
                            context,
                            arrayOf(sourceFile.absolutePath),
                            arrayOf(mime),
                            null
                        )
                    } catch (e: Exception) {
                        // non-fatal
                    }

                    Pair(true, "Saved to Gallery (${formatBytes(bytesCopied)})")
                } else {
                    // Fallback for Rooted devices, Custom ROMs, or Android <= 9
                    val publicMoviesDir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                        "VIPDownloads"
                    )
                    publicMoviesDir.mkdirs()
                    val targetPublicFile = File(publicMoviesDir, cleanFileName)
                    sourceFile.copyTo(targetPublicFile, overwrite = true)

                    try {
                        android.media.MediaScannerConnection.scanFile(
                            context,
                            arrayOf(targetPublicFile.absolutePath, sourceFile.absolutePath),
                            arrayOf(mime),
                            null
                        )
                    } catch (e: Exception) {
                        // non-fatal
                    }

                    Pair(true, "Saved to Public Movies (${formatBytes(targetPublicFile.length())})")
                }
            } catch (e: Exception) {
                Log.e("DownloaderEngine", "Failed to export to gallery", e)
                Pair(false, e.localizedMessage ?: "Unknown error")
            }
        }
    }
}
