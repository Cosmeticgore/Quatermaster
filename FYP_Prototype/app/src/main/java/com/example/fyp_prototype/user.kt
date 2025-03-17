package com.example.fyp_prototype

// this is the User class
// this class is the framework of how we store data in the database

// DEPRECTIATED USE APPDATA OBJECT FOR STORING DATA INSTEAD THIS IS PURELY FOR CREATION ON FIREBASE

data class user(
    val userId: String = "",
    val location: locat = locat(),
    var team: String = "None",
    var role: String = "Player",
    var status: String = "Nominal",
    var not_token: String = "",
    var username: String = "Jeff"
)


data class locat(
    var longitude: Double = 0.0,
    var latitude: Double = 0.0

    )


