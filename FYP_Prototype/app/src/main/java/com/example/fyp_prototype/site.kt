package com.example.fyp_prototype

data class site (
    var name: String = "",
    var site_ID: String ="",
    var users_IDs: MutableList<String> = mutableListOf<String>(),
    var Games: Map<String, game> = mapOf(),
    var brief: String = ""
)





