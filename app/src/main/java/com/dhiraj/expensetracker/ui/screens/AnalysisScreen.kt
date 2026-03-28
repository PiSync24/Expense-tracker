package com.dhiraj.expensetracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhiraj.expensetracker.data.AppDatabase
import com.dhiraj.expensetracker.ui.utils.TransactionClassifier
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private data class CategorySlice(val name: String, val value: Double)
private data class DayPoint(val label: String, val amount: Double)

@Composable
fun AnalysisScreen() {
    val context = LocalContext.current
    val db = remember(context) { AppDatabase.getDatabase(context) }
    val dao = remember(db) { db.transactionDao() }
    val transactions by dao.getAllTransactions().collectAsState(initial = emptyList())

    val expenses = remember(transactions) { transactions.filterNot { TransactionClassifier.isCredit(it) } }
    val total = remember(expenses) { expenses.sumOf { abs(it.amount) }.coerceAtLeast(1.0) }

    val byCategory = remember(expenses) {
        expenses.groupBy { it.category.ifBlank { "Other" } }
            .map { CategorySlice(it.key, it.value.sumOf { tx -> abs(tx.amount) }) }
            .sortedByDescending { it.value }
            .take(6)
    }

    val weekly = remember(expenses) {
        val today = LocalDate.now()
        (0..6).map { offset ->
            val day = today.minusDays((6 - offset).toLong())
            val sum = expenses
                .filter { it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == day }
                .sumOf { abs(it.amount) }
            DayPoint(day.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())), sum)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Spending by Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    PieChart(slices = byCategory, total = total)
                    byCategory.forEachIndexed { index, slice ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${legendColor(index)} ${slice.name}")
                            Text("${((slice.value / total) * 100).toInt()}%")
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Weekly Spending", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    WeeklyBars(points = weekly)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Category Share", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    byCategory.forEachIndexed { index, slice ->
                        val pct = (slice.value / total).toFloat().coerceIn(0f, 1f)
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(slice.name)
                                Text("${(pct * 100).toInt()}%")
                            }
                            LinearProgressIndicator(
                                progress = pct,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                color = chartColors[index % chartColors.size]
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PieChart(slices: List<CategorySlice>, total: Double) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(190.dp)) {
            var start = -90f
            slices.forEachIndexed { index, slice ->
                val sweep = ((slice.value / total) * 360f).toFloat()
                drawArc(
                    color = chartColors[index % chartColors.size],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 48f),
                    size = Size(size.width, size.height)
                )
                start += sweep
            }
        }
        Text("Rs ${String.format("%.0f", total)}", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WeeklyBars(points: List<DayPoint>) {
    val maxAmount = points.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        points.forEach { point ->
            val barHeight = (((point.amount / maxAmount) * 120).toInt().coerceAtLeast(8)).dp
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(barHeight)
                        .background(Color(0xFF4D8DFF))
                )
                Text(point.label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private val chartColors = listOf(
    Color(0xFF4D8DFF),
    Color(0xFF24B47E),
    Color(0xFFFFA340),
    Color(0xFFFF6B6B),
    Color(0xFF8A63D2),
    Color(0xFF16A8C7)
)

private fun legendColor(index: Int): String {
    return listOf("?", "¦", "?", "?", "?", "?")[index % 6]
}
