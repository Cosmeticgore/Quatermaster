package com.example.fyp_prototype

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapObject_UT {

    private val mockMap: MapView = mockk(relaxed = true)
    private val mockContext: Context = mockk(relaxed = true)
    private val overlayList = mutableListOf<Overlay>()

    @Before
    fun setup() {
        every { mockMap.context } returns mockContext
        every { mockMap.overlays } returns overlayList
    }

    @Test
    fun draw_basic_marker(){
        //arrange
        val testGeopoint = mutableListOf(MapObject.GeoPointData(2.0,-1.0))
        val mockDrawable :Drawable = mockk()

        val slot = slot<Int>()
        every { ContextCompat.getDrawable(mockContext, capture(slot)) } returns mockDrawable

        val testmapObject = MapObject(
            type = 0, // 0 for marker
            title = "Test Title",
            desc = "Test Desc",
            icon = "Flag",
            geopoints = testGeopoint
        )
        //act
        testmapObject.draw(mockMap, true)
        //assert
        val marker = overlayList.find { it is Marker } as Marker
        assert(marker != null)
        assert(marker.title == "Test Title")
        assert(marker.snippet == "Test Desc")
        assert(marker.icon == mockDrawable)
    }

    @Test
    fun draw_line_marker(){
        //arrange
        val testGeopoints = mutableListOf(MapObject.GeoPointData(2.0,-1.0), MapObject.GeoPointData(3.0,-2.0))

        val testmapObject = MapObject(
            type = 1, // 1 for line
            title = "Test Title",
            desc = "Test Desc",
            geopoints = testGeopoints
        )
        //act
        testmapObject.draw(mockMap, true)
        //assert
        val polyline = overlayList.find { it is Polyline } as Polyline
        assert(polyline.actualPoints.size == 2)
        assert(polyline.title == "Test Title")
        assert(polyline.snippet == "Test Desc")
    }

    @Test
    fun draw_poly_marker(){
        //arrange
        val testGeopoints = mutableListOf(MapObject.GeoPointData(2.0,-1.0), MapObject.GeoPointData(3.0,-2.0),MapObject.GeoPointData(3.0,-3.0),MapObject.GeoPointData(4.0,-2.0))

        val testmapObject = MapObject(
            type = 2, // 2 for polygon
            title = "Test Title",
            desc = "Test Desc",
            geopoints = testGeopoints
        )
        //act
        testmapObject.draw(mockMap, true)
        //assert
        val polygon = overlayList.find { it is Polygon } as Polygon
        assert(polygon.actualPoints.size == 4)
        assert(polygon.title == "Test Title")
        assert(polygon.snippet == "Test Desc")
    }
}