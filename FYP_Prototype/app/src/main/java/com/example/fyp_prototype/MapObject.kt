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

    constructor()

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

        val geoPointsList = geopoints.map { GeoPoint(it.latitude, it.longitude) }

        if (type == 0 && geoPointsList.isNotEmpty()) {
            val point = geoPointsList.first()
            val marker = Marker(map)
            marker.position = point
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            marker.title = title
            marker.snippet = desc

            val teamColor = when (team) {
                "Team 1" -> if (red) "red" else "blue"
                "Team 2" -> if (red) "blue" else "red"
                else -> "None"
            }

            if (icon == "Respawn"){
                if (teamColor == "red"){
                    marker.icon = ContextCompat.getDrawable(map.context,R.drawable.redrespawn)
                }else if (teamColor == "blue"){
                    marker.icon = ContextCompat.getDrawable(map.context,R.drawable.bluerespwawn)
                }else{
                    marker.icon = ContextCompat.getDrawable(map.context,R.drawable.greenrespawn)
                }
            }else if (icon == "Flag"){
                if (teamColor == "red"){
                    marker.icon = ContextCompat.getDrawable(map.context,R.drawable.redflag)
                }else if (teamColor == "blue"){
                    marker.icon = ContextCompat.getDrawable(map.context,R.drawable.blueflag)
                }else{
                    marker.icon = ContextCompat.getDrawable(map.context,R.drawable.greenflag)
                }
            }else if (icon == "Warning"){
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

            map.overlays.add(marker)
        }

        if (type == 1 && geoPointsList.size > 1) {
            val polyline = Polyline(map)
            polyline.setPoints(geoPointsList)

            polyline.outlinePaint.color = colour
            polyline.outlinePaint.strokeWidth = width

            map.overlays.add(polyline)
        }

        if (type == 2 && geoPointsList.size > 2 ) {
            val polygon = Polygon(map)
            polygon.setPoints(geoPointsList)
            polygon.fillPaint.color = colour
            polygon.outlinePaint.strokeWidth = width
            map.overlays.add(polygon)
        }
    }

    data class GeoPointData(
        val latitude: Double = 0.0,
        val longitude: Double = 0.0
    ){
        constructor() : this(0.0, 0.0)
    }
}