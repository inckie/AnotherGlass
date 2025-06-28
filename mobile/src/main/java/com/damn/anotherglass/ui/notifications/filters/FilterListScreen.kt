package com.damn.anotherglass.ui.notifications.filters

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.damn.anotherglass.ui.notifications.AppRoute
import com.damn.anotherglass.ui.theme.AnotherGlassTheme

// Assuming AppDestinations and FilterListViewModel are accessible

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterListScreen(
    navController: NavController?,
    viewModel: FilterListViewModel = viewModel(factory = FilterListViewModel.Companion.Factory(LocalContext.current.applicationContext as Application))
) {
    val filters by viewModel.filters.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Filters") },
                // You could add navigation back if this screen isn't the root
                actions = {
                    IconButton(onClick = {
                        navController?.navigate(AppRoute.NotificationHistory.route)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "Notification History"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // Navigate to create a new filter (no filterId passed)
                navController?.navigate(
                    AppRoute.FilterEditScreen.buildFilterEditRoute(
                    )
                )
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add New Filter")
            }
        }
    ) { paddingValues ->
        if (filters.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No filters defined yet.\nClick the '+' button to add one.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filters, key = { it.id }) { filterItem ->
                    FilterCard(
                        filterItem = filterItem,
                        onEdit = {
                            navController?.navigate(
                                AppRoute.FilterEditScreen.buildFilterEditRoute(
                                    filterId = filterItem.id
                                )
                            )
                        },
                        onDelete = { viewModel.deleteFilter(filterItem.id) },
                        onToggleEnabled = {
                            viewModel.toggleFilterEnabled(
                                filterItem.id,
                                filterItem.isEnabled
                            )
                        }
                    )
                }
            }
        }
    }
}

// --- Preview ---
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun FilterListScreenPreview_WithItems() {
    //val previewViewModel = FilterListViewModel(Application()) // Or use a mock Application/Repository
    // Manually inject preview data into the ViewModel's StateFlow if possible,
    // or mock the repository it uses. For simplicity here, we assume it might fetch some
    // default/empty state or you'd mock UserFilterRepository.getFiltersFlow.
    // For a more robust preview, you'd directly provide state to the Composable.
    val sampleFilters = listOf(
        FilterListItemUI(
            "1",
            "Block Social Media",
            true,
            "3 conditions, Matches All (AND)",
            actionDisplay = "Allow",
            appDetails = null
        ),
        FilterListItemUI(
            "2",
            "Allow Work Emails",
            true,
            "2 conditions, Matches Any (OR)",
            actionDisplay = "Allow",
            appDetails = null
        ),
        FilterListItemUI(
            "3",
            "Silent Gaming Notifications",
            false,
            "1 condition, Matches All (AND)",
            actionDisplay = "Block",
            appDetails = null
        )
    )

    AnotherGlassTheme {
        //FilterListScreen(navController = null, viewModel = previewViewModel) // Using ViewModel
        // OR: More direct preview by passing state:
        Scaffold(
            topBar = { TopAppBar(title = { Text("Notification Filters (Preview)") }) },
            floatingActionButton = {
                FloatingActionButton(onClick = { /* Nav */ }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add New Filter")
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sampleFilters, key = { it.id }) { filterItem ->
                    FilterCard(
                        filterItem = filterItem,
                        onEdit = {},
                        onDelete = {},
                        onToggleEnabled = {})
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun FilterListScreenPreview_Empty() {
    AnotherGlassTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Notification Filters (Preview)") }) },
            floatingActionButton = {
                FloatingActionButton(onClick = { /* Nav */ }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add New Filter")
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No filters defined yet.\nClick the '+' button to add one.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
