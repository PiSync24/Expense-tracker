package com.dhiraj.expensetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dhiraj.expensetracker.ui.utils.InputFilters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionPickerSheet(
    title: String,
    options: List<String>,
    selected: String?,
    allowAdd: Boolean = false,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var newValue by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title)

            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(options.distinct(), key = { it }) { option ->
                    TextButton(
                        onClick = {
                            onSelect(option)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(option)
                            if (selected.equals(option, true)) {
                                Text("Selected")
                            }
                        }
                    }
                }
            }

            if (allowAdd) {
                OutlinedTextField(
                    value = newValue,
                    onValueChange = { newValue = InputFilters.category(it) },
                    label = { Text("Add new") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                TextButton(
                    onClick = {
                        val normalized = newValue.trim()
                        if (normalized.isBlank()) return@TextButton
                        onSelect(normalized)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use this")
                }
            }
        }
    }
}
