package com.variaflow.mobile.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.variaflow.mobile.model.BatchStatus
import com.variaflow.mobile.ui.components.*
import com.variaflow.mobile.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val sourceMeta by viewModel.sourceMetadata.collectAsState()
    val processedMeta by viewModel.processedMetadata.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val batchItems by viewModel.batchItems.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val qualityWarning by viewModel.qualityWarning.collectAsState()

    val singleVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.onSingleVideoSelected(it) }
    }
    val multipleVideoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) viewModel.onMultipleVideosSelected(uris)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VariaFlow Mobile", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            userMessage?.let { msg ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = msg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearUserMessage() }) { Text("Dismiss") }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { singleVideoPicker.launch("video/*") }, modifier = Modifier.weight(1f), enabled = !progress.isRunning) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Video")
                }
                OutlinedButton(onClick = { multipleVideoPicker.launch("video/*") }, modifier = Modifier.weight(1f), enabled = !progress.isRunning) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Batch Select")
                }
            }

            if (batchItems.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Batch Queue: ${batchItems.size} items", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        val remaining = batchItems.count { it.status == BatchStatus.PENDING }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.startBatchProcessing() }, enabled = !progress.isRunning && remaining > 0, modifier = Modifier.fillMaxWidth()) {
                            Text("Process Batch ($remaining left)")
                        }
                    }
                }
            }

            sourceMeta?.let { meta -> VideoInfoCard(metadata = meta, title = "Original Video Information") }

            qualityWarning?.let { warning ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }

            SettingsSelector(settings = settings, onSettingsChanged = { viewModel.updateSettings(it) }, sourceMetadata = sourceMeta)

            Button(
                onClick = { viewModel.startProcessingSingleVideo() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = sourceMeta != null && !progress.isRunning
            ) {
                Text("Process Video", fontWeight = FontWeight.Bold)
            }

            if (progress.isRunning) {
                ProgressSection(progress = progress, onCancel = { viewModel.cancelProcessing() })
            }

            if (progress.isCompleted && processedMeta != null && sourceMeta != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing Completed", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val file = File(progress.outputFilePath ?: "")
                                if (file.exists()) {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "video/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(viewIntent, "Open Video"))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Output Video")
                        }
                    }
                }
                ComparisonView(original = sourceMeta!!, processed = processedMeta!!)
            }
        }
    }
}
