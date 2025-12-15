package com.example.dealtracker.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.dealtracker.MainActivity

/**
 * Helper object for creating and displaying local notifications.
 */
object NotificationHelper {

    private const val CHANNEL_ID = "price_alerts"
    private const val CHANNEL_NAME = "Price Drop Alerts"

    /**
     * Creates a notification channel (required for Android 8.0+).
     * @param context Application context.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for price drops on wishlist items"
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Displays a price drop notification.
     * The notification carries uid and pid, and is marked as read upon click.
     * @param context Application context.
     * @param uid User ID.
     * @param pid Product ID.
     * @param title Product title.
     * @param currentPrice The current price.
     * @param targetPrice The target price set by the user.
     */
    fun showPriceDropNotification(
        context: Context,
        uid: Int,
        pid: Int,
        title: String,
        currentPrice: Double,
        targetPrice: Double
    ) {
        // Create an Intent for clicking the notification, carrying uid and pid
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_clicked", true)
            putExtra("notification_uid", uid)
            putExtra("notification_pid", pid)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            pid, // Use pid as requestCode to ensure unique notification intent
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Price Drop Alert!")
            .setContentText("$title is now $${"%.2f".format(currentPrice)} (Target: $${"%.2f".format(targetPrice)})")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$title\n\nCurrent price: $${"%.2f".format(currentPrice)}\nYour target: $${"%.2f".format(targetPrice)}\n\nTap to view details")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Dismiss automatically when clicked
            .setContentIntent(pendingIntent)
            .build()

        // Display the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(pid, notification)
    }
}