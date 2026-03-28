package com.dhiraj.expensetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhiraj.expensetracker.data.AppDatabase
import com.dhiraj.expensetracker.data.CategoryEntity
import com.dhiraj.expensetracker.ui.theme.AppPreferences
import com.dhiraj.expensetracker.ui.utils.InputFilters
import com.dhiraj.expensetracker.ui.utils.SmartCategoryRecommender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryOverlaySheet(
    merchantHint: String?,
    amountHint: Double?,
    initialSelection: String? = null,
    onDismiss: () -> Unit,
    onCategorySelected: (String) -> Unit
) {
    val context = LocalContext.current
    val db = remember(context) { AppDatabase.getDatabase(context) }
    val categoryDao = remember(db) { db.categoryDao() }
    val transactionDao = remember(db) { db.transactionDao() }
    val scope = rememberCoroutineScope()

    val categories by categoryDao.getAllCategories().collectAsState(initial = emptyList())

    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryEmoji by remember { mutableStateOf("\uD83D\uDCE6") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var recommendations by remember { mutableStateOf<List<String>>(emptyList()) }

    val smartEnabled = remember(context) { AppPreferences.isSmartCategoryEnabled(context) }

    LaunchedEffect(merchantHint, smartEnabled) {
        if (!smartEnabled) {
            recommendations = emptyList()
            return@LaunchedEffect
        }

        recommendations = withContext(Dispatchers.IO) {
            SmartCategoryRecommender.recommend(transactionDao, merchantHint, limit = 4)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Choose Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (!merchantHint.isNullOrBlank()) {
                Text(
                    text = "Merchant: $merchantHint",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (amountHint != null) {
                Text(
                    text = "Amount: Rs ${String.format("%.2f", kotlin.math.abs(amountHint))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (smartEnabled && recommendations.isNotEmpty()) {
                Text(
                    text = "Recommended",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recommendations.forEach { categoryName ->
                        FilterChip(
                            selected = initialSelection.equals(categoryName, ignoreCase = true),
                            onClick = {
                                onCategorySelected(categoryName)
                                onDismiss()
                            },
                            label = { Text(categoryName) }
                        )
                    }
                }
            }

            Text(
                text = "All Categories",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            if (categories.isEmpty()) {
                Text(
                    text = "No categories found",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories, key = { it.id }) { category ->
                        val selected = initialSelection.equals(category.name, ignoreCase = true)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onCategorySelected(category.name)
                                    onDismiss()
                                }
                            ) {
                                Text(
                                    text = "${category.emoji} ${category.name}",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "Add Category",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { showEmojiPicker = true }) {
                    Text(newCategoryEmoji)
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = {
                        newCategoryName = InputFilters.category(it, 24)
                        errorMessage = null
                    },
                    label = { Text("Category name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            TextButton(
                onClick = {
                    scope.launch {
                        val normalizedName = InputFilters.category(newCategoryName).trim()
                        val normalizedEmoji = newCategoryEmoji.trim().ifEmpty { "\uD83D\uDCE6" }

                        if (normalizedName.isBlank()) {
                            errorMessage = "Category name cannot be empty"
                            return@launch
                        }

                        val exists = withContext(Dispatchers.IO) {
                            categoryDao.exists(normalizedName)
                        }

                        if (exists > 0) {
                            errorMessage = "Category already exists"
                            return@launch
                        }

                        withContext(Dispatchers.IO) {
                            categoryDao.insert(
                                CategoryEntity(
                                    name = normalizedName,
                                    emoji = normalizedEmoji,
                                    isCustom = true
                                )
                            )
                        }

                        onCategorySelected(normalizedName)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save and Select")
            }
        }
    }

    if (showEmojiPicker) {
        EmojiPickerSheet(
            selectedEmoji = newCategoryEmoji,
            onDismiss = { showEmojiPicker = false },
            onSelect = {
                newCategoryEmoji = it
                showEmojiPicker = false
            }
        )
    }
}


