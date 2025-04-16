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

    private lateinit var userdata: AppData

    override fun onCreate() {
        super.onCreate()
        userdata = AppData.getInstance(application) // gets userdata
        Log.d("FCM TOKEN", "Notification Service Created")
    }

    override fun onNewToken(token: String) { //called when a new token is created
        super.onNewToken(token)
        Log.d("FCM TOKEN", "Calling OnNewToken")
        sendRegistrationToServer(token)
    }

    @SuppressLint("MissingPermission")
    override fun onMessageReceived(message: RemoteMessage) { //when we recieve a message from firebase messaging
        val title = message.data["title"] //get title
        val text = message.data["message"] //get message

        val channelId = "HEADS_UP_NOTIFICATION" //notification channel name

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Player Notifications",
                NotificationManager.IMPORTANCE_HIGH //safety message is high importance
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId) //builds notifaction
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.icon)
            .setAutoCancel(true)
            .build()

        val notificationManagerCompat = NotificationManagerCompat.from(this)

        notificationManagerCompat.notify(1, notification) //sends that notification

        super.onMessageReceived(message)
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }

    private fun sendRegistrationToServer(token: String?){

    }

}