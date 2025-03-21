package com.example.fyp_prototype

import android.graphics.Color
import android.graphics.drawable.Drawable
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
    var icon: Drawable? = null
    var geopoints: MutableList<GeoPointData> = mutableListOf()

    constructor()

    constructor(
        type: Int,
        title: String = "",
        desc: String = "",
        colour: Int = Color.GRAY,
        width: Float = 1F,
        icon: Drawable? = null,
        geopoints: MutableList<GeoPointData>
    ) {
        this.type = type
        this.title = title
        this.desc = desc
        this.colour = colour
        this.width = width
        this.icon = icon
        this.geopoints = geopoints
    }


    fun draw(map: MapView){

        val geoPointsList = geopoints.map { GeoPoint(it.latitude, it.longitude) }

        if (type == 0 && geoPointsList.isNotEmpty()) {
            val point = geoPointsList.first()
            val marker = Marker(map)
            marker.position = point
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            marker.title = title
            marker.snippet = desc

            if (icon != null) {
                marker.icon = icon
            }

            map.overlays.add(marker)
        }

        if (type == 1) {
            val polyline = Polyline(map)
            polyline.setPoints(geoPointsList)
            polyline.outlinePaint.color = colour
            polyline.outlinePaint.strokeWidth = width
            map.overlays.add(polyline)
        }

        if (type == 2) {
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
    )

    fun convertToOsmdroidGeoPoints(): List<GeoPoint> {
        return geopoints.map { GeoPoint(it.latitude, it.longitude) }
    }
}