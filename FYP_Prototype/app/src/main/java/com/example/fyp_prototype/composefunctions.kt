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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Firebase
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.database
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.contains
import kotlin.random.Random


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
fun areyoursureDialog(OnConfirm: () -> Unit,OnDismiss:() -> Unit, Message: String){
    AlertDialog(
        onDismissRequest = {OnDismiss()},
        title = { Text(Message)},
        text = {},
        confirmButton = {TextButton(onClick = { OnConfirm() }) {
            Text("Yes")
        }},
        dismissButton = {
            TextButton(onClick = { OnDismiss() }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun site_view_screen(navController: NavController, userdata : AppData, edit: Boolean) {
    val sites = remember { mutableStateOf<List<site>>(emptyList()) }
    val database = FirebaseDatabase.getInstance()
    val databaseRef = database.getReference("sites")
    var showDialog by remember { mutableStateOf(false) }
    var FirebaseAccess = FirebaseAccess()

    fun fetchsitelist(){
        val tempsites = mutableListOf<site>()

        FirebaseAccess.get_from_reference(databaseRef, callback = { snapshot ->
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
                    SiteListItem(
                        site, onItemclick = { selectedSite ->
                            if (edit == true) {
                                userdata.Cur_Site.value = site
                                userdata.Cur_Game.value = null
                                navController.navigate("games_list/${selectedSite.site_ID}")
                            } else {
                                userdata.Cur_Site.value = site
                                val session_Ref = database.getReference("sessions")
                                    .child(userdata.Session_ID.value.toString())
                                session_Ref.child("stid").setValue(site.site_ID)
                                navController.navigate("games_list/${selectedSite.site_ID}") {
                                    popUpTo("info") { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        edit = edit,
                        navController = navController,
                        userdata = userdata
                    )
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
private fun SiteListItem(site: site, onItemclick: (site) -> Unit, edit: Boolean, navController: NavController, userdata: AppData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable{onItemclick(site)}

    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Site: ${site.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if(edit == true){
                SiteDropdown(
                    site,
                    onSiteEditClick = {
                        userdata.Cur_Site.value = site
                        navController.navigate("games_designer/${"Site"}")}
                )
            }
        }


    }
}

@Composable
private fun SiteDropdown(site: site, onSiteEditClick: (site) -> Unit){
    var expanded by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val database = FirebaseDatabase.getInstance()
    val databaseRef = database.getReference("sites")

    Box(modifier = Modifier.padding(16.dp))
    {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    onSiteEditClick(site)
                    expanded = false
                }
            )

            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    showDeleteDialog = true
                    expanded = false
                }
            )

            DropdownMenuItem(
                text = { Text("Add User") },
                onClick = {
                    showAddUserDialog = true
                    expanded = false
                }
            )
        }
    }
    //add another user dialog
    if(showAddUserDialog) {
        TextinputDialog(
            onInputSubmitted = { newUser ->
                site.users_IDs.add(newUser)
                databaseRef.child(site.site_ID).setValue(site)
                showAddUserDialog = false
            },
            "User ID"
        )
    }
    //delete site dialog
    if(showDeleteDialog){
        areyoursureDialog(
            OnConfirm = {
                databaseRef.child(site.site_ID).removeValue()
                showDeleteDialog = false
            },
            OnDismiss = {
                showDeleteDialog = false
            },
            Message = "Are You sure you want to delete?"
        )
    }

}

@Composable
fun games_list(navController: NavController, userdata: AppData, edit: Boolean){
    val games = remember { mutableStateOf<List<game>>(emptyList()) }
    val database = FirebaseDatabase.getInstance()
    var showDialog by remember { mutableStateOf(false) }
    var STID: String? = null
    var FirebaseAccess = FirebaseAccess()

    if (edit == true){
        STID = navController.currentBackStackEntry?.arguments?.getString("Site_ID")
    }else {
        STID = userdata.Cur_Site.value?.site_ID
    }

    val databaseRef = database.getReference("sites").child(STID.toString()).child("games")

    fun fetchgamelist(){
        val tempgames = mutableListOf<game>()
        FirebaseAccess.get_from_reference(databaseRef, callback = { snapshot ->
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
                    GameListItem(
                        game, onItemclick = { selectedGame ->
                            if (edit == true) {
                                userdata.Cur_Game.value = game
                                navController.navigate("games_designer/${"Game"}")
                            } else {
                                userdata.Cur_Game.value = game
                                val session_Ref = database.getReference("sessions")
                                    .child(userdata.Session_ID.value.toString())
                                session_Ref.child("gid").setValue(game.gid)
                                navController.navigate("map")
                            }
                        },
                        onDeleteclick = { game -> databaseRef.child(game.gid).removeValue()},
                        edit = edit
                    )
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
private fun GameListItem(game: game, onItemclick: (game) -> Unit, onDeleteclick: (game) -> Unit, edit: Boolean = false) {
    var DeleteDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable{onItemclick(game)}
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Game: ${game.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (edit == true){
                IconButton(onClick = {DeleteDialog = true}) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Gray
                        )
                    }
                }
            }
        }
    }

    if(DeleteDialog == true){
        areyoursureDialog(
            OnConfirm = {DeleteDialog = false
                        onDeleteclick(game)},
            OnDismiss = {DeleteDialog = false},
            Message = "Are you sure you want to delete game: ${game.name}"
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

@Composable
fun LinePolyEditorDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit,
){
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    val ColourOptions = listOf("Grey", "Red", "Blue","Green","Black","Yellow")
    var WeightOptions = listOf("1", "2", "4","5","10")

    var firstDropdownExpanded by remember { mutableStateOf(false) }
    var secondDropdownExpanded by remember { mutableStateOf(false) }
    var selectedColourOption by remember { mutableStateOf(ColourOptions[0]) }
    var selectedWeightOption by remember { mutableStateOf(ColourOptions[0]) }


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
                    Text("Select Colour:")
                    Box {
                        OutlinedTextField(
                            value = selectedColourOption,
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
                            ColourOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedColourOption = option
                                        firstDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Column {
                    Text("Select Weight")
                    Box {
                        OutlinedTextField(
                            value = selectedWeightOption,
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
                            WeightOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedWeightOption = option
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
                    Log.d("EditorDialog", "Confirm clicked: title=$title, desc=$desc, colour=$selectedColourOption, weight=$WeightOptions")
                    onConfirm(title, desc, selectedColourOption, WeightOptions.toString())
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
            Text(text = String, color = Color.White) // title
        },
        navigationIcon = {// navigate back a screen
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        actions = { // add button
          onAddClick?.let { // if there is no callback setup do not show the button
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
    // colors
    val selectedColor = Color.White
    val selectedTextColor = Color.DarkGray
    val unselectedTextColor = Color.Black
    val unselectedColor = Color.Green
    val backgroundColor = Color.Gray
    //states for dialogs and dropdowns
    var expanded by remember { mutableStateOf(false) }
    var PingDialog by remember { mutableStateOf(false) }
    var UrgentPingDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val Landscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    TopAppBar(
        title = { // buttons in here
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column( // map button
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .background(
                            color = if (Tab == "Map") // if selected be grey
                                selectedColor.copy(alpha = 0.3f)
                            else unselectedColor, // otherwise green
                            shape = CircleShape
                        )
                        .clickable(onClick = Button2Click) //callback
                        .padding(8.dp)
                ) {
                    Icon( // map icon
                        imageVector = Icons.Default.Place,
                        contentDescription = "Map",
                        tint = if (Tab == "Map") selectedTextColor else unselectedTextColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text( // label
                        text = "Map",
                        color = if (Tab == "Map") selectedTextColor else unselectedTextColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column( // info button
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .background(
                            color = if (Tab == "Info") // if selected be grey
                                selectedColor.copy(alpha = 0.3f)
                            else unselectedColor, // other wise green
                            shape = CircleShape
                        )
                        .clickable(onClick = Button1Click) // callback
                        .padding(8.dp)
                ) {
                    Icon( // info icon
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = if (Tab == "Info") selectedTextColor else unselectedTextColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text( // label
                        text = "Info",
                        color = if (Tab == "Info") selectedTextColor else unselectedTextColor,
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
                    onDismissRequest = {expanded = false},
                    modifier = Modifier.background(Color.White)) {
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
                            PingDialog = true
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("SOS") },
                        onClick = {
                            UrgentPingDialog = true
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
    if (PingDialog == true){
        areyoursureDialog(
            OnConfirm = {pingclick()
                PingDialog =false},
            OnDismiss = {PingDialog =false},
            Message = "Are you sure you want to ping the marshal?"
        )
    }
    if (UrgentPingDialog == true){
        areyoursureDialog(
            OnConfirm = {urgentpingclick()
                UrgentPingDialog =false},
            OnDismiss = {UrgentPingDialog =false},
            Message = "Are you sure you want to call an Emergency?"
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun infotopbar(onUsernameClick: () -> Unit,onSiteClick: () -> Unit,onUserclick: () -> Unit){
    var expanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { // title of app
            Text(text = "Quartermaster", color = Color.White)
        },
        actions = {
            Box(){
                IconButton(onClick = {expanded = !expanded}) { // show dropdown button
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White
                    )
                }

                DropdownMenu(expanded = expanded, // drop down menu options
                    onDismissRequest = {expanded = false},
                    modifier = Modifier.background(Color.White)) {
                    DropdownMenuItem( //set username
                        text = { Text("Set Username") },
                        onClick = {
                            onUsernameClick()
                            expanded = false
                        }
                    )
                    DropdownMenuItem( // go to sites list
                        text = { Text("View Sites") },
                        onClick = {
                            onSiteClick()
                            expanded = false
                        }
                    )
                    DropdownMenuItem( //shows the user ID - needed to add people to a site for sharing
                        text = { Text("Show User ID") },
                        onClick = {
                            onUserclick()
                            expanded = false
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Gray,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun editortopbar(String: String, onBackClick: () -> Unit,onAddClick: (() -> Unit)? = null,
                 onMarkerClick: () -> Unit,onLineClick: () -> Unit,onPolygonClick: () -> Unit,onBriefClick: () -> Unit){
    var expanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Text(text = String, color = Color.White)
        },
        navigationIcon = { // back button
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        actions = {
            onAddClick?.let { // if add click call back exists show
                IconButton(onClick = {expanded = true}) { // expand drop down
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.White
                    )
                }
            }

            DropdownMenu(expanded = expanded, // dropdown menu
                onDismissRequest = {expanded = false},
                modifier = Modifier.background(Color.White)) {
                DropdownMenuItem( // icon marker option
                    text = { Text("Marker") },
                    onClick = {
                        onMarkerClick()
                        expanded = false
                    }
                )
                DropdownMenuItem(// line marker option
                    text = { Text("Line") },
                    onClick = {
                        onLineClick()
                        expanded = false
                    }
                )
                DropdownMenuItem( // polygon marker option
                    text = { Text("Polygon") },
                    onClick = {
                        onPolygonClick()
                        expanded = false
                    }
                )
                DropdownMenuItem( // edit the text brief
                    text = { Text("Edit Brief") },
                    onClick = {
                        onBriefClick()
                        expanded = false
                    }
                )
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
fun button_common(String: String, onClick: () -> Unit){
    ElevatedButton( //elevated give the button a slight drop shadow
        onClick = {onClick()}, // callback onclick
        modifier = Modifier
            .padding(4.dp),
            colors = ButtonDefaults.elevatedButtonColors( // default button colors for quatermaster
                containerColor = Color.Green,
                contentColor = Color.Black
            )
            )
        {
        Text(String) // button label
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Join_session(userdata: AppData, firebaseAccess: FirebaseAccess,
                 onSucc:(user, String) -> Unit, snackbar: SnackbarHostState) {
    var code_input by remember { mutableStateOf("") }
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

                firebaseAccess.get_from_reference(databaseRef.child(code_input), callback = { snapshot ->
                    val newUser = user( //create a new user
                        userId = userdata.user_ID.value.toString(),
                        username = userdata.Username.value.toString()
                    )
                    if (snapshot.exists()) {
                        firebaseAccess.set_from_reference( databaseRef.child(code_input).child("users").child(newUser.userId),
                            onSucc = {
                                onSucc(newUser,code_input)
                            },newUser, onFail = {

                                CoroutineScope(Dispatchers.Main).launch{
                                    snackbar.showSnackbar("Failed to Join Session - Connection Issue")
                                }
                            })
                    } else {
                        CoroutineScope(Dispatchers.Main).launch{
                            snackbar.showSnackbar("Failed to Join Session - Code Invalid")
                        }
                    }
                })
            } else {
                CoroutineScope(Dispatchers.Main).launch{
                    snackbar.showSnackbar("Code must be 6 digits long")
                }
            }

        } )
    }
}

// this is the create session button, has all the logic inside
@Composable
fun Create_session(userdata: AppData, FirebaseAccess: FirebaseAccess,
                   onSucc:(user, String) -> Unit, snackbar: SnackbarHostState) {
    val database = Firebase.database
    val databaseRef = database.getReference("sessions")


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

                FirebaseAccess.set_from_reference(databaseRef.child(id), onSucc = {
                    onSucc(newUser,id)
                },session, onFail = {
                    CoroutineScope(Dispatchers.Main).launch{
                        snackbar.showSnackbar("Failed to Create Session")
                    }
                })
            }
        )
    }
}
