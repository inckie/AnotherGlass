package com.damn.anotherglass.ui.notifications.editfilter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.damn.anotherglass.extensions.notifications.filter.ConditionType
import com.damn.anotherglass.extensions.notifications.filter.FilterConditionItem
import com.damn.anotherglass.ui.theme.AnotherGlassTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConditionEditItemView(
    conditionItem: FilterConditionItem,
    availableTypes: List<ConditionType>,
    onTypeChange: (ConditionType) -> Unit,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Condition Type Dropdown
                Box( // Wrap ExposedDropdownMenuBox in a Box if you need to apply weight correctly
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp) // Add some padding if next to icon
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = {
                            expanded = !expanded
                        } // This is the primary handler for expansion
                    ) {
                        OutlinedTextField(
                            value = conditionItem.type.name.replace("_", " ").lowercase()
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }, // Pretty print
                            onValueChange = {}, // Still no-op as value selected from menu
                            readOnly = true, // Crucial: makes the TextField not focusable for text input
                            label = { Text("Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            // IMPORTANT: Add this modifier to make the TextField act as the anchor
                            // and respond to clicks for the dropdown.
                            modifier = Modifier
                                .menuAnchor() // This is the key change for M3 ExposedDropdownMenuBox
                                .fillMaxWidth()
                            // For M2 ExposedDropdownMenuBox, direct click on TextField
                            // relies on the parent Box handling onExpandedChange.
                            // The .menuAnchor() is more specific to M3, but general principle
                            // is the TextField should be readOnly and the box controls expansion.
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            availableTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            type.name.replace("_", " ").lowercase()
                                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
                                    },
                                    onClick = {
                                        onTypeChange(type)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove Condition")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Condition Value (remains the same)
            if (conditionItem.type == ConditionType.IS_ONGOING_EQUALS) {
                // ... (special handling for boolean type)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Value is: ", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = conditionItem.value.toBooleanStrictOrNull() ?: false,
                        onCheckedChange = { onValueChange(it.toString()) },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Text(
                        if (conditionItem.value.toBooleanStrictOrNull() == true) "Ongoing" else "Not Ongoing",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            } else {
                OutlinedTextField(
                    value = conditionItem.value,
                    onValueChange = onValueChange,
                    label = { Text("Value") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConditionEditItemViewPreview() {
    val condition = remember {
        mutableStateOf(
            FilterConditionItem(
                type = ConditionType.TITLE_CONTAINS,
                value = "Hello Preview"
            )
        )
    }
    AnotherGlassTheme {
        ConditionEditItemView(
            conditionItem = condition.value,
            availableTypes = ConditionType.entries,
            onTypeChange = { condition.value = condition.value.copy(type = it) },
            onValueChange = { condition.value = condition.value.copy(value = it) },
            onRemove = {}
        )
    }
}