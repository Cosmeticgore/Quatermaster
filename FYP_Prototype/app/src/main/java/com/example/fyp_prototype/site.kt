package com.example.fyp_prototype

import org.osmdroid.views.MapView

data class site (
    var name: String = "",
    var site_ID: String ="",
    var users_IDs: MutableList<String> = mutableListOf<String>(),
    var games: Map<String, game> = mapOf(),
    var brief: String = "",
    var markers: MutableList<MapObject> = mutableListOf<MapObject>(),
    var Red: Boolean = true
){
    fun drawMarkers(map: MapView){
        markers.forEach{ marker ->
            marker.draw(map, Red)
        }
        map.invalidate()
    }
}





