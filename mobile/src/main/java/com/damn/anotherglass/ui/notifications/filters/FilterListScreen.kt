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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.damn.anotherglass.extensions.notifications.filter.ConditionType
import com.damn.anotherglass.extensions.notifications.filter.FilterAction
import com.damn.anotherglass.extensions.notifications.filter.FilterConditionItem
import com.damn.anotherglass.extensions.notifications.filter.IFilterRepository
import com.damn.anotherglass.extensions.notifications.filter.NotificationFilter
import com.damn.anotherglass.ui.notifications.AppRoute
import com.damn.anotherglass.ui.theme.AnotherGlassTheme
import com.damn.anotherglass.utility.AndroidAppDetailsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterListScreen(
    navController: NavController?,
    viewModel: FilterListViewModel
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

    val sampleFilter = NotificationFilter(
        id = "1",
        name = "Sample Filter",
        isEnabled = true,
        conditions = listOf(
            FilterConditionItem(
                type = ConditionType.IS_ONGOING_EQUALS,
                value = "false"
            ),
            FilterConditionItem(
                type = ConditionType.TITLE_EQUALS,
                value = "title"
            )
        ),
        action = FilterAction.ALLOW_SILENTLY
    )

    val previewViewModel = FilterListViewModel(
        appDetailsProvider = AndroidAppDetailsProvider(Application()),
        filterRepository = object : IFilterRepository {
            override fun getFiltersFlow(): Flow<List<NotificationFilter>> =
                MutableStateFlow(listOf(sampleFilter)) // Mocking repository response
            override suspend fun saveFilters(filters: List<NotificationFilter>) = Unit
            override suspend fun addFilter(newFilter: NotificationFilter) = Unit
            override suspend fun updateFilter(updatedFilter: NotificationFilter) = Unit
            override suspend fun deleteFilter(filterId: String) = Unit
        }
    )

    AnotherGlassTheme {
        FilterListScreen(navController = null, viewModel = previewViewModel)
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun FilterListScreenPreview_Empty() {
    val previewViewModel = FilterListViewModel(
        appDetailsProvider = AndroidAppDetailsProvider(Application()),
        filterRepository = object : IFilterRepository {
            override fun getFiltersFlow(): Flow<List<NotificationFilter>> =
                MutableStateFlow(emptyList()) // Mocking repository response
            override suspend fun saveFilters(filters: List<NotificationFilter>) = Unit
            override suspend fun addFilter(newFilter: NotificationFilter) = Unit
            override suspend fun updateFilter(updatedFilter: NotificationFilter) = Unit
            override suspend fun deleteFilter(filterId: String) = Unit
        }
    )

    AnotherGlassTheme {
        FilterListScreen(navController = null, viewModel = previewViewModel)
    }
}
