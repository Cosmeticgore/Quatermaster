package com.example.fyp_prototype

import com.google.firebase.database.DatabaseReference
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.osmdroid.views.MapView


class LocationMarker_UT {

    private lateinit var locationMarker: LocationMarker
    private val mockUserData: AppData = mock()
    private val mockMap: MapView = mock()

    @Before
    fun setUp() {
        locationMarker = LocationMarker(mockUserData, mock())
    }

    @Test fun updates_data(){
        //arrange
        val currentUser = user(userId = "UID_123", team = "Red", role = "Player")
        val users = listOf(
            user(userId = "UID_123", team = "Red", role = "Player"),
            user(userId = "UID_125", team = "Red", role = "Player")
        )
        var updatedUser: user? = null

        //act
        locationMarker.handleUserupdates(currentUser,users,mockMap, updateAppData = { updatedUser = it})

        //assert
        assert(updatedUser != null)
        assert(updatedUser?.userId == "UID_123")

    }

    @Test fun updates_team(){
        //arrange
        val currentUser = user(userId = "UID_123", team = "Red", role = "Player")
        val users = listOf(
            user(userId = "UID_123", team = "Red", role = "Player"),
            user(userId = "UID_125", team = "Red", role = "Player"),
            user(userId = "UID_125", team = "Blue", role = "Player")
        )

        val susLocationMarker = spy(locationMarker)
        doNothing().`when`(susLocationMarker).updatemarker(any(), any())
        //act
        susLocationMarker.handleUserupdates(
            currentUser,
            users,
            mockMap,
            updateAppData = {}
        )
        //assert
        verify(susLocationMarker).updatemarker(mockMap,users[1])
    }

    @Test fun admin_sees_all(){
        //arrange
        val currentUser = user(userId = "UID_123", team = "Red", role = "Admin")
        val users = listOf(
            user(userId = "UID_123", team = "Red", role = "Admin"),
            user(userId = "UID_125", team = "Red", role = "Player"),
            user(userId = "UID_125", team = "Blue", role = "Player")
        )

        val susLocationMarker = spy(locationMarker)
        doNothing().`when`(susLocationMarker).updatemarker(any(), any())
        //act
        susLocationMarker.handleUserupdates(
            currentUser,
            users,
            mockMap,
            updateAppData = {}
        )
        //assert
        verify(susLocationMarker).updatemarker(mockMap,users[1])
        verify(susLocationMarker).updatemarker(mockMap,users[2])
    }
}