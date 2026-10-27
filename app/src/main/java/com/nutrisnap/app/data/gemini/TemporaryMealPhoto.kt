package com.nutrisnap.app.data.gemini

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object TemporaryMealPhoto {
    fun createCaptureUri(context: Context): Uri {
        val directory = File(context.cacheDir, "meal-images").apply { mkdirs() }
        val file = File(directory, "meal-${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun deleteIfTemporary(context: Context, uri: Uri) {
        val directory = File(context.cacheDir, "meal-images").canonicalFile
        val fileName = uri.lastPathSegment ?: return
        val file = File(directory, fileName).canonicalFile
        if (file.toPath().startsWith(directory.toPath())) file.delete()
    }
}
