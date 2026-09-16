package com.variaflow.mobile.model

import android.net.Uri

data class VideoMetadata(
    val uri: Uri,
    val fileName: String = "Unknown",
    val fileSizeBytes: Long = 0L,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val fps: Double = 0.0,
    val videoCodec: String = "Unknown",
    val audioCodec: String = "None",
    val bitrateKbps: Long = 0L,
    val containerFormat: String = "Unknown",
    val audioSampleRateHz: Int = 0,
    val audioChannels: Int = 0,
    val localFilePath: String = ""
) {
    val resolutionFormatted: String
        get() = if (width > 0 && height > 0) "${width}x${height}" else "Unknown"

    val durationFormatted: String
        get() {
            val totalSec = durationMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return String.format("%02d:%02d", min, sec)
        }

    val fileSizeFormatted: String
        get() {
            val mb = fileSizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1024.0) {
                String.format("%.2f GB", mb / 1024.0)
            } else {
                String.format("%.2f MB", mb)
            }
        }
}
