package com.dhiraj.expensetracker.ui.utils

import com.dhiraj.expensetracker.data.TransactionDao
import java.util.Locale

object SmartCategoryRecommender {

    suspend fun recommend(
        transactionDao: TransactionDao,
        merchantHint: String?,
        limit: Int = 4
    ): List<String> {
        val merchant = merchantHint.orEmpty().trim()
        if (merchant.isBlank()) {
            return transactionDao
                .getTopCategoryFrequency(limit)
                .map { it.category }
        }

        val exact = transactionDao
            .getCategoryFrequencyForMerchant(merchant, limit)
            .map { it.category }

        if (exact.isNotEmpty()) return exact

        val recent = transactionDao.getRecentTransactions(250)
        val hintTokens = tokenize(merchant)
        if (hintTokens.isEmpty()) {
            return transactionDao
                .getTopCategoryFrequency(limit)
                .map { it.category }
        }

        val scored = linkedMapOf<String, Double>()

        recent.forEachIndexed { index, tx ->
            val txMerchant = tx.merchantName.orEmpty()
            val txTokens = tokenize(txMerchant)
            if (txTokens.isEmpty()) return@forEachIndexed

            val overlap = hintTokens.intersect(txTokens).size
            if (overlap == 0) return@forEachIndexed

            val normalizedHint = merchant.lowercase(Locale.getDefault())
            val normalizedMerchant = txMerchant.lowercase(Locale.getDefault())
            val containsBonus = if (
                normalizedHint.contains(normalizedMerchant) ||
                normalizedMerchant.contains(normalizedHint)
            ) {
                1.5
            } else {
                0.0
            }

            val recencyBonus = (recent.size - index).toDouble() / recent.size
            val score = overlap * 2.5 + containsBonus + recencyBonus

            scored[tx.category] = (scored[tx.category] ?: 0.0) + score
        }

        val ranked = scored
            .toList()
            .sortedByDescending { (_, score) -> score }
            .map { (category, _) -> category }
            .take(limit)

        if (ranked.isNotEmpty()) return ranked

        return transactionDao
            .getTopCategoryFrequency(limit)
            .map { it.category }
    }

    private fun tokenize(value: String): Set<String> {
        return Regex("[a-z0-9]{3,}")
            .findAll(value.lowercase(Locale.getDefault()))
            .map { it.value }
            .toSet()
    }
}
