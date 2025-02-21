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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Session_ID = intent?.getStringExtra("SESSION_ID").toString()
        User_ID = intent?.getStringExtra("USER").toString()
        when(intent?.action){

            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {
        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Tracking Location...")
            .setContentText("Quatermaster")
            .setSmallIcon(R.drawable.icon)
            .setOngoing(true)

        startForeground(1, notification.build())

        // Launch in serviceScope
        serviceScope.launch {
            locationClient.getLocationUpdates(10000L)
                .catch { e -> e.printStackTrace() }
                .collect { location ->  // Changed from onEach to collect
                    val lat = location.latitude.toString()
                    val long = location.longitude.toString()

                    val curlocref = databaseRef.child(Session_ID).child("users").child(User_ID).child("location")

                    // map the location data
                    val locationData = mapOf(
                        "latitude" to lat.toDouble(),
                        "longitude" to long.toDouble()
                    )

                    try {
                        // Use suspending setValue instead of callbacks for better error handling
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