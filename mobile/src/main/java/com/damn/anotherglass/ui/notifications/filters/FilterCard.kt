package com.damn.anotherglass.ui.notifications.filters

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.damn.anotherglass.R
import com.damn.anotherglass.extensions.notifications.filter.FilterAction
import com.damn.anotherglass.ui.theme.AnotherGlassTheme
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun FilterCard(
    filterItem: FilterListItemUI,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: () -> Unit
) {
    // todo: move to parent non composable controller
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                filterItem.appDetails?.icon?.let {
                    Image(
                        painter = rememberDrawablePainter(drawable = it),
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(40.dp)
                            .padding(end = 16.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = filterItem.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if(filterItem.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = filterItem.description,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp)) // Added spacer
                    Text( // Display the action
                        text = filterItem.action.toDisplayStringList(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant // Slightly different color
                    )
                }
                Switch(
                    checked = filterItem.isEnabled,
                    onCheckedChange = { onToggleEnabled() },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onEdit) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Filter") },
            text = { Text("Are you sure you want to delete the filter \"${filterItem.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FilterAction?.toDisplayStringList(): String = when (this) {
    null -> "Not Set"
    FilterAction.BLOCK -> stringResource(R.string.filter_action_block)
    FilterAction.ALLOW_WITH_NOTIFICATION -> stringResource(R.string.filter_action_allow_with_notification)
    FilterAction.ALLOW_SILENTLY -> stringResource(R.string.filter_action_allow_silently)
}

@Preview
@Composable
fun FilterCardPreview() {
    val sampleItem =
        FilterListItemUI(
            "prev1",
            "Block Gmail",
            true,
            "App: Gmail",
            action = FilterAction.ALLOW_WITH_NOTIFICATION,
            appDetails = null
        )
    AnotherGlassTheme {
        FilterCard(filterItem = sampleItem, onEdit = {}, onDelete = {}, onToggleEnabled = {})
    }
}
