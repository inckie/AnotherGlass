package com.damn.xray.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.damn.xray.core.Core
import com.damn.xray.core.Event
import com.damn.xray.core.LogLevel
import com.damn.xray.core.sinks.InMemoryLogSink
import com.damn.xray.ui.utility.SharedFileHelper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SearchField(val label: String) {
    MESSAGE("Message"),
    CATEGORY("Category"),
    SUBSYSTEM("Subsystem"),
    CONTEXT("Context"),
    DATA("Data"),
}

class XRayLogController(
    val sink: InMemoryLogSink,
    defaultLevel: LogLevel = LogLevel.VERBOSE,
    private val scope: CoroutineScope,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    val dialogManager = ModalDialogManager()
    private val prettyGson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    var minLevel by mutableStateOf(defaultLevel)
        private set
    var levelDropdownExpanded by mutableStateOf(false)
        private set

    var subsystemFilter by mutableStateOf("")
        private set
    var categoryFilter by mutableStateOf("")
        private set
    var showFilters by mutableStateOf(false)
        private set

    var showSearch by mutableStateOf(false)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var searchFields by mutableStateOf(SearchField.entries.toSet())
        private set
    var currentMatchIndex by mutableIntStateOf(0)
        private set

    var hiddenCount by mutableIntStateOf(0)
        private set

    var allEvents by mutableStateOf<List<Event>>(emptyList())
        private set
    var visibleEvents by mutableStateOf<List<Event>>(emptyList())
        private set
    var matchingIndices by mutableStateOf<List<Int>>(emptyList())
        private set

    private var filterJob: Job? = null
    private var searchJob: Job? = null
    private val expandedItems = mutableStateMapOf<String, Boolean>()

    val hasActiveFilters: Boolean
        get() = subsystemFilter.isNotEmpty() || categoryFilter.isNotEmpty()

    init {
        scope.launch {
            sink.events.collect { events ->
                allEvents = events
                triggerFilter(events)
            }
        }
    }

    private fun triggerFilter(eventsSnapshot: List<Event> = sink.events.value) {
        filterJob?.cancel()
        val countToSkip = hiddenCount.coerceAtMost(eventsSnapshot.size)
        val level = minLevel
        val subsystem = subsystemFilter
        val category = categoryFilter

        filterJob = scope.launch(workerDispatcher) {
            val total = eventsSnapshot.size
            val filtered = ArrayList<Event>(total - countToSkip)
            for (i in countToSkip until total) {
                if (i % 256 == 0 && !isActive) return@launch
                val event = eventsSnapshot[i]
                if (event.level.level >= level.level &&
                    (subsystem.isBlank() || event.subsystem.contains(subsystem, ignoreCase = true)) &&
                    (category.isBlank() || event.category.contains(category, ignoreCase = true))
                ) {
                    filtered.add(event)
                }
            }
            if (!isActive) return@launch
            visibleEvents = filtered
            triggerSearch(filtered)
        }
    }

    private fun triggerSearch(events: List<Event> = visibleEvents) {
        searchJob?.cancel()
        val query = searchQuery
        val fields = searchFields.toSet()

        if (query.isBlank() || events.isEmpty()) {
            matchingIndices = emptyList()
            return
        }

        searchJob = scope.launch(workerDispatcher) {
            val matches = ArrayList<Int>()
            for (i in events.indices) {
                if (i % 256 == 0 && !isActive) return@launch
                if (matchesSearch(events[i], query, fields)) {
                    matches.add(i)
                }
            }
            if (!isActive) return@launch
            matchingIndices = matches
        }
    }

    fun openLevelDropdown() {
        levelDropdownExpanded = true
    }

    fun dismissLevelDropdown() {
        levelDropdownExpanded = false
    }

    fun selectLevel(level: LogLevel) {
        minLevel = level
        levelDropdownExpanded = false
        triggerFilter()
    }

    fun toggleFilters() {
        showFilters = !showFilters
    }

    fun updateSubsystemFilter(value: String) {
        subsystemFilter = value
        triggerFilter()
    }

    fun clearSubsystemFilter() {
        subsystemFilter = ""
        triggerFilter()
    }

    fun updateCategoryFilter(value: String) {
        categoryFilter = value
        triggerFilter()
    }

    fun clearCategoryFilter() {
        categoryFilter = ""
        triggerFilter()
    }

    fun toggleSearch() {
        showSearch = !showSearch
    }

    fun updateSearchQuery(value: String) {
        searchQuery = value
        currentMatchIndex = 0
        triggerSearch()
    }

    fun clearSearchQuery() {
        searchQuery = ""
        currentMatchIndex = 0
        triggerSearch()
    }

    fun onPrevMatch(
        scope: CoroutineScope,
        listState: LazyListState,
    ) {
        val matches = matchingIndices
        if (matches.isEmpty()) return
        currentMatchIndex = if (currentMatchIndex > 0) currentMatchIndex - 1 else matches.size - 1
        scope.launch {
            listState.animateScrollToItem(matches[currentMatchIndex])
        }
    }

    fun onNextMatch(
        scope: CoroutineScope,
        listState: LazyListState,
    ) {
        val matches = matchingIndices
        if (matches.isEmpty()) return
        currentMatchIndex = if (currentMatchIndex < matches.size - 1) currentMatchIndex + 1 else 0
        scope.launch {
            listState.animateScrollToItem(matches[currentMatchIndex])
        }
    }

    fun onScrollToStart(
        scope: CoroutineScope,
        listState: LazyListState,
    ) {
        if (visibleEvents.isEmpty()) return
        scope.launch {
            listState.animateScrollToItem(0)
        }
    }

    fun onScrollToEnd(
        scope: CoroutineScope,
        listState: LazyListState,
    ) {
        if (visibleEvents.isEmpty()) return
        scope.launch {
            listState.animateScrollToItem(visibleEvents.size - 1)
        }
    }

    fun onHideCurrentClick() {
        hiddenCount = allEvents.size
        triggerFilter()
    }

    fun onRestoreHiddenClick() {
        hiddenCount = 0
        triggerFilter()
    }

    fun onClearDialogClick() {
        dialogManager.show {
            setTitle("Clear or Hide Logs")
            setMessage("Hide current logs from view (can be restored) or permanently clear the in-memory log buffer?")
            setPositiveButton("Hide Current") {
                hiddenCount = allEvents.size
                triggerFilter()
            }
            setNegativeButton("Clear Buffer") {
                sink.clear()
                hiddenCount = 0
                triggerFilter()
            }
            setNeutralButton("Cancel")
        }
    }

    fun onSearchFieldsClick() {
        val fields = SearchField.entries
        val checked = BooleanArray(fields.size) { i -> fields[i] in searchFields }
        dialogManager.show {
            setTitle("Search Fields")
            setMultiChoiceItems(
                items = fields.map { it.label },
                checkedItems = checked,
            ) { which, isChecked ->
                checked[which] = isChecked
            }
            setPositiveButton("OK") {
                val selected = fields.filterIndexed { index, _ -> checked[index] }.toSet()
                if (selected.isNotEmpty()) {
                    searchFields = selected
                    currentMatchIndex = 0
                    triggerSearch()
                }
            }
            setNegativeButton("Cancel")
        }
    }

    fun onShareClick(context: Context) {
        var selectedScope = 0
        val performExport = { toDownloads: Boolean ->
            val events = if (selectedScope == 0) visibleEvents else allEvents
            exportEvents(context, events, toDownloads)
        }
        dialogManager.show {
            setTitle("Export / Share Logs")
            setSingleChoiceItems(
                items = listOf(
                    "Filtered events (${visibleEvents.size})",
                    "All events (${allEvents.size})",
                ),
                checkedItem = 0,
            ) { which ->
                selectedScope = which
            }
            setPositiveButton("Save to Downloads") { performExport(true) }
            setNegativeButton("Share") { performExport(false) }
            setNeutralButton("Cancel")
        }
    }

    private fun exportEvents(
        context: Context,
        events: List<Event>,
        toDownloads: Boolean,
    ) {
        if (events.isEmpty()) {
            Toast.makeText(context, "No events to share", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch(Dispatchers.IO) {
            val json = prettyGson.toJson(events)
            if (toDownloads) {
                val filename = "events_${System.currentTimeMillis()}.json"
                val path = SharedFileHelper.saveToDownloads(
                    context,
                    json,
                    filename,
                    "application/json",
                )
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(context, "Saved to $path", Toast.LENGTH_LONG).show()
                }
            } else {
                scope.launch(Dispatchers.Main) {
                    SharedFileHelper.shareText(
                        context,
                        json,
                        "X-Ray Log Report (${events.size} events)",
                    )
                }
            }
        }
    }

    fun isEventExpanded(key: String): Boolean = expandedItems[key] == true

    fun onEventClick(key: String, hasDetails: Boolean) {
        if (hasDetails) {
            expandedItems[key] = !(expandedItems[key] ?: false)
        }
    }

    fun onEventLongClick(context: Context, event: Event) {
        val textToCopy = formatEventForCopy(event)
        context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("log event", textToCopy))
        Toast.makeText(context, "Copied log event to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun formatTimestamp(timestamp: Long): String = synchronized(dateFormat) {
        dateFormat.format(Date(timestamp))
    }

    fun formatJson(value: Any?): String = prettyGson.toJson(value)

    fun emptyStateMessage(): String {
        val total = allEvents.size
        return when {
            total == 0 -> "No logs recorded yet"
            hiddenCount >= total -> "All $hiddenCount events hidden (tap below to restore)"
            else -> "No logs match current filters"
        }
    }

    fun hiddenStatusText(): String =
        if (hiddenCount > 0) "$hiddenCount events hidden (tap to restore)" else "No events hidden"

    fun matchCountText(matchesCount: Int): String =
        if (matchesCount == 0) "0/0" else "${currentMatchIndex + 1}/$matchesCount"

    private fun formatEventForCopy(event: Event): String {
        val sb = StringBuilder()
        sb.append("${formatTimestamp(event.timestamp)} [${event.level.name}] ")
        if (event.subsystem.isNotEmpty()) {
            sb.append("[${event.subsystem}] ")
        }
        if (event.category.isNotEmpty()) {
            sb.append("${event.category}: ")
        }
        sb.append(event.message).append("\n")
        if (!event.data.isNullOrEmpty()) {
            sb.append("Data:\n").append(prettyGson.toJson(event.data)).append("\n")
        }
        if (!event.context.isNullOrEmpty()) {
            sb.append("Context:\n").append(prettyGson.toJson(event.context)).append("\n")
        }
        if (event.exception != null) {
            sb.append("Exception:\n").append(Log.getStackTraceString(event.exception)).append("\n")
        }
        return sb.toString()
    }

    private fun matchesSearch(
        event: Event,
        query: String,
        fields: Set<SearchField>,
    ): Boolean {
        if (query.isEmpty()) return false
        if (SearchField.MESSAGE in fields && event.message.contains(query, ignoreCase = true)) return true
        if (SearchField.CATEGORY in fields && event.category.contains(query, ignoreCase = true)) return true
        if (SearchField.SUBSYSTEM in fields && event.subsystem.contains(query, ignoreCase = true)) return true
        if (SearchField.CONTEXT in fields && event.context?.toString()?.contains(query, ignoreCase = true) == true) return true
        if (SearchField.DATA in fields && event.data?.toString()?.contains(query, ignoreCase = true) == true) return true
        return false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XRayLogScreen(
    sinkName: String = "memory_sink",
    sink: InMemoryLogSink? = null,
    defaultLevel: LogLevel = LogLevel.VERBOSE,
    title: String = "X-Ray Logs",
    onBack: (() -> Unit)? = null,
) {
    val resolvedSink = sink ?: (Core.get().getSink(sinkName) as? InMemoryLogSink)

    if (resolvedSink == null) {
        MissingSinkScreen(title = title, sinkName = sinkName, onBack = onBack)
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val controller = remember(resolvedSink, defaultLevel) {
        XRayLogController(resolvedSink, defaultLevel, coroutineScope)
    }
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val visibleEvents = controller.visibleEvents
    val matchingIndices = controller.matchingIndices

    Scaffold(
        topBar = {
            TopControlBar(
                controller = controller,
                onBack = onBack,
                onShareClick = { controller.onShareClick(context) },
            )
        },
        bottomBar = {
            BottomControlBar(
                controller = controller,
                onHideClick = controller::onHideCurrentClick,
                onScrollStartClick = { controller.onScrollToStart(coroutineScope, listState) },
                onScrollEndClick = { controller.onScrollToEnd(coroutineScope, listState) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AnimatedVisibility(visible = controller.showFilters) {
                FilterPanel(controller = controller)
            }

            AnimatedVisibility(visible = controller.showSearch) {
                SearchPanel(
                    controller = controller,
                    matchesCount = matchingIndices.size,
                    onPrevClick = { controller.onPrevMatch(coroutineScope, listState) },
                    onNextClick = { controller.onNextMatch(coroutineScope, listState) },
                )
            }

            HorizontalDivider()

            if (visibleEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = controller.emptyStateMessage(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    itemsIndexed(
                        items = visibleEvents,
                        key = { index, event -> "${event.timestamp}_$index" },
                    ) { index, event ->
                        val itemKey = "${event.timestamp}_$index"
                        EventItemRow(
                            event = event,
                            itemKey = itemKey,
                            isMatch = matchingIndices.contains(index),
                            isActiveMatch = matchingIndices.getOrNull(controller.currentMatchIndex) == index,
                            controller = controller,
                            onLongClick = { controller.onEventLongClick(context, event) },
                        )
                    }
                }
            }
        }
    }

    ModalDialogHost(manager = controller.dialogManager)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissingSinkScreen(
    title: String,
    sinkName: String,
    onBack: (() -> Unit)?,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Log sink '$sinkName' is not registered",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TopControlBar(
    controller: XRayLogController,
    onBack: (() -> Unit)?,
    onShareClick: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = controller::openLevelDropdown,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(10.dp)
                            .background(getLevelColor(controller.minLevel), CircleShape),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = controller.minLevel.name,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Log Level",
                    )
                }

                DropdownMenu(
                    expanded = controller.levelDropdownExpanded,
                    onDismissRequest = controller::dismissLevelDropdown,
                ) {
                    LogLevel.entries.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .width(10.dp)
                                        .height(10.dp)
                                        .background(getLevelColor(level), CircleShape),
                                )
                            },
                            onClick = { controller.selectLevel(level) },
                        )
                    }
                }
            }

            FilledIconToggleButton(
                checked = controller.showFilters || controller.hasActiveFilters,
                onCheckedChange = { controller.toggleFilters() },
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Toggle Filters",
                )
            }

            FilledIconToggleButton(
                checked = controller.showSearch || controller.searchQuery.isNotEmpty(),
                onCheckedChange = { controller.toggleSearch() },
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Toggle Search",
                )
            }

            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share logs",
                )
            }
        }
    }
}

