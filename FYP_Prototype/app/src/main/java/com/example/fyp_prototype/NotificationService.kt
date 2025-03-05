package com.example.fyp_prototype

import android.util.Log
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

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
    }

    private fun sendRegistrationToServer(token: String?){

    }



}