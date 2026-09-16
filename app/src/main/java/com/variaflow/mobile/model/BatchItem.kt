package com.variaflow.mobile.model

import android.net.Uri

enum class BatchStatus { PENDING, PROCESSING, COMPLETED, FAILED }

data class BatchItem(
    val uri: Uri,
    val displayName: String,
    val status: BatchStatus = BatchStatus.PENDING,
    val progress: Int = 0,
    val outputPath: String? = null,
    val error: String? = null
)
