package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

@Entity(tableName = "download_items")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val originalUrl: String,
    val downloadUrl: String,
    val filePath: String,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val speedText: String = "0 KB/s",
    val etaSeconds: Long = 0L,
    val isPrivateChannel: Boolean = false,
    val channelInfo: String = "",
    val messageId: String = "",
    val thumbnailUri: String? = null,
    val durationSeconds: Long = 0L,
    val mimeType: String = "video/mp4",
    val multiThreaded: Boolean = true,
    val errorMessage: String? = null,
    val isLocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
