package com.example.fyp_prototype

import android.graphics.Color
import android.graphics.drawable.Drawable
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

class MapObject(
    val type: Int, // 0 is point, 1 is line, 2 is polygon
    val title: String = "",
    val desc: String = "",
    val colour: Int = Color.GRAY,
    val width: Float = 1F,
    val icon: Drawable? = null,
    val geopoints: MutableList<GeoPoint>
) {

    fun draw(map: MapView){

        if (type == 0){
            val point = geopoints.first()
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

        if (type == 1){
            val polyline = Polyline(map)
            polyline.setPoints(geopoints)
            polyline.outlinePaint.color = colour
            polyline.outlinePaint.strokeWidth = width
            map.overlays.add(polyline)
        }

        if (type == 2){
            val Polygon = Polygon(map)
            Polygon.setPoints(geopoints)
            Polygon.fillPaint.color = colour
            Polygon.outlinePaint.strokeWidth = width
            map.overlays.add(Polygon)
        }
    }
}