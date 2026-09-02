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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class TelegramVideoDownloaderEngine(
    private val context: Context,
    private val downloadDao: DownloadDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val activeJobs = ConcurrentHashMap<Long, Job>()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
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

        var downloadedBytes = if (partFile.exists()) partFile.length() else 0L

        // Prepare request with Range header for resuming big files
        val requestBuilder = Request.Builder()
            .url(item.downloadUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Android; TelegramDownloader/2.0)")

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
                    .url(item.downloadUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Android; TelegramDownloader/2.0)")
                    .build()
                val retryResponse = okHttpClient.newCall(freshRequest).execute()
                if (!retryResponse.isSuccessful) {
                    throw IllegalStateException("HTTP ${retryResponse.code}: ${retryResponse.message}")
                }
                processResponseBody(item.id, retryResponse, partFile, destinationFile, 0L)
                return@withContext
            }
            throw IllegalStateException("HTTP ${response.code}: ${response.message}")
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

                // Register with MediaStore so it can be seen in Gallery
                scanFileInMediaStore(context, destinationFile)
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

        fun exportToGallery(context: Context, sourceFile: File, title: String): Boolean {
            return try {
                if (!sourceFile.exists()) return false
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.TITLE, title)
                    put(MediaStore.Video.Media.DISPLAY_NAME, sourceFile.name)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/TelegramDownloads")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                }

                val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return false
                resolver.openOutputStream(uri)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                true
            } catch (e: Exception) {
                Log.e("DownloaderEngine", "Failed to export to gallery", e)
                false
            }
        }
    }
}
