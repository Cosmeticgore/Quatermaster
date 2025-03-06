package com.example.fyp_prototype

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.Firebase
import com.google.firebase.database.database
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NotificationService: FirebaseMessagingService() {

    private val database = Firebase.database
    private lateinit var userdata: AppData
    private val databaseRef = database.getReference("sessions")

    override fun onCreate() {
        super.onCreate()
        userdata = AppData.getInstance(application)
        Log.d("FCM TOKEN", "Notification Service Created")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM TOKEN", "Calling OnNewToken")
        sendRegistrationToServer(token)
    }

    @SuppressLint("MissingPermission")
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
        val text = message.notification?.body

        val channelId = "HEADS_UP_NOTIFICATION"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Ping Notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setAutoCancel(true)
            .build()

        val notificationManagerCompat = NotificationManagerCompat.from(this)

        notificationManagerCompat.notify(1, notification)



        super.onMessageReceived(message)


    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }

    private fun sendRegistrationToServer(token: String?){

    }



}