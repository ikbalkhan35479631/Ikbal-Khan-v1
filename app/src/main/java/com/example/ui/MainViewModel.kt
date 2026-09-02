package com.example.ui

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DownloadItem
import com.example.data.DownloadRepository
import com.example.data.DownloadStatus
import com.example.engine.LinkType
import com.example.engine.ParsedLink
import com.example.engine.PrivateChannelHelper
import com.example.engine.PrivateSettings
import com.example.engine.TelegramLinkParser
import com.example.engine.TelegramVideoDownloaderEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class SampleLink(
    val title: String,
    val sizeText: String,
    val badge: String,
    val url: String,
    val isPrivateDemo: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = DownloadRepository(database.downloadDao())
    val downloaderEngine = TelegramVideoDownloaderEngine(application, database.downloadDao())
    private val privateHelper = PrivateChannelHelper(application)

    val allDownloads: StateFlow<List<DownloadItem>> = repository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloads: StateFlow<List<DownloadItem>> = repository.activeDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedDownloads: StateFlow<List<DownloadItem>> = repository.completedDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _parsedLink = MutableStateFlow<ParsedLink?>(null)
    val parsedLink: StateFlow<ParsedLink?> = _parsedLink.asStateFlow()

    private val _customTitle = MutableStateFlow("")
    val customTitle: StateFlow<String> = _customTitle.asStateFlow()

    private val _multiThreaded = MutableStateFlow(true)
    val multiThreaded: StateFlow<Boolean> = _multiThreaded.asStateFlow()

    private val _privateSettings = MutableStateFlow(privateHelper.getSettings())
    val privateSettings: StateFlow<PrivateSettings> = _privateSettings.asStateFlow()

    private val _playingVideo = MutableStateFlow<DownloadItem?>(null)
    val playingVideo: StateFlow<DownloadItem?> = _playingVideo.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    val sampleLinks = listOf(
        SampleLink(
            title = "Small Video (For Quick Test)",
            sizeText = "5.6 MB",
            badge = "Small Video",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        ),
        SampleLink(
            title = "Medium Video (HD 720p)",
            sizeText = "24.8 MB",
            badge = "Medium Video",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        ),
        SampleLink(
            title = "Big Video (Full HD 1080p Stream)",
            sizeText = "158 MB",
            badge = "Big Video",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
        ),
        SampleLink(
            title = "Telegram Private Link (Format Demo)",
            sizeText = "Private",
            badge = "Private Channel",
            url = "https://t.me/c/1839201948/425",
            isPrivateDemo = true
        )
    )

    fun onTabSelected(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun onUrlChanged(newUrl: String) {
        _urlInput.value = newUrl
        if (newUrl.isNotBlank()) {
            val parsed = TelegramLinkParser.parse(newUrl)
            _parsedLink.value = parsed
            if (_customTitle.value.isBlank() || _customTitle.value.startsWith("Telegram_") || _customTitle.value.startsWith("TG_")) {
                _customTitle.value = parsed.suggestedTitle
            }
        } else {
            _parsedLink.value = null
        }
    }

    fun onCustomTitleChanged(newTitle: String) {
        _customTitle.value = newTitle
    }

    fun toggleMultiThreaded(enabled: Boolean) {
        _multiThreaded.value = enabled
    }

    fun pasteFromClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipData = clipboard?.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val pasted = clipData.getItemAt(0).text?.toString() ?: ""
            if (pasted.isNotBlank()) {
                onUrlChanged(pasted)
                Toast.makeText(context, "Link pasted!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearInput() {
        _urlInput.value = ""
        _parsedLink.value = null
        _customTitle.value = ""
    }

    fun loadSample(sample: SampleLink) {
        onUrlChanged(sample.url)
        _customTitle.value = sample.title.replace(" ", "_") + ".mp4"
    }

    fun startDownload(context: Context) {
        val rawUrl = _urlInput.value.trim()
        if (rawUrl.isBlank()) {
            Toast.makeText(context, "Please enter or paste a Telegram link", Toast.LENGTH_SHORT).show()
            return
        }

        val parsed = _parsedLink.value ?: TelegramLinkParser.parse(rawUrl)
        val resolvedUrl = privateHelper.resolveDownloadUrl(parsed)

        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        val safeTitle = if (_customTitle.value.isNotBlank()) {
            _customTitle.value.trim()
        } else {
            parsed.suggestedTitle
        }

        val fileName = if (safeTitle.endsWith(".mp4") || safeTitle.endsWith(".mkv") || safeTitle.endsWith(".webm")) {
            safeTitle
        } else {
            "$safeTitle.mp4"
        }

        val destinationFile = File(storageDir, "${System.currentTimeMillis()}_$fileName")

        viewModelScope.launch {
            val downloadItem = DownloadItem(
                title = safeTitle,
                originalUrl = rawUrl,
                downloadUrl = resolvedUrl,
                filePath = destinationFile.absolutePath,
                isPrivateChannel = parsed.isPrivate,
                channelInfo = parsed.channelIdentifier,
                messageId = parsed.messageId,
                multiThreaded = _multiThreaded.value,
                status = DownloadStatus.QUEUED
            )

            val newId = repository.insert(downloadItem)
            downloaderEngine.startOrResumeDownload(newId)

            Toast.makeText(context, "Download started: $safeTitle", Toast.LENGTH_SHORT).show()
            // Switch to Active Downloads tab to show live progress
            _selectedTab.value = 1
            clearInput()
        }
    }

    fun pauseDownload(id: Long) {
        downloaderEngine.pauseDownload(id)
    }

    fun resumeDownload(id: Long) {
        downloaderEngine.startOrResumeDownload(id)
    }

    fun cancelDownload(id: Long) {
        downloaderEngine.cancelDownload(id)
    }

    fun pauseAll() {
        downloaderEngine.pauseAll()
    }

    fun resumeAll() {
        viewModelScope.launch {
            val active = repository.activeDownloads
            // Start all queued or paused items
            activeDownloads.value.forEach { item ->
                if (item.status == DownloadStatus.PAUSED || item.status == DownloadStatus.QUEUED) {
                    downloaderEngine.startOrResumeDownload(item.id)
                }
            }
        }
    }

    fun deleteCompleted(item: DownloadItem, context: Context) {
        viewModelScope.launch {
            try {
                val file = File(item.filePath)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                // Ignore file delete exception
            }
            repository.delete(item)
            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportToGallery(item: DownloadItem, context: Context) {
        val file = File(item.filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist on disk", Toast.LENGTH_SHORT).show()
            return
        }
        val success = TelegramVideoDownloaderEngine.exportToGallery(context, file, item.title)
        if (success) {
            Toast.makeText(context, "Saved to Gallery / Movies folder!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Could not save to gallery", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareVideo(item: DownloadItem, context: Context) {
        val file = File(item.filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Video via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Sharing failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun playVideo(item: DownloadItem) {
        _playingVideo.value = item
    }

    fun closePlayer() {
        _playingVideo.value = null
    }

    fun updatePrivateSettings(settings: PrivateSettings) {
        privateHelper.saveSettings(settings)
        _privateSettings.value = settings
    }
}
