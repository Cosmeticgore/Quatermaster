package com.example.fyp_prototype

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.MutableLiveData
import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.google.firebase.database.DataSnapshot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import junit.framework.TestCase.assertEquals


@RunWith(AndroidJUnit4::class)
class Join_Session_IT {
    @get:Rule
    val composeTestRule = createComposeRule()

    var mockFirebaseAccess = mockk<FirebaseAccess>() // I HATE FINAL CLASSES RAHHHH
    var mockContext: Context = mockk()
    var mockuserData: AppData = mockk()

    @Test
    fun Join_Session_Input_Visable_nocode() {
        //arrange
        val snackbarHostState = SnackbarHostState()
        composeTestRule.setContent {
            Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { paddingValues ->
                Box(// make sure everything aligns
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Join_session(
                        userdata = mockuserData,
                        firebaseAccess = mockFirebaseAccess,
                        onSucc = { _, _ -> },
                        snackbar = snackbarHostState
                    )
                }
            }
        }
        //act

        composeTestRule.onNodeWithText("Join Session").performClick()

        //assert

        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Code must be 6 digits long")
                .fetchSemanticsNodes().isNotEmpty()
        }

        val inputField = composeTestRule.onNodeWithText("Enter 6-digit code")
        assertTrue(inputField.isDisplayed())

        composeTestRule.onNodeWithText("Code must be 6 digits long").assertExists()
    }

    @Test
    fun Join_Session_Input_Visable_wrongcode() {

        //arrange
        val callbackCap = slot<(DataSnapshot) -> Unit>()
        val mockID = MutableLiveData("UID_123")
        val mockUsername = MutableLiveData("Kozi")
        val snackbarHostState = SnackbarHostState()

        every { mockuserData.user_ID } returns mockID
        every { mockuserData.Username } returns mockUsername

        every { mockFirebaseAccess.get_from_reference(any(), capture(callbackCap)) } answers {
            val fakeSnap = mockk<DataSnapshot> {
                every { exists() } returns false
            }
            callbackCap.captured.invoke(fakeSnap)
        }


        composeTestRule.setContent {
            Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { paddingValues ->
                Box(// make sure everything aligns
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Join_session(
                        userdata = mockuserData,
                        firebaseAccess = mockFirebaseAccess,
                        onSucc = { _, _ -> },
                        snackbar = snackbarHostState
                    )
                }
            }
        }
        //act

        composeTestRule.onNodeWithText("Enter 6-digit code").performTextInput("123456")

        composeTestRule.onNodeWithText("Join Session").performClick()

        //assert
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Failed to Join Session - Code Invalid")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Failed to Join Session - Code Invalid").assertExists()
    }

    @Test
    fun Join_Session_Input_Visable_Rightcode() {

        //arrange

        val callbackCap = slot<(DataSnapshot) -> Unit>()
        val mockID = MutableLiveData("UID_123")
        val mockUsername = MutableLiveData("Kozi")
        val codeCaptor = slot<String>()
        val userCaptor = slot<user>()
        var SuccCalled = false

        every { mockuserData.user_ID } returns mockID
        every { mockuserData.Username } returns mockUsername

        every { mockFirebaseAccess.get_from_reference(any(), capture(callbackCap)) } answers {
            val fakeSnap = mockk<DataSnapshot> {
                every { exists() } returns true
            }
            callbackCap.captured.invoke(fakeSnap)
        }

        val onSucc = slot<()-> Unit>()
        every{
            mockFirebaseAccess.set_from_reference(any(), capture(onSucc), any(), any())
        } answers {
            onSucc.captured.invoke()
        }

        val snackbarHostState = SnackbarHostState()
        composeTestRule.setContent {
            Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { paddingValues ->
                Box(// make sure everything aligns
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Join_session(
                        userdata = mockuserData,
                        firebaseAccess = mockFirebaseAccess,
                        onSucc = { user, input_code ->
                            userCaptor.captured = user
                            codeCaptor.captured = input_code
                            SuccCalled = true
                        },
                        snackbar = snackbarHostState
                    )
                }
            }
        }

        //act

        composeTestRule.onNodeWithText("Enter 6-digit code").performTextInput("123456")

        composeTestRule.onNodeWithText("Join Session").performClick()

        composeTestRule.waitUntil(5000) {
            SuccCalled
        }
        //assert

        assertTrue(SuccCalled)
        assertEquals("UID_123",userCaptor.captured.userId)
        assertEquals("Kozi",userCaptor.captured.username)
        assertEquals("123456", codeCaptor.captured)
    }
}