package com.dhiraj.expensetracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val commonEmojis = listOf(
    "\uD83C\uDF54", "\uD83C\uDF55", "\uD83C\uDF71", "\u2615", "\uD83D\uDE95", "\uD83D\uDE8C",
    "\u26FD", "\uD83D\uDED2", "\uD83D\uDCA1", "\uD83D\uDCF1", "\uD83C\uDFE0", "\uD83C\uDFAC",
    "\uD83D\uDC8A", "\uD83E\uDDFE", "\uD83D\uDCB8", "\uD83D\uDCB0", "\u2708\uFE0F", "\uD83C\uDF81",
    "\uD83C\uDFBF", "\uD83C\uDF89", "\uD83D\uDCDA", "\uD83C\uDFE6", "\uD83D\uDC5A", "\uD83D\uDC36"
)

@Composable
fun EmojiPickerSheet(
    selectedEmoji: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var showCustomPicker by rememberSaveable { mutableStateOf(false) }
    var customEmojiInput by rememberSaveable { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pick Emoji", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showCustomPicker = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Use keyboard emoji")
                    }
                }

                Text(
                    "Tap + to enter any emoji from your keyboard pack",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    items(commonEmojis) { emoji ->
                        Card(
                            modifier = Modifier
                                .padding(6.dp)
                                .clickable {
                                    onSelect(emoji)
                                    onDismiss()
                                }
                        ) {
                            Text(
                                text = if (emoji == selectedEmoji) "$emoji *" else emoji,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }

    if (showCustomPicker) {
        AlertDialog(
            onDismissRequest = { showCustomPicker = false },
            title = { Text("Use Any Emoji") },
            text = {
                OutlinedTextField(
                    value = customEmojiInput,
                    onValueChange = { customEmojiInput = it.take(8) },
                    label = { Text("Emoji") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = customEmojiInput.trim()
                    if (value.isNotEmpty()) {
                        onSelect(value)
                        showCustomPicker = false
                        onDismiss()
                    }
                }) {
                    Text("Use")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
