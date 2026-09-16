package com.variaflow.mobile.model

data class ProcessingProgress(
    val isRunning: Boolean = false,
    val percentage: Int = 0,
    val stage: String = "Idle",
    val outputFilePath: String? = null,
    val errorMessage: String? = null,
    val isCompleted: Boolean = false,
    val isCancelled: Boolean = false
)
