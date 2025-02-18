package com.example.fyp_prototype

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Message
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class notificationhandler (private val context: Context){
    companion object {
        private const val CHANNEL_ID = "default_channel"
        private const val CHANNEL_NAME = "Default Channel"
        private const val CHANNEL_DESCRIPTION = "Default notification channel"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun shownotification(title: String, message: String, notificationID: Int = System.currentTimeMillis().toInt(), targetActivity: Class<*>? = null){
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        targetActivity?.let {
            val intent = Intent(context, it)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(pendingIntent)
        }
        try {
            NotificationManagerCompat.from(context).notify(notificationID, builder.build())
        } catch (e: SecurityException) {
            Log.e("Notification", "Permission denied: ${e.message}")
        }
    }
}


