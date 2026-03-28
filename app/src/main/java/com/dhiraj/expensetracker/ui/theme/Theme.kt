package com.dhiraj.expensetracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FintechDarkScheme = darkColorScheme(
    primary = FintechPrimary,
    onPrimary = Color.White,
    secondary = FintechAccent,
    onSecondary = Color(0xFF022C22),
    tertiary = FintechWarning,
    error = FintechDanger,
    background = FintechDarkBackground,
    onBackground = FintechTextPrimary,
    surface = FintechDarkSurface,
    onSurface = FintechTextPrimary,
    onSurfaceVariant = FintechTextSecondary,
    surfaceVariant = Color(0xFF1C2436)
)

private val FintechLightScheme = lightColorScheme(
    primary = FintechPrimary,
    onPrimary = Color.White,
    secondary = FintechAccent,
    onSecondary = Color.White,
    tertiary = FintechWarning,
    error = FintechDanger,
    background = FintechLightBackground,
    onBackground = FintechLightTextPrimary,
    surface = FintechLightSurface,
    onSurface = FintechLightTextPrimary,
    onSurfaceVariant = FintechLightTextSecondary,
    surfaceVariant = Color(0xFFEFF3FA)
)

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean,
    palette: AppPalette = AppPalette.CALM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) FintechDarkScheme else FintechLightScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
