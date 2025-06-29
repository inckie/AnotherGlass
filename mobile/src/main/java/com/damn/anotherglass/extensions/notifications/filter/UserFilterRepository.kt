package com.damn.anotherglass.extensions.notifications.filter

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.applicaster.xray.core.Logger
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.filterDataStore by preferencesDataStore(name = "user_filters")
private val FILTERS_KEY = stringPreferencesKey("notification_filters_list")
private val gson = Gson()

interface IFilterRepository {
    fun getFiltersFlow(): Flow<List<NotificationFilter>>
    suspend fun saveFilters(filters: List<NotificationFilter>)
    suspend fun addFilter(newFilter: NotificationFilter)
    suspend fun updateFilter(updatedFilter: NotificationFilter)
    suspend fun deleteFilter(filterId: String)

    companion object
}

// some temporary glue
fun IFilterRepository.Companion.from(context: Context): IFilterRepository = object : IFilterRepository {
    override fun getFiltersFlow() =
        UserFilterRepository.getFiltersFlow(context)

    override suspend fun saveFilters(filters: List<NotificationFilter>) =
        UserFilterRepository.saveFilters(context, filters)

    override suspend fun addFilter(newFilter: NotificationFilter) =
        UserFilterRepository.addFilter(context, newFilter)

    override suspend fun updateFilter(updatedFilter: NotificationFilter) =
        UserFilterRepository.updateFilter(context, updatedFilter)

    override suspend fun deleteFilter(filterId: String) =
        UserFilterRepository.deleteFilter(context, filterId)
}

object UserFilterRepository {

    private val logger = Logger.get("UserFilterRepository")

    suspend fun saveFilters(context: Context, filters: List<NotificationFilter>) {
        val jsonString = gson.toJson(Filters(filters))
        context.filterDataStore.edit { preferences ->
            preferences[FILTERS_KEY] = jsonString
        }
    }

    fun getFiltersFlow(context: Context): Flow<List<NotificationFilter>> {
        return context.filterDataStore.data.map { preferences ->
            preferences[FILTERS_KEY]
                ?.let {
                    try {
                        gson.fromJson(it, Filters::class.java)
                    } catch (e: Exception) {
                        context.filterDataStore.edit { it.clear() }
                        logger.e("UserFilterRepository").exception(e).message("Error parsing filters, data will be dropped")
                        return@let null
                    }
                }
                ?.filters
                ?: emptyList()
        }
    }

    // --- Helper functions to add, update, delete individual filters ---
    suspend fun addFilter(context: Context, newFilter: NotificationFilter) {
        val currentFilters = getFiltersFlow(context).firstOrNull()?.toMutableList() ?: mutableListOf()
        currentFilters.add(newFilter)
        saveFilters(context, currentFilters)
    }

    suspend fun updateFilter(context: Context, updatedFilter: NotificationFilter) {
        val currentFilters = getFiltersFlow(context).firstOrNull()?.toMutableList() ?: mutableListOf()
        val index = currentFilters.indexOfFirst { it.id == updatedFilter.id }
        if (index != -1) {
            currentFilters[index] = updatedFilter
            saveFilters(context, currentFilters)
        }
    }

    suspend fun deleteFilter(context: Context, filterId: String) {
        val currentFilters = getFiltersFlow(context).firstOrNull()?.toMutableList() ?: mutableListOf()
        currentFilters.removeAll { it.id == filterId }
        saveFilters(context, currentFilters)
    }

    suspend fun exportFilters(context: Context): String =
        getFiltersFlow(context).firstOrNull()?.let { filters ->
            gson.toJson(Filters(filters))
        } ?: ""
}