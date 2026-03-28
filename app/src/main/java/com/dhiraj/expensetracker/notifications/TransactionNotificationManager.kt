package com.dhiraj.expensetracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dhiraj.expensetracker.R
import com.dhiraj.expensetracker.receivers.CategoryActionReceiver
import com.dhiraj.expensetracker.data.Category
import com.dhiraj.expensetracker.utils.ParsedTransaction
import com.dhiraj.expensetracker.ui.CategorizeTransactionActivity


object TransactionNotificationManager {

    private const val CHANNEL_ID = "transaction_notifications"
    private const val CHANNEL_NAME = "Transaction Alerts"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for quick transaction categorization"
                enableVibration(true)
                enableLights(true)
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showTransactionNotification(
        context: Context,
        parsedTransaction: ParsedTransaction,
        notificationId: Int
    ) {


        createNotificationChannel(context)


        // Optional suggestion logic (safe even if unused)
        //SmsParser.suggestCategory(parsedTransaction.merchantName)

        // ðŸ”¹ STEP 1: Build notification (ONLY UI config here)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("ðŸ’° Quick Categorize")
            .setContentText("â‚¹${parsedTransaction.amount} â€¢ ${parsedTransaction.merchantName}")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "â‚¹${parsedTransaction.amount}\n" +
                            "${parsedTransaction.merchantName}\n" +
                            "Just now â€¢ ${parsedTransaction.bankName}\n\n" +
                            "Tap a category to save:"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setColor(0xFF667EEA.toInt())
            .setVibrate(longArrayOf(0, 250, 250, 250))

// ðŸ”¹ STEP 2: Add category action buttons
        builder.apply {
            val maxActions = getMaxNotificationActions(context)
            val maxCategoryButtons = maxActions - 2 // reserve for More

            val suggestedCategories = listOf(
                Category.FOOD,
                Category.TRANSPORT,
                Category.SHOPPING,
                Category.BILLS
            ).take(maxCategoryButtons)

            // Category buttons
            suggestedCategories.forEach { category ->
                val intent = Intent(context, CategoryActionReceiver::class.java).apply {
                    action = "CATEGORIZE_TRANSACTION"
                    putExtra("notification_id", notificationId)
                    putExtra("amount", parsedTransaction.amount)
                    putExtra("merchant", parsedTransaction.merchantName)
                    putExtra("bank", parsedTransaction.bankName)
                    putExtra("category", category.displayName)
                    putExtra("upi_ref", parsedTransaction.upiReference)
                    putExtra("raw_sms", parsedTransaction.rawSms)
                    putExtra("date", parsedTransaction.date.time)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId + category.ordinal,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                addAction(
                    0,
                    "${category.emoji} ${category.displayName}",
                    pendingIntent
                )
            }

            // âž• More button (ALWAYS)
            val moreIntent = Intent(context, CategorizeTransactionActivity::class.java).apply {
                putExtra("notification_id", notificationId)
                putExtra("amount", parsedTransaction.amount)
                putExtra("merchant", parsedTransaction.merchantName)
                putExtra("bank", parsedTransaction.bankName)
                putExtra("upi_ref", parsedTransaction.upiReference)
                putExtra("raw_sms", parsedTransaction.rawSms)
                putExtra("date", parsedTransaction.date.time)
            }

            val morePendingIntent = PendingIntent.getActivity(
                context,
                notificationId + 999,
                moreIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            addAction(
                0,
                "âž• More",
                morePendingIntent
            )




        }

// ðŸ”¹ STEP 3: Show notification
        try {
            NotificationManagerCompat
                .from(context)
                .notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }


    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.cancel(notificationId)
    }


    fun showSuccessNotification(
        context: Context,
        amount: Double,
        category: String
    ) {

        createNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("âœ“ Saved!")
            .setContentText("â‚¹$amount â†’ $category")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(2000)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                (System.currentTimeMillis() % 1000).toInt(),
                notification
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}

private fun getMaxNotificationActions(context: Context): Int {
    val config = context.resources.configuration
    val fontScale = config.fontScale
    val screenWidthDp = config.screenWidthDp

    return when {
        fontScale > 1.2f -> 3
        screenWidthDp < 360 -> 3
        else -> 4
    }
}


