package com.example.fyp_prototype

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Firebase
import com.google.firebase.database.database

// this is the User class
// this class is the framework of how we store data in the database

// DEPRECTIATED USE APPDATA OBJECT FOR STORING DATA INSTEAD THIS IS PURELY FOR CREATION ON FIREBASE

data class user(
    val userId: String = "",
    val location: user_loc = user_loc(),
    var team: String = "None",
    var role: String = "Player",
    var status: String = "Nominal",
    var not_token: String = ""
)


data class user_loc(
    var longitude: Double = 0.0,
    var latitude: Double = 0.0

    )


