package com.example.fyp_prototype

import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class LocationMarker(userdata: AppData,databaseRef: DatabaseReference) {

    val userdata = userdata
    val databaseRef = databaseRef

    // gets all the users in the database and marks them on the map
    fun updateLocations(mapView: MapView, user: user, S_ID: String) {
        databaseRef.child(S_ID).child("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) { //called when data is changed
                    val userData = snapshot.children.map { it.getValue(user::class.java) }
                    handleUserupdates(user,userData,mapView, updateAppData = { updateUser ->
                        userdata.updateAppData(
                            updateUser.userId,
                            userdata.Session_ID.value.toString(),
                            updateUser.role,
                            updateUser.team,
                            updateUser.status)

                    })
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
        }

    fun handleUserupdates(targetUser: user, users: List<user?>,mapView: MapView,updateAppData: (user) -> Unit){
        for (user in users){
            try {
                if (user != null){
                    if (user.userId != targetUser.userId && user.team == targetUser.team || targetUser.role == "Admin") { //only show location if you are an admin or they are on your team
                        updatemarker(mapView, user)
                    }
                    if(user.userId == targetUser.userId){
                        updateAppData(user)
                    }
                }
            }catch (e: Exception) {
            }
        }
    }

    // marks a user on the map
    fun updatemarker(mapView: MapView, user: user) {
        val marker = userdata.userMarkers.getOrPut(user.userId) {
            Marker(mapView).apply { // create map marker for each user
                val teamMarkers = when (user.team) { // set user colour
                    "Red" -> R.drawable.redplayermarker
                    "Blue" -> R.drawable.blueplayermarker
                    else -> R.drawable.greenplayermarker
                }
                val playerMarker = ContextCompat.getDrawable(mapView.context,teamMarkers) //create player marker
                playerMarker?.let {
                    it.setBounds(0,0,it.intrinsicWidth,it.intrinsicHeight)
                    icon = it
                }
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER) //set marker to draw with centre on centre
                mapView.overlays.add(this) // add to map
            }
        }

        if (user.status != "Nominal") { // if user is not ok

            val helpkey = "${user.userId}_help"
            val helpmarker =userdata.userMarkers.getOrPut(helpkey) {
                Marker(mapView).apply { // set warn marker icon
                    val warnMarkers = when (user.status) {
                        "Help_Needed" -> R.drawable.help
                        "Critical" -> R.drawable.emergency
                        else -> R.drawable.help
                    }
                    val warnMarker = ContextCompat.getDrawable(mapView.context,warnMarkers) //create warn marker
                    warnMarker?.let {
                        it.setBounds(0,0,it.intrinsicWidth,it.intrinsicHeight)
                        icon = it
                    }

                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) // set marker to draw bottom on centre
                    mapView.overlays.add(this) // add to map
                }
            }

            //tooltip for emergency marker
            helpmarker.position = GeoPoint(user.location.latitude, user.location.longitude)
            helpmarker.title = "User: ${user.username}"
            helpmarker.snippet = "Team: ${user.team}\nRole: ${user.role}"
        }

        //tooltip for player marker
        marker.position = GeoPoint(user.location.latitude, user.location.longitude)
        marker.title = "User: ${user.username}"
        marker.snippet = "Team: ${user.team}\nRole: ${user.role}"
    }
}