package com.dhiraj.expensetracker.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dhiraj.expensetracker.data.AppDatabase
import com.dhiraj.expensetracker.data.FinancialPlanEntry
import com.dhiraj.expensetracker.data.LoanEntry
import com.dhiraj.expensetracker.data.Transaction
import com.dhiraj.expensetracker.ui.components.CategoryOverlaySheet
import com.dhiraj.expensetracker.ui.components.OptionPickerSheet
import com.dhiraj.expensetracker.ui.theme.AppPreferences
import com.dhiraj.expensetracker.ui.theme.FintechAccent
import com.dhiraj.expensetracker.ui.theme.FintechDanger
import com.dhiraj.expensetracker.ui.theme.FintechWarning
import com.dhiraj.expensetracker.ui.utils.InputFilters
import com.dhiraj.expensetracker.ui.utils.TransactionClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private data class RecurrenceOption(val code: String, val label: String, val needsWeekDay: Boolean = false)

private val recurrenceOptions = listOf(
    RecurrenceOption("ONE_TIME", "One-time"),
    RecurrenceOption("WEEKLY", "Weekly", true),
    RecurrenceOption("MONTHLY", "Monthly"),
    RecurrenceOption("YEARLY", "Yearly")
)

private val buckets = listOf("Bills", "Essential", "Food", "Transport", "Shopping", "Savings", "Fun")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    onViewAnalysis: () -> Unit = {},
    onQuickAddExpense: () -> Unit = {},
    onQuickAddIncome: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = remember(context) { AppDatabase.getDatabase(context) }
    val planDao = remember(db) { db.financialPlanDao() }
    val loanDao = remember(db) { db.loanEntryDao() }
    val transactionDao = remember(db) { db.transactionDao() }

    val selectedTemplateId by AppPreferences.selectedTemplateIdState(context)
        .collectAsState(initial = AppPreferences.getSelectedTemplateId(context))
    val monthlyIncome by AppPreferences.monthlyIncomeState(context)
        .collectAsState(initial = AppPreferences.getMonthlyIncome(context))

    val planEntries by remember(selectedTemplateId) {
        planDao.getEntriesForTemplate(selectedTemplateId)
    }.collectAsState(initial = emptyList())
    val loans by loanDao.getAllLoans().collectAsState(initial = emptyList())
    val transactions by transactionDao.getAllTransactions().collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()

    var titleInput by rememberSaveable { mutableStateOf("") }
    var merchantInput by rememberSaveable { mutableStateOf("") }
    var categoryInput by rememberSaveable { mutableStateOf("Other") }
    var bucketInput by rememberSaveable { mutableStateOf("Bills") }
    var amountInput by rememberSaveable { mutableStateOf("") }
    var recurrenceCode by rememberSaveable { mutableStateOf("MONTHLY") }
    var weeklyDay by rememberSaveable { mutableStateOf(DayOfWeek.SUNDAY.value) }
    var showCategoryPicker by rememberSaveable { mutableStateOf(false) }
    var showBucketPicker by rememberSaveable { mutableStateOf(false) }

    var friendNameInput by rememberSaveable { mutableStateOf("") }
    var friendAmountInput by rememberSaveable { mutableStateOf("") }
    var friendDirectionGive by rememberSaveable { mutableStateOf(true) }

    val today = remember { LocalDate.now() }
    val monthTx = remember(transactions, today.monthValue, today.year) {
        transactions.filter {
            val txDate = it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            txDate.monthValue == today.monthValue && txDate.year == today.year
        }
    }

    val spent = remember(monthTx) { monthTx.filterNot { TransactionClassifier.isCredit(it) }.sumOf { abs(it.amount) } }
    val creditIncome = remember(monthTx) { monthTx.filter { TransactionClassifier.isCredit(it) }.sumOf { abs(it.amount) } }
    val remaining = (monthlyIncome + creditIncome - spent).coerceAtLeast(0.0)
    val daysRemaining = (today.lengthOfMonth() - today.dayOfMonth + 1).coerceAtLeast(1)
    val safeSpend = remaining / daysRemaining

    val progress = if (monthlyIncome <= 0.0) 0f else (spent / monthlyIncome).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(progress, label = "progress")

    val upcomingBills = remember(planEntries) {
        planEntries.filter { !it.isCompleted }.sortedBy { estimateDueDays(it, today) }.take(4)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FinCard {
                Text("Safe to spend today", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Rs ${formatMoney(safeSpend)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Remaining this month  Rs ${formatMoney(remaining)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier.fillMaxWidth(),
                    color = if (progress >= 1f) FintechDanger else FintechAccent
                )
                if (spent > monthlyIncome && monthlyIncome > 0.0) {
                    Text("Overspent this month", color = FintechWarning)
                }
            }
        }

        item {
            FinCard {
                Text("Monthly Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("Income", monthlyIncome + creditIncome, FintechAccent)
                    StatItem("Spent", spent, FintechDanger)
                    StatItem("Remaining", remaining, MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAction("+ Add Expense", onQuickAddExpense, Modifier.weight(1f))
                QuickAction("+ Add Income", onQuickAddIncome, Modifier.weight(1f))
                QuickAction("View Analysis", onViewAnalysis, Modifier.weight(1f))
            }
        }

        item {
            FinCard {
                Text("Upcoming Bills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (upcomingBills.isEmpty()) {
                    Text("No upcoming bills", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    upcomingBills.forEach { entry ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Due in ${estimateDueDays(entry, today)} days", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Rs ${formatMoney(entry.amount)}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        item {
            FinCard {
                Text("Planned Expenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                planEntries.take(6).forEach { entry ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${entry.bucket} • ${recurrenceLabel(entry.recurrence)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Rs ${formatMoney(entry.amount)}")
                    }
                }
            }
        }

        item {
            FinCard {
                Text("Plan Transaction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = InputFilters.name(it, 36) },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = merchantInput,
                    onValueChange = { merchantInput = InputFilters.name(it, 36) },
                    label = { Text("Merchant name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { showCategoryPicker = true }, modifier = Modifier.weight(1f)) { Text("Category: $categoryInput") }
                    TextButton(onClick = { showBucketPicker = true }, modifier = Modifier.weight(1f)) { Text("Bucket: $bucketInput") }
                }
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = InputFilters.amount(it) },
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items = recurrenceOptions, key = { it.code }) { option ->
                        FilterChip(selected = recurrenceCode == option.code, onClick = { recurrenceCode = option.code }, label = { Text(option.label) })
                    }
                }

                if (recurrenceOptions.firstOrNull { it.code == recurrenceCode }?.needsWeekDay == true) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items = DayOfWeek.values().toList(), key = { it.value }) { day: DayOfWeek ->
                            FilterChip(selected = weeklyDay == day.value, onClick = { weeklyDay = day.value }, label = { Text(day.name.take(3)) })
                        }
                    }
                }

                TextButton(onClick = {
                    val amount = amountInput.toDoubleOrNull() ?: return@TextButton
                    if (titleInput.isBlank() || amount <= 0.0) return@TextButton

                    val merchant = merchantInput.trim().ifBlank { titleInput.trim() }
                    scope.launch(Dispatchers.IO) {
                        val entry = FinancialPlanEntry(
                            templateId = selectedTemplateId,
                            title = titleInput.trim(),
                            category = categoryInput,
                            bucket = bucketInput,
                            amount = amount,
                            recurrence = recurrenceCode,
                            weeklyDay = if (recurrenceCode == "WEEKLY") weeklyDay else null
                        )
                        val entryId = planDao.insert(entry)
                        val tag = "PLANNER_ENTRY:$entryId"
                        transactionDao.insertTransaction(
                            Transaction(
                                amount = amount,
                                merchantName = merchant,
                                category = categoryInput,
                                date = Date(),
                                bankName = null,
                                upiId = null,
                                rawSmsText = "PLANNER",
                                isManualEntry = true,
                                notes = "$tag;Bucket:$bucketInput"
                            )
                        )
                    }
                    titleInput = ""
                    merchantInput = ""
                    amountInput = ""
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Add Planned Expense")
                }
            }
        }

        item {
            FinCard {
                Text("Friends Money Tracker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = friendNameInput,
                    onValueChange = { friendNameInput = InputFilters.name(it, 30) },
                    label = { Text("Friend name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = friendAmountInput,
                    onValueChange = { friendAmountInput = InputFilters.amount(it) },
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = friendDirectionGive, onClick = { friendDirectionGive = true }, label = { Text("You gave") })
                    FilterChip(selected = !friendDirectionGive, onClick = { friendDirectionGive = false }, label = { Text("You received") })
                }
                TextButton(onClick = {
                    val amount = friendAmountInput.toDoubleOrNull() ?: return@TextButton
                    if (friendNameInput.isBlank() || amount <= 0.0) return@TextButton
                    scope.launch(Dispatchers.IO) {
                        loanDao.insert(LoanEntry(friendName = friendNameInput.trim(), amount = if (friendDirectionGive) amount else -amount))
                    }
                    friendNameInput = ""
                    friendAmountInput = ""
                }) { Text("Add Friend Entry") }

                loans.take(6).forEach { loan ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(loan.friendName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val detail = if (loan.amount >= 0) "You gave Rs ${formatMoney(abs(loan.amount))}" else "You received Rs ${formatMoney(abs(loan.amount))}"
                            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    loanDao.update(loan.copy(status = "SETTLED", settledAt = System.currentTimeMillis(), settledBy = "MANUAL"))
                                }
                            }) { Text("Settle") }
                            TextButton(onClick = {
                                scope.launch(Dispatchers.IO) { loanDao.update(loan.copy(notes = "Reminded")) }
                            }) { Text("Remind") }
                        }
                    }
                }
            }
        }
    }

    if (showCategoryPicker) {
        CategoryOverlaySheet(
            merchantHint = merchantInput.ifBlank { titleInput },
            amountHint = amountInput.toDoubleOrNull(),
            initialSelection = categoryInput,
            onDismiss = { showCategoryPicker = false },
            onCategorySelected = {
                categoryInput = it
                showCategoryPicker = false
            }
        )
    }

    if (showBucketPicker) {
        OptionPickerSheet(
            title = "Pick Bucket",
            options = buckets,
            selected = bucketInput,
            allowAdd = true,
            onDismiss = { showBucketPicker = false },
            onSelect = {
                bucketInput = it
                showBucketPicker = false
            }
        )
    }
}

@Composable
private fun FinCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

@Composable
private fun StatItem(label: String, value: Double, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Rs ${formatMoney(value)}", color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QuickAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun estimateDueDays(entry: FinancialPlanEntry, today: LocalDate): Int {
    val created = Date(entry.createdAt).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    return when (entry.recurrence.uppercase(Locale.getDefault())) {
        "WEEKLY" -> {
            val target = entry.weeklyDay ?: DayOfWeek.SUNDAY.value
            val delta = (target - today.dayOfWeek.value + 7) % 7
            if (delta == 0) 7 else delta
        }
        "MONTHLY" -> {
            val targetDay = created.dayOfMonth.coerceAtMost(today.lengthOfMonth())
            if (today.dayOfMonth <= targetDay) targetDay - today.dayOfMonth else (today.lengthOfMonth() - today.dayOfMonth + targetDay)
        }
        "YEARLY" -> 30
        else -> 3
    }
}

private fun recurrenceLabel(code: String): String {
    return recurrenceOptions.firstOrNull { it.code == code }?.label ?: code
}

private fun formatMoney(value: Double): String = String.format("%.0f", value.coerceAtLeast(0.0))






