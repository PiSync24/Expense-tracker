package com.dhiraj.expensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhiraj.expensetracker.data.AppDatabase
import com.dhiraj.expensetracker.data.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun CategoriesScreen() {
    val context = LocalContext.current
    val db = remember(context) { AppDatabase.getDatabase(context) }
    val categoryDao = remember(db) { db.categoryDao() }
    val scope = rememberCoroutineScope()

    val categories by categoryDao.getAllCategories().collectAsState(initial = emptyList())

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var nameInput by rememberSaveable { mutableStateOf("") }
    var emojiInput by rememberSaveable { mutableStateOf("??") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Categories", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add Category")
            }
        }

        items(categories, key = { it.id }) { category ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(colorFor(category.name), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(category.emoji)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(category.name, fontWeight = FontWeight.SemiBold)
                            Text(if (category.isCustom) "Custom" else "Default", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    TextButton(onClick = {
                        scope.launch(Dispatchers.IO) { categoryDao.delete(category) }
                    }) {
                        Text("Delete")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it.take(24) },
                        label = { Text("Name") }
                    )
                    OutlinedTextField(
                        value = emojiInput,
                        onValueChange = { emojiInput = it.take(2) },
                        label = { Text("Emoji") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val safeName = nameInput.trim()
                    if (safeName.isBlank()) return@TextButton
                    scope.launch(Dispatchers.IO) {
                        categoryDao.insert(CategoryEntity(name = safeName, emoji = emojiInput.ifBlank { "??" }))
                    }
                    showAddDialog = false
                    nameInput = ""
                    emojiInput = "??"
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun colorFor(category: String): Color {
    val normalized = category.lowercase()
    return when {
        normalized.contains("food") -> Color(0xFFFFE6C7)
        normalized.contains("transport") -> Color(0xFFDDF2FF)
        normalized.contains("bill") -> Color(0xFFFFF1CC)
        normalized.contains("shop") -> Color(0xFFFFE0F1)
        normalized.contains("health") -> Color(0xFFE3F7E3)
        normalized.contains("entertain") -> Color(0xFFE8E3FF)
        else -> Color(0xFFE9EDF4)
    }
}
