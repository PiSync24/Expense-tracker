package com.dhiraj.expensetracker.utils

import java.util.Date

data class ParsedTransaction(
    val amount: Double,
    val merchantName: String,
    val bankName: String,
    val date: Date,
    val upiReference: String?,   // ✅ canonical name
    val rawSms: String,
    val transactionType: TransactionType,
    val rail: PaymentRail,
    val confidence: Float
)

enum class TransactionType {
    DEBIT, CREDIT, UNKNOWN
}

enum class PaymentRail {
    UPI, CARD, NETBANKING, IMPS, ACCOUNT, UNKNOWN
}
