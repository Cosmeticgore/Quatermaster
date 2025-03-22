package com.example.fyp_prototype

import org.osmdroid.views.MapView

data class site (
    var name: String = "",
    var site_ID: String ="",
    var users_IDs: MutableList<String> = mutableListOf<String>(),
    var games: Map<String, game> = mapOf(),
    var brief: String = "",
    var Markers: MutableList<MapObject> = mutableListOf<MapObject>(),
    var Red: Boolean = true
){
    fun drawMarkers(map: MapView){
        Markers.forEach{ marker ->
            marker.draw(map, Red)
        }
        map.invalidate()
    }
}





