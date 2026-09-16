package com.variaflow.mobile.model

enum class PresetType { HIGH_QUALITY, BALANCED, SMALL_FILE, CUSTOM }
enum class ContainerFormat(val extension: String, val mime: String) {
    MP4("mp4", "video/mp4"), MKV("mkv", "video/x-matroska"), MOV("mov", "video/quicktime")
}
enum class VideoCodecOption(val ffmpegEncoder: String, val displayName: String) {
    H264("libx264", "H.264 (AVC)"), H265("libx265", "H.265 (HEVC)"), VP9("libvpx-vp9", "VP9")
}
enum class AudioCodecOption(val ffmpegEncoder: String, val displayName: String) {
    AAC("aac", "AAC"), MP3("libmp3lame", "MP3"), OPUS("libopus", "Opus"), COPY("copy", "Direct Copy")
}

data class ProcessingSettings(
    val preset: PresetType = PresetType.BALANCED,
    val containerFormat: ContainerFormat = ContainerFormat.MP4,
    val targetWidth: Int = 1920,
    val targetHeight: Int = 1080,
    val targetFps: Int = 30,
    val videoCodec: VideoCodecOption = VideoCodecOption.H264,
    val targetVideoBitrateKbps: Int = 3500,
    val audioCodec: AudioCodecOption = AudioCodecOption.AAC,
    val targetAudioBitrateKbps: Int = 192,
    val audioSampleRateHz: Int = 44100,
    val audioChannels: Int = 2
) {
    companion object {
        fun fromPreset(preset: PresetType, source: VideoMetadata?): ProcessingSettings {
            val srcWidth = if (source != null && source.width > 0) source.width else 1920
            val srcHeight = if (source != null && source.height > 0) source.height else 1080
            val srcFps = if (source != null && source.fps > 0) source.fps.toInt().coerceIn(24, 60) else 30

            return when (preset) {
                PresetType.HIGH_QUALITY -> ProcessingSettings(
                    preset = PresetType.HIGH_QUALITY,
                    containerFormat = ContainerFormat.MP4,
                    targetWidth = srcWidth,
                    targetHeight = srcHeight,
                    targetFps = srcFps,
                    videoCodec = VideoCodecOption.H264,
                    targetVideoBitrateKbps = 8000,
                    audioCodec = AudioCodecOption.AAC,
                    targetAudioBitrateKbps = 256,
                    audioSampleRateHz = 48000,
                    audioChannels = 2
                )
                PresetType.BALANCED -> ProcessingSettings(
                    preset = PresetType.BALANCED,
                    containerFormat = ContainerFormat.MP4,
                    targetWidth = if (srcWidth >= 1920) 1920 else srcWidth,
                    targetHeight = if (srcHeight >= 1080) 1080 else srcHeight,
                    targetFps = srcFps.coerceAtMost(30),
                    videoCodec = VideoCodecOption.H264,
                    targetVideoBitrateKbps = 3200,
                    audioCodec = AudioCodecOption.AAC,
                    targetAudioBitrateKbps = 160,
                    audioSampleRateHz = 44100,
                    audioChannels = 2
                )
                PresetType.SMALL_FILE -> ProcessingSettings(
                    preset = PresetType.SMALL_FILE,
                    containerFormat = ContainerFormat.MP4,
                    targetWidth = 1280.coerceAtMost(srcWidth),
                    targetHeight = 720.coerceAtMost(srcHeight),
                    targetFps = 24.coerceAtMost(srcFps),
                    videoCodec = VideoCodecOption.H265,
                    targetVideoBitrateKbps = 1200,
                    audioCodec = AudioCodecOption.AAC,
                    targetAudioBitrateKbps = 96,
                    audioSampleRateHz = 44100,
                    audioChannels = 2
                )
                PresetType.CUSTOM -> ProcessingSettings(
                    preset = PresetType.CUSTOM,
                    containerFormat = ContainerFormat.MP4,
                    targetWidth = srcWidth,
                    targetHeight = srcHeight,
                    targetFps = srcFps,
                    videoCodec = VideoCodecOption.H264,
                    targetVideoBitrateKbps = 3500,
                    audioCodec = AudioCodecOption.AAC,
                    targetAudioBitrateKbps = 192,
                    audioSampleRateHz = 44100,
                    audioChannels = 2
                )
            }
        }
    }
}

