package com.variaflow.mobile.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.variaflow.mobile.model.VideoMetadata

@Composable
fun VideoInfoCard(metadata: VideoMetadata, title: String = "Video Information") {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(10.dp))
            InfoRow("File Name", metadata.fileName)
            InfoRow("File Size", metadata.fileSizeFormatted)
            InfoRow("Duration", metadata.durationFormatted)
            InfoRow("Resolution", metadata.resolutionFormatted)
            InfoRow("Frame Rate", String.format("%.2f FPS", metadata.fps))
            InfoRow("Video Codec", metadata.videoCodec)
            InfoRow("Audio Codec", metadata.audioCodec)
            InfoRow("Bitrate", if (metadata.bitrateKbps > 0) "${metadata.bitrateKbps} kbps" else "Unknown")
            InfoRow("Container", metadata.containerFormat)
            InfoRow("Audio Sample Rate", if (metadata.audioSampleRateHz > 0) "${metadata.audioSampleRateHz} Hz" else "N/A")
            InfoRow("Audio Channels", if (metadata.audioChannels > 0) "${metadata.audioChannels} ch" else "N/A")
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
