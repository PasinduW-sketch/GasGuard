package com.example.gasml.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gasml.MainActivity

class NotificationHelper(private val context: Context) {
    // Incrementing channel ID to force device to apply new sound/vibration settings (v5000)
    private val criticalChannelId = "gas_leak_emergency_v5000"
    private val infoChannelId = "general_updates_v5000"
    
    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // 1. Critical Leak Channel
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()

                val leakChannel = NotificationChannel(
                    criticalChannelId,
                    "🚨 CRITICAL Gas Leak Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Urgent high-priority alarms for gas leak detection"
                    setSound(soundUri, audioAttributes)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000)
                    setBypassDnd(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    enableLights(true)
                    lightColor = Color.RED
                }

                // 2. Info Channel
                val infoChannel = NotificationChannel(
                    infoChannelId,
                    "General Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for orders and messages"
                    enableVibration(true)
                }

                notificationManager.createNotificationChannel(leakChannel)
                notificationManager.createNotificationChannel(infoChannel)
                Log.d("NotificationHelper", "Channels v5000 created")
            } catch (e: Exception) {
                Log.e("NotificationHelper", "Error creating channels", e)
            }
        }
    }

    fun showLeakAlert() {
        try {
            Log.d("NotificationHelper", "Triggering Leak Alert")
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("OPEN_LEAK_DIALOG", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context, 
                911, 
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            // Building the notification with maximum priority and urgent category
            val builder = NotificationCompat.Builder(context, criticalChannelId)
                .setSmallIcon(android.R.drawable.stat_sys_warning) // Using standard system icon to avoid resource issues
                .setContentTitle("🚨 CRITICAL GAS LEAK DETECTED!")
                .setContentText("DANGER: A gas leak has been detected. Check your kitchen immediately.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true) 
                .setAutoCancel(false)
                .setSound(soundUri)
                .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000))
                .setContentIntent(pendingIntent)
                .setColor(Color.RED)
                .setLights(Color.RED, 500, 500)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Acknowledge Safety", pendingIntent)

            // Forcing insistent flag safely
            val notification = builder.build()
            notification.flags = notification.flags or Notification.FLAG_INSISTENT

            notificationManager.notify(911, notification)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "showLeakAlert failed", e)
        }
    }

    fun showOrderNotification(title: String, message: String) {
        try {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 
                System.currentTimeMillis().toInt(), 
                intent, 
                PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, infoChannelId)
                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Order notification error", e)
        }
    }

    fun showChatNotification(senderName: String, message: String) {
        try {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 
                senderName.hashCode(), 
                intent, 
                PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, infoChannelId)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle("New message from $senderName")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            notificationManager.notify(senderName.hashCode(), builder.build())
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Chat notification error", e)
        }
    }

    fun clearAlert() {
        try {
            Log.d("NotificationHelper", "Clearing alert 911")
            notificationManager.cancel(911)
        } catch (e: Exception) {
            Log.e("NotificationHelper", "clearAlert error", e)
        }
    }
}
