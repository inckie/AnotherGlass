package com.damn.xray.ui.utility

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

object SharedFileHelper {

    fun copyToDownloads(
        context: Context,
        file: File,
        target: String,
        mime: String
    ): String? {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> saveAndroidQ(
                context,
                file.readText(),
                target,
                mime
            )
            else -> file.copyTo(File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), target), overwrite = true).toString()
        }
    }

    /**
     * Try to save given string as a file to user Downloads folder.
     * Does not guarantee to return real path for Android 10+
     */
    fun saveToDownloads(
        context: Context,
        text: String,
        name: String,
        mime: String
    ): String? {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> saveAndroidQ(
                context,
                text,
                name,
                mime
            )
            else -> saveLegacy(context, name, text)
        }
    }

    fun shareText(context: Context, text: String, subject: String = "X-Ray Log Report") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, subject).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun saveLegacy(
        context: Context,
        name: String,
        text: String
    ): String {
        return File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), name
        ).apply {
            writeText(text)
        }.toString()
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private fun saveAndroidQ(
        context: Context,
        text: String,
        name: String,
        mime: String
    ): String? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val contentResolver = context.contentResolver
        val uri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val itemUri = contentResolver.insert(uri, contentValues)
            ?: throw IOException("Failed to access storage")
        contentResolver.openOutputStream(itemUri).use { outputStream ->
            outputStream!!.write(text.toByteArray(StandardCharsets.UTF_8))
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            contentResolver.update(itemUri, contentValues, null, null)
        }
        // return some fake name to give user some idea where file is located
        return itemUri.toString().replaceAfterLast("/", name)
    }
}
