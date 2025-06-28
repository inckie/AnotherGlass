package com.damn.anotherglass.extensions.notifications.filter

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.filterDataStore by preferencesDataStore(name = "user_filters")
private val FILTERS_KEY = stringPreferencesKey("notification_filters_list")
private val gson = Gson()

object UserFilterRepository {

    suspend fun saveFilters(context: Context, filters: List<UserFilter>) {
        val jsonString = gson.toJson(filters)
        context.filterDataStore.edit { preferences ->
            preferences[FILTERS_KEY] = jsonString
        }
    }

    fun getFiltersFlow(context: Context): Flow<List<UserFilter>> {
        return context.filterDataStore.data.map { preferences ->
            val jsonString = preferences[FILTERS_KEY]
            if (jsonString != null) {
                val type = object : TypeToken<List<UserFilter>>() {}.type
                gson.fromJson(jsonString, type) ?: emptyList()
            } else {
                emptyList()
            }
        }
    }

    // --- Helper functions to add, update, delete individual filters ---
    suspend fun addFilter(context: Context, newFilter: UserFilter) {
        val currentFilters = getFiltersFlow(context).firstOrNull()?.toMutableList() ?: mutableListOf()
        currentFilters.add(newFilter)
        saveFilters(context, currentFilters)
    }

    suspend fun updateFilter(context: Context, updatedFilter: UserFilter) {
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
}