package com.damn.anotherglass.ui.mainscreen.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier


data class DropDownMenuItem(val text: String, val onClick: () -> Unit)

@Composable
fun TopAppBarDropdownMenu(
    items: List<DropDownMenuItem>
) {
    val expanded = remember { mutableStateOf(false) }

    Box(
        Modifier.wrapContentSize(Alignment.TopEnd)
    ) {
        IconButton(onClick = {
            expanded.value = true
        }) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "More Menu"
            )
        }
    }

    DropdownMenu(
        expanded = expanded.value,
        onDismissRequest = { expanded.value = false },
    ) {
        items.forEach { item ->
            DropdownMenuItem(
                onClick = {
                    expanded.value = false
                    item.onClick()
                },
                text = { Text(item.text) }
            )
        }
    }
}
