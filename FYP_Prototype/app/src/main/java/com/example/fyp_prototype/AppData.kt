package com.example.fyp_prototype

import android.app.Application
import android.se.omapi.Session
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.Firebase
import com.google.firebase.database.database
import org.osmdroid.views.overlay.Marker

class AppData private constructor(application: Application) : AndroidViewModel(application){

    companion object { //singleton object no more than one can exist at each time
        @Volatile
        private var instance: AppData? = null

        fun getInstance(application: Application): AppData{
            return instance?:synchronized(this){ // sync to avoid a concurrency issue
                instance ?: AppData(application).also {instance = it}
            }
        }
    }

    val user_ID = MutableLiveData<String>()
    val Session_ID = MutableLiveData<String>()
    val Role = MutableLiveData<String>()
    val Team = MutableLiveData<String>()
    val Status = MutableLiveData<String>()
    val Username = MutableLiveData<String>()
    val Cur_Site= MutableLiveData<site?>()
    val Cur_Game= MutableLiveData<game?>()
    val userMarkers = mutableMapOf<String, Marker>()
    var FirebaseAccess = FirebaseAccess()

    fun updateAppData(UID: String, SID: String, role: String, team: String, status: String){
        user_ID.value = UID
        Session_ID.value = SID
        Role.value = role
        Team.value = team
        Status.value = status
    }

    fun reset_data(){
        user_ID.value = ""
        Session_ID.value = ""
        Role.value = "Player"
        Team.value =  "Not Set"
        Status.value = "Nominal"
        Username.value = ""
        Cur_Site.value = null
        Cur_Game.value = null
    }

    fun update_team(team: String) {
        val database = Firebase.database
        val databaseRef = database.getReference("sessions").child(Session_ID.value.toString()).child("users").child(user_ID.value.toString()).child("team")
        FirebaseAccess.set_from_reference(
            ref = databaseRef,
            onSucc = {Team.value = team},
            set = team,
            onFail = {}
        )
    }

    fun update_status(status: String) {
        val database = Firebase.database
        val databaseRef = database.getReference("sessions").child(Session_ID.value.toString()).child("users").child(user_ID.value.toString()).child("status")
        FirebaseAccess.set_from_reference(
            ref = databaseRef,
            onSucc = {Status.value = status},
            set = status,
            onFail = { }
        )
    }
}