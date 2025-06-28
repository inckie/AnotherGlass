package com.damn.anotherglass.ui.notifications.editfilter

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.damn.anotherglass.extensions.notifications.filter.ConditionType
import com.damn.anotherglass.extensions.notifications.filter.FilterAction
import com.damn.anotherglass.extensions.notifications.filter.FilterConditionItem
import com.damn.anotherglass.ui.notifications.AppRoute
import com.damn.anotherglass.utility.AndroidAppDetailsProvider

@OptIn(ExperimentalMaterial3Api::class) // For TopAppBar, etc.
@Composable
fun FilterEditScreen(
    navController: NavController?,
    viewModel: FilterEditViewModel = viewModel(factory = FilterEditViewModel.Companion.Factory(
        application = LocalContext.current.applicationContext as Application,
        savedStateHandle = SavedStateHandle()
    ))
) {
    LocalContext.current
    val filterName by viewModel.filterName
    val isFilterEnabled by viewModel.isFilterEnabled
    val matchAllConditions by viewModel.matchAllConditions
    // conditions is a SnapshotStateList, observed directly
    val currentAction by viewModel.filterAction // Get current action
    var actionMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.filterId.value == null) "Create Filter" else "Edit Filter") },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.saveFilter {
                            navController?.popBackStack() // Navigate back after saving
                        }
                    }) {
                        Icon(Icons.Filled.Done, contentDescription = "Save Filter")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filter Name
            item {
                OutlinedTextField(
                    value = filterName,
                    onValueChange = { viewModel.filterName.value = it },
                    label = { Text("Filter Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = viewModel.packageName.value,
                    onValueChange = { viewModel.packageName.value = it },
                    label = { Text("Package Name (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g., com.google.android.gm") }
                )
            }

            // Is Enabled Switch
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enabled", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = isFilterEnabled,
                        onCheckedChange = { viewModel.isFilterEnabled.value = it }
                    )
                }
            }

            // Filter Action Dropdown
            item {
                Text("Action to perform:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = actionMenuExpanded,
                    onExpandedChange = { actionMenuExpanded = !actionMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentAction.toDisplayString(), // Use a helper for pretty display
                        onValueChange = {}, // Not directly editable
                        readOnly = true,
                        label = { Text("Action") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = actionMenuExpanded,
                        onDismissRequest = { actionMenuExpanded = false }
                    ) {
                        viewModel.availableFilterActions.forEach { action ->
                            DropdownMenuItem(
                                text = { Text(action.toDisplayString()) },
                                onClick = {
                                    viewModel.filterAction.value = action
                                    actionMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Match Type (AND/OR)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Condition Matching:", style = MaterialTheme.typography.bodyLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = matchAllConditions,
                            onClick = { viewModel.matchAllConditions.value = true }
                        )
                        Text(
                            "All (AND)",
                            Modifier
                                .clickable { viewModel.matchAllConditions.value = true }
                                .padding(start = 4.dp, end = 8.dp)
                        )
                        RadioButton(
                            selected = !matchAllConditions,
                            onClick = { viewModel.matchAllConditions.value = false }
                        )
                        Text(
                            "Any (OR)",
                            Modifier
                                .clickable { viewModel.matchAllConditions.value = false }
                                .padding(start = 4.dp)
                        )
                    }
                }
            }

            // Conditions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Conditions", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { viewModel.addCondition() }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Condition")
                    }
                }
            }

            // List of Conditions
            if (viewModel.conditions.isEmpty()) {
                item {
                    Text(
                        "No conditions added. This filter will match all notifications if enabled (or none, depending on default interpretation).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                itemsIndexed(
                    viewModel.conditions,
                    key = { _, item -> item.hashCode() /* Assuming FilterConditionItem has a unique ID */ }) { index, conditionItem ->
                    ConditionEditItemView(
                        conditionItem = conditionItem,
                        availableTypes = viewModel.availableConditionTypes,
                        onTypeChange = { newType -> viewModel.updateConditionType(index, newType) },
                        onValueChange = { newValue ->
                            viewModel.updateConditionValue(
                                index,
                                newValue
                            )
                        },
                        onRemove = { viewModel.removeCondition(conditionItem) }
                    )
                }
            }
        }
    }
}

// Preview
@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, device = "id:pixel_4")
@Composable
fun FilterEditScreenPreview_NewFilter() {
    // In a real preview with ViewModel, you'd use a Hilt preview ViewModel or mock SavedStateHandle
    val app = Application()//LocalContext.current.applicationContext as Application
    val previewViewModel = FilterEditViewModel(
        application = app,
        savedStateHandle = SavedStateHandle(),
        appDetailsProvider = AndroidAppDetailsProvider(app)
    )
    // Simulate pre-fill from notification
    previewViewModel.conditions.add(
        FilterConditionItem(
            type = ConditionType.TITLE_CONTAINS,
            value = "Important News"
        )
    )
    previewViewModel.conditions.add(
        FilterConditionItem(
            type = ConditionType.TEXT_CONTAINS,
            value = "Order Confirmation"
        )
    )
    previewViewModel.filterName.value = "Filter from News"

    MaterialTheme { // Replace with your actual theme
        FilterEditScreen(navController = null, viewModel = previewViewModel)
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun FilterEditScreenPreview_ExistingFilter() {
    val app = Application()
    val previewViewModel =
        FilterEditViewModel(
            application = app,
            savedStateHandle = SavedStateHandle().apply {
                set(
                    AppRoute.FilterEditScreen.FILTER_EDIT_ARG_FILTER_ID,
                    "some-existing-id"
                ) // Simulate loading existing
            },
            appDetailsProvider = AndroidAppDetailsProvider(app)
        )
    // Manually set some state as if loaded
    previewViewModel.filterId.value = "some-existing-id"
    previewViewModel.filterName.value = "My Old Important Filter"
    previewViewModel.isFilterEnabled.value = false
    previewViewModel.matchAllConditions.value = false // OR
    previewViewModel.conditions.add(
        FilterConditionItem(
            type = ConditionType.TEXT_CONTAINS,
            value = "urgent"
        )
    )
    previewViewModel.conditions.add(
        FilterConditionItem(
            type = ConditionType.IS_ONGOING_EQUALS,
            value = "true"
        )
    )

    MaterialTheme { // Replace with your actual theme
        FilterEditScreen(navController = null, viewModel = previewViewModel)
    }
}

// Helper extension function for display names (optional, but good for UI)
fun FilterAction.toDisplayString(): String = when (this) {
    FilterAction.BLOCK -> "Block Notification"
    FilterAction.ALLOW_WITH_NOTIFICATION -> "Allow & Notify Me"
    FilterAction.ALLOW_SILENTLY -> "Allow Silently"
}