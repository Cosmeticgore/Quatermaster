package com.example.fyp_prototype

import org.osmdroid.views.MapView


data class game (
    var gid: String = "game_${System.currentTimeMillis()}",
    var name: String = "",
    var desc: String = "MY LIFE IS LIKE A VIDEO GAME TRYING TO BEAT THE STAGE ALL THE WHILE IM STILL COLLECTING COINS",
    var Markers: MutableList<MapObject> = mutableListOf<MapObject>()
){
    fun drawMarkers(map: MapView){
        Markers.forEach{ marker ->
            marker.draw(map)
        }
    }
}