@Composable
private fun BottomControlBar(
    controller: XRayLogController,
    onHideClick: () -> Unit,
    onScrollStartClick: () -> Unit,
    onScrollEndClick: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onHideClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hide current logs",
                    )
                }

                Text(
                    text = controller.hiddenStatusText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (controller.hiddenCount > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            enabled = controller.hiddenCount > 0,
                            onClick = controller::onRestoreHiddenClick,
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )

                IconButton(onClick = onScrollStartClick) {
                    Icon(
                        imageVector = Icons.Default.FirstPage,
                        contentDescription = "Scroll to start",
                    )
                }

                IconButton(onClick = onScrollEndClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.LastPage,
                        contentDescription = "Scroll to end",
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPanel(controller: XRayLogController) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = controller.subsystemFilter,
                onValueChange = controller::updateSubsystemFilter,
                label = { Text("Subsystem") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (controller.subsystemFilter.isNotEmpty()) {
                        IconButton(onClick = controller::clearSubsystemFilter) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
            )
            OutlinedTextField(
                value = controller.categoryFilter,
                onValueChange = controller::updateCategoryFilter,
                label = { Text("Category") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (controller.categoryFilter.isNotEmpty()) {
                        IconButton(onClick = controller::clearCategoryFilter) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun SearchPanel(
    controller: XRayLogController,
    matchesCount: Int,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = controller::onSearchFieldsClick) {
                Icon(Icons.Default.Tune, contentDescription = "Search Fields")
            }
            OutlinedTextField(
                value = controller.searchQuery,
                onValueChange = controller::updateSearchQuery,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Text") },
                singleLine = true,
                trailingIcon = {
                    if (controller.searchQuery.isNotEmpty()) {
                        IconButton(onClick = controller::clearSearchQuery) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
            )
            Text(
                text = controller.matchCountText(matchesCount),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            IconButton(
                onClick = onPrevClick,
                enabled = matchesCount > 0,
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous Match")
            }
            IconButton(
                onClick = onNextClick,
                enabled = matchesCount > 0,
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next Match")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventItemRow(
    event: Event,
    itemKey: String,
    isMatch: Boolean,
    isActiveMatch: Boolean,
    controller: XRayLogController,
    onLongClick: () -> Unit,
) {
    val hasDetails = !event.data.isNullOrEmpty() ||
        !event.context.isNullOrEmpty() ||
        event.exception != null
    val expanded = controller.isEventExpanded(itemKey)

    val backgroundColor = when {
        isActiveMatch -> MaterialTheme.colorScheme.primaryContainer
        isMatch -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }

    val dateStr = remember(event.timestamp) {
        controller.formatTimestamp(event.timestamp)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .combinedClickable(
                onClick = { controller.onEventClick(itemKey, hasDetails) },
                onLongClick = onLongClick,
            ),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(getLevelColor(event.level)),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = event.level.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = getLevelColor(event.level),
                    )
                    if (event.subsystem.isNotEmpty()) {
                        Text(
                            text = "[${event.subsystem}]",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (event.category.isNotEmpty()) {
                        Text(
                            text = event.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (hasDetails) {
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = event.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (expanded && hasDetails) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(6.dp))

                    if (!event.data.isNullOrEmpty()) {
                        Text(
                            text = "Data:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = controller.formatJson(event.data),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        )
                    }

                    if (!event.context.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Context:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = controller.formatJson(event.context),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        )
                    }

                    if (event.exception != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Exception:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = Log.getStackTraceString(event.exception),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun getLevelColor(level: LogLevel): Color = when (level) {
    LogLevel.VERBOSE -> Color(0xFF9E9E9E)
    LogLevel.DEBUG -> Color(0xFF2196F3)
    LogLevel.INFO -> Color(0xFF4CAF50)
    LogLevel.WARNING -> Color(0xFFFF9800)
    LogLevel.ERROR -> Color(0xFFF44336)
}

@Preview(showBackground = true)
@Composable
private fun XRayLogScreenPreview() {
    MaterialTheme {
        XRayLogScreen(
            sinkName = "preview_sink",
            sink = InMemoryLogSink().apply {
                log(Event(category = "Network", subsystem = "HTTP", message = "Request completed 200 OK", level = LogLevel.INFO))
                log(Event(category = "Auth", subsystem = "Token", message = "Token refreshed", level = LogLevel.DEBUG))
                log(Event(category = "Database", subsystem = "Room", message = "Failed to query table", level = LogLevel.ERROR, exception = RuntimeException("Disk full")))
            },
        )
    }
}
