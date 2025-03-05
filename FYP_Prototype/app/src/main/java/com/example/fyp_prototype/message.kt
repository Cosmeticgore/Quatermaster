package com.example.fyp_prototype

class message {
    var type : Int = 0 // 1 for ping, 2 for emergency
    var title : String = ""
    val ID : String = System.currentTimeMillis().toString()
    var message_con : String = ""
}

