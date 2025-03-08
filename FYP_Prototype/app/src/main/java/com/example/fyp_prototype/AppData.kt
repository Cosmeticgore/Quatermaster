package com.example.fyp_prototype

import android.app.Application
import android.se.omapi.Session
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.Firebase
import com.google.firebase.database.database

class AppData private constructor(application: Application) : AndroidViewModel(application){

    companion object {
        @Volatile
        private var instance: AppData? = null

        fun getInstance(application: Application): AppData{
            return instance?:synchronized(this){
                instance ?: AppData(application).also {instance = it}
            }
        }
    }

    val user_ID = MutableLiveData<String>()
    val Session_ID = MutableLiveData<String>()
    val Role = MutableLiveData<String>()
    val Team = MutableLiveData<String>()
    val Status = MutableLiveData<String>()

    fun updateAppData(UID: String, SID: String, role: String, team: String, status: String){
        user_ID.value = UID
        Session_ID.value = SID
        Role.value = role
        Team.value = team
        Status.value = status
    }

    fun update_team(team: String) {
        val database = Firebase.database
        val databaseRef = database.getReference("sessions")
        databaseRef.child(Session_ID.value.toString()).child("users").child(user_ID.value.toString()).child("team").setValue(team)
        Team.value = team
    }

    fun update_status(status: String) {
        val database = Firebase.database
        val databaseRef = database.getReference("sessions")
        databaseRef.child(Session_ID.value.toString()).child("users").child(user_ID.value.toString()).child("status").setValue(status)
        Status.value = status

    }

    fun update_role(role: String) {
        val database = Firebase.database
        val databaseRef = database.getReference("sessions")
        databaseRef.child(Session_ID.value.toString()).child("users").child(user_ID.value.toString()).child("role").setValue(role)
        Role.value = role

    }

}