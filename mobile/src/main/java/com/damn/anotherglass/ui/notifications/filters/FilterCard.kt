package com.damn.anotherglass.ui.notifications.filters

import androidx.compose.foundation.Image
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                filterItem.appDetails?.icon?.let {
                    Image(
                        painter = rememberDrawablePainter(drawable = it),
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(32.dp)
                            .padding(end = 10.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = filterItem.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (filterItem.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = filterItem.description,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = filterItem.action.toDisplayStringList(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = filterItem.isEnabled,
                        onCheckedChange = { onToggleEnabled() }
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.btn_edit),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.btn_delete),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
