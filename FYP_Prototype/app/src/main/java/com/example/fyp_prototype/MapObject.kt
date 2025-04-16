package com.example.fyp_prototype

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

class MapObject {
    var type: Int = 0 // 0 is point, 1 is line, 2 is polygon
    var title: String = ""
    var desc: String = ""
    var colour: Int = Color.GRAY
    var width: Float = 1F
    var icon: String = "Flag"
    var geopoints: MutableList<GeoPointData> = mutableListOf()
    var team: String = "None"

    constructor() // no arg constructor to avoid causing firebase errors

    constructor(
        type: Int,
        title: String = "",
        desc: String = "",
        colour: Int = Color.GRAY,
        width: Float = 1F,
        icon: String = "Flag",
        geopoints: MutableList<GeoPointData>,
        team: String = "None"
    ) {
        this.type = type
        this.title = title
        this.desc = desc
        this.colour = colour
        this.width = width
        this.icon = icon
        this.geopoints = geopoints
        this.team = team
    }


    fun draw(map: MapView, red: Boolean){ //red false means team 1 is blue, true means team 1 is red

        val geoPointsList = geopoints.map { GeoPoint(it.latitude, it.longitude) } // is a list because some markers have more than one

        if (type == 0 && geoPointsList.isNotEmpty()) { // basic marker
            val point = geoPointsList.first()
            val marker = Marker(map)
            marker.position = point
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            marker.title = title
            marker.snippet = desc

            val teamColor = when (team) { // setting teams
                "Team 1" -> if (red) "red" else "blue"
                "Team 2" -> if (red) "blue" else "red"
                else -> "None"
            }

            if (icon == "Respawn"){ //respawn icon
                if (teamColor == "red"){
                    marker.icon = ContextCompat.getDrawable(map.context,R.drawable.redrespawn)
                }else if (teamColor == "blue"){
                    marker.icon = ContextCompat.getDrawable(map.context,R.drawable.bluerespwawn)
                }else{
                    marker.icon = ContextCompat.getDrawable(map.context,R.drawable.greenrespawn)
                }
            }else if (icon == "Flag"){ // fleg icon
                if (teamColor == "red"){
                    marker.icon = ContextCompat.getDrawable(map.context,R.drawable.redflag)
                }else if (teamColor == "blue"){
                    marker.icon = ContextCompat.getDrawable(map.context,R.drawable.blueflag)
                }else{
                    marker.icon = ContextCompat.getDrawable(map.context,R.drawable.greenflag)
                }
            }else if (icon == "Warning"){ // warning icon
                marker.icon = ContextCompat.getDrawable(map.context,R.drawable.emergency)
            }else{
                if (teamColor == "red"){
                    marker.icon = ContextCompat.getDrawable(map.context,R.drawable.redx)
                }else if (teamColor == "blue"){
                    marker.icon = ContextCompat.getDrawable(map.context,R.drawable.bluex)
                }else{
                    marker.icon = ContextCompat.getDrawable(map.context,R.drawable.greenx)
                }
            }

            map.overlays.add(marker) // add marker to the map
        }

        if (type == 1 && geoPointsList.size > 1) { // drawing line
            val polyline = Polyline(map)
            polyline.setPoints(geoPointsList) // set points from geopoints list

            polyline.outlinePaint.color = colour
            polyline.outlinePaint.strokeWidth = width
            polyline.title = title
            polyline.snippet = desc

            map.overlays.add(polyline) // add to map
        }

        if (type == 2 && geoPointsList.size > 2 ) { // drawing polygon
            val polygon = Polygon(map)
            polygon.setPoints(geoPointsList)// set points from geopoints list
            polygon.fillPaint.color = colour
            polygon.outlinePaint.strokeWidth = width
            polygon.title = title
            polygon.snippet = desc
            map.overlays.add(polygon) // add to map
        }
    }

    data class GeoPointData( // needed to allow geopoints to be easily sent and retrieved from the database
        val latitude: Double = 0.0,
        val longitude: Double = 0.0
    ){
        constructor() : this(0.0, 0.0)
    }
}