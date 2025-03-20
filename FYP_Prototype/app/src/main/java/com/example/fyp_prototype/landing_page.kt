package com.example.fyp_prototype

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.fyp_prototype.ui.theme.FYP_PrototypeTheme
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.serialization.json.Json
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

import kotlin.random.Random

//This is the Landing page, users start here when the app is opened

class landing_page : ComponentActivity() {
    lateinit var userdata: AppData


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userdata = AppData.getInstance(application)
        userdata.reset_data()
        setContent { // UI
            App(userdata)
        }
    }


    @Composable
    private fun App(userdata: AppData) {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "join_create") {
            composable("join_create") {
                join_create(userdata, navController)
            }
            composable("sites") {
                site_view_screen(navController, userdata, true)
            }
            composable("stats") {

            }
            composable(
                route = "games_list/{Site_ID}",
                arguments = listOf(navArgument("Site_ID") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                games_list(navController,userdata, true)
            }
            composable("game_designer") {

            }
        }

    }


    // this is the join session button, it has the logic for joining a session inside it
    @Composable
    private fun Join_session(userdata: AppData) {
        var code_input by remember { mutableStateOf("") }
        val context = LocalContext.current
        val database = Firebase.database
        val databaseRef = database.getReference("sessions")
        val modifier = Modifier
            .padding(16.dp)
            .width(200.dp)
        Button( // code submission
            onClick = {
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
                        Toast.makeText(context, "Code must be 6 digits long", Toast.LENGTH_SHORT)
                    err_toast.show()
                }

            }
        ) {
            Text("Join Session")
        }

        Spacer(Modifier.height(8.dp))

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
            modifier = modifier
        )

    }

    // this is the create session button, has all the logic inside
    @Composable
    private fun Create_session(userdata: AppData) {
        val database = Firebase.database
        val databaseRef = database.getReference("sessions")
        val context = LocalContext.current
        Button(
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
                            Toast.makeText(context, "Failed to Create Session", Toast.LENGTH_SHORT)
                        err_toast.show()
                    }
            }
        ) {
            Text("Create Session")
        }
    }

    @Composable
    private fun join_create(userdata: AppData, navController: NavController) {
        val context = LocalContext.current
        val sharedPref =
            context.getSharedPreferences("Quatermaster.storedData", MODE_PRIVATE)

        val username =
            remember { mutableStateOf(sharedPref.getString("Quatermaster.key.username", "") ?: "") }
        val UID =
            remember { mutableStateOf(sharedPref.getString("Quatermaster.key.UID", "") ?: "") }

        val UIDset = remember { mutableStateOf(UID.value.isEmpty()) }
        val showDialog = remember { mutableStateOf(username.value.isEmpty()) }

        if (UIDset.value) {
            val newUID = "user_${System.currentTimeMillis()}"
            sharedPref.edit().putString("Quatermaster.key.UID", newUID).apply()
            userdata.user_ID.value = newUID
        } else {
            userdata.user_ID.value = UID.value
        }

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
        } else {
            userdata.Username.value = username.value
        }
        FYP_PrototypeTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column( // make sure the all the buttons stay in the middle
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Join_session(userdata)
                    Spacer(Modifier.height(32.dp))
                    Create_session(userdata)
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = {
                            navController.navigate("sites")
                        }
                    ) {
                        Text(
                            text = "Sites",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun MarkerPlacer(){
        val context = LocalContext.current
        var mapView = remember { MapView(context) }
        var selectedPoint: GeoPoint? = null
        var tempObject: MapObject
        var initialLocation: GeoPoint = GeoPoint(48.8584, 2.2945)

        LaunchedEffect(Unit) {
            Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
            Configuration.getInstance().userAgentValue = "AirsoftAPP"
        }
    }

}





