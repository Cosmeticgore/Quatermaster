package com.example.fyp_prototype

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fyp_prototype.ui.theme.FYP_PrototypeTheme
import com.google.firebase.Firebase
import com.google.firebase.database.database
import kotlin.random.Random

//This is the Landing page, users start her when the app is opened

class landing_page : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { // UI
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
                        Join_session()
                        Spacer(Modifier.height(32.dp))
                        Create_session()
                    }
                }
            }
        }
    }
}

// this is the join session button, it has the logic for joining a session inside it
@Composable
fun Join_session() {
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
                    if (snapshot.exists()){
                        val newUser = user( //create a new user
                            userId = "user_${System.currentTimeMillis()}",
                        )
                        databaseRef.child(code_input).child("users").child(newUser.userId).setValue(newUser).addOnSuccessListener {
                            val Intent = Intent(context,MainActivity::class.java).apply{ // pass values to the main activity
                                putExtra("SESSION_ID", code_input)
                                putExtra("USER",newUser.userId)
                            }
                            context.startActivity(Intent) // move to main
                        }.addOnFailureListener{ e -> // error handling
                            val err_toast = Toast.makeText(context,"Failed to Join Session - Connection Issue", Toast.LENGTH_SHORT)
                            err_toast.show()
                        }
                    }else{
                        val err_toast = Toast.makeText(context,"Failed to Join Session - Code Invalid", Toast.LENGTH_SHORT)
                        err_toast.show()
                    }
                }
            }else{
                val err_toast = Toast.makeText(context,"Code must be 6 digits long", Toast.LENGTH_SHORT)
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
            if (it.length <=6 && it.all {char -> char.isDigit()}){
                code_input = it
            }
        },
        label = { Text("Enter 6-digit code")},
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), //only takes numerical input
        modifier = modifier
    )

}

// this is the create session button, has all the logic inside
@Composable
fun Create_session() {
    val database = Firebase.database
    val databaseRef = database.getReference("sessions")
    val context = LocalContext.current
    Button(
        onClick = {
            val id = Random.nextInt(100000, 999999).toString() //generates a session ID

            val newUser = user( //creates a new user
                userId = "user_${System.currentTimeMillis()}", //generates a user id
                role = "Admin"
            )

            val session = session( //creates the session
                session_Id = id,
                users = mapOf(newUser.userId to newUser)
            )
            databaseRef.child(id).setValue(session) // adds that session to the database
                .addOnSuccessListener {
                    val Intent = Intent(context,MainActivity::class.java).apply{ // passes values to main
                        putExtra("SESSION_ID", id)
                        putExtra("USER",newUser.userId)
                        putExtra("ROLE",newUser.role)
                    }
                    context.startActivity(Intent) // move to main
                }
                .addOnFailureListener{ e ->// error handling
                    val err_toast = Toast.makeText(context,"Failed to Create Session", Toast.LENGTH_SHORT)
                    err_toast.show()
                }
        }
    ) {
        Text("Create Session")
    }
}