package com.example.fyp_prototype

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import kotlin.collections.contains


/* USAGE

TextinputDialog(
                onInputSubmitted = { newUsername ->
                    sharedPref.edit().putString("Quatermaster.key.username", newUsername).apply()
                    username.value = newUsername
                    userdata.Username.value = newUsername
                    showDialog.value = false
                },
                "Callsign"
            )

 */
@Composable
fun TextinputDialog(onInputSubmitted: (String) -> Unit, Message: String) {
    var input by remember { mutableStateOf("") }
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
                    text = "Set $Message:",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        isError = false
                    },
                    label = { Text("$Message") },
                    isError = isError,
                    supportingText = if (isError) {
                        @Composable {
                            Text("$Message Cannot be Empty")
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
                            if (input.trim().isNotEmpty()) {
                                onInputSubmitted(input.trim())
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

@Composable
fun site_view_screen(navController: NavController, userdata : AppData, edit: Boolean) {
    val sites = remember { mutableStateOf<List<site>>(emptyList()) }
    val database = FirebaseDatabase.getInstance()
    val databaseRef = database.getReference("sites")
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val tempsites = mutableListOf<site>()
        databaseRef
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (siteSnapshot in snapshot.children) {
                        try {
                            val sitesData = siteSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                            if (sitesData != null) {

                                val name = sitesData["name"] as? String ?: ""
                                val siteId = sitesData["site_ID"] as? String ?: ""

                                val userIDs = when (val rawUID = sitesData["users_IDs"]) {
                                    is List<*> -> (rawUID as? List<String>) ?: emptyList()
                                    is Map<*,*> -> (rawUID as? Map<String, String>)?.values?.toList() ?: emptyList()
                                    else -> emptyList()
                                }

                                if (userIDs.contains(userdata.user_ID.value)){
                                    tempsites.add(
                                        site(
                                            name = name,
                                            Site_ID = siteId,
                                            users_IDs = userIDs as MutableList<String>
                                        )
                                    )
                                }
                            } else {
                                Log.e("SiteList", "Site List is null")
                            }
                        } catch (e: Exception) {
                            Log.e("FirebaseDebug", "Error deserializing SiteData", e)
                        }
                    }
                    sites.value = tempsites
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Database query failed: ${error.message}")
                }
            })
    }
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
            ) {
                items(sites.value) { site ->
                    SiteListItem(site, onItemclick = { selectedSite ->
                        if (edit == true){
                            navController.navigate("games_list/${selectedSite.Site_ID}")
                        } else {
                            userdata.Cur_Site.value = site
                            val session_Ref = database.getReference("sessions").child(userdata.Session_ID.value.toString())
                            session_Ref.child("stid").setValue(site.Site_ID)
                            navController.navigate("games_list/${selectedSite.Site_ID}"){
                                popUpTo("info") { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    })
                    Log.i("SitesList", "Displaying Site")
                    Divider()
                }

            }

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
                onClick = { showDialog = true },
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
        if (showDialog == true){
            TextinputDialog(
                onInputSubmitted = {Input ->
                    val newSite = site(
                        name = Input,
                        Site_ID = "site_${System.currentTimeMillis()}",
                        users_IDs = mutableListOf(userdata.user_ID.value.toString())
                    )
                    databaseRef.child(newSite.Site_ID).setValue(newSite)
                    showDialog = false
                },
                "Site Name"
            )
        }
    }
}

@Composable
private fun SiteListItem(site: site, onItemclick: (site) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable{onItemclick(site)}

    ) {
        Text(
            text = "Site: ${site.name}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun games_list(navController: NavController, userdata: AppData, edit: Boolean){
    val games = remember { mutableStateOf<List<game>>(emptyList()) }
    val database = FirebaseDatabase.getInstance()
    var showDialog by remember { mutableStateOf(false) }
    var STID: String? = null

    if (edit == true){
        STID = navController.currentBackStackEntry?.arguments?.getString("Site_ID")
    }else {
        STID = userdata.Cur_Site.value?.Site_ID
    }

    val databaseRef = database.getReference("sites").child(STID.toString()).child("Games")

    LaunchedEffect(Unit) {
        val tempgames = mutableListOf<game>()
        databaseRef
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (siteSnapshot in snapshot.children) {
                        try {
                            val gamesData = siteSnapshot.getValue(game::class.java)
                            if (gamesData != null) {
                                tempgames.add(
                                    game(
                                        gamesData.gid,
                                        gamesData.name,
                                        gamesData.desc
                                    )
                                )
                                Log.i("GameList", "Added Site ${gamesData?.name}")
                            } else {
                                Log.e("GameList", "Site List is null")
                            }
                        } catch (e: Exception) {
                            Log.e("FirebaseDebug", "Error deserializing GameData", e)
                        }
                    }
                    games.value = tempgames
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Database query failed: ${error.message}")
                }
            })
    }
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
                text = "Games",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(games.value) { game ->
                    GameListItem(game, onItemclick = { selectedGame ->
                        if (edit == true){
                        null
                        } else {
                            userdata.Cur_Game.value = game
                            val session_Ref = database.getReference("sessions").child(userdata.Session_ID.value.toString())
                            session_Ref.child("gid").setValue(game.gid)
                            navController.navigateUp()
                        }
                    })
                    Log.i("GameList", "Displaying Site")
                    Divider()
                }

            }

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

            if (edit == true){
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.8f),
                    )
                ) {
                    Text(
                        text = "New Game",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        if (showDialog == true){
            TextinputDialog(
                onInputSubmitted = {Input ->
                    val newgame = game(
                        name = Input
                    )
                    databaseRef.child(newgame.gid).setValue(newgame)
                    showDialog = false
                },
                "Game Name"
            )
        }
    }
}
@Composable
private fun GameListItem(game: game, onItemclick: (game) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable{onItemclick(game)}
    ) {
        Text(
            text = "Game: ${game.name}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}