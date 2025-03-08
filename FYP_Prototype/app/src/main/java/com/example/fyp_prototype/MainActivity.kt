package com.example.fyp_prototype

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import com.google.firebase.messaging.FirebaseMessaging
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
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf


class MainActivity : ComponentActivity() {
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private val database = Firebase.database
    private val databaseRef = database.getReference("sessions")
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val userMarkers = mutableMapOf<String, Marker>()
    private lateinit var userdata: AppData
    private val functions: FirebaseFunctions = Firebase.functions


    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request necessary permissions for locations etc
        requestPermissions()
        // Initialize OSMDroid configuration *important so i dont get banned of OSM*
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = "AirsoftAPP"
        userdata = AppData.getInstance(application)

        val Session_ID = userdata.Session_ID.value
        val User_ID = userdata.user_ID.value
        val role = userdata.Role.value

        val User = user(
            userId = User_ID.toString(),
            location = user_loc(0.0, 0.0),
            role = role.toString()
        )
        Intent(applicationContext, LocationService::class.java).apply {
            action = LocationService.ACTION_START
            startService(this)
        }
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("Token Creation", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d("FCM TOKEN", token)
            databaseRef.child(Session_ID.toString()).child("users").child(User_ID.toString())
                .child("not_token").setValue(token.toString()).addOnSuccessListener {
                    Log.d("FCM TOKEN", "Token Updated")
                }.addOnFailureListener {
                    Log.d("FCM TOKEN", "Failed to upload token")
                }

        })
        changeteam(userdata, this)

        //UI
        setContent {
            App(User, Session_ID.toString())
        }
    }

    //FUNCTIONS

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun requestPermissions() { // put needed permissions here
        val permissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_LOCATION
        )

        for (permission in permissions) { // check to see if they are granted
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    REQUEST_PERMISSIONS_REQUEST_CODE
                )
                break
            }
        }
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
                                if (userData.userId != user.userId && userData.team == user.team || user.role == "Admin") { //only show location if you are an admin or they are on your team
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

    private fun ping(sessionId: String): Task<String> {
        // Create a HashMap with the correct structure
        val data = hashMapOf(
            "sessionId" to sessionId,  // Make sure sessionId is not empty
            "title" to "ping",
            "message" to "ping"
        )

        // Log what you're sending
        Log.d("FCM_CALL", "Sending data: $data")

        return functions.getHttpsCallable("sendAdminNotifications")
            .call(data)
            .continueWith { task ->
                if (task.isSuccessful) {
                    val result = task.result?.data
                    Log.d("FCM_CALL", "Success: $result")
                    "Success: Notification sent"
                } else {
                    val exception = task.exception
                    Log.e("FCM_CALL", "Error: ${exception?.message}")
                    "Error: ${exception?.message}"
                }
            }
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

    private fun changeteam(User: AppData, context: Context) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle("Select Team")
            .setNegativeButton("RED") { dialog, which ->
                User.update_team("Red")
            }
            .setPositiveButton("BLUE") { dialog, which ->
                User.update_team("Blue")
            }
            .setNeutralButton("NONE") { dialog, which ->
                User.update_team("None")
            }
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }


    //COMPOSABLE FUNCTIONS


    @Composable
    fun OsmdroidMapView(User: user, S_ID: String) { // view that displays map
        val context = LocalContext.current
        val locationProvider = GpsMyLocationProvider(context)
        val mapView = remember { MapView(context) }

        val locationOverlay = remember { // needed for location to work
            MyLocationNewOverlay(locationProvider, mapView).apply {
                enableMyLocation()
            }
        }

        DisposableEffect(Unit) {
            val job = scope.launch { // co routine to have these tasks run with the rest of the app
                while (isActive) { //gets the location off the location provider *TODO SWITCH THIS TO USING A FUSED LOCATION PROVIDER*
                    if (locationProvider.lastKnownLocation != null) {
                        User.location.latitude = locationProvider.lastKnownLocation.latitude
                        User.location.longitude = locationProvider.lastKnownLocation.longitude
                        updateLocations(
                            mapView,
                            User,
                            S_ID
                        ) // gets the other users location from the database and updates them and marks them
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

    @Composable
    private fun App(User: user, Session_ID: String) {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "map") {
            composable("map") {
                MapHome(User, Session_ID, navController)
            }
            composable("info") {
                InfoScreen(navController, Session_ID)
            }
            composable("players") {
                PlayersListScreen(navController, Session_ID)
            }
        }

    }

    @Composable
    private fun MapHome(User: user, Session_ID: String, navController: NavController) {
        Box(modifier = Modifier.fillMaxSize()) {
            OsmdroidMapView(User, Session_ID)

            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomStart)
            ) {
                Button(
                    onClick = { ping(Session_ID) },
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
            Button(
                onClick = { navController.navigate("info") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.8f)
                )
            ) {
                Text(
                    text = "Info",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

        }
    }

    @Composable
    private fun InfoScreen(navController: NavController, Session_ID: String) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Session ID: $Session_ID",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Game Brief",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Lorem Ipsum",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = { navController.navigate("players") },
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.8f)
                )
            ) {
                Text(
                    text = "View Session Players",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = { navController.navigateUp() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.8f)
                )
            ) {
                Text(
                    text = "Back to Map",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    @Composable
    private fun PlayersListScreen(navController: NavController, SID: String) {
        val users = remember { mutableStateOf<List<user>>(emptyList()) }
        val database = FirebaseDatabase.getInstance()
        val databaseRef = database.getReference("sessions")


        LaunchedEffect(SID) {
            val tempusers = mutableListOf<user>()
            databaseRef.child(SID).child("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) { //called when data is changed
                    for (userSnapshot in snapshot.children) {
                        try {
                            val userData = userSnapshot.getValue(user::class.java)
                            if (userData != null) {
                                tempusers.add(
                                    user(
                                        userData.userId,
                                        user_loc(
                                            userData.location.longitude,
                                            userData.location.latitude
                                        ),
                                        userData.team,
                                        userData.role,
                                        userData.status,
                                        ""
                                    )
                                )
                                Log.i("PlayerList", "Added User ${userData.userId}")

                            } else { //error handling
                                Log.e("FirebaseDebug", "UserData is null")
                            }
                        } catch (e: Exception) {
                            Log.e("FirebaseDebug", "Error deserializing UserData", e)
                        }
                    }
                    users.value = tempusers
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Database query failed: ${error.message}")
                }

            })}



        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Session Players",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { items(users.value) { user ->
                    Log.i("PlayerList", "Displaying User")
                    PlayerListItem(user)
                    Divider()
                }

                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigateUp() },
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.8f),
                )
            ) {
                Text(
                    text = "Back",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    @Composable
    private fun PlayerListItem(user: user) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = "User ID: ${user.userId}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Role: ${user.role}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Status: ${user.status}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Team: ${user.team}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

        }
    }
}














