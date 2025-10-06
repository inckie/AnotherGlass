package com.damn.anotherglass.extensions.notifications.filter

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.applicaster.xray.ui.utility.SharedFileHelper
import kotlinx.coroutines.launch

class IImportExportController(
    private val onImport: () -> Unit,
    private val onExport: () -> Unit
) {
    fun import() = onImport()
    fun export() = onExport()

    companion object
}

fun IImportExportController.Companion.fromActivity(activity: ComponentActivity): IImportExportController {
    val importLauncher = activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) {
            return@registerForActivityResult
        }
        activity.lifecycleScope.launch {
            importFiltersFromFile(activity, uri)
        }
    }

    return IImportExportController(
        onImport = { importLauncher.launch("application/json") },
        onExport = {
            activity.lifecycleScope.launch {
                exportFiltersToFile(activity)
            }
        }
    )
}


suspend fun exportFiltersToFile(context: Activity) {
    try {
        val filters = UserFilterRepository.exportFilters(context)
        val file = SharedFileHelper.saveToDownloads(
            context,
            filters,
            "filters.json",
            "application/json",
        )
        Toast.makeText(context, "Saved to $file", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Failed to export filters: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}

suspend fun importFiltersFromFile(context: Context, uri: Uri) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val json = inputStream.bufferedReader().use { it.readText() }
            UserFilterRepository.importFilters(context, json)
            Toast.makeText(context, "Filters imported successfully", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Failed to import filters: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}
