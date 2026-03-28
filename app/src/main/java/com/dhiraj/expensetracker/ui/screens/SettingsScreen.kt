package com.dhiraj.expensetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhiraj.expensetracker.ui.theme.AppPalette
import com.dhiraj.expensetracker.ui.theme.AppPreferences
import com.dhiraj.expensetracker.ui.utils.InputFilters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    selectedPalette: AppPalette,
    onPaletteChange: (AppPalette) -> Unit
) {
    val context = LocalContext.current

    var smartCategory by rememberSaveable {
        mutableStateOf(AppPreferences.isSmartCategoryEnabled(context))
    }
    var autoSettleLoans by rememberSaveable {
        mutableStateOf(AppPreferences.isAutoSettleLoansEnabled(context))
    }
    var reduceMotion by rememberSaveable {
        mutableStateOf(AppPreferences.isReduceMotionEnabled(context))
    }

    val selectedTemplateId by AppPreferences
        .selectedTemplateIdState(context)
        .collectAsState(initial = AppPreferences.getSelectedTemplateId(context))

    val monthlyIncome by AppPreferences
        .monthlyIncomeState(context)
        .collectAsState(initial = AppPreferences.getMonthlyIncome(context))

    var monthlyIncomeInput by rememberSaveable { mutableStateOf("") }
    var incomeError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(monthlyIncome, selectedTemplateId) {
        monthlyIncomeInput = if (monthlyIncome > 0.0) String.format("%.2f", monthlyIncome) else ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Dark theme", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = isDarkTheme, onCheckedChange = onThemeChange)
                }

                Text("Color palette", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AppPalette.values().toList()) { palette ->
                        FilterChip(
                            selected = selectedPalette == palette,
                            onClick = { onPaletteChange(palette) },
                            label = {
                                Text(
                                    when (palette) {
                                        AppPalette.CALM -> "Calm"
                                        AppPalette.OCEAN -> "Ocean"
                                        AppPalette.SUNSET -> "Sunset"
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Smart Assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Category recommendations")
                    Switch(
                        checked = smartCategory,
                        onCheckedChange = {
                            smartCategory = it
                            AppPreferences.setSmartCategoryEnabled(context, it)
                        }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Auto-settle friend repayments")
                    Switch(
                        checked = autoSettleLoans,
                        onCheckedChange = {
                            autoSettleLoans = it
                            AppPreferences.setAutoSettleLoansEnabled(context, it)
                        }
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Reduce motion")
                    Switch(
                        checked = reduceMotion,
                        onCheckedChange = {
                            reduceMotion = it
                            AppPreferences.setReduceMotionEnabled(context, it)
                        }
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Template Income", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Current template id: $selectedTemplateId", style = MaterialTheme.typography.bodySmall)

                OutlinedTextField(
                    value = monthlyIncomeInput,
                    onValueChange = {
                        monthlyIncomeInput = InputFilters.amount(it)
                        incomeError = null
                    },
                    label = { Text("Monthly income") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = incomeError != null
                )

                incomeError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                TextButton(
                    onClick = {
                        val parsed = monthlyIncomeInput.trim().toDoubleOrNull()
                        if (parsed == null || parsed < 0.0) {
                            incomeError = "Enter valid number"
                            return@TextButton
                        }
                        AppPreferences.setMonthlyIncome(context, selectedTemplateId, parsed)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save income")
                }
            }
        }
    }
}
