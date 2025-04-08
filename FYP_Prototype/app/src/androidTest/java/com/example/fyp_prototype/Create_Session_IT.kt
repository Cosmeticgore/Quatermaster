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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.MutableLiveData
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class Create_Session_IT {
    @get:Rule
    val composeTestRule = createComposeRule()

    var mockFirebaseAccess = mockk<FirebaseAccess>()
    var mockContext: Context = mockk()
    var mockuserData: AppData = mockk()

    @Test
    fun Create_Session_Fail(){

        //arrange

        val mockID = MutableLiveData("UID_123")
        val mockUsername = MutableLiveData("Kozi")
        val mockMessaging = mockk<FirebaseMessaging>()
        val mockTask = mockk<Task<String>>()
        val mockListener = slot<OnCompleteListener<String>>()
        val userCaptor = slot<user>()
        val sessionIdCaptor = slot<String>()
        var onSucc = false

        every { mockuserData.user_ID } returns mockID
        every { mockuserData.Username } returns mockUsername

        mockkStatic(Random::class)
        every { Random.nextInt(100000,999999) } returns 123456
        mockkStatic(FirebaseMessaging::class)

        val snackbarHostState = SnackbarHostState()

        every { FirebaseMessaging.getInstance() } returns mockMessaging
        every { mockMessaging.token } returns mockTask
        every {mockTask.addOnCompleteListener(capture(mockListener))} answers {
            val listen = mockListener.captured
            val succTask = mockk<Task<String>> {
                every { isSuccessful } returns true
                every { result } returns "fake-fcm-token"
                every { exception } returns null
            }
            listen.onComplete(succTask)
            mockTask
        }

        val onFailCaptor = slot<(Exception) -> Unit>()
        every {mockFirebaseAccess.set_from_reference(
            any(),
            any(),
            any<session>(),
            capture(onFailCaptor)
        )
        }answers {
            onFailCaptor.captured.invoke(mockk<Exception>())
        }

        composeTestRule.setContent {
            Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { paddingValues ->
                Box(// make sure everything aligns
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Create_session(
                        userdata = mockuserData,
                        FirebaseAccess = mockFirebaseAccess,
                        context = mockContext,
                        onSucc = { user, SID ->
                            userCaptor.captured = user
                            sessionIdCaptor.captured =SID
                            onSucc = true
                        },
                        snackbar = snackbarHostState
                    )
                }
            }
        }

        //act

        composeTestRule.onNodeWithText("Create Session").performClick()

        composeTestRule.waitUntil(5000){ try {
            composeTestRule.onNodeWithText("Failed to Create Session").isDisplayed()
            true
        } catch (_: Exception) {
            false
        } }

        //assert

        composeTestRule.onNodeWithText("Failed to Create Session").assertExists()

        assertFalse(onSucc)
    }

    @Test
    fun Create_Session_Success(){

        //arrange

        val mockID = MutableLiveData("UID_123")
        val mockUsername = MutableLiveData("Kozi")
        val mockMessaging = mockk<FirebaseMessaging>()
        val mockTask = mockk<Task<String>>()
        val mockListener = slot<OnCompleteListener<String>>()
        val userCaptor = slot<user>()
        val sessionIdCaptor = slot<String>()
        var onSucc = false

        every { mockuserData.user_ID } returns mockID
        every { mockuserData.Username } returns mockUsername

        mockkStatic(Random::class)
        every { Random.nextInt(100000,999999) } returns 123456
        mockkStatic(FirebaseMessaging::class)

        val snackbarHostState = SnackbarHostState()

        every { FirebaseMessaging.getInstance() } returns mockMessaging
        every { mockMessaging.token } returns mockTask
        every {mockTask.addOnCompleteListener(capture(mockListener))} answers {
            val listen = mockListener.captured
            val succTask = mockk<Task<String>> {
                every { isSuccessful } returns true
                every { result } returns "fake-fcm-token"
                every { exception } returns null
            }
            listen.onComplete(succTask)
            mockTask
        }

        val sessionCaptor = slot<session>()
        val onSuccCaptor = slot<() -> Unit>()
        every {mockFirebaseAccess.set_from_reference(any(), capture(onSuccCaptor), capture(sessionCaptor), any())
        }answers {
            onSuccCaptor.captured.invoke()
        }

        composeTestRule.setContent {
            Scaffold(snackbarHost = { SnackbarHost(hostState = snackbarHostState) }) { paddingValues ->
                Box(// make sure everything aligns
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Create_session(
                        userdata = mockuserData,
                        FirebaseAccess = mockFirebaseAccess,
                        context = mockContext,
                        onSucc = { user, SID ->
                            userCaptor.captured = user
                            sessionIdCaptor.captured =SID
                            onSucc = true
                        },
                        snackbar = snackbarHostState
                    )
                }
            }
        }

        //act

        composeTestRule.onNodeWithText("Create Session").performClick()

        composeTestRule.waitUntil(5000){ onSucc }

        //assert

        assertTrue(onSucc)

        assertEquals("123456", sessionCaptor.captured.session_Id)
        assertEquals(1, sessionCaptor.captured.users.size)
        assertTrue(sessionCaptor.captured.users.containsKey("UID_123"))

        val user = sessionCaptor.captured.users["UID_123"]
        assertNotNull(user)
        assertEquals("UID_123", user?.userId)
        assertEquals("Kozi", user?.username)
        assertEquals("Admin", user?.role)

        assertEquals("UID_123", userCaptor.captured.userId)
        assertEquals("Kozi", userCaptor.captured.username)
        assertEquals("Admin", userCaptor.captured.role)
        assertEquals("123456", sessionIdCaptor.captured)

    }
}