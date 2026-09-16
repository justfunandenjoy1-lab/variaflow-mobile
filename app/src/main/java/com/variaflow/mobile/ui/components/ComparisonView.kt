package com.variaflow.mobile.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.variaflow.mobile.model.VideoMetadata
import java.io.File

@Composable
fun ComparisonView(original: VideoMetadata, processed: VideoMetadata) {
    var showSideBySideVideo by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Optimization & Quality Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ORIGINAL", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Text("Res: ${original.resolutionFormatted}", style = MaterialTheme.typography.bodySmall)
                    Text("FPS: ${String.format("%.1f", original.fps)}", style = MaterialTheme.typography.bodySmall)
                    Text("Bitrate: ${original.bitrateKbps}k", style = MaterialTheme.typography.bodySmall)
                    Text("Size: ${original.fileSizeFormatted}", style = MaterialTheme.typography.bodySmall)
                }
                Divider(modifier = Modifier.height(90.dp).width(1.dp).padding(horizontal = 8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("OUTPUT", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Text("Res: ${processed.resolutionFormatted}", style = MaterialTheme.typography.bodySmall)
                    Text("FPS: ${String.format("%.1f", processed.fps)}", style = MaterialTheme.typography.bodySmall)
                    Text("Bitrate: ${processed.bitrateKbps}k", style = MaterialTheme.typography.bodySmall)
                    Text("Size: ${processed.fileSizeFormatted}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = { showSideBySideVideo = !showSideBySideVideo }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showSideBySideVideo) "Hide Video Preview" else "Side-by-Side Video Preview")
            }
            if (showSideBySideVideo) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth().height(180.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) { ExoPlayerPreview(filePath = original.localFilePath) }
                    Box(modifier = Modifier.weight(1f)) { ExoPlayerPreview(filePath = processed.localFilePath) }
                }
            }
        }
    }
}

@Composable
fun ExoPlayerPreview(filePath: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            if (File(filePath).exists()) {
                setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(File(filePath))))
                prepare()
            }
        }
    }
    DisposableEffect(filePath) { onDispose { exoPlayer.release() } }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
        }
    )
}
