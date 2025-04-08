package com.example.fyp_prototype

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FirebaseAccess(database: FirebaseDatabase) {

    fun get_from_reference(ref: DatabaseReference, callback: (DataSnapshot) -> Unit){
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callback(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    fun set_from_reference(ref: DatabaseReference, onSucc: () -> Unit, set: Any? ,onFail: (Exception) -> Unit){
        ref.setValue(set).addOnSuccessListener{
            onSucc()
        }
            .addOnFailureListener{
                e -> onFail(e)
            }
    }
}