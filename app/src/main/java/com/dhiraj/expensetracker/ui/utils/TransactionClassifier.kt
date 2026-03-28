package com.dhiraj.expensetracker.ui.utils

import com.dhiraj.expensetracker.data.Transaction
import java.util.Locale

object TransactionClassifier {

    fun isCredit(tx: Transaction): Boolean {
        if (tx.amount < 0) return false

        val text = tx.rawSmsText.orEmpty().lowercase(Locale.getDefault())
        val category = tx.category.lowercase(Locale.getDefault())
        val merchant = tx.merchantName.orEmpty().lowercase(Locale.getDefault())

        return text.contains("credited") ||
            text.contains("received") ||
            text.contains("deposit") ||
            text.contains("refund") ||
            category.contains("salary") ||
            category.contains("refund") ||
            category.contains("income") ||
            merchant.contains("salary")
    }
}
