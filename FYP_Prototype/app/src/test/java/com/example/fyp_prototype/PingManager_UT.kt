package com.example.fyp_prototype

import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.every
import io.mockk.mockk
import com.google.android.gms.tasks.Task
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import org.jetbrains.annotations.TestOnly
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.Continuation

class PingManager_UT {

    private val mockAppData: AppData = mockk(relaxed = true)
    private val mockFirebaseFunction: FirebaseFunctions = mockk()
    private lateinit var pingManager: PingManager
    private val mockCallable: HttpsCallableReference = mockk()
    private val mockResult: HttpsCallableResult = mockk()
    private val mockTask: Task<HttpsCallableResult> = mockk()

    @Before
    fun setup(){
        // setup https mocking
        pingManager = PingManager(mockAppData,mockFirebaseFunction)
        every { mockFirebaseFunction.getHttpsCallable("sendAdminNotifications") } returns mockCallable
        every { mockCallable.call(any()) } returns mockTask
        every { mockTask.isSuccessful } returns true
        every { mockTask.continueWith<String>(any()) } returns Tasks.forResult("Success: Notification sent")
        every { mockTask.result } returns mockResult
    }

    @Test
    fun ping_test_update_user(){
        //act
        val resultTask = pingManager.ping("SID_123").result
        //assert
        verify{ mockAppData.update_status("Help_Needed")}
        assertEquals("Success: Notification sent",resultTask)
    }

    @Test
    fun ping_urgent_test_update_user(){
        //act
        val resultTask = pingManager.urgentping("SID_123").result

        //assert
        verify{ mockAppData.update_status("Critical")}
        assertEquals("Success: Notification sent",resultTask)
    }
}