package com.example.fyp_prototype

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.navigation.NavType
import androidx.navigation.navArgument
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import androidx.compose.material3.AlertDialog


class MainActivity : ComponentActivity() {
    private val database = Firebase.database
    private val databaseRef = database.getReference("sessions")
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val userMarkers = mutableMapOf<String, Marker>()
    private lateinit var userdata: AppData
    private val functions: FirebaseFunctions = Firebase.functions


    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            location = locat(0.0, 0.0),
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

                                if(userData.userId == user.userId){
                                    userdata.updateAppData(userData.userId,
                                        userdata.Session_ID.value.toString(),
                                        userData.role,
                                        userData.team,
                                        userData.status)
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

    fun ping(sessionId: String): Task<String> {
        val data = hashMapOf(
            "sessionId" to sessionId,
            "title" to "Player Needs Help",
            "message" to "Non Urgent Help wanted"
        )

        Log.d("FCM_CALL", "Sending data: $data")

        userdata.update_status("Help_Needed")

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

    fun urgentping(sessionId: String): Task<String> {
        val data = hashMapOf(
            "sessionId" to sessionId,
            "title" to "Player is in Danger!",
            "message" to "Player is in urgent need of help!"
        )

        Log.d("FCM_CALL", "Sending data: $data")

        userdata.update_status("Critical")

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
                val teamMarkers = when (user.team) {
                    "Red" -> R.drawable.redplayermarker
                    "Blue" -> R.drawable.blueplayermarker
                    else -> R.drawable.greenplayermarker
                }

                val customMarker = ContextCompat.getDrawable(mapView.context,teamMarkers)

                customMarker?.let {
                    it.setBounds(0,0,it.intrinsicWidth,it.intrinsicHeight)
                    icon = it
                }

                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                mapView.overlays.add(this)
            }
        }

        if (user.status != "Nominal") {

            val helpkey = "${user.userId}_help"
            val helpmarker = userMarkers.getOrPut(helpkey) {
                Marker(mapView).apply {
                    val warnMarkers = when (user.status) {
                        "Help_Needed" -> R.drawable.help
                        "Critical" -> R.drawable.emergency
                        else -> R.drawable.help
                    }

                    val customMarker = ContextCompat.getDrawable(mapView.context,warnMarkers)

                    customMarker?.let {
                        it.setBounds(0,0,it.intrinsicWidth,it.intrinsicHeight)
                        icon = it
                    }

                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mapView.overlays.add(this)
                }
            }

            helpmarker.position = GeoPoint(user.location.latitude, user.location.longitude)
            helpmarker.title = "User: ${user.userId}"
            helpmarker.snippet = "Team: ${user.team}\nRole: ${user.role}"
        }

        marker.position = GeoPoint(user.location.latitude, user.location.longitude)
        marker.title = "User: ${user.userId}"
        marker.snippet = "Team: ${user.team}\nRole: ${user.role}"
    }

    private fun changeteam(User: AppData, context: Context) {

        if (User.Team.value == "Red" || User.Team.value =="Blue" || User.Team.value == "None"){

        }else{
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

    }


    //COMPOSABLE FUNCTIONS

    @Composable
    fun OsmdroidMapView(User: user, S_ID: String) { // view that displays map
        val context = LocalContext.current
        val locationProvider = GpsMyLocationProvider(context)
        val mapView = remember { MapView(context) }
        val initialLocationSet = remember { mutableStateOf(false) }

        val locationOverlay = remember { // needed for location to work
            MyLocationNewOverlay(locationProvider, mapView).apply {
                enableMyLocation()
            }
        }

        val compassOverlay = remember {
            CompassOverlay(context, InternalCompassOrientationProvider(context), mapView).apply {
                enableCompass()
            }
        }

        DisposableEffect(Unit) {
            val job = scope.launch { // co routine to have these tasks run with the rest of the app
                while (isActive) { //gets the location off the location provider *TODO SWITCH THIS TO USING A FUSED LOCATION PROVIDER*
                    if (locationProvider.lastKnownLocation != null) {
                        User.location.latitude = locationProvider.lastKnownLocation.latitude
                        User.location.longitude = locationProvider.lastKnownLocation.longitude

                        if (!initialLocationSet.value) {
                            mapView.controller.setCenter(
                                GeoPoint(User.location.latitude, User.location.longitude)
                            )
                            initialLocationSet.value = true
                        }

                        updateLocations(
                            mapView,
                            User,
                            S_ID
                        ) // gets the other users location from the database and updates them and marks them
                    }
                    if (!initialLocationSet.value) {
                        delay(1000)
                    }else{
                        delay(15000) // delay of 15 seconds between updates
                    }
                }
            }

            onDispose { //cleanup
                job.cancel()
                locationOverlay.disableMyLocation()
                compassOverlay.disableCompass()
                userMarkers.clear()
            }
        }

        AndroidView( // this is the map
            modifier = Modifier.fillMaxSize(),//sets the view to cover the entire screen
            factory = {
                mapView.apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    minZoomLevel = 17.0
                    maxZoomLevel = 20.0
                    controller.setZoom(18.0)
                    setBuiltInZoomControls(true)
                    setMultiTouchControls(true)
                    overlays.add(locationOverlay)
                    overlays.add(compassOverlay)
                }
            },
            update = { view ->
                userdata.Cur_Site.value?.drawMarkers(mapView)
                userdata.Cur_Game.value?.drawMarkers(mapView)
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
            composable("selectSite"){
                site_view_screen(navController, userdata, false)
            }
            composable(
                route = "games_list/{Site_ID}",
                arguments = listOf(navArgument("Site_ID") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                games_list(navController,userdata, false)
            }
        }

    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun MapHome(User: user, Session_ID: String, navController: NavController) {
        var admin : Boolean
        var showDialog by remember { mutableStateOf(false) }
        updateAppdate(userdata)
        if (userdata.Role.value == "Admin"){
            admin = true
        } else{
            admin = false
        }
        Scaffold(
            topBar = {
                gametopbar(
                    Button1Click = { navController.navigate("info") },
                    Button2Click = {},
                    pingclick = { ping(Session_ID) },
                    urgentpingclick = { urgentping(Session_ID) },
                    Tab = "Map",
                    navController = navController,
                    sessionClick = {showDialog = true},
                    Admin = admin
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                OsmdroidMapView(User, Session_ID)
                if (showDialog == true){
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("Session ID:") },
                        text = {
                            Text(userdata.Session_ID.value.toString())
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showDialog = false }) {
                                Text("Close")
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun InfoScreen(navController: NavController, Session_ID: String) {
        var admin : Boolean
        updateAppdate(userdata)
        var showDialog by remember { mutableStateOf(false) }
        if (userdata.Role.value == "Admin"){
            admin = true
        } else{
            admin = false
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                gametopbar(
                    Button1Click = {},
                    Button2Click = {navController.navigate("map") },
                    pingclick = { ping(Session_ID) },
                    urgentpingclick = { urgentping(Session_ID) },
                    Tab = "Info",
                    navController = navController,
                    sessionClick = {showDialog = true},
                    Admin = admin
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Current Site: ${userdata.Cur_Site.value?.name}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${userdata.Cur_Game.value?.name}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Divider()

                        Box(
                            modifier = Modifier
                                .weight(3f)
                                .fillMaxWidth()
                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            val scrollState = rememberScrollState()
                            Column {
                                Text(
                                    text = "${userdata.Cur_Game.value?.desc}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .verticalScroll(scrollState)
                                        .fillMaxWidth()
                                )
                                Divider()
                                Text(
                                    text = "${userdata.Cur_Site.value?.brief}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .verticalScroll(scrollState)
                                        .fillMaxWidth()
                                )
                            }


                            if (scrollState.canScrollForward) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(24.dp)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
                                                ),
                                                startY = 0f,
                                                endY = 24f
                                            )
                                        )
                                )
                            }
                            if (showDialog == true){
                                AlertDialog(
                                    onDismissRequest = { showDialog = false },
                                    title = { Text("Session ID:") },
                                    text = { Text(userdata.Session_ID.value.toString()) },
                                    confirmButton = {},
                                    dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Close") } }
                                )
                            }
                        }
                    }
                }
            }
    }}

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
                                        locat(
                                            userData.location.longitude,
                                            userData.location.latitude
                                        ),
                                        userData.team,
                                        userData.role,
                                        userData.status,
                                        username = userData.username
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
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                topbarwithtext("Players in Session", onBackClick = {navController.navigateUp()})
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) { items(users.value) { user ->
                    Log.i("PlayerList", "Displaying User")
                    PlayerListItem(user)
                    Divider()
                }

                }
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
                text = "Callsign: ${user.username}",
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
                if(userdata.Role.value == "Admin"){
                    PlayerListDropdown(user)
                }
            }
        }
    }

    @Composable
    private fun PlayerListDropdown(user: user) {
        var expanded by remember { mutableStateOf(false) }
        var showTeamDialog by remember { mutableStateOf(false) }
        var showRoleDialog by remember { mutableStateOf(false) }

        val database = FirebaseDatabase.getInstance()
        val databaseRef = database.getReference("sessions")

        val sessionId = userdata.Session_ID.value.toString()

        Box(
            modifier = Modifier.padding(16.dp)
        ){
            IconButton(onClick = { expanded = !expanded }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Set Role") },
                    onClick = {
                        showRoleDialog = true
                        expanded = false
                    }
                )

                DropdownMenuItem(
                    text = { Text("Set Team") },
                    onClick = {
                        showTeamDialog = true
                        expanded = false
                    }
                )

                DropdownMenuItem(
                    text = { Text("Reset Status") },
                    onClick = {
                        databaseRef.child(sessionId).child("users").child(user.userId)
                            .child("status").setValue("Nominal")
                        expanded = false
                    }
                )

                DropdownMenuItem(
                    text = { Text("Kick Player") },
                    onClick = {
                        databaseRef.child(sessionId).child("users").child(user.userId)
                            .removeValue()
                        expanded = false
                    }
                )
            }
        }

        if (showTeamDialog) {
            AlertDialog(
                onDismissRequest = { showTeamDialog = false },
                title = { Text("Select Team") },
                text = {
                    Column {
                        Button(
                            onClick = {
                                databaseRef.child(sessionId).child("users").child(user.userId)
                                    .child("team").setValue("Red")
                                showTeamDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("RED")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                databaseRef.child(sessionId).child("users").child(user.userId)
                                    .child("team").setValue("Blue")
                                showTeamDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("BLUE")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                databaseRef.child(sessionId).child("users").child(user.userId)
                                    .child("team").setValue("None")
                                showTeamDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("NONE")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showTeamDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showRoleDialog) {
            AlertDialog(
                onDismissRequest = { showRoleDialog = false },
                title = { Text("Select Role") },
                text = {
                    Column {
                        Button(
                            onClick = {
                                databaseRef.child(sessionId).child("users").child(user.userId)
                                    .child("role").setValue("Admin")
                                showRoleDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Admin")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                databaseRef.child(sessionId).child("users").child(user.userId)
                                    .child("role").setValue("Player")
                                showRoleDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Player")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showRoleDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    private fun updateAppdate(userData: AppData){

        val database = Firebase.database
        val sessiondatabaseRef = database.getReference("sessions")
        val sitesdatabaseRef = database.getReference("sites")
        var GID = ""
        var STID = ""

        sessiondatabaseRef.child(userData.Session_ID.value.toString()).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val session = snapshot.getValue(session::class.java)
                GID = session?.gid ?: return
                STID = session.stid

                sitesdatabaseRef.child(STID).addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val site = snapshot.getValue(site::class.java)
                        userData.Cur_Site.value = site
                        val game = site?.games?.get(GID)
                        if (game != null) {
                            userData.Cur_Game.value = game
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FirebaseError", "Database query failed: ${error.message}")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Database query failed: ${error.message}")
            }
        })
    }
}