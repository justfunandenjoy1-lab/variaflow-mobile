package com.variaflow.mobile.processor

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.SessionState
import com.variaflow.mobile.model.AudioCodecOption
import com.variaflow.mobile.model.ProcessingProgress
import com.variaflow.mobile.model.ProcessingSettings
import com.variaflow.mobile.model.VideoMetadata
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File

object VideoProcessorEngine {
    private var activeSessionId: Long? = null

    fun cancelActiveJob() {
        activeSessionId?.let { id ->
            try {
                FFmpegKit.cancel(id)
            } catch (_: Throwable) {}
            activeSessionId = null
        }
    }

    fun processVideo(
        sourceMetadata: VideoMetadata,
        settings: ProcessingSettings,
        outputFile: File
    ): Flow<ProcessingProgress> = callbackFlow {
        val durationMs = if (sourceMetadata.durationMs > 0) sourceMetadata.durationMs else 1000L

        trySend(ProcessingProgress(isRunning = true, percentage = 0, stage = "Initializing local engine..."))

        val cmdArgs = mutableListOf<String>()
        cmdArgs.add("-y")
        cmdArgs.add("-i")
        cmdArgs.add(sourceMetadata.localFilePath)

        // Ensure resolution dimensions are divisible by 2 to prevent encoder failure
        val targetW = if (settings.targetWidth % 2 == 0) settings.targetWidth else settings.targetWidth - 1
        val targetH = if (settings.targetHeight % 2 == 0) settings.targetHeight else settings.targetHeight - 1

        val scaleFilter = "scale=$targetW:$targetH:force_original_aspect_ratio=decrease,pad=$targetW:$targetH:(ow-iw)/2:(oh-ih)/2"
        cmdArgs.add("-vf")
        cmdArgs.add(scaleFilter)
        cmdArgs.add("-r")
        cmdArgs.add(settings.targetFps.toString())
        cmdArgs.add("-c:v")
        cmdArgs.add(settings.videoCodec.ffmpegEncoder)
        cmdArgs.add("-b:v")
        cmdArgs.add("${settings.targetVideoBitrateKbps}k")
        cmdArgs.add("-preset")
        cmdArgs.add("veryfast")

        if (settings.audioCodec == AudioCodecOption.COPY) {
            cmdArgs.add("-c:a")
            cmdArgs.add("copy")
        } else {
            cmdArgs.add("-c:a")
            cmdArgs.add(settings.audioCodec.ffmpegEncoder)
            cmdArgs.add("-b:a")
            cmdArgs.add("${settings.targetAudioBitrateKbps}k")
            cmdArgs.add("-ar")
            cmdArgs.add(settings.audioSampleRateHz.toString())
            cmdArgs.add("-ac")
            cmdArgs.add(settings.audioChannels.toString())
        }

        if (settings.containerFormat.extension == "mp4" || settings.containerFormat.extension == "mov") {
            cmdArgs.add("-movflags")
            cmdArgs.add("+faststart")
        }

        cmdArgs.add(outputFile.absolutePath)

        val session = FFmpegKit.executeWithArgumentsAsync(
            cmdArgs.toTypedArray(),
            { executionSession ->
                try {
                    activeSessionId = null
                    val returnCode = executionSession.returnCode
                    val state = executionSession.state
                    val failStack = executionSession.failStackTrace

                    if (ReturnCode.isSuccess(returnCode)) {
                        trySend(
                            ProcessingProgress(
                                isRunning = false,
                                percentage = 100,
                                stage = "Completed successfully",
                                outputFilePath = outputFile.absolutePath,
                                isCompleted = true
                            )
                        )
                    } else if (ReturnCode.isCancel(returnCode) || (state == SessionState.FAILED && failStack?.contains("cancel", true) == true)) {
                        if (outputFile.exists()) outputFile.delete()
                        trySend(ProcessingProgress(isRunning = false, percentage = 0, stage = "Cancelled", isCancelled = true))
                    } else {
                        if (outputFile.exists()) outputFile.delete()
                        val errorLog = executionSession.allLogsAsString ?: "Encoding failure."
                        val humanMsg = when {
                            errorLog.contains("No space left", true) -> "Storage full."
                            errorLog.contains("Invalid data", true) -> "Corrupt video data."
                            else -> "Encoding failed: " + errorLog.takeLast(150).trim()
                        }
                        trySend(ProcessingProgress(isRunning = false, percentage = 0, stage = "Failed", errorMessage = humanMsg))
                    }
                } catch (t: Throwable) {
                    trySend(ProcessingProgress(isRunning = false, percentage = 0, stage = "Failed", errorMessage = t.message))
                } finally {
                    channel.close()
                }
            },
            { /* log callback */ },
            { stats ->
                try {
                    val timeInMs = stats.time
                    if (timeInMs > 0 && durationMs > 0) {
                        val rawPercent = ((timeInMs.toDouble() / durationMs.toDouble()) * 100).toInt().coerceIn(2, 99)
                        trySend(ProcessingProgress(isRunning = true, percentage = rawPercent, stage = "Encoding ($rawPercent%)..."))
                    }
                } catch (_: Throwable) {}
            }
        )

        activeSessionId = session.sessionId

        awaitClose {
            try {
                if (activeSessionId == session.sessionId) {
                    FFmpegKit.cancel(session.sessionId)
                    activeSessionId = null
                    if (outputFile.exists()) outputFile.delete()
                }
            } catch (_: Throwable) {}
        }
    }
}
