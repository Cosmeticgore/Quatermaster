package com.example.fyp_prototype

import android.Manifest
import android.R
import android.app.Notification
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MainActivity : ComponentActivity() {
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private val database = Firebase.database
    private val databaseRef = database.getReference("sessions")
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val userMarkers = mutableMapOf<String,Marker>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request necessary permissions for locations etc
        requestPermissions()
        // Initialize OSMDroid configuration *important so i dont get banned of OSM*
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = "AirsoftAPP"

        val Session_ID = intent.getStringExtra("SESSION_ID")
        val User_ID = intent.getStringExtra("USER")
        val role = intent.getStringExtra("ROLE")

        val Notificationhandler = notificationhandler(this)

        val User = user(
            userId = User_ID.toString(),
            location = user_loc(0.0,0.0),
            role = role.toString()
        )



        setContent {
            Box(modifier = Modifier.fillMaxSize()) { // UI
                OsmdroidMapView(User,Session_ID.toString())

                // Session ID Box in top left corner
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text( //displays the session id for the user to share
                        text = "Session ID: $Session_ID",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Button(
                        onClick = { ping(Session_ID.toString()) },
                        modifier = Modifier
                            .background(
                                color = Color.White.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.8f)
                        )
                    ) {
                        Text(
                            text = "Ping",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    private fun requestPermissions() { // put needed permissions here
        val permissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.POST_NOTIFICATIONS
        )

        for (permission in permissions) { // check to see if they are granted
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS_REQUEST_CODE)
                break
            }
        }
    }

    @Composable
    fun OsmdroidMapView(User: user, S_ID : String) { // view that displays map
        val context = LocalContext.current
        val locationProvider = GpsMyLocationProvider(context)
        val mapView = remember { MapView(context) }

        val locationOverlay = remember { // needed for location to work
            MyLocationNewOverlay(locationProvider, mapView).apply {
                enableMyLocation()
            }
        }

        DisposableEffect(Unit) {
            val job = scope.launch(){ // co routine to have these tasks run with the rest of the app
                while(isActive){ //gets the location off the location provider *TODO SWITCH THIS TO USING A FUSED LOCATION PROVIDER*
                    if (locationProvider.lastKnownLocation != null){
                        User.location.latitude = locationProvider.lastKnownLocation.latitude
                        User.location.longitude = locationProvider.lastKnownLocation.longitude
                        updateMyLocation(User,User.location.latitude,User.location.longitude, S_ID) //Updates my location in the database
                        updateLocations(mapView, User, S_ID) // gets the other users location from the database and updates them and marks them
                    }
                    delay(15000) // delay of 15 seconds between updates
                }
            }

            onDispose { //cleanup
                job.cancel()
                locationOverlay.disableMyLocation()
                userMarkers.clear()
            }
        }

        AndroidView( // this is the map
            modifier = Modifier.fillMaxSize(),//sets the view to cover the entire screen
            factory = {
                mapView.apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    minZoomLevel = 3.0
                    maxZoomLevel = 20.0
                    controller.setZoom(10.0)
                    controller.setCenter(GeoPoint(48.8583, 2.2944))
                    setBuiltInZoomControls(true)
                    setMultiTouchControls(true)
                    overlays.add(locationOverlay)
                }
            },
            update = { view ->
                // Update logic if needed
            }
        )
    }

    // gets all the users in the database and marks them on the map
    private fun updateLocations(mapView: MapView, user: user, S_ID: String) {
        databaseRef.child(S_ID).child("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) { //called when data is changed
                    for (userSnapshot in snapshot.children) {
                        try {
                            val userData = userSnapshot.getValue(user::class.java)
                            if (userData != null) {
                                if(userData.userId != user.userId && userData.team == user.team || user.role == "Admin"){ //only show location if you are an admin or they are on your team
                                    updatemarker(mapView, userData)
                                }

                            } else { //error handling
                                Log.e("FirebaseDebug", "UserData is null")
                            }
                        } catch (e: Exception) {
                            Log.e("FirebaseDebug", "Error deserializing UserData", e)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Database query failed: ${error.message}")
                }
            })
    }

    private fun ping(S_ID: String){
        val ping = message()
        ping.message_con = "Ping"
        ping.title = "Ping"
        ping.type = 1
        databaseRef.child(S_ID).child("messages").setValue(ping)
    }

    // marks a user on the map
    private fun updatemarker(mapView: MapView, user: user) {
        val marker = userMarkers.getOrPut(user.userId) {
            Marker(mapView).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                mapView.overlays.add(this)
            }
        }

        marker.position = GeoPoint(user.location.latitude, user.location.longitude)
        marker.title = "User: ${user.userId}"
        marker.snippet = "Team: ${user.team}\nRole: ${user.role}"
    }

    // updates the users location in the database
    fun updateMyLocation(user_u: user, latitude: Double, longitude: Double, S_ID: String) {
        val curlocref = databaseRef.child(S_ID).child("users").child(user_u.userId).child("location")

        // map the location data
        val locationData = mapOf(
            "latitude" to latitude,
            "longitude" to longitude
        )
        // Update the location in Firebase
        curlocref.setValue(locationData)
            .addOnSuccessListener {
                Log.d("UpdateLocations", "Location updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("UpdateLocations", "Failed to update location", e)
            }
    }
}






