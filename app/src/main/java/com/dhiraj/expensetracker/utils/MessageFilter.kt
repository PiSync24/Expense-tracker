package com.dhiraj.expensetracker.utils

object MessageFilter {

    // ✅ Only allow known BANK senders
    private val BANK_SENDER_REGEX = Regex(
        """(?i).*(SBI|HDFC|ICICI|AXIS|KOTAK|YES|PNB|BOB|IDFC|INDUS).*"""
    )

    // ❌ Merchant / food / ecommerce confirmations
    private val MERCHANT_KEYWORDS = listOf(
        "swiggy",
        "zomato",
        "zepto",
        "blinkit",
        "instamart",
        "amazon",
        "flipkart",
        "myntra",
        "order",
        "order confirmed",
        "payment confirmed",
        "enjoy your",
        "will be delivered",
        "delivery",
        "track",
        "tracking",
        "invoice",
        "bill",
        "txn id"
    )

    // ✅ Real bank transactions MUST contain these
    private val REQUIRED_BANK_VERBS = listOf(
        "debited",
        "credited",
        "spent",
        "withdrawn"
    )

    fun shouldIgnore(sender: String?, message: String): Boolean {
        val text = message.lowercase()

        // 1️⃣ Not from a bank → ignore
        if (sender == null || !BANK_SENDER_REGEX.containsMatchIn(sender)) {
            return true
        }

        // 2️⃣ Merchant confirmation → ignore
        if (MERCHANT_KEYWORDS.any { text.contains(it) }) {
            return true
        }

        // 3️⃣ No debit / credit verb → ignore
        if (REQUIRED_BANK_VERBS.none { text.contains(it) }) {
            return true
        }

        return false
    }
}
