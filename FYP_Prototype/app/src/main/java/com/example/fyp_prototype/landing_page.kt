package com.example.fyp_prototype

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.fyp_prototype.MapObject.GeoPointData
import com.example.fyp_prototype.ui.theme.FYP_PrototypeTheme
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.messaging.FirebaseMessaging
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import kotlin.random.Random

//This is the Landing page, users start here when the app is opened

class landing_page : ComponentActivity() {
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    lateinit var userdata: AppData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userdata = AppData.getInstance(application)
        // Request necessary permissions for locations etc
        requestPermissions()

        setContent { // UI
            App(userdata)
        }
    }


    fun requestPermissions() { // put needed permissions here
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


    @Composable
    private fun App(userdata: AppData) { //nav map for the landing page
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "join_create") { //start on join create page
            composable("join_create") {
                join_create(userdata, navController)
            }
            composable("sites") { //sites list
                site_view_screen(navController, userdata, true)
            }
            composable( // games list
                route = "games_list/{Site_ID}",
                arguments = listOf(navArgument("Site_ID") { //pass the id if its in edit mode
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                games_list(navController, userdata, true)
            }
            composable(
                route = "games_designer/{mode}",
                arguments = listOf(navArgument("mode") { //pass the mode of the game designer either Game or Site
                    type = NavType.StringType
                })
            ){ backStackEntry ->
                val mode = backStackEntry.arguments?.getString("mode") ?: "default"
                Editor(navController, userdata,mode)
            }
        }
    }


    // this is the join session button, it has the logic for joining a session inside it
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Join_session(userdata: AppData) {
        var code_input by remember { mutableStateOf("") }
        val context = LocalContext.current
        val database = Firebase.database
        val databaseRef = database.getReference("sessions")
        val modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()

        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField( // code input field
                value = code_input,
                onValueChange = {
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        code_input = it
                    }
                },
                label = { Text("Enter 6-digit code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), //only takes numerical input
                modifier = modifier.padding(4.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = androidx.compose.ui.graphics.Color.White,
                ),
            )
            button_common("Join Session", onClick = {
                if (code_input.length == 6) {

                    databaseRef.child(code_input).get().addOnSuccessListener { snapshot ->

                        val newUser = user( //create a new user
                            userId = userdata.user_ID.value.toString(),
                            username = userdata.Username.value.toString()
                        )

                        if (snapshot.exists()) {
                            databaseRef.child(code_input).child("users").child(newUser.userId)
                                .setValue(newUser).addOnSuccessListener {
                                    val Intent = Intent(
                                        context,
                                        MainActivity::class.java
                                    ).apply { // pass values to the main activity
                                        userdata.updateAppData(
                                            newUser.userId,
                                            code_input,
                                            "Player",
                                            userdata.Team.value.toString(),
                                            userdata.Status.value.toString()
                                        )
                                    }
                                    context.startActivity(Intent) // move to main
                                }.addOnFailureListener { e -> // error handling
                                    val err_toast = Toast.makeText(
                                        context,
                                        "Failed to Join Session - Connection Issue",
                                        Toast.LENGTH_SHORT
                                    )
                                    err_toast.show()
                                }
                        } else {
                            val err_toast = Toast.makeText(
                                context,
                                "Failed to Join Session - Code Invalid",
                                Toast.LENGTH_SHORT
                            )
                            err_toast.show()
                        }
                    }
                } else {
                    val err_toast =
                        Toast.makeText(
                            context,
                            "Code must be 6 digits long",
                            Toast.LENGTH_SHORT
                        )
                    err_toast.show()
                }

            } )
        }
    }

    // this is the create session button, has all the logic inside
    @Composable
    private fun Create_session(userdata: AppData) {
        val database = Firebase.database
        val databaseRef = database.getReference("sessions")
        val context = LocalContext.current

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            button_common("Create Session",
                onClick = {
                    val id = Random.nextInt(100000, 999999).toString() //generates a session ID
                    val newUser = user( //creates a new user
                        userId = userdata.user_ID.value.toString(),
                        role = "Admin",
                        username = userdata.Username.value.toString()
                    )

                    val session = session( //creates the session
                        session_Id = id,
                        users = mapOf(userdata.user_ID.value.toString() to newUser)
                    )

                    FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Log.w("Fetching FCM registration token failed", task.exception)
                            return@OnCompleteListener
                        }
                        newUser.not_token = task.result
                    })

                    databaseRef.child(id).setValue(session) // adds that session to the database
                        .addOnSuccessListener {
                            val Intent = Intent(
                                context,
                                MainActivity::class.java
                            ).apply { // passes values to main
                                userdata.updateAppData(
                                    newUser.userId,
                                    id,
                                    newUser.role,
                                    userdata.Team.value.toString(),
                                    userdata.Status.value.toString()
                                )
                            }
                            context.startActivity(Intent) // move to main
                        }
                        .addOnFailureListener { e ->// error handling
                            val err_toast =
                                Toast.makeText(
                                    context,
                                    "Failed to Create Session",
                                    Toast.LENGTH_SHORT
                                )
                            err_toast.show()
                        }
                }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun join_create(userdata: AppData, navController: NavController) {
        userdata.reset_data() // reset the stored user data
        val context = LocalContext.current

        // get the username and UID from the shared preferences
        val sharedPref =
            context.getSharedPreferences("Quatermaster.storedData", MODE_PRIVATE)
        val username =
            remember { mutableStateOf(sharedPref.getString("Quatermaster.key.username", "") ?: "") }
        val UID =
            remember { mutableStateOf(sharedPref.getString("Quatermaster.key.UID", "") ?: "") }

        val configuration = LocalConfiguration.current
        val Landscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE


        val UIDset = remember { mutableStateOf(UID.value.isEmpty()) }
        var showDialog = remember { mutableStateOf(username.value.isEmpty()) }
        var showUserDialog by remember { mutableStateOf(false) }

        if (UIDset.value) { // if the UID isnt set create a new one otherwise use the old one
            val newUID = "user_${System.currentTimeMillis()}"
            sharedPref.edit().putString("Quatermaster.key.UID", newUID).apply()
            userdata.user_ID.value = newUID
        } else {
            userdata.user_ID.value = UID.value
        }

        // Set username dialog
        if (showDialog.value) {
            TextinputDialog(
                onInputSubmitted = { newUsername ->
                    sharedPref.edit().putString("Quatermaster.key.username", newUsername).apply()
                    username.value = newUsername
                    userdata.Username.value = newUsername
                    showDialog.value = false
                },
                "Callsign"
            )
        } else { // if username is set, set it in userdata
            userdata.Username.value = username.value
        }
        //UI
        FYP_PrototypeTheme {
            if (Landscape){

                Box(// make sure everything aligns
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        infotopbar( //top nav bar
                            onUsernameClick = { showDialog.value = true },
                            onSiteClick = { navController.navigate("sites") },
                            onUserclick = { showUserDialog = true}
                        )
                        Column(modifier = Modifier.padding(16.dp).clip(RoundedCornerShape(12.dp))
                            .background(androidx.compose.ui.graphics.Color.Gray)
                            .padding(16.dp)
                        )
                        {
                            Join_session(userdata)
                            if (showUserDialog == true) {
                                AlertDialog(
                                    onDismissRequest = { showUserDialog = false },
                                    title = { Text("User ID:") },
                                    text = { Text(userdata.user_ID.value.toString()) },
                                    confirmButton = {},
                                    dismissButton = {
                                        TextButton(onClick = {
                                            showUserDialog = false
                                        }) { Text("Close") }
                                    }
                                )
                            }
                        }
                    }
                }

            }else{
                Box(// make sure everything aligns
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        infotopbar( //top nav bar
                            onUsernameClick = { showDialog.value = true },
                            onSiteClick = { navController.navigate("sites") },
                            onUserclick = { showUserDialog = true}
                        )
                        Column(modifier = Modifier.padding(16.dp).clip(RoundedCornerShape(12.dp))
                            .background(androidx.compose.ui.graphics.Color.Gray)
                            .padding(16.dp)
                        )
                        {
                            Image(
                                painter = painterResource(id = R.drawable.logo2),
                                contentDescription = "logo",
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .size(220.dp)
                                    .padding(bottom = 16.dp)
                            )

                            Join_session(userdata)
                            Spacer(Modifier.height(32.dp))
                            Create_session(userdata)
                            Spacer(Modifier.height(32.dp))
                            if (showUserDialog == true) {
                                AlertDialog(
                                    onDismissRequest = { showUserDialog = false },
                                    title = { Text("User ID:") },
                                    text = { Text(userdata.user_ID.value.toString()) },
                                    confirmButton = {},
                                    dismissButton = {
                                        TextButton(onClick = {
                                            showUserDialog = false
                                        }) { Text("Close") }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Editor(navController: NavController, userdata: AppData, mode: String = "Game") {
        val context = LocalContext.current
        val database = FirebaseDatabase.getInstance()
        var mapView = remember { MapView(context) }
        var selectedPoint: GeoPointData? = null
        var showBrief by remember { mutableStateOf(false) }
        var showMarkerEdit by remember { mutableStateOf(false) }
        var showPolyEdit by remember { mutableStateOf(false) }
        var markersVersion by remember { mutableStateOf(0) }
        var expanded by remember { mutableStateOf(false) }
        var shownedit by remember { mutableStateOf("Marker") }
        var linePoints by remember { mutableStateOf<MutableList<GeoPointData>>(mutableListOf()) }
        val SID = userdata.Cur_Site.value?.site_ID
        val GID = userdata.Cur_Game.value?.gid
        val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
        var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
        var type by remember { mutableStateOf(0) }

        Intent(applicationContext, LocationService::class.java).apply {
            action = LocationService.ACTION_START
            startService(this)
        }

        val compassOverlay = remember {
            CompassOverlay(context, InternalCompassOrientationProvider(context), mapView).apply {
                enableCompass()
            }
        }

        var Markers: MutableList<MapObject>

        if (mode == "Game"){
            Markers = userdata.Cur_Game.value?.markers ?: return
        }else if(mode == "Site"){
            Markers = userdata.Cur_Site.value?.markers ?: return

        }else{ //base case
            Markers = userdata.Cur_Game.value?.markers ?: return
        }

        fun saveandexit() {
            val site = userdata.Cur_Site.value
            val game = userdata.Cur_Game.value

            if (site != null && (game != null || mode == "Site")) {
                if (mode == "Game") {
                    userdata.Cur_Game.value?.markers = Markers

                    val Ref = database.getReference("sites")
                    Ref.child(site.site_ID)
                        .child("games")
                        .child(game?.gid ?: return)
                        .child("markers")
                        .setValue(Markers)
                        .addOnSuccessListener {
                            Log.d("SaveAndExit", "Markers saved successfully")
                            navController.navigateUp()
                        }
                        .addOnFailureListener { exception ->
                            Log.e("SaveAndExit", "Failed to save markers", exception)
                            navController.navigateUp()
                        }
                } else if (mode == "Site") {
                    userdata.Cur_Site.value?.markers = Markers

                    val Ref = database.getReference("sites")
                    Ref.child(site.site_ID)
                        .child("markers")
                        .setValue(Markers)
                        .addOnSuccessListener {
                            Log.d("SaveAndExit", "Markers saved successfully")
                            navController.navigateUp()
                        }
                        .addOnFailureListener { exception ->
                            Log.e("SaveAndExit", "Failed to save markers", exception)
                            navController.navigateUp()
                        }
                }

            } else {

                Log.e("SaveAndExit", "Cannot save: Site or Game is null")

            }
        }

        fun getCurLocation() {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.flushLocations().addOnSuccessListener{
                    fusedLocationClient.getCurrentLocation(PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { location: Location? ->
                            location?.let {
                                val geoPoint = GeoPoint(it.latitude, it.longitude)
                                currentLocation = geoPoint

                                mapView.controller.setCenter(geoPoint)
                                mapView.controller.setZoom(20.0)
                                mapView.invalidate()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Location", "Error getting location", e)
                        }
                }
            }
        }

        DisposableEffect(mapView) {
            mapView.onResume()
            onDispose {
                mapView.onPause()
            }
        }

        LaunchedEffect(Unit) {
            Configuration.getInstance()
                .load(context, PreferenceManager.getDefaultSharedPreferences(context))
            Configuration.getInstance().userAgentValue = "AirsoftAPP"
            val pullRef = database.getReference("sites")

            if (mode == "Game"){
                pullRef.child(SID.toString()).child("games").child(GID.toString()).child("markers").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val loadedMarkers = snapshot.children.mapNotNull { child ->
                            child.getValue(MapObject::class.java)
                        }.toMutableList()

                        Markers = loadedMarkers
                        userdata.Cur_Game.value?.markers = loadedMarkers
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Editor", "Error loading markers: ${error.message}")
                    }
                })
            }else{
                pullRef.child(SID.toString()).child("markers").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val loadedMarkers = snapshot.children.mapNotNull { child ->
                            child.getValue(MapObject::class.java)
                        }.toMutableList()

                        Markers = loadedMarkers
                        userdata.Cur_Site.value?.markers = loadedMarkers
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Editor", "Error loading markers: ${error.message}")
                    }
                })
            }
            getCurLocation()
        }

        Scaffold(topBar = {
            editortopbar(
                "Editor", onBackClick = { saveandexit() },
                onAddClick = { expanded = true },
                onMarkerClick = {shownedit = "Marker"},
                onLineClick = {shownedit = "Line"},
                onPolygonClick = {shownedit = "Polygon"},
                onBriefClick = {showBrief = true}
            )
        }) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                AndroidView(// map view
                    modifier = Modifier.fillMaxSize(),
                    factory = { mapView.apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        minZoomLevel = 17.0
                        maxZoomLevel = 20.0
                        controller.setZoom(18.0)
                        setBuiltInZoomControls(true)
                        setMultiTouchControls(true)
                        overlays.add(compassOverlay)
                    } },
                    update = { mapView ->
                        mapView.overlays.clear()
                        Markers.forEach { marker ->
                            marker.draw(mapView, true)
                        }
                        mapView.overlays.add(compassOverlay)
                        mapView.invalidate()
                    }
                )

                Image(
                    painter = painterResource(id = R.drawable.crosshair),
                    contentDescription = "Crosshair",
                    modifier = Modifier.align(Alignment.Center)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        if (shownedit == "Marker") {
                            button_common("Add Marker",
                                onClick = {
                                    showMarkerEdit = true
                                }
                            )
                        } else if (shownedit == "Line") {
                            button_common("Add Point",
                                    onClick = {
                                        val centerIGeoPoint = mapView.mapCenter
                                        if (centerIGeoPoint != null) {
                                            val centerPoint = GeoPointData(
                                                centerIGeoPoint.latitude,
                                                centerIGeoPoint.longitude
                                            )
                                            linePoints.add(centerPoint)
                                        }
                                    }
                                )

                            button_common("Finish Line",
                                    onClick = {
                                        type = 1
                                        showPolyEdit = true
                                    }
                                )
                        } else {
                                button_common("Add Point",
                                    onClick = {
                                        val centerIGeoPoint = mapView.mapCenter
                                        if (centerIGeoPoint != null) {
                                            val centerPoint = GeoPointData(
                                                centerIGeoPoint.latitude,
                                                centerIGeoPoint.longitude
                                            )
                                            linePoints.add(centerPoint)
                                        }

                                    }
                                )

                                button_common("Finish Polygon",
                                    onClick = {
                                        type = 2
                                        showPolyEdit = true
                                    }
                                )
                        }
                        button_common("Undo",
                            onClick = {
                                if (!Markers.isEmpty()){
                                    Markers.removeAt(Markers.lastIndex)
                                }
                                markersVersion++
                                mapView.invalidate()
                            }
                        )
                        button_common("My Location",
                            onClick = {
                                getCurLocation()
                            }
                        )
                    }


                    if (showBrief == true) {
                        if(mode == "Game"){
                            twotextinputDialog(
                                userdata.Cur_Game.value?.name ?: return@Box,
                                userdata.Cur_Game.value?.desc ?: return@Box,
                                onDismiss = { showBrief = false },
                                onConfirm = { title, desc ->
                                    val Ref = database.getReference("sites")
                                    Ref.child(userdata.Cur_Site.value?.site_ID.toString())
                                        .child("games")
                                        .child(userdata.Cur_Game.value?.gid.toString()).child("name")
                                        .setValue(title).addOnSuccessListener {
                                            userdata.Cur_Game.value?.name = title
                                        }
                                    Ref.child(userdata.Cur_Site.value?.site_ID.toString())
                                        .child("games")
                                        .child(userdata.Cur_Game.value?.gid.toString()).child("desc")
                                        .setValue(desc).addOnSuccessListener {
                                            userdata.Cur_Game.value?.desc = desc
                                        }
                                    showBrief = false

                                }
                            )
                        }else{
                            twotextinputDialog(
                                userdata.Cur_Site.value?.name ?: return@Box,
                                userdata.Cur_Site.value?.brief ?: return@Box,
                                onDismiss = { showBrief = false },
                                onConfirm = { title, desc ->
                                    val Ref = database.getReference("sites")
                                    Ref.child(userdata.Cur_Site.value?.site_ID.toString())
                                        .child("name")
                                        .setValue(title).addOnSuccessListener {
                                            userdata.Cur_Game.value?.name = title
                                        }
                                    Ref.child(userdata.Cur_Site.value?.site_ID.toString())
                                        .child("brief")
                                        .setValue(desc).addOnSuccessListener {
                                            userdata.Cur_Game.value?.desc = desc
                                        }
                                    showBrief = false
                                }
                            )
                        }
                    }


                    // marker dialog
                    if (showMarkerEdit == true) {
                        EditorDialog(
                            onDismiss = { showMarkerEdit = false },
                            onConfirm = { title, desc, team, markerType ->
                                val centerIGeoPoint = mapView.mapCenter
                                if (centerIGeoPoint != null) {
                                    val centerPoint =
                                        GeoPointData(
                                            centerIGeoPoint.latitude,
                                            centerIGeoPoint.longitude
                                        )
                                    selectedPoint = centerPoint
                                    showMarkerEdit = true
                                }
                                selectedPoint?.let { point ->
                                    Log.i("Marker maker", "Confirm clicked in Editor")
                                    val tempList: MutableList<GeoPointData> = ArrayList()
                                    tempList.add(point)
                                    Log.i("Marker maker", "Adding Marker $title at $point")
                                    Markers.add(
                                        MapObject(
                                            type = 0,
                                            title = title,
                                            desc = desc,
                                            geopoints = tempList,
                                            team = team,
                                            icon = markerType
                                        )
                                    )

                                    markersVersion++
                                } ?: Log.e("Marker maker", "selectedPoint is null")
                                showMarkerEdit = false
                            })
                    }
                    //line/polygon dialog
                    if (showPolyEdit == true) {
                        LinePolyEditorDialog(
                            onDismiss = { showPolyEdit = false },
                            onConfirm = { title, desc, colour, weight ->
                                val centerIGeoPoint = mapView.mapCenter
                                if (centerIGeoPoint != null) {
                                    val centerPoint =
                                        GeoPointData(
                                            centerIGeoPoint.latitude,
                                            centerIGeoPoint.longitude
                                        )
                                    selectedPoint = centerPoint
                                    showPolyEdit = true
                                }
                                var ColourCon: Int = Color.GRAY
                                var WeightCon = 1F

                                when (weight) {
                                    "1" -> WeightCon = 1F
                                    "2" -> WeightCon = 2F
                                    "4" -> WeightCon = 4F
                                    "5" -> WeightCon = 5F
                                    "10" -> WeightCon = 10F
                                }

                                when (colour) {
                                    "Grey" -> ColourCon = Color.GRAY
                                    "Red" -> ColourCon = Color.RED
                                    "Blue" -> ColourCon = Color.BLUE
                                    "Green" -> ColourCon = Color.GREEN
                                    "Black" -> ColourCon = Color.BLACK
                                    "Yellow" -> ColourCon = Color.YELLOW
                                }


                                selectedPoint?.let { point ->
                                    Markers.add(
                                        MapObject(
                                            type = type,
                                            title = title,
                                            desc = desc,
                                            geopoints = linePoints.toMutableList(),
                                            width = WeightCon,
                                            colour = ColourCon
                                        )
                                    )
                                    linePoints.clear()
                                    markersVersion++
                                    mapView.invalidate()

                                    markersVersion++
                                } ?: Log.e("Marker maker", "selectedPoint is null")
                                showMarkerEdit = false
                            })
                    }
                }
            }
        }
    }
}





