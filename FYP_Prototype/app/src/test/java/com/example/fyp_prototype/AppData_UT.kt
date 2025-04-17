package com.example.fyp_prototype

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppData_UT {

    private lateinit var appData: AppData
    private val mockApp: Application = mockk(relaxed = true)
    var mockAccess = mockk<FirebaseAccess>(relaxed = true)
    val mockRef = mockk<DatabaseReference>(relaxed = true)
    val mockDatabase: FirebaseDatabase =mockk()

    @Before
    fun setup(){
        mockkStatic(FirebaseDatabase::class)
        appData = AppData.getInstance(mockApp)
        appData.FirebaseAccess = mockAccess
        every { FirebaseDatabase.getInstance() } returns mockDatabase
        every { mockRef.child(any()) } returns mockRef
        every { mockDatabase.getReference(any()) } returns mockRef
    }

    @Test
    fun updateAppData(){
        //act
        appData.updateAppData("123","321","Admin","Red","Nominal")
        //assert
        assertEquals("123", appData.user_ID.value)
        assertEquals("321", appData.Session_ID.value)
        assertEquals("Admin", appData.Role.value)
        assertEquals("Red", appData.Team.value)
        assertEquals("Nominal", appData.Status.value)
    }

    @Test
    fun reset_data(){
        //arrange
        appData.updateAppData("123","321","Admin","Red","Nominal")
        //act
        appData.reset_data()
        //assert
        assertEquals("", appData.user_ID.value)
        assertEquals("", appData.Session_ID.value)
        assertEquals("Player", appData.Role.value)
        assertEquals("Not Set", appData.Team.value)
        assertEquals("Nominal", appData.Status.value)
        assertEquals("", appData.Username.value)
        assertNull(appData.Cur_Site.value)
        assertNull(appData.Cur_Game.value)

    }

    @Test
    fun update_team(){
        //act
        appData.update_team("Red")
        //assert
        verify {
            mockAccess.set_from_reference(
                ref = mockRef,
                onSucc = any(),
                set = "Red",
                onFail = any()
            )
        }
        assertEquals("Red", appData.Team.value)
    }

    @Test
    fun update_status(){
        //act
        appData.update_status("I NEED A MEDIC BAG")
        //assert
        verify {
            mockAccess.set_from_reference(
                ref = mockRef,
                onSucc = any(),
                set = "I NEED A MEDIC BAG",
                onFail = any()
            )
        }
        assertEquals("I NEED A MEDIC BAG", appData.Status.value)
    }

    @Test
    fun appdata_singleton(){
        //act
        val testinstance = AppData.getInstance(mockApp)
        testinstance.Team.value = "blah"
        //assert
        assertEquals(appData,testinstance)
        assertEquals(appData.Team.value, "blah")
    }
}