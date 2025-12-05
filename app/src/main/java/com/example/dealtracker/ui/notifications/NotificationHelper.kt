package com.example.dealtracker.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.dealtracker.R
import com.example.dealtracker.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID = "price_alerts"
    private const val CHANNEL_NAME = "Price Drop Alerts"

    /**
     * 创建通知渠道（Android 8.0+）
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
     * 显示降价通知
     * ⭐ 通知中包含 uid 和 pid，点击后标记为已读
     */
    fun showPriceDropNotification(
        context: Context,
        uid: Int,
        pid: Int,
        title: String,
        currentPrice: Double,
        targetPrice: Double
    ) {
        // ⭐ 创建点击通知的 Intent，携带 uid 和 pid
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_clicked", true)
            putExtra("notification_uid", uid)
            putExtra("notification_pid", pid)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            pid, // 使用 pid 作为 requestCode，确保每个通知唯一
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 构建通知
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // ⭐ 使用系统图标
            .setContentTitle("🎉 Price Drop Alert!") // ⭐ 修复 setContentTitle
            .setContentText("$title is now $${"%.2f".format(currentPrice)} (Target: $${"%.2f".format(targetPrice)})")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$title\n\nCurrent price: $${"%.2f".format(currentPrice)}\nYour target: $${"%.2f".format(targetPrice)}\n\nTap to view details")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // 点击后自动消失
            .setContentIntent(pendingIntent)
            .build()

        // 显示通知
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(pid, notification)
    }
}