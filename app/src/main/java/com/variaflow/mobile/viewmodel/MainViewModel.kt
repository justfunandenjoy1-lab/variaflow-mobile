package com.variaflow.mobile.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.variaflow.mobile.model.*
import com.variaflow.mobile.processor.VideoProcessorEngine
import com.variaflow.mobile.util.FileUtil
import com.variaflow.mobile.util.MediaInspector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    private val _sourceMetadata = MutableStateFlow<VideoMetadata?>(null)
    val sourceMetadata: StateFlow<VideoMetadata?> = _sourceMetadata.asStateFlow()

    private val _processedMetadata = MutableStateFlow<VideoMetadata?>(null)
    val processedMetadata: StateFlow<VideoMetadata?> = _processedMetadata.asStateFlow()

    private val _settings = MutableStateFlow(ProcessingSettings())
    val settings: StateFlow<ProcessingSettings> = _settings.asStateFlow()

    private val _progress = MutableStateFlow(ProcessingProgress())
    val progress: StateFlow<ProcessingProgress> = _progress.asStateFlow()

    private val _batchItems = MutableStateFlow<List<BatchItem>>(emptyList())
    val batchItems: StateFlow<List<BatchItem>> = _batchItems.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _qualityWarning = MutableStateFlow<String?>(null)
    val qualityWarning: StateFlow<String?> = _qualityWarning.asStateFlow()

    private var processingJob: Job? = null

    fun clearUserMessage() { _userMessage.value = null }

    fun updateSettings(newSettings: ProcessingSettings) {
        _settings.value = newSettings
        evaluateQualityWarning(newSettings, _sourceMetadata.value)
    }

    private fun evaluateQualityWarning(settings: ProcessingSettings, source: VideoMetadata?) {
        if (source == null) {
            _qualityWarning.value = null
            return
        }
        val warnings = mutableListOf<String>()
        if (source.bitrateKbps > 0 && settings.targetVideoBitrateKbps < (source.bitrateKbps * 0.35)) {
            warnings.add("Bitrate is much lower than original. Compression artifacts may appear.")
        }
        if (source.width > 0 && settings.targetWidth < (source.width / 2)) {
            warnings.add("Resolution is downscaled over 50%. Visual clarity will be reduced.")
        }
        _qualityWarning.value = if (warnings.isNotEmpty()) warnings.joinToString(" ") else null
    }

    fun onSingleVideoSelected(uri: Uri) {
        viewModelScope.launch {
            _progress.value = ProcessingProgress(isRunning = true, stage = "Reading video streams...")
            val localPath = withContext(Dispatchers.IO) { FileUtil.copyUriToLocalCache(context, uri) }
            val fileName = File(localPath).name
            if (!MediaInspector.isFormatSupported(fileName)) {
                _progress.value = ProcessingProgress(isRunning = false)
                _userMessage.value = "Unsupported format: $fileName. Supported: MP4, MOV, MKV, AVI, WEBM"
                return@launch
            }
            val metadata = withContext(Dispatchers.IO) { MediaInspector.inspectUri(context, uri, localPath) }
            _sourceMetadata.value = metadata
            _processedMetadata.value = null
            val defaultSettings = ProcessingSettings.fromPreset(PresetType.BALANCED, metadata)
            _settings.value = defaultSettings
            evaluateQualityWarning(defaultSettings, metadata)
            _progress.value = ProcessingProgress(isRunning = false)
        }
    }

    fun onMultipleVideosSelected(uris: List<Uri>) {
        _batchItems.value = uris.map { uri ->
            BatchItem(uri = uri, displayName = uri.lastPathSegment ?: "Video_${System.currentTimeMillis()}")
        }
    }

    fun startProcessingSingleVideo() {
        val metadata = _sourceMetadata.value ?: return
        val currentSettings = _settings.value
        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            val outputFile = withContext(Dispatchers.IO) {
                FileUtil.createUniqueOutputFile(context, metadata.fileName, currentSettings.containerFormat.extension)
            }
            VideoProcessorEngine.processVideo(metadata, currentSettings, outputFile).collect { currentProgress ->
                _progress.value = currentProgress
                if (currentProgress.isCompleted && currentProgress.outputFilePath != null) {
                    val outMetadata = withContext(Dispatchers.IO) {
                        MediaInspector.inspectUri(context, Uri.fromFile(File(currentProgress.outputFilePath)), currentProgress.outputFilePath)
                    }
                    _processedMetadata.value = outMetadata
                }
            }
        }
    }

    fun startBatchProcessing() {
        val items = _batchItems.value
        if (items.isEmpty()) return
        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            val updatedList = items.toMutableList()
            for (index in updatedList.indices) {
                val currentItem = updatedList[index]
                updatedList[index] = currentItem.copy(status = BatchStatus.PROCESSING, progress = 0)
                _batchItems.value = updatedList.toList()
                val localPath = withContext(Dispatchers.IO) { FileUtil.copyUriToLocalCache(context, currentItem.uri) }
                val meta = withContext(Dispatchers.IO) { MediaInspector.inspectUri(context, currentItem.uri, localPath) }
                val outputFile = withContext(Dispatchers.IO) {
                    FileUtil.createUniqueOutputFile(context, meta.fileName, _settings.value.containerFormat.extension)
                }
                var success = false
                var errorReason: String? = null
                VideoProcessorEngine.processVideo(meta, _settings.value, outputFile).collect { prog ->
                    updatedList[index] = currentItem.copy(status = BatchStatus.PROCESSING, progress = prog.percentage)
                    _batchItems.value = updatedList.toList()
                    if (prog.isCompleted) success = true
                    if (prog.errorMessage != null) errorReason = prog.errorMessage
                }
                updatedList[index] = if (success) {
                    currentItem.copy(status = BatchStatus.COMPLETED, progress = 100, outputPath = outputFile.absolutePath)
                } else {
                    currentItem.copy(status = BatchStatus.FAILED, error = errorReason ?: "Halted")
                }
                _batchItems.value = updatedList.toList()
            }
        }
    }

    fun cancelProcessing() {
        VideoProcessorEngine.cancelActiveJob()
        processingJob?.cancel()
        _progress.value = ProcessingProgress(isRunning = false, stage = "Cancelled", isCancelled = true)
    }

    override fun onCleared() {
        super.onCleared()
        FileUtil.clearCache(context)
    }
}
