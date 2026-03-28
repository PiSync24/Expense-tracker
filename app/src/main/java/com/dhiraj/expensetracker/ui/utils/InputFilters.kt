package com.dhiraj.expensetracker.ui.utils

object InputFilters {
    fun name(value: String, maxLen: Int = 40): String {
        val cleaned = value.filter { ch ->
            ch.isLetterOrDigit() || ch == ' ' || ch == '.' || ch == '&' || ch == '-' || ch == '_'
        }
        return cleaned.take(maxLen)
    }

    fun category(value: String, maxLen: Int = 24): String {
        val cleaned = value.filter { ch ->
            ch.isLetterOrDigit() || ch == ' ' || ch == '&' || ch == '-'
        }
        return cleaned.take(maxLen)
    }

    fun amount(value: String): String {
        val filtered = value.filter { it.isDigit() || it == '.' }
        if (filtered.isEmpty()) return ""

        val firstDot = filtered.indexOf('.')
        if (firstDot < 0) return filtered.take(10)

        val intPart = filtered.substring(0, firstDot).take(10)
        val decimalPart = filtered
            .substring(firstDot + 1)
            .replace(".", "")
            .take(2)

        return if (decimalPart.isEmpty()) "$intPart." else "$intPart.$decimalPart"
    }
}
