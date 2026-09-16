package com.variaflow.mobile.util

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.variaflow.mobile.model.VideoMetadata
import java.io.File

object MediaInspector {
    private val SUPPORTED_EXTENSIONS = setOf("mp4", "mov", "mkv", "avi", "webm")

    fun isFormatSupported(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return SUPPORTED_EXTENSIONS.contains(ext)
    }

    fun inspectUri(context: Context, uri: Uri, localPath: String): VideoMetadata {
        var fileName = "video_" + System.currentTimeMillis()
        var fileSize = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx != -1) fileName = cursor.getString(nameIdx) ?: fileName
                if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
            }
        }

        if (fileSize <= 0L) {
            val file = File(localPath)
            if (file.exists()) fileSize = file.length()
        }

        val retriever = MediaMetadataRetriever()
        var width = 0
        var height = 0
        var durationMs = 0L
        var bitrate = 0L
        var audioSampleRate = 0
        var audioChannels = 0
        var videoCodec = "Unknown"
        var audioCodec = "None"
        var fps = 0.0

        try {
            retriever.setDataSource(localPath)
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            bitrate = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L) / 1000L

            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) {
                val temp = width
                width = height
                height = temp
            }

            val extractor = MediaExtractor()
            extractor.setDataSource(localPath)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoCodec = mime.removePrefix("video/").uppercase()
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        fps = format.getInteger(MediaFormat.KEY_FRAME_RATE).toDouble()
                    }
                } else if (mime.startsWith("audio/")) {
                    audioCodec = mime.removePrefix("audio/").uppercase()
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        audioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        audioChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                }
            }
            extractor.release()

            if (fps <= 0.0) {
                val captureRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toDoubleOrNull()
                fps = captureRate ?: 30.0
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        return VideoMetadata(
            uri = uri,
            fileName = fileName,
            fileSizeBytes = fileSize,
            durationMs = durationMs,
            width = width,
            height = height,
            fps = fps,
            videoCodec = videoCodec,
            audioCodec = audioCodec,
            bitrateKbps = bitrate,
            containerFormat = fileName.substringAfterLast('.', "mp4").uppercase(),
            audioSampleRateHz = audioSampleRate,
            audioChannels = audioChannels,
            localFilePath = localPath
        )
    }
}
