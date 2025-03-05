package com.example.fyp_prototype

import android.app.Application
import android.se.omapi.Session
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

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

    fun updateAppData(UID: String, SID: String, role: String){
        user_ID.value = UID
        Session_ID.value = SID
        Role.value = role
    }

    fun clearData() {
        user_ID.value = null
        Session_ID.value = null
    }

}