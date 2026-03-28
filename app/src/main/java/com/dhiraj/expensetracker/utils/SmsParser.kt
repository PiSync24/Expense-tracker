package com.dhiraj.expensetracker.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log
import com.dhiraj.expensetracker.utils.ParsedTransaction
import com.dhiraj.expensetracker.utils.TransactionType
import com.dhiraj.expensetracker.utils.PaymentRail

// ---------- MODELS ----------




// ---------- PARSER ----------

object SmsParser {

    // ---- HARD FILTERS ----

    private val IGNORE_KEYWORDS = listOf(
        "otp", "one time password",
        "undelivered", "delivery",
        "tracking", "awb"
    )

    // INR ONLY

    private val CURRENCY_AMOUNT_REGEX = Regex(
        """(?i)(?:rs\.?|inr|₹)\s*([0-9,]+(?:\.\d{1,2})?)"""
    )

    private val DEBIT_CREDIT_AMOUNT_REGEX = Regex(
        """(?i)(?:debited|credited)\s+by\s+(\d+(?:\.\d{1,2})?)"""
    )



    private val CREDIT_KEYWORDS = listOf("credited", "received")
    private val DEBIT_KEYWORDS = listOf("debited", "spent", "paid", "withdrawn", "sent", "dr ")

    private val INVALID_MERCHANT_WORDS = listOf(
        "dispute",
        "assistance",
        "support",
        "help",
        "customer",
        "bank",
        "balance"
    )

    private val SBI_UPI_PATTERN = Regex(
        """(?i)debited\s+by\s+(\d+(?:\.\d+)?)\s+on\s+date\s+\d{1,2}[A-Za-z]{3}\d{2}\s+trf\s+to\s+(.+?)(?:\s+refno|\s*$)""",
        RegexOption.IGNORE_CASE
    )







    // ---------- ENTRY POINT ----------

    fun parse(sender: String?, rawSms: String): ParsedTransaction? {
        val sms = rawSms.lowercase()

        // 1️⃣ Ignore junk early
        if (IGNORE_KEYWORDS.any { sms.contains(it) }) return null

        // 2️⃣ Ignore balance-only SMS
        if (sms.contains("balance") && !hasTransactionSignal(sms)) {
            Log.d("SMS_PARSER", "Ignoring balance-only SMS")
            return null
        }

        // 3️⃣ Amount is mandatory
        val amount = extractAmount(rawSms) ?: return null

        // 4️⃣ Detect transaction type
        val type = detectTransactionType(sms)

        // 5️⃣ Parse rails using THE SAME sms
        parseUpi(sms, amount, type)?.let { return it }
        parseCard(sms, amount, type)?.let { return it }
        parseNetBanking(sms, amount, type)?.let { return it }
        parseAccount(sms, amount, type)?.let { return it }

        // 6️⃣ Final fallback
        return fallbackParse(sms, amount, type)
    }


    // ---------- COMMON HELPERS ----------

    private fun hasTransactionSignal(sms: String): Boolean {
        return listOf(
            "debit", "debited",
            "credit", "credited",
            "transaction",
            "spent", "paid",
            "upi", "imps", "neft",
            "card", "p2m"
        ).any { sms.contains(it) }
    }


    private fun extractAmount(sms: String): Double? {

        // 1️⃣ Highest confidence: explicit currency (INR / Rs / ₹)
        CURRENCY_AMOUNT_REGEX.find(sms)?.let {
            return it.groupValues[1].replace(",", "").toDoubleOrNull()
        }

        // 2️⃣ Next: "debited by X" / "credited by X"
        DEBIT_CREDIT_AMOUNT_REGEX.find(sms)?.let {
            return it.groupValues[1].replace(",", "").toDoubleOrNull()
        }

        // 3️⃣ NO generic numeric fallback ❌
        return null
    }





    private fun detectTransactionType(sms: String): TransactionType =
        when {
            CREDIT_KEYWORDS.any { sms.contains(it) } -> TransactionType.CREDIT
            DEBIT_KEYWORDS.any { sms.contains(it) } -> TransactionType.DEBIT
            else -> TransactionType.UNKNOWN
        }

    private fun extractMerchant(sms: String): String {
        val patterns = listOf(
            Regex("""upi/[a-z0-9]+/[0-9]+/([a-z0-9 &._-]+)""", RegexOption.IGNORE_CASE),
            Regex("""\bto\s+([A-Za-z][A-Za-z0-9 &._-]{2,})""", RegexOption.IGNORE_CASE),
            Regex("""\bat\s+([A-Za-z][A-Za-z0-9 &._-]{2,})""", RegexOption.IGNORE_CASE),

        )

        for (p in patterns) {
            val m = p.find(sms) ?: continue
            val candidate = cleanMerchant(m.groupValues[1])
            if (isValidMerchant(candidate)) {
                return candidate
            }
        }

        return "Unknown"
    }
    private fun extractMerchant(
        sms: String,
        rail: PaymentRail
    ): String {

        val patterns = when (rail) {
            PaymentRail.UPI -> listOf(
                Regex("""upi/(?:p2m|p2p)/\d+/([A-Za-z][A-Za-z0-9 &._-]+)""", RegexOption.IGNORE_CASE)
            )

            PaymentRail.CARD -> listOf(
                Regex("""at\s+([A-Za-z][A-Za-z0-9 &._-]+)""", RegexOption.IGNORE_CASE)
            )

            else -> emptyList()
        }

        for (p in patterns) {
            val m = p.find(sms) ?: continue
            val candidate = cleanMerchant(m.groupValues[1])
            if (isValidMerchant(candidate)) {
                return candidate
            }
        }

        return "Unknown"
    }



