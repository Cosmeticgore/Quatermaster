package com.example.fyp_prototype




data class site (
    var name: String = "",
    var Site_ID: String ="",
    var users_IDs: MutableList<String> = mutableListOf<String>(),
    var Games: MutableList<game> = mutableListOf<game>(),
    var brief: String = ""
)





