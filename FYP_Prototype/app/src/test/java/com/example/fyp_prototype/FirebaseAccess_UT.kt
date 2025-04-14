package com.example.fyp_prototype

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class FirebaseAccess_UT {

    private lateinit var firebaseAccess: FirebaseAccess

    @Before
    fun setup() {
        firebaseAccess = FirebaseAccess()
    }

    @Test
    fun firebaseaccess_get_from_reference_callback(){
        //arrange
        val mockRef = mockk<DatabaseReference>(relaxed = true)
        val mockSnap = mockk<DataSnapshot>()
        val listenerSlot = slot<ValueEventListener>()
        var result: DataSnapshot? = null

        every { mockRef.addListenerForSingleValueEvent(capture(listenerSlot)) } answers {}

        //Act
        firebaseAccess.get_from_reference(mockRef){
            result = it
        }
        listenerSlot.captured.onDataChange(mockSnap)

        // assert
        assert(result === mockSnap)

    }

    @Test
    fun firebaseaccess_set_from_reference_callback(){
        val mockRef = mockk<DatabaseReference>(relaxed = true)
        val mockTask = mockk<Task<Void>>(relaxed = true)
        val succSlot = slot<OnSuccessListener<Void>>()
        val onSucc = mockk<() -> Unit>(relaxed = true)
        val onFail = mockk<(Exception) -> Unit>(relaxed = true)

        every {mockRef.setValue(any()) } returns mockTask
        every {mockTask.addOnSuccessListener(capture(succSlot)) } returns mockTask
        //Act
        firebaseAccess.set_from_reference(mockRef,onSucc,"BORGER",onFail)
        succSlot.captured.onSuccess(null)
        // assert
        verify{ onSucc.invoke()}
        verify(exactly = 0){onFail.invoke(any())}
    }

    @Test
    fun firebaseaccess_set_from_reference_callback_fail(){
        val mockRef = mockk<DatabaseReference>(relaxed = true)
        val mockTask = mockk<Task<Void>>(relaxed = true)
        val excep = Exception("Failed")
        val succSlot = slot<OnSuccessListener<Void>>()
        val failSlot = slot<OnFailureListener>()
        val onSucc = mockk<() -> Unit>(relaxed = true)
        val onFail = mockk<(Exception) -> Unit>(relaxed = true)

        every {mockRef.setValue(any()) } returns mockTask
        every {mockTask.addOnSuccessListener(capture(succSlot)) } returns mockTask
        every {mockTask.addOnFailureListener(capture(failSlot)) } returns mockTask
        //Act
        firebaseAccess.set_from_reference(mockRef,onSucc,"BORGER",onFail)
        failSlot.captured.onFailure(excep)
        // assert
        verify{ onFail.invoke(excep)}
        verify(exactly = 0){onSucc.invoke()}
    }

}