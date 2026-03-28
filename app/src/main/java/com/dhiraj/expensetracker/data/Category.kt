package com.dhiraj.expensetracker.data

enum class Category(val displayName: String, val emoji: String) {
    FOOD("Food", "🍔"),
    TRANSPORT("Transport", "🚗"),
    SHOPPING("Shopping", "🛍️"),
    BILLS("Bills", "📱"),
    ENTERTAINMENT("Entertainment", "🎬"),
    GROCERIES("Groceries", "🛒"),
    HEALTH("Health", "🏥"),
    EDUCATION("Education", "📚"),
    OTHER("Other", "💸");

    companion object {
        // Helper function to get all categories as a list
        fun getAllCategories(): List<Category> = values().toList()

        // Helper to find category by name
        fun fromString(name: String): Category {
            return values().find { it.displayName.equals(name, ignoreCase = true) } ?: OTHER
        }
    }
}


