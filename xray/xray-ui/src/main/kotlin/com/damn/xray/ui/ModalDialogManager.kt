package com.damn.xray.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class ModalDialogManager {

    class DialogButton(
        val text: String,
        val onClick: (() -> Unit)? = null,
    )

    class SingleChoiceConfig(
        val items: List<String>,
        initialSelected: Int,
        private val onItemSelected: ((Int) -> Unit)?,
    ) {
        var selectedIndex by mutableIntStateOf(initialSelected)
            private set

        fun select(index: Int) {
            selectedIndex = index
            onItemSelected?.invoke(index)
        }
    }

    class MultiChoiceConfig(
        val items: List<String>,
        initialChecked: BooleanArray,
        private val onItemCheckedChange: ((Int, Boolean) -> Unit)?,
    ) {
        val checkedStates = mutableStateListOf<Boolean>().apply {
            addAll(initialChecked.toList())
        }

        fun toggle(index: Int) {
            val newValue = !checkedStates[index]
            checkedStates[index] = newValue
            onItemCheckedChange?.invoke(index, newValue)
        }

        fun setChecked(index: Int, checked: Boolean) {
            if (checkedStates[index] != checked) {
                checkedStates[index] = checked
                onItemCheckedChange?.invoke(index, checked)
            }
        }
    }

    class DialogSpec internal constructor(
        val title: String?,
        val message: String?,
        val singleChoice: SingleChoiceConfig?,
        val multiChoice: MultiChoiceConfig?,
        val positiveButton: DialogButton?,
        val negativeButton: DialogButton?,
        val neutralButton: DialogButton?,
    )

    class Builder {
        private var title: String? = null
        private var message: String? = null
        private var singleChoice: SingleChoiceConfig? = null
        private var multiChoice: MultiChoiceConfig? = null
        private var positiveButton: DialogButton? = null
        private var negativeButton: DialogButton? = null
        private var neutralButton: DialogButton? = null

        fun setTitle(title: String) = apply { this.title = title }

        fun setMessage(message: String) = apply { this.message = message }

        fun setSingleChoiceItems(
            items: List<String>,
            checkedItem: Int = 0,
            onItemSelected: ((Int) -> Unit)? = null,
        ) = apply {
            this.singleChoice = SingleChoiceConfig(items, checkedItem, onItemSelected)
        }

        fun setMultiChoiceItems(
            items: List<String>,
            checkedItems: BooleanArray,
            onItemCheckedChange: ((Int, Boolean) -> Unit)? = null,
        ) = apply {
            this.multiChoice = MultiChoiceConfig(items, checkedItems, onItemCheckedChange)
        }

        fun setPositiveButton(text: String, onClick: (() -> Unit)? = null) = apply {
            this.positiveButton = DialogButton(text, onClick)
        }

        fun setNegativeButton(text: String, onClick: (() -> Unit)? = null) = apply {
            this.negativeButton = DialogButton(text, onClick)
        }

        fun setNeutralButton(text: String, onClick: (() -> Unit)? = null) = apply {
            this.neutralButton = DialogButton(text, onClick)
        }

        internal fun build() = DialogSpec(
            title = title,
            message = message,
            singleChoice = singleChoice,
            multiChoice = multiChoice,
            positiveButton = positiveButton,
            negativeButton = negativeButton,
            neutralButton = neutralButton,
        )
    }

    var currentDialog by mutableStateOf<DialogSpec?>(null)
        private set

    fun show(block: Builder.() -> Unit) {
        currentDialog = Builder().apply(block).build()
    }

    fun dismiss() {
        currentDialog = null
    }

    fun onButtonClick(button: DialogButton) {
        button.onClick?.invoke()
        dismiss()
    }
}

@Composable
fun ModalDialogHost(manager: ModalDialogManager) {
    val dialog = manager.currentDialog ?: return

    AlertDialog(
        onDismissRequest = manager::dismiss,
        title = dialog.title?.let { titleText -> { Text(titleText) } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (dialog.message != null) {
                    Text(
                        text = dialog.message,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                dialog.singleChoice?.let { single ->
                    single.items.forEachIndexed { index, label ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { single.select(index) },
                        ) {
                            RadioButton(
                                selected = single.selectedIndex == index,
                                onClick = { single.select(index) },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
                dialog.multiChoice?.let { multi ->
                    multi.items.forEachIndexed { index, label ->
                        val isChecked = multi.checkedStates[index]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { multi.toggle(index) }
                                .padding(vertical = 2.dp),
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked -> multi.setChecked(index, checked) },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            dialog.positiveButton?.let { btn ->
                TextButton(onClick = { manager.onButtonClick(btn) }) {
                    Text(btn.text)
                }
            }
        },
        dismissButton = if (dialog.negativeButton != null || dialog.neutralButton != null) {
            {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    dialog.neutralButton?.let { btn ->
                        TextButton(onClick = { manager.onButtonClick(btn) }) {
                            Text(btn.text)
                        }
                    }
                    dialog.negativeButton?.let { btn ->
                        TextButton(onClick = { manager.onButtonClick(btn) }) {
                            Text(btn.text)
                        }
                    }
                }
            }
        } else {
            null
        },
    )
}
