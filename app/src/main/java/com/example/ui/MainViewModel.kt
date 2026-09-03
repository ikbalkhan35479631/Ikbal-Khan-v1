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
import com.example.engine.MultiPlatformVideoResolver
import com.example.engine.PlatformAnalysis
import com.example.engine.PrivateChannelHelper
import com.example.engine.PrivateSettings
import com.example.engine.SupportedPlatform
import com.example.engine.TelegramLinkParser
import com.example.engine.TelegramVideoDownloaderEngine
import com.example.engine.VideoQuality
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
    val platform: SupportedPlatform = SupportedPlatform.WEB_DIRECT,
    val quality: String = "1080p HD",
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

    private val _platformAnalysis = MutableStateFlow<PlatformAnalysis?>(null)
    val platformAnalysis: StateFlow<PlatformAnalysis?> = _platformAnalysis.asStateFlow()

    private val _selectedPlatform = MutableStateFlow(SupportedPlatform.ALL)
    val selectedPlatform: StateFlow<SupportedPlatform> = _selectedPlatform.asStateFlow()

    private val _selectedQuality = MutableStateFlow(VideoQuality.RES_1080P)
    val selectedQuality: StateFlow<VideoQuality> = _selectedQuality.asStateFlow()

    private val _customTitle = MutableStateFlow("")
    val customTitle: StateFlow<String> = _customTitle.asStateFlow()

    private val _multiThreaded = MutableStateFlow(true)
    val multiThreaded: StateFlow<Boolean> = _multiThreaded.asStateFlow()

    // Batch download state (10+ videos simultaneously)
    private val _isBatchMode = MutableStateFlow(false)
    val isBatchMode: StateFlow<Boolean> = _isBatchMode.asStateFlow()

    private val _batchUrlsInput = MutableStateFlow("")
    val batchUrlsInput: StateFlow<String> = _batchUrlsInput.asStateFlow()

    private val _isResolving = MutableStateFlow(false)
    val isResolving: StateFlow<Boolean> = _isResolving.asStateFlow()

    private val _resolvingMessage = MutableStateFlow("")
    val resolvingMessage: StateFlow<String> = _resolvingMessage.asStateFlow()

    private val _privateSettings = MutableStateFlow(privateHelper.getSettings())
    val privateSettings: StateFlow<PrivateSettings> = _privateSettings.asStateFlow()

    private val _playingVideo = MutableStateFlow<DownloadItem?>(null)
    val playingVideo: StateFlow<DownloadItem?> = _playingVideo.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Curated high quality multi-platform sample videos
    val sampleLinks = listOf(
        SampleLink(
            title = "Big Buck Bunny (1080p Full HD)",
            sizeText = "24.8 MB",
            badge = "1080p FHD",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            platform = SupportedPlatform.WEB_DIRECT,
            quality = "1080p HD"
        ),
        SampleLink(
            title = "Tears of Steel (4K Ultra Stream)",
            sizeText = "158 MB",
            badge = "4K Cinema",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            platform = SupportedPlatform.WEB_DIRECT,
            quality = "4K UHD"
        ),
        SampleLink(
            title = "Sintel Fantasy Film (1080p High Speed)",
            sizeText = "32.4 MB",
            badge = "1080p FHD",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            platform = SupportedPlatform.WEB_DIRECT,
            quality = "1080p HD"
        ),
        SampleLink(
            title = "For Bigger Blazes (Fast Action HD)",
            sizeText = "5.6 MB",
            badge = "Quick HD",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            platform = SupportedPlatform.WEB_DIRECT,
            quality = "720p HD"
        ),
        SampleLink(
            title = "Telegram Private Channel Demo (Protected)",
            sizeText = "Private",
            badge = "TG Private",
            url = "https://t.me/c/1839201948/425",
            platform = SupportedPlatform.TELEGRAM,
            quality = "Original",
            isPrivateDemo = true
        ),
        SampleLink(
            title = "Telegram Public Channel Demo",
            sizeText = "Public",
            badge = "TG Public",
            url = "https://t.me/durov/192",
            platform = SupportedPlatform.TELEGRAM,
            quality = "Original"
        )
    )

    // 10 high-speed videos ready for simultaneous 10+ batch testing
    val batchSampleUrls = listOf(
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    )

    fun onTabSelected(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun onPlatformFilterChanged(platform: SupportedPlatform) {
        _selectedPlatform.value = platform
    }

    fun onQualityChanged(quality: VideoQuality) {
        _selectedQuality.value = quality
    }

    fun toggleBatchMode(enabled: Boolean) {
        _isBatchMode.value = enabled
    }

    fun onBatchUrlsChanged(newText: String) {
        _batchUrlsInput.value = newText
    }

    fun load10SampleBatch() {
        _batchUrlsInput.value = batchSampleUrls.joinToString("\n")
    }

    fun onUrlChanged(newUrl: String) {
        _urlInput.value = newUrl
        if (newUrl.isNotBlank()) {
            val analysis = MultiPlatformVideoResolver.analyzeUrl(newUrl)
            _platformAnalysis.value = analysis
            _selectedPlatform.value = analysis.platform
            if (_customTitle.value.isBlank() || _customTitle.value.startsWith("Telegram_") ||
                _customTitle.value.startsWith("TG_") || _customTitle.value.startsWith("YouTube_") ||
                _customTitle.value.startsWith("Facebook_") || _customTitle.value.startsWith("Instagram_")
            ) {
                _customTitle.value = analysis.suggestedTitle
            }
        } else {
            _platformAnalysis.value = null
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
                if (_isBatchMode.value) {
                    val current = _batchUrlsInput.value
                    _batchUrlsInput.value = if (current.isBlank()) pasted else "$current\n$pasted"
                } else {
                    onUrlChanged(pasted)
                }
                Toast.makeText(context, "Link pasted!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearInput() {
        _urlInput.value = ""
        _platformAnalysis.value = null
        _customTitle.value = ""
    }

    fun clearBatchInput() {
        _batchUrlsInput.value = ""
    }

    fun loadSample(sample: SampleLink) {
        onUrlChanged(sample.url)
        _customTitle.value = sample.title.replace(" ", "_").replace("(", "").replace(")", "") + ".mp4"
    }

    fun startDownload(context: Context) {
        val rawUrl = _urlInput.value.trim()
        if (rawUrl.isBlank()) {
            Toast.makeText(context, "ভিডিও লিঙ্ক দিন (Please enter a video link)", Toast.LENGTH_SHORT).show()
            return
        }

        val analysis = _platformAnalysis.value ?: MultiPlatformVideoResolver.analyzeUrl(rawUrl)

        viewModelScope.launch {
            _isResolving.value = true
            _resolvingMessage.value = "হাই-কোয়ালিটি স্ট্রিম লিঙ্ক প্রস্তুত করা হচ্ছে..."

            try {
                val resolvedUrl: String
                if (analysis.platform == SupportedPlatform.TELEGRAM) {
                    val tgParsed = TelegramLinkParser.parse(rawUrl)
                    resolvedUrl = privateHelper.resolveDownloadUrl(tgParsed)
                } else {
                    resolvedUrl = MultiPlatformVideoResolver.resolveStreamUrl(
                        rawUrl,
                        analysis.platform,
                        _selectedQuality.value
                    )
                }

                val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
                val safeTitle = if (_customTitle.value.isNotBlank()) {
                    _customTitle.value.trim()
                } else {
                    analysis.suggestedTitle
                }

                val ext = if (_selectedQuality.value == VideoQuality.AUDIO_MP3) ".mp3" else ".mp4"
                val baseClean = safeTitle.removeSuffix(".mp4").removeSuffix(".mkv").removeSuffix(".webm").removeSuffix(".mp3")
                val fileName = "$baseClean$ext"

                val destinationFile = File(storageDir, "${System.currentTimeMillis()}_$fileName")

                val downloadItem = DownloadItem(
                    title = fileName,
                    originalUrl = rawUrl,
                    downloadUrl = resolvedUrl,
                    filePath = destinationFile.absolutePath,
                    isPrivateChannel = analysis.isPrivateTelegram,
                    channelInfo = analysis.channelInfo,
                    messageId = analysis.messageId,
                    multiThreaded = _multiThreaded.value,
                    mimeType = if (_selectedQuality.value == VideoQuality.AUDIO_MP3) "audio/mpeg" else "video/mp4",
                    status = DownloadStatus.QUEUED
                )

                val newId = repository.insert(downloadItem)
                downloaderEngine.startOrResumeDownload(newId)

                Toast.makeText(context, "ডাউনলোড শুরু হয়েছে: $fileName", Toast.LENGTH_SHORT).show()
                _selectedTab.value = 1
                clearInput()
            } catch (e: Exception) {
                Toast.makeText(context, "ত্রুটি: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _isResolving.value = false
            }
        }
    }

    /**
     * Downloads 10+ videos simultaneously in batch mode
     */
    fun startBatchDownload(context: Context) {
        val lines = _batchUrlsInput.value.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            Toast.makeText(context, "কমপক্ষে ১টি বা ততোধিক লিঙ্ক দিন", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            _isResolving.value = true
            _resolvingMessage.value = "${lines.size}টি ভিডিও লিঙ্ক প্রস্তুত করা হচ্ছে..."

            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
            var addedCount = 0

            lines.forEachIndexed { index, rawUrl ->
                try {
                    val analysis = MultiPlatformVideoResolver.analyzeUrl(rawUrl)
                    val resolvedUrl = if (analysis.platform == SupportedPlatform.TELEGRAM) {
                        val tgParsed = TelegramLinkParser.parse(rawUrl)
                        privateHelper.resolveDownloadUrl(tgParsed)
                    } else {
                        MultiPlatformVideoResolver.resolveStreamUrl(rawUrl, analysis.platform, _selectedQuality.value)
                    }

                    val ext = if (_selectedQuality.value == VideoQuality.AUDIO_MP3) ".mp3" else ".mp4"
                    val baseName = analysis.suggestedTitle.removeSuffix(".mp4").removeSuffix(".mp3")
                    val fileName = "${baseName}_#${index + 1}$ext"
                    val destFile = File(storageDir, "${System.currentTimeMillis()}_${index}_$fileName")

                    val downloadItem = DownloadItem(
                        title = fileName,
                        originalUrl = rawUrl,
                        downloadUrl = resolvedUrl,
                        filePath = destFile.absolutePath,
                        isPrivateChannel = analysis.isPrivateTelegram,
                        channelInfo = analysis.channelInfo,
                        messageId = analysis.messageId,
                        multiThreaded = _multiThreaded.value,
                        mimeType = if (_selectedQuality.value == VideoQuality.AUDIO_MP3) "audio/mpeg" else "video/mp4",
                        status = DownloadStatus.QUEUED
                    )

                    val newId = repository.insert(downloadItem)
                    // Start concurrent download on IO pool
                    downloaderEngine.startOrResumeDownload(newId)
                    addedCount++
                } catch (e: Exception) {
                    // Continue with other links
                }
            }

            _isResolving.value = false
            Toast.makeText(context, "$addedCount টি ভিডিও একসাথে ডাউনলোড শুরু হয়েছে!", Toast.LENGTH_LONG).show()
            _selectedTab.value = 1
            clearBatchInput()
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
            activeDownloads.value.forEach { item ->
                if (item.status == DownloadStatus.PAUSED || item.status == DownloadStatus.QUEUED || item.status == DownloadStatus.FAILED) {
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
            Toast.makeText(context, "মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportToGallery(item: DownloadItem, context: Context) {
        val file = File(item.filePath)
        if (!file.exists()) {
            Toast.makeText(context, "ফাইলটি স্টোরেজে পাওয়া যায়নি!", Toast.LENGTH_SHORT).show()
            return
        }
        val (success, message) = TelegramVideoDownloaderEngine.exportToGallery(context, file, item.title)
        if (success) {
            Toast.makeText(context, "সফলভাবে গ্যালারিতে সেভ হয়েছে! (Movies/TelegramDownloads)", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "সেভ করা যায়নি: $message", Toast.LENGTH_LONG).show()
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
                type = item.mimeType
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
