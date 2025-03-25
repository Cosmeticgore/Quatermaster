package com.example.fyp_prototype

import android.R
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.fyp_prototype.ui.theme.PurpleGrey40
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

    fun fetchsitelist(){
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
                                            site_ID = siteId,
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

    LaunchedEffect(Unit) {
        fetchsitelist()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if(edit == true){
                topbarwithtext("Sites", onBackClick = {navController.navigateUp()}, onAddClick = {
                    showDialog = true
                })
            }else{
                topbarwithtext("Sites: Select Site", onBackClick = {navController.navigateUp()})
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                items(sites.value) { site ->
                    SiteListItem(site, onItemclick = { selectedSite ->
                        if (edit == true){
                            userdata.Cur_Site.value = site
                            navController.navigate("games_list/${selectedSite.site_ID}")
                        } else {
                            userdata.Cur_Site.value = site
                            val session_Ref = database.getReference("sessions").child(userdata.Session_ID.value.toString())
                            session_Ref.child("stid").setValue(site.site_ID)
                            navController.navigate("games_list/${selectedSite.site_ID}"){
                                popUpTo("info") { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    })
                    Log.i("SitesList", "Displaying Site")
                    Divider()
                }
            }
        }
        if (showDialog == true){
            TextinputDialog(
                onInputSubmitted = {Input ->
                    val newSite = site(
                        name = Input,
                        site_ID = "site_${System.currentTimeMillis()}",
                        users_IDs = mutableListOf(userdata.user_ID.value.toString())
                    )
                    databaseRef.child(newSite.site_ID).setValue(newSite)
                    fetchsitelist()
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
        STID = userdata.Cur_Site.value?.site_ID
    }

    val databaseRef = database.getReference("sites").child(STID.toString()).child("games")

    fun fetchgamelist(){
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

    LaunchedEffect(Unit) {
        fetchgamelist()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if(edit == true){
                topbarwithtext("Games", onBackClick = {navController.navigateUp()}, onAddClick = {
                    showDialog = true
                })
            }else{
                topbarwithtext("Games: Select game to load", onBackClick = {navController.navigateUp()})
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                items(games.value) { game ->
                    GameListItem(game, onItemclick = { selectedGame ->
                        if (edit == true){
                            userdata.Cur_Game.value = game
                            navController.navigate("games_designer")
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
        }
        if (showDialog == true){
            TextinputDialog(
                onInputSubmitted = {Input ->
                    val newgame = game(
                        name = Input
                    )
                    databaseRef.child(newgame.gid).setValue(newgame).addOnSuccessListener{fetchgamelist()}
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

@Composable
fun twotextinputDialog(
    title: String,
    desc: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
){
    var titlein by remember { mutableStateOf("") }
    var descriptionin by remember { mutableStateOf("") }

    titlein = title
    descriptionin = desc

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Details") },
        text = {
            Column {
                OutlinedTextField(
                    value = titlein,
                    onValueChange = { titlein = it },
                    label = { Text("Title") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = descriptionin,
                    onValueChange = { descriptionin = it },
                    label = { Text("Description") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(titlein, descriptionin) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun EditorDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit,
){
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    val teamOptions = listOf("Team 1", "Team 2", "None")
    val markerOptions = listOf("Respawn", "Flag", "X", "Warning")

    var firstDropdownExpanded by remember { mutableStateOf(false) }
    var secondDropdownExpanded by remember { mutableStateOf(false) }
    var selectedteamOption by remember { mutableStateOf(teamOptions[0]) }
    var selectedmarkerOption by remember { mutableStateOf(markerOptions[0]) }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Marker Settings")},
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ){
                OutlinedTextField(
                    value = title,
                    onValueChange = {title = it},
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = {desc = it},
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text("Select Team:")
                    Box {
                        OutlinedTextField(
                            value = selectedteamOption,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { firstDropdownExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Dropdown"
                                    )
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = firstDropdownExpanded,
                            onDismissRequest = { firstDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            teamOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedteamOption = option
                                        firstDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Column {
                    Text("Select Icon")
                    Box {
                        OutlinedTextField(
                            value = selectedmarkerOption,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { secondDropdownExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Dropdown"
                                    )
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = secondDropdownExpanded,
                            onDismissRequest = { secondDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            markerOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedmarkerOption = option
                                        secondDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    Log.d("EditorDialog", "Confirm clicked: title=$title, desc=$desc, team=$selectedteamOption, marker=$selectedmarkerOption")
                    onConfirm(title, desc, selectedteamOption, selectedmarkerOption)
                    onDismiss()
                }
            ) {
                Text("Confirm")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun topbarwithtext(String: String, onBackClick: () -> Unit,onAddClick: (() -> Unit)? = null){
    TopAppBar(
        title = {
            Text(text = String, color = Color.White)
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        actions = {
          onAddClick?.let {
              IconButton(onClick = it) {
                  Icon(
                      imageVector = Icons.Default.Add,
                      contentDescription = "Add",
                      tint = Color.White
                  )
              }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Gray, // Ensure grey background for the entire TopAppBar
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        )

    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun gametopbar(Button1Click: () -> Unit, Button2Click: () -> Unit,pingclick: () -> Unit,urgentpingclick: () -> Unit, sessionClick: () -> Unit, Tab: String = "Map", navController: NavController, Admin: Boolean = false){
    val selectedColor = Color.White
    val unselectedColor = Color.LightGray
    val backgroundColor = Color.Gray
    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .background(
                            color = if (Tab == "Map")
                                selectedColor.copy(alpha = 0.3f)
                            else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable(onClick = Button2Click)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Map",
                        tint = if (Tab == "Map") selectedColor else unselectedColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Map",
                        color = if (Tab == "Map") selectedColor else unselectedColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .background(
                            color = if (Tab == "Info")
                                selectedColor.copy(alpha = 0.3f)
                            else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable(onClick = Button1Click)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = if (Tab == "Info") selectedColor else unselectedColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Info",
                        color = if (Tab == "Info") selectedColor else unselectedColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        actions = {
            Box {IconButton(onClick = {expanded = !expanded}) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White
                    )
                }
                DropdownMenu(expanded = expanded,
                    onDismissRequest = {expanded = false}) {
                    DropdownMenuItem(
                        text = { Text("View Players") },
                        onClick = {
                            navController.navigate("players")
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("View Session ID") },
                        onClick = {
                            sessionClick()
                            expanded = false
                        }
                    )
                    if (Admin == true){
                        DropdownMenuItem(
                            text = { Text("Select Game") },
                            onClick = {
                                navController.navigate("selectSite")
                                expanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Request Help") },
                        onClick = {
                            pingclick()
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("SOS") },
                        onClick = {
                            urgentpingclick()
                            expanded = false
                        }
                    )
                }
            }
        },
        modifier = Modifier.background(backgroundColor),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor,
            titleContentColor = Color.White
        )
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun infotopbar(onUsernameClick: () -> Unit,onSiteClick: () -> Unit){
    var expanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Text(text = "Join or Create a Session", color = Color.White)
        },
        actions = {
            Box(){
                IconButton(onClick = {expanded = !expanded}) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White
                    )
                }

                DropdownMenu(expanded = expanded,
                    onDismissRequest = {expanded = false}) {
                    DropdownMenuItem(
                        text = { Text("Set Username") },
                        onClick = {
                            onUsernameClick()
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("View Sites") },
                        onClick = {
                            onSiteClick()
                            expanded = false
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Gray, // Ensure grey background for the entire TopAppBar
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        )

    )



}


