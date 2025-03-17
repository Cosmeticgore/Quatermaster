package com.example.fyp_prototype

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.Global.getString
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fyp_prototype.ui.theme.FYP_PrototypeTheme
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.random.Random

//This is the Landing page, users start her when the app is opened

class landing_page : ComponentActivity() {
    lateinit var userdata: AppData


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userdata = AppData.getInstance(application)
        val sharedPref = getSharedPreferences(
            "Quartermaster.StoredData", MODE_PRIVATE
        )
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
                site_view_screen(navController, userdata.user_ID.value.toString())
            }
            composable("stats") {

            }
            composable("game_designer") {

            }
        }

    }

    // this is the join session button, it has the logic for joining a session inside it
    @Composable
    fun Join_session(userdata: AppData) {
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
    fun Create_session(userdata: AppData) {
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
    fun join_create(userdata: AppData, navController: NavController) {
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
            usernameDialog(
                onUsernameSubmitted = { newUsername ->

                    sharedPref.edit().putString("Quatermaster.key.username", newUsername).apply()
                    username.value = newUsername
                    userdata.Username.value = newUsername
                    showDialog.value = false
                }
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
                        Text(text = "Sites",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    @Composable
    fun site_view_screen(navController: NavController, UID: String) {
        val sites = remember { mutableStateOf<List<site>>(emptyList()) }
        val database = FirebaseDatabase.getInstance()
        val databaseRef = database.getReference("sites")

        LaunchedEffect(UID) {
            val tempsites = mutableListOf<site>()
            databaseRef.child(UID).child("sites")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (siteSnapshot in snapshot.children) {
                            try {
                                val sitesData = siteSnapshot.getValue(site::class.java)
                                if (sitesData != null) {
                                    tempsites.add(
                                        site(
                                            sitesData.name,
                                            locat(
                                                sitesData.location.longitude,
                                                sitesData.location.latitude
                                            )
                                        )
                                    )
                                    Log.i("SiteList", "Added Site ${sitesData?.name}")
                                } else {
                                    Log.e("SiteList", "Site List is null")
                                }
                            } catch (e : Exception) {
                                Log.e("FirebaseDebug", "Error deserializing SiteData", e)
                            }
                        }
                        sites.value = tempsites
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
                    text = "Sites",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { items(sites.value) { site ->
                    Log.i("SitesList", "Displaying Site")
                    SiteListItem(site)
                    Divider()
                }

                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxSize().align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ){
                Button(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.fillMaxWidth(),
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

                Button(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.8f),
                    )
                ) {
                    Text(
                        text = "New Site",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }


        }
    }

    @Composable
    private fun SiteListItem(site: site){
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = "Site: ${site.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }


    @Composable
    fun usernameDialog(onUsernameSubmitted: (String) -> Unit) {
        var inputUsername by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { //NO DISMISS ALLOWED >:(
        }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Set Callsign:",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inputUsername,
                        onValueChange = {
                            inputUsername = it
                            isError = false
                        },
                        label = { Text("Callsign") },
                        isError = isError,
                        supportingText = if (isError) {
                            @Composable {
                                Text("Callsign Cannot be Empty")
                            }
                        } else null,

                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (inputUsername.trim().isNotEmpty()) {
                                    onUsernameSubmitted(inputUsername.trim())
                                } else {
                                    isError = true
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}



