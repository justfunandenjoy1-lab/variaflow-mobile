package com.variaflow.mobile.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.variaflow.mobile.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSelector(
    settings: ProcessingSettings,
    onSettingsChanged: (ProcessingSettings) -> Unit,
    sourceMetadata: VideoMetadata?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Processing Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(10.dp))

            Text("Presets", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PresetType.values().forEach { preset ->
                    FilterChip(
                        selected = settings.preset == preset,
                        onClick = { onSettingsChanged(ProcessingSettings.fromPreset(preset, sourceMetadata)) },
                        label = { Text(preset.name.replace("_", " ")) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text("Container Format", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContainerFormat.values().forEach { container ->
                    FilterChip(
                        selected = settings.containerFormat == container,
                        onClick = { onSettingsChanged(settings.copy(containerFormat = container, preset = PresetType.CUSTOM)) },
                        label = { Text(container.name) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text("Video Codec", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VideoCodecOption.values().forEach { codec ->
                    FilterChip(
                        selected = settings.videoCodec == codec,
                        onClick = { onSettingsChanged(settings.copy(videoCodec = codec, preset = PresetType.CUSTOM)) },
                        label = { Text(codec.displayName) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text("Target Video Bitrate: ${settings.targetVideoBitrateKbps} kbps", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = settings.targetVideoBitrateKbps.toFloat(),
                onValueChange = { onSettingsChanged(settings.copy(targetVideoBitrateKbps = it.toInt(), preset = PresetType.CUSTOM)) },
                valueRange = 500f..15000f
            )

            Text("Target FPS: ${settings.targetFps}", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(24, 30, 60).forEach { fps ->
                    FilterChip(
                        selected = settings.targetFps == fps,
                        onClick = { onSettingsChanged(settings.copy(targetFps = fps, preset = PresetType.CUSTOM)) },
                        label = { Text("$fps FPS") }
                    )
                }
            }
        }
    }
}
