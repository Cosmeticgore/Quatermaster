package com.example.fyp_prototype

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.Firebase
import com.google.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LocationService: Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient
    private lateinit var userdata: AppData


    var Session_ID = ""
    var User_ID = ""

    private val database = Firebase.database
    private val databaseRef = database.getReference("sessions")

    override
    fun onBind(intent: Intent?): IBinder? {

        return null
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
        userdata = AppData.getInstance(application)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Session_ID = userdata.Session_ID.value.toString()
        User_ID = userdata.user_ID.value.toString()
        when(intent?.action){

            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {
        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Tracking Location...")
            .setContentText("Quartermaster")
            .setSmallIcon(R.drawable.icon)
            .setOngoing(true)

        startForeground(1, notification.build())

        // Launch in serviceScope
        serviceScope.launch {
            locationClient.getLocationUpdates(15000L) // every 15 seconds
                .catch { e -> e.printStackTrace() }
                .collect { location ->
                    val lat = location.latitude.toString()
                    val long = location.longitude.toString()

                    val curlocref = databaseRef.child(Session_ID).child("users").child(User_ID).child("location")
                    // map the location data
                    val locationData = mapOf(
                        "latitude" to lat.toDouble(),
                        "longitude" to long.toDouble()
                    )

                    try {
                        curlocref.setValue(locationData).await()
                        Log.d("UpdateLocations", "Location updated successfully")
                    } catch (e: Exception) {
                        Log.e("UpdateLocations", "Failed to update location", e)
                    }
                }
        }
    }

    private fun stop(){
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

}