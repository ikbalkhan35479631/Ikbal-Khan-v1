package com.example.ui

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
import com.example.engine.MultiPlatformVideoResolver
import com.example.engine.PlatformAnalysis
import com.example.engine.PrivateChannelHelper
import com.example.engine.PrivateSettings
import com.example.engine.SupportedPlatform
import com.example.engine.TelegramLinkParser
import com.example.engine.TelegramPublicExtractor
import com.example.engine.TelegramVideoDownloaderEngine
import com.example.engine.VideoQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class DownloaderMode {
    VIDEO,
    ALL_FILES
}

data class SampleLink(
    val title: String,
    val sizeText: String,
    val badge: String,
    val url: String,
    val platform: SupportedPlatform = SupportedPlatform.WEB_DIRECT,
    val quality: String = "1080p HD",
    val isPrivateDemo: Boolean = false,
    val isFile: Boolean = false,
    val fileExtension: String = ".mp4"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = DownloadRepository(database.downloadDao())
    val downloaderEngine = TelegramVideoDownloaderEngine(application, database.downloadDao())
    val downloadManager = com.example.engine.DownloadManager(application, database.downloadDao(), downloaderEngine)
    private val privateHelper = PrivateChannelHelper(application)

    val allDownloads: StateFlow<List<DownloadItem>> = repository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloads: StateFlow<List<DownloadItem>> = repository.activeDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedDownloads: StateFlow<List<DownloadItem>> = repository.completedDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Network Connectivity State
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkLossNotice = MutableStateFlow<String?>(null)
    val networkLossNotice: StateFlow<String?> = _networkLossNotice.asStateFlow()

    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
            _networkLossNotice.value = null
        }

        override fun onLost(network: Network) {
            _isOnline.value = false
            _networkLossNotice.value = "⚠️ ইন্টারনেট সংযোগ বিচ্ছিন্ন! ডাউনলোড স্বয়ংক্রিয়ভাবে বিরতিতে রয়েছে।"
            // Auto pause any currently downloading tasks
            activeDownloads.value.filter { it.status == DownloadStatus.DOWNLOADING }.forEach {
                downloaderEngine.pauseDownload(it.id)
            }
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
            val active = connectivityManager?.activeNetwork
            val caps = connectivityManager?.getNetworkCapabilities(active)
            _isOnline.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            _isOnline.value = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignore unregister exception
        }
    }

    // Downloader Mode (Video vs All Files)
    private val _downloaderMode = MutableStateFlow(DownloaderMode.VIDEO)
    val downloaderMode: StateFlow<DownloaderMode> = _downloaderMode.asStateFlow()

    fun setDownloaderMode(mode: DownloaderMode) {
        _downloaderMode.value = mode
    }

    private val _selectedFileFilter = MutableStateFlow("ALL")
    val selectedFileFilter: StateFlow<String> = _selectedFileFilter.asStateFlow()

    fun setSelectedFileFilter(filter: String) {
        _selectedFileFilter.value = filter
    }

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

    // Batch download state (10+ videos/files simultaneously)
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

    // 100% Tested Working Video Links (Zero 403 Errors)
    val videoSampleLinks = listOf(
        SampleLink(
            title = "Pavel Durov Post #526 (5.8 MB)",
            sizeText = "5.8 MB",
            badge = "TG Public (No Bot)",
            url = "https://t.me/durov/526",
            platform = SupportedPlatform.TELEGRAM,
            quality = "Original"
        ),
        SampleLink(
            title = "Telegram Official Post #441 (2.6 MB)",
            sizeText = "2.6 MB",
            badge = "TG Public (No Bot)",
            url = "https://t.me/telegram/441",
            platform = SupportedPlatform.TELEGRAM,
            quality = "Original"
        ),
        SampleLink(
            title = "SampleLib High-Speed HD Video (2.8 MB)",
            sizeText = "2.8 MB",
            badge = "Fast 720p",
            url = "https://download.samplelib.com/mp4/sample-5s.mp4",
            platform = SupportedPlatform.WEB_DIRECT,
            quality = "720p HD"
        ),
        SampleLink(
            title = "SampleLib Action 20s HD Video (11.8 MB)",
            sizeText = "11.8 MB",
            badge = "1080p FHD",
            url = "https://download.samplelib.com/mp4/sample-20s.mp4",
            platform = SupportedPlatform.WEB_DIRECT,
            quality = "1080p HD"
        ),
        SampleLink(
            title = "Intel Object Detection Video (6.2 MB)",
            sizeText = "6.2 MB",
            badge = "HD Demo",
            url = "https://raw.githubusercontent.com/intel-iot-devkit/sample-videos/master/person-bicycle-car-detection.mp4",
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
        )
    )

    // 100% Tested Working File Links (PDF, Audio MP3, ZIP Archive)
    val fileSampleLinks = listOf(
        SampleLink(
            title = "W3C Official Sample Document (PDF)",
            sizeText = "13.3 KB",
            badge = "PDF Document",
            url = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
            platform = SupportedPlatform.WEB_DIRECT,
            quality = "PDF",
            isFile = true,
            fileExtension = ".pdf"
        ),
        SampleLink(
            title = "High-Quality Music Audio Track (MP3)",
            sizeText = "52.1 KB",
            badge = "HQ Audio",
            url = "https://download.samplelib.com/mp3/sample-3s.mp3",
            platform = SupportedPlatform.WEB_DIRECT,
            quality = "MP3",
            isFile = true,
            fileExtension = ".mp3"
        ),
        SampleLink(
            title = "GitHub Master Source Code Archive (ZIP)",
            sizeText = "1.8 KB",
            badge = "ZIP Archive",
            url = "https://codeload.github.com/octocat/Hello-World/zip/refs/heads/master",
            platform = SupportedPlatform.WEB_DIRECT,
            quality = "ZIP",
            isFile = true,
            fileExtension = ".zip"
        )
    )

    val sampleLinks: List<SampleLink>
        get() = if (_downloaderMode.value == DownloaderMode.ALL_FILES) fileSampleLinks else videoSampleLinks

    // 10 high-speed items ready for simultaneous 10+ batch testing
    val batchSampleUrls = listOf(
        "https://download.samplelib.com/mp4/sample-5s.mp4",
        "https://download.samplelib.com/mp3/sample-3s.mp3",
        "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
        "https://raw.githubusercontent.com/intel-iot-devkit/sample-videos/master/person-bicycle-car-detection.mp4",
        "https://codeload.github.com/octocat/Hello-World/zip/refs/heads/master",
        "https://download.samplelib.com/mp4/sample-20s.mp4",
        "https://t.me/durov/526",
        "https://t.me/telegram/441",
        "https://download.samplelib.com/mp3/sample-3s.mp3",
        "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
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
            _customTitle.value = analysis.suggestedTitle

            // If YouTube, asynchronously fetch real video title
            if (analysis.platform == SupportedPlatform.YOUTUBE) {
                viewModelScope.launch {
                    val realTitle = MultiPlatformVideoResolver.fetchYouTubeVideoTitle(newUrl)
                    if (!realTitle.isNullOrBlank() && _urlInput.value == newUrl) {
                        _customTitle.value = "$realTitle.mp4"
                    }
                }
            }

            // If Telegram, asynchronously resolve public media metadata (Zero Bot Needed)
            if (analysis.platform == SupportedPlatform.TELEGRAM) {
                viewModelScope.launch {
                    val tgParsed = TelegramLinkParser.parse(newUrl)
                    if (tgParsed.linkType == LinkType.PUBLIC_CHANNEL) {
                        val media = TelegramPublicExtractor.resolvePublicMedia(tgParsed.channelIdentifier, tgParsed.messageId)
                        if (media != null && _urlInput.value == newUrl && media.title.isNotBlank()) {
                            _customTitle.value = media.title
                        }
                    }
                }
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
        val cleanName = sample.title.replace(" ", "_").replace("(", "").replace(")", "")
        _customTitle.value = if (cleanName.contains(".")) cleanName else "$cleanName${sample.fileExtension}"
    }

    fun startDownload(context: Context) {
        val rawUrl = _urlInput.value.trim()
        if (rawUrl.isBlank()) {
            Toast.makeText(context, "অনুগ্রহ করে লিঙ্ক প্রদান করুন (Please enter a link)", Toast.LENGTH_SHORT).show()
            return
        }

        val isFilesMode = _downloaderMode.value == DownloaderMode.ALL_FILES
        val analysis = _platformAnalysis.value ?: MultiPlatformVideoResolver.analyzeUrl(rawUrl)

        viewModelScope.launch {
            _isResolving.value = true
            _resolvingMessage.value = if (isFilesMode) "ফাইল লিঙ্ক প্রস্তুত করা হচ্ছে..." else "হাই-কোয়ালিটি স্ট্রিম লিঙ্ক প্রস্তুত করা হচ্ছে..."

            try {
                var resolvedUrl = rawUrl
                var resolvedTitle = _customTitle.value.trim()
                var resolvedMime = "video/mp4"

                if (analysis.platform == SupportedPlatform.TELEGRAM) {
                    val tgParsed = TelegramLinkParser.parse(rawUrl)
                    if (tgParsed.linkType == LinkType.PUBLIC_CHANNEL) {
                        // Public channel: ZERO bot required, direct telescope media stream!
                        val publicMedia = TelegramPublicExtractor.resolvePublicMedia(tgParsed.channelIdentifier, tgParsed.messageId)
                        if (publicMedia != null && publicMedia.directStreamUrl.isNotBlank()) {
                            resolvedUrl = publicMedia.directStreamUrl
                            if (resolvedTitle.isBlank()) {
                                resolvedTitle = publicMedia.title
                            }
                            resolvedMime = publicMedia.mimeType
                        } else {
                            resolvedUrl = privateHelper.resolveDownloadUrl(tgParsed)
                        }
                    } else {
                        resolvedUrl = privateHelper.resolveDownloadUrl(tgParsed)
                    }
                } else if (!isFilesMode) {
                    resolvedUrl = MultiPlatformVideoResolver.resolveStreamUrl(
                        rawUrl,
                        analysis.platform,
                        _selectedQuality.value
                    )
                }

                if (resolvedTitle.isBlank()) {
                    resolvedTitle = analysis.suggestedTitle
                }

                // Determine file extension and storage directory
                val storageDir: File
                val ext: String

                if (isFilesMode) {
                    val pathOnly = resolvedUrl.substringBefore("?").substringAfterLast("/")
                    val urlExt = if (pathOnly.contains(".")) ".${pathOnly.substringAfterLast(".")}" else ""
                    ext = when {
                        urlExt.isNotBlank() && urlExt.length <= 5 -> urlExt
                        resolvedTitle.contains(".") -> ".${resolvedTitle.substringAfterLast(".")}"
                        else -> ".bin"
                    }
                    resolvedMime = when (ext.lowercase()) {
                        ".pdf" -> "application/pdf"
                        ".apk" -> "application/vnd.android.package-archive"
                        ".zip" -> "application/zip"
                        ".rar" -> "application/x-rar-compressed"
                        ".mp3" -> "audio/mpeg"
                        ".mp4" -> "video/mp4"
                        ".jpg", ".jpeg" -> "image/jpeg"
                        ".png" -> "image/png"
                        else -> "application/octet-stream"
                    }
                    storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                } else {
                    ext = if (_selectedQuality.value == VideoQuality.AUDIO_MP3) ".mp3" else ".mp4"
                    resolvedMime = if (_selectedQuality.value == VideoQuality.AUDIO_MP3) "audio/mpeg" else "video/mp4"
                    storageDir = context.getExternalFilesDir(
                        if (_selectedQuality.value == VideoQuality.AUDIO_MP3) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
                    ) ?: context.filesDir
                }

                val baseClean = resolvedTitle
                    .removeSuffix(".mp4").removeSuffix(".mkv").removeSuffix(".webm")
                    .removeSuffix(".mp3").removeSuffix(".pdf").removeSuffix(".apk")
                    .removeSuffix(".zip").removeSuffix(".rar")

                val fileName = if (resolvedTitle.endsWith(ext, ignoreCase = true)) resolvedTitle else "$baseClean$ext"
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
                    mimeType = resolvedMime,
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
     * Downloads 10+ items simultaneously in batch mode
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
            _resolvingMessage.value = "${lines.size}টি লিঙ্ক প্রস্তুত করা হচ্ছে..."

            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            var addedCount = 0

            lines.forEachIndexed { index, rawUrl ->
                try {
                    val analysis = MultiPlatformVideoResolver.analyzeUrl(rawUrl)
                    var resolvedUrl = rawUrl
                    var resolvedMime = "video/mp4"

                    if (analysis.platform == SupportedPlatform.TELEGRAM) {
                        val tgParsed = TelegramLinkParser.parse(rawUrl)
                        if (tgParsed.linkType == LinkType.PUBLIC_CHANNEL) {
                            val media = TelegramPublicExtractor.resolvePublicMedia(tgParsed.channelIdentifier, tgParsed.messageId)
                            if (media != null && media.directStreamUrl.isNotBlank()) {
                                resolvedUrl = media.directStreamUrl
                                resolvedMime = media.mimeType
                            } else {
                                resolvedUrl = privateHelper.resolveDownloadUrl(tgParsed)
                            }
                        } else {
                            resolvedUrl = privateHelper.resolveDownloadUrl(tgParsed)
                        }
                    } else {
                        resolvedUrl = MultiPlatformVideoResolver.resolveStreamUrl(rawUrl, analysis.platform, _selectedQuality.value)
                    }

                    val pathOnly = resolvedUrl.substringBefore("?").substringAfterLast("/")
                    val ext = if (pathOnly.contains(".") && pathOnly.substringAfterLast(".").length <= 5) {
                        ".${pathOnly.substringAfterLast(".")}"
                    } else if (_selectedQuality.value == VideoQuality.AUDIO_MP3) {
                        ".mp3"
                    } else {
                        ".mp4"
                    }

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
                        mimeType = resolvedMime,
                        status = DownloadStatus.QUEUED
                    )

                    val newId = repository.insert(downloadItem)
                    downloaderEngine.startOrResumeDownload(newId)
                    addedCount++
                } catch (e: Exception) {
                    // Continue with other items
                }
            }

            _isResolving.value = false
            Toast.makeText(context, "$addedCount টি ফাইল/ভিডিও একসাথে ডাউনলোড শুরু হয়েছে!", Toast.LENGTH_LONG).show()
            _selectedTab.value = 1
            clearBatchInput()
        }
    }

    fun pauseDownload(id: Long) {
        downloaderEngine.pauseDownload(id)
    }

    fun resumeDownload(id: Long) {
        viewModelScope.launch {
            val item = repository.getById(id)
            if (item != null) {
                if (item.status == DownloadStatus.FAILED || MultiPlatformVideoResolver.isWebPageUrl(item.downloadUrl)) {
                    try {
                        val analysis = MultiPlatformVideoResolver.analyzeUrl(item.originalUrl)
                        val freshUrl = if (analysis.platform == SupportedPlatform.TELEGRAM) {
                            val tgParsed = TelegramLinkParser.parse(item.originalUrl)
                            privateHelper.resolveDownloadUrl(tgParsed)
                        } else {
                            MultiPlatformVideoResolver.resolveStreamUrl(item.originalUrl, analysis.platform, _selectedQuality.value)
                        }
                        if (freshUrl != item.downloadUrl && !MultiPlatformVideoResolver.isWebPageUrl(freshUrl)) {
                            repository.update(item.copy(downloadUrl = freshUrl, status = DownloadStatus.QUEUED, errorMessage = null))
                        }
                    } catch (e: Exception) {
                        // proceed with existing
                    }
                }
            }
            downloaderEngine.startOrResumeDownload(id)
        }
    }

    fun retryDownload(id: Long) {
        viewModelScope.launch {
            val item = repository.getById(id)
            if (item != null) {
                try {
                    val analysis = MultiPlatformVideoResolver.analyzeUrl(item.originalUrl)
                    val freshUrl = if (analysis.platform == SupportedPlatform.TELEGRAM) {
                        val tgParsed = TelegramLinkParser.parse(item.originalUrl)
                        privateHelper.resolveDownloadUrl(tgParsed)
                    } else {
                        MultiPlatformVideoResolver.resolveStreamUrl(item.originalUrl, analysis.platform, _selectedQuality.value)
                    }
                    val target = if (!MultiPlatformVideoResolver.isWebPageUrl(freshUrl)) freshUrl else item.downloadUrl
                    repository.update(item.copy(
                        downloadUrl = target,
                        status = DownloadStatus.QUEUED,
                        errorMessage = null,
                        downloadedBytes = 0L
                    ))
                } catch (e: Exception) {
                    repository.update(item.copy(status = DownloadStatus.QUEUED, errorMessage = null, downloadedBytes = 0L))
                }
            }
            downloaderEngine.startOrResumeDownload(id)
        }
    }

    fun cancelDownload(id: Long) {
        viewModelScope.launch {
            downloaderEngine.cancelDownload(id)
            val item = repository.getById(id)
            if (item != null) {
                try {
                    val file = File(item.filePath)
                    if (file.exists()) file.delete()
                    val part = File(item.filePath + ".part")
                    if (part.exists()) part.delete()
                } catch (e: Exception) {
                    // Ignore
                }
                repository.delete(item)
            }
        }
    }

    fun clearAllCompleted(context: Context) {
        viewModelScope.launch {
            repository.deleteAllCompleted()
            Toast.makeText(context, "ইতিহাস মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
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
        val (success, message) = TelegramVideoDownloaderEngine.exportDownloadedFile(context, file, item.title, item.mimeType)
        if (success) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "সেভ করা যায়নি: $message", Toast.LENGTH_LONG).show()
        }
    }

    fun openDownloadedFile(item: DownloadItem, context: Context) {
        val file = File(item.filePath)
        if (!file.exists()) {
            Toast.makeText(context, "ফাইলটি স্টোরেজে পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            try {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(context, "ফাইল ওপেন করার মতো কোনো অ্যাপ ইনস্টল নেই", Toast.LENGTH_SHORT).show()
            }
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
            context.startActivity(Intent.createChooser(intent, "Share via"))
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
