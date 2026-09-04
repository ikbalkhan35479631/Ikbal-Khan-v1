package com.example.engine

import android.content.Context
import com.example.data.DownloadDao
import com.example.data.DownloadItem
import com.example.data.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

/**
 * DownloadManager
 * Manages downloading tasks and persists task status, progress percentage,
 * status transitions (QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED),
 * and local file paths using Room database (DownloadDao).
 */
class DownloadManager(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val downloaderEngine: TelegramVideoDownloaderEngine
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val allTasks: Flow<List<DownloadItem>> = downloadDao.getAllDownloads()
    val activeTasks: Flow<List<DownloadItem>> = downloadDao.getActiveDownloads()
    val completedTasks: Flow<List<DownloadItem>> = downloadDao.getCompletedDownloads()

    val queuedTasks: Flow<List<DownloadItem>> = downloadDao.getAllDownloads().map { list ->
        list.filter { it.status == DownloadStatus.QUEUED }
    }

    val downloadingTasks: Flow<List<DownloadItem>> = downloadDao.getAllDownloads().map { list ->
        list.filter { it.status == DownloadStatus.DOWNLOADING }
    }

    val pausedTasks: Flow<List<DownloadItem>> = downloadDao.getAllDownloads().map { list ->
        list.filter { it.status == DownloadStatus.PAUSED }
    }

    /**
     * Calculates the progress percentage (0 - 100) for a given download item.
     */
    fun getProgressPercentage(item: DownloadItem): Int {
        if (item.totalBytes <= 0) return 0
        return ((item.downloadedBytes.toDouble() / item.totalBytes.toDouble()) * 100.0)
            .toInt()
            .coerceIn(0, 100)
    }

    /**
     * Enqueues a new download task into the Room database with initial QUEUED status.
     *
     * @param originalUrl The original URL or Telegram post link
     * @param directUrl Direct stream URL or downloadable link
     * @param title Title/name of the file or video
     * @param localFilePath Destination file path on local storage
     * @param mimeType MIME type of the file
     * @param isMultiThreaded Whether multi-threaded downloading is enabled
     * @param isPrivate Whether it is from a private channel
     * @return The generated task ID from Room database
     */
    suspend fun enqueueTask(
        originalUrl: String,
        directUrl: String,
        title: String,
        localFilePath: String,
        mimeType: String = "video/mp4",
        isMultiThreaded: Boolean = true,
        isPrivate: Boolean = false
    ): Long {
        val newItem = DownloadItem(
            originalUrl = originalUrl,
            downloadUrl = directUrl,
            title = title,
            filePath = localFilePath,
            mimeType = mimeType,
            status = DownloadStatus.QUEUED,
            downloadedBytes = 0L,
            totalBytes = 0L,
            speedText = "0 KB/s",
            multiThreaded = isMultiThreaded,
            isPrivateChannel = isPrivate,
            createdAt = System.currentTimeMillis()
        )
        return downloadDao.insert(newItem)
    }

    /**
     * Starts execution of a task by ID.
     */
    fun startTask(id: Long) {
        downloaderEngine.startOrResumeDownload(id)
    }

    /**
     * Pauses an active downloading task.
     */
    fun pauseTask(id: Long) {
        downloaderEngine.pauseDownload(id)
    }

    /**
     * Resumes a paused or failed task.
     */
    fun resumeTask(id: Long) {
        downloaderEngine.startOrResumeDownload(id)
    }

    /**
     * Cancels a download task and cleans up temporary local files.
     */
    fun cancelTask(id: Long) {
        scope.launch {
            downloaderEngine.cancelDownload(id)
            val item = downloadDao.getById(id)
            if (item != null) {
                try {
                    val file = File(item.filePath)
                    if (file.exists()) file.delete()
                    val part = File(item.filePath + ".part")
                    if (part.exists()) part.delete()
                } catch (e: Exception) {
                    // Ignore file deletion error
                }
                downloadDao.delete(item)
            }
        }
    }

    /**
     * Deletes a completed task record and its local file.
     */
    suspend fun deleteTask(item: DownloadItem) {
        try {
            val file = File(item.filePath)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            // Ignore
        }
        downloadDao.delete(item)
    }

    /**
     * Clears all completed download records from Room database.
     */
    suspend fun clearCompletedTasks() {
        downloadDao.deleteAllCompleted()
    }

    /**
     * Gets a task by ID from Room database.
     */
    suspend fun getTaskById(id: Long): DownloadItem? {
        return downloadDao.getById(id)
    }
}
