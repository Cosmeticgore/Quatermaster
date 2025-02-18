package com.example.fyp_prototype

import android.os.Parcel
import android.os.Parcelable

// this is the User class
// this class is the framework of how we store data in the database

data class user(
    val userId: String = "",
    val location: user_loc = user_loc(),
    val team: String = "None",
    val role: String = "Player",
    val status: String = "Nominal"
)


data class user_loc(
    var longitude: Double = 0.0,
    var latitude: Double = 0.0

    )
