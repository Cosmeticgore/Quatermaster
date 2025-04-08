package com.example.fyp_prototype

import io.mockk.verify
import com.google.firebase.database.DatabaseReference
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Before
import org.junit.Test
import org.osmdroid.views.MapView


class LocationMarker_UT {

    private lateinit var locationMarker: LocationMarker
    private val mockUserData: AppData = mockk()
    private val mockMap: MapView = mockk()

    @Before
    fun setUp() {
        locationMarker = LocationMarker(mockUserData, mockk())
    }

    @Test
    fun updates_data(){
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

    @Test
    fun updates_team(){
        //arrange
        val currentUser = user(userId = "UID_123", team = "Red", role = "Player")
        val users = listOf(
            user(userId = "UID_123", team = "Red", role = "Player"),
            user(userId = "UID_125", team = "Red", role = "Player"),
            user(userId = "UID_125", team = "Blue", role = "Player")
        )

        val susLocationMarker = spyk(locationMarker)
        every { susLocationMarker.updatemarker(any(), any()) } just Runs
        //act
        susLocationMarker.handleUserupdates(
            currentUser,
            users,
            mockMap,
            updateAppData = {}
        )
        //assert
        verify(exactly = 1) {
            susLocationMarker.updatemarker(mockMap, users[1])
        }
    }

    @Test
    fun admin_sees_all(){
        //arrange
        val currentUser = user(userId = "UID_123", team = "Red", role = "Admin")
        val users = listOf(
            user(userId = "UID_123", team = "Red", role = "Admin"),
            user(userId = "UID_125", team = "Red", role = "Player"),
            user(userId = "UID_125", team = "Blue", role = "Player")
        )

        val susLocationMarker = spyk(locationMarker)
        every { susLocationMarker.updatemarker(any(), any()) } just Runs
        //act
        susLocationMarker.handleUserupdates(
            currentUser,
            users,
            mockMap,
            updateAppData = {}
        )
        //assert
        verify { susLocationMarker.updatemarker(mockMap, users[1]) }
        verify { susLocationMarker.updatemarker(mockMap, users[2]) }
    }
}