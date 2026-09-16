package com.variaflow.mobile.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtil {
    fun copyUriToLocalCache(context: Context, uri: Uri): String {
        var displayName = "cached_input_" + System.currentTimeMillis()
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) displayName = name
                }
            }
        }
        val cacheDir = File(context.cacheDir, "variaflow_inputs")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val destinationFile = File(cacheDir, displayName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        }
        return destinationFile.absolutePath
    }

    fun createUniqueOutputFile(context: Context, originalName: String, targetExtension: String): File {
        val baseDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir,
            "VariaFlow/Output"
        )
        if (!baseDir.exists()) baseDir.mkdirs()
        val baseName = originalName.substringBeforeLast('.', "video").replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        var counter = 1
        var candidate = File(baseDir, "${baseName}_${timestamp}_processed.$targetExtension")
        while (candidate.exists()) {
            candidate = File(baseDir, "${baseName}_${timestamp}_(${counter}).$targetExtension")
            counter++
        }
        return candidate
    }

    fun clearCache(context: Context) {
        try {
            val cacheDir = File(context.cacheDir, "variaflow_inputs")
            if (cacheDir.exists()) cacheDir.deleteRecursively()
        } catch (_: Exception) {}
    }
}
