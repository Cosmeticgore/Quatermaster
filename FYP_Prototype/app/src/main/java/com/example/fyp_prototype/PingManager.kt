package com.example.fyp_prototype

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions

class PingManager(userData: AppData, function: FirebaseFunctions) {
    private val functions = function
    private val userdata = userData

    // non-urgent ping
    fun ping(sessionId: String): Task<String> {
        val data = hashMapOf( //construct message
            "sessionId" to sessionId,
            "title" to "Player Needs Help",
            "message" to "Non Urgent Help wanted"
        )

        userdata.update_status("Help_Needed") //update status on userdata and database

        return functions.getHttpsCallable("sendAdminNotifications") //send a https request to Cloud Function
            .call(data)
            .continueWith { task ->
                if (task.isSuccessful) { // when task is successful log it
                    val result = task.result?.data
                    "Success: Notification sent"
                } else {
                    val exception = task.exception
                    "Error: ${exception?.message}"
                }
            }
    }

    // urgent ping - if this is called someone is in danger
    fun urgentping(sessionId: String): Task<String> {
        val data = hashMapOf( //construct message
            "sessionId" to sessionId,
            "title" to "Player is in Danger!",
            "message" to "Player is in urgent need of help!"
        )

        userdata.update_status("Critical")//update usedata and database

        return functions.getHttpsCallable("sendAdminNotifications") //send a https request to cloud function
            .call(data)
            .continueWith { task ->
                if (task.isSuccessful) { // when task is successful log it
                    val result = task.result?.data
                    "Success: Notification sent"
                } else {
                    val exception = task.exception
                    "Error: ${exception?.message}"
                }
            }
    }
}