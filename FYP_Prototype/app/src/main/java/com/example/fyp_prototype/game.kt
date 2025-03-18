package com.example.fyp_prototype


data class game (
    var gid: String = "game_${System.currentTimeMillis()}",
    var name: String = "",
    var desc: String = ""
)

