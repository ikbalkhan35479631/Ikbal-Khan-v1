package com.example.data

import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val dao: DownloadDao) {
    val allDownloads: Flow<List<DownloadItem>> = dao.getAllDownloads()
    val activeDownloads: Flow<List<DownloadItem>> = dao.getActiveDownloads()
    val completedDownloads: Flow<List<DownloadItem>> = dao.getCompletedDownloads()
    val lockedDownloads: Flow<List<DownloadItem>> = dao.getLockedDownloads()

    suspend fun getById(id: Long): DownloadItem? = dao.getById(id)

    suspend fun insert(item: DownloadItem): Long = dao.insert(item)

    suspend fun update(item: DownloadItem) = dao.update(item)

    suspend fun delete(item: DownloadItem) = dao.delete(item)

    suspend fun deleteById(id: Long) = dao.deleteById(id)
    
    suspend fun deleteAllCompleted() = dao.deleteAllCompleted()

    suspend fun updateStatus(id: Long, status: DownloadStatus) = dao.updateStatus(id, status)

    suspend fun updateProgress(id: Long, downloaded: Long, total: Long, speed: String, eta: Long) =
        dao.updateProgress(id, downloaded, total, speed, eta)

    suspend fun updateLockStatus(id: Long, isLocked: Boolean) =
        dao.updateLockStatus(id, isLocked)
}