    private val MERCHANT_STOP_WORDS = listOf(
        "not you",
        "call",
        "sms",
        "block",
        "cust id",
        "for dispute"
    )

    private fun cleanMerchant(raw: String): String {
        var result = raw
        for (stop in MERCHANT_STOP_WORDS) {
            val idx = result.lowercase().indexOf(stop)
            if (idx != -1) {
                result = result.substring(0, idx)
            }
        }
        return result.trim()
    }

    private fun isValidMerchant(name: String): Boolean {
        val lower = name.lowercase()

        if (name.length < 3) return false
        if (name.matches(Regex("""^\d+.*"""))) return false
        if (!name.any { it.isLetter() }) return false
        if (INVALID_MERCHANT_WORDS.any { lower.contains(it) }) return false

        return true
    }

    private fun extractAmountAndMerchant(sms: String): Pair<Double, String>? {

        // SBI UPI – generic merchant
        SBI_UPI_PATTERN.find(sms)?.let {
            val amount = parseAmount(it.groupValues[1])
            val merchant = it.groupValues[2]
                .trim()
                .replace(Regex("""\s+"""), " ")
            return amount to merchant
        }

        // fallback: no match
        return null
    }

    private fun parseAmount(amountStr: String): Double {
        return amountStr.replace(",", "").toDoubleOrNull() ?: 0.0
    }



    private fun extractReference(sms: String): String? {
        val patterns = listOf(
            Regex("""upi\s*ref\s*no\.?\s*(\w+)""", RegexOption.IGNORE_CASE),
            Regex("""ref(?:erence)?\s*no\.?\s*[:\-]?\s*(\w+)""", RegexOption.IGNORE_CASE),
            Regex("""upi/p2m/(\d{8,})""", RegexOption.IGNORE_CASE),
            Regex("""upi/p2p/(\d{8,})""", RegexOption.IGNORE_CASE)
        )

        for (p in patterns) {
            val m = p.find(sms)
            if (m != null) return m.groupValues[1]
        }
        return null
    }

    // ---------- RAIL-SPECIFIC PARSERS ----------



    private fun parseUpi(
        sms: String,
        amount: Double,
        type: TransactionType
    ): ParsedTransaction? {

        if (!sms.contains("upi", ignoreCase = true)) return null

        // ✅ SBI UPI special case (HIGH confidence)
        extractAmountAndMerchant(sms)?.let { (_, merchant) ->
            return ParsedTransaction(
                amount = amount,
                merchantName = merchant,
                bankName = "SBI",
                date = Date(),
                rail = PaymentRail.UPI,
                transactionType = type,
                upiReference = extractReference(sms),
                rawSms = sms,
                confidence = 0.95f
            )
        }

        // 🔁 Generic UPI fallback
        return ParsedTransaction(
            amount = amount,
            merchantName = extractMerchant(sms),
            bankName = "Unknown",
            date = Date(),
            rail = PaymentRail.UPI,
            transactionType = type,
            upiReference = extractReference(sms),
            rawSms = sms,
            confidence = 0.9f
        )
    }


    private fun parseCard(
        sms: String,
        amount: Double,
        type: TransactionType
    ): ParsedTransaction? {
        if (!sms.contains("card")) return null

        return ParsedTransaction(
            amount = amount,
            merchantName = extractMerchant(sms),
            bankName = "Unknown",
            date = Date(),
            rail = PaymentRail.CARD,
            transactionType = type,
            upiReference = extractReference(sms),
            rawSms = sms,
            confidence = 0.85f
        )
    }

    private fun parseNetBanking(
        sms: String,
        amount: Double,
        type: TransactionType
    ): ParsedTransaction? {
        if (!sms.contains("netbanking") && !sms.contains("internet banking")) return null

        return ParsedTransaction(
            amount = amount,
            merchantName = extractMerchant(sms),
            bankName = "Unknown",
            date = Date(),
            rail = PaymentRail.NETBANKING,
            transactionType = type,
            upiReference = extractReference(sms),
            rawSms = sms,
            confidence = 0.85f
        )
    }

    private fun parseAccount(
        sms: String,
        amount: Double,
        type: TransactionType
    ): ParsedTransaction? {
        if (!sms.contains("account") && !sms.contains("a/c")) return null

        return ParsedTransaction(
            amount = amount,
            merchantName = extractMerchant(sms),
            bankName = "Unknown",
            date = Date(),
            rail = PaymentRail.ACCOUNT,
            transactionType = type,
            upiReference = extractReference(sms),
            rawSms = sms,
            confidence = 0.7f
        )
    }

    // ---------- UNIVERSAL FALLBACK ----------

    private fun fallbackParse(
        sms: String,
        amount: Double,
        type: TransactionType
    ): ParsedTransaction {

        return ParsedTransaction(
            amount = amount,
            merchantName = extractMerchant(sms),
            bankName = "Unknown",
            date = Date(),
            rail = PaymentRail.UNKNOWN,
            transactionType = type,
            upiReference = extractReference(sms),
            rawSms = sms,
            confidence = 0.3f
        )
    }
}
