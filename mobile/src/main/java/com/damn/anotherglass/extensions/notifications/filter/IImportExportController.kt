package com.damn.anotherglass.extensions.notifications.filter

import android.app.Activity
import android.widget.Toast
import androidx.activity.ComponentActivity
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

fun IImportExportController.Companion.fromActivity(activity: ComponentActivity) =
    IImportExportController(
        onImport = {
            activity.lifecycleScope.launch {
                importFiltersFromFile(activity)
            }
        },
        onExport = {
            activity.lifecycleScope.launch {
                exportFiltersToFile(activity)
            }
        }
    )

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

// this one will require file picker intent, so will be part of the Activity in fact
suspend fun importFiltersFromFile(context: Activity) {
    // todo
}