package com.example.fyp_prototype

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FirebaseAccess() {
    // read from provided reference
    fun get_from_reference(ref: DatabaseReference, callback: (DataSnapshot) -> Unit){
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callback(snapshot) // send back snapshot
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
    // write to provided reference
    fun set_from_reference(ref: DatabaseReference, onSucc: () -> Unit, set: Any? ,onFail: (Exception) -> Unit){
        ref.setValue(set).addOnSuccessListener{
            onSucc() // called when write is successful
        }
            .addOnFailureListener{
                e -> onFail(e) //otherwise send an exception
            }
    }
}