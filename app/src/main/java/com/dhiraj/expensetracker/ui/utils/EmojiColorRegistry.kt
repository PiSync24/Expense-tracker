package com.dhiraj.expensetracker.ui.utils

import android.content.Context
import androidx.compose.ui.graphics.Color

object EmojiColorRegistry {
    private const val PREFS_NAME = "emoji_color_registry"
    private const val KEY_PREFIX = "emoji_color_"

    private val inMemoryCache = mutableMapOf<String, Int>()

    private val palette = listOf(
        0xFFE67E22.toInt(),
        0xFF16A085.toInt(),
        0xFF3498DB.toInt(),
        0xFF8E44AD.toInt(),
        0xFFD35400.toInt(),
        0xFF2E86C1.toInt(),
        0xFF1E8449.toInt(),
        0xFFAF601A.toInt(),
        0xFF7D3C98.toInt(),
        0xFF117864.toInt(),
        0xFF9A7D0A.toInt(),
        0xFF566573.toInt()
    )

    fun getOrCreateColor(context: Context, emoji: String): Color {
        if (emoji.isBlank()) return Color(0xFF5D6D7E)

        inMemoryCache[emoji]?.let { return Color(it) }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = KEY_PREFIX + emojiToKey(emoji)

        val stored = prefs.getInt(key, Int.MIN_VALUE)
        if (stored != Int.MIN_VALUE) {
            inMemoryCache[emoji] = stored
            return Color(stored)
        }

        val generated = palette[(emoji.hashCode().ushr(1)) % palette.size]
        prefs.edit().putInt(key, generated).apply()
        inMemoryCache[emoji] = generated
        return Color(generated)
    }

    private fun emojiToKey(emoji: String): String {
        return emoji.codePoints()
            .toArray()
            .joinToString(separator = "_") { codePoint -> codePoint.toString(16) }
    }
}
