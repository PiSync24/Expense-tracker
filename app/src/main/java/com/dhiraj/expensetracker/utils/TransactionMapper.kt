package com.dhiraj.expensetracker.utils

import com.dhiraj.expensetracker.data.Transaction
import com.dhiraj.expensetracker.utils.ParsedTransaction

fun ParsedTransaction.toTransaction(
    selectedCategory: String
): Transaction {
    return Transaction(
        amount = this.amount,
        merchantName = this.merchantName,
        category = selectedCategory,
        date = this.date,
        bankName = this.bankName,
        upiId = this.upiReference,
        rawSmsText = this.rawSms, // ✅ this is correct
        isManualEntry = false
    )
}
