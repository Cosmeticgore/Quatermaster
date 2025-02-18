package com.example.fyp_prototype

//this is the session class
//this contains the session id
//and a list of every user in the session

data class session(
    val session_Id: String = "",
    val users: Map<String, user> = emptyMap()
)

