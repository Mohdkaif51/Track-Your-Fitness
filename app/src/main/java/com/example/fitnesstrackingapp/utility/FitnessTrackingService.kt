package com.example.fitnesstrackingapp.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.session.PlaybackState.ACTION_STOP
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.fitnesstrackingapp.R
import com.example.fitnesstrackingapp.model.FitnessDao
import com.example.fitnesstrackingapp.model.FitnessDataBase
import com.example.fitnesstrackingapp.model.LocationPoint
import com.example.fitnesstrackingapp.model.WorkoutSession
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import com.google.firebase.database.FirebaseDatabase

class FitnessTrackingService : Service() {

    private lateinit var fitnessDao: FitnessDao
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager

    private var currentSessionId: Long = 0

    private var firebaseDb: DatabaseReference = FirebaseDatabase.getInstance().reference
    private var locationCallback: LocationCallback? = null

    // Tracking variables
    private var lastLocation: Location? = null
    private var totalDistance = 0.0  // meters
    private var totalCalories = 0.0  // kcal

    override fun onCreate() {
        super.onCreate()
        val database = FitnessDataBase.getDatabase(applicationContext)
        fitnessDao = database.fitnessDao()
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ✅ Create notification channel immediately
        createNotificationChannel()
        Log.d("FitnessService", "Notification channel created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        // ✅ Create notification first
        val initialNotification = createTrackingNotification("0.00 km", "0.0 kcal")
        notificationManager.notify(ONGOING_NOTIFICATION_ID, initialNotification)


        // ✅ Start foreground service with notification
        startForeground(ONGOING_NOTIFICATION_ID, initialNotification)

        CoroutineScope(Dispatchers.IO).launch {
            val session = WorkoutSession(
                startTime = System.currentTimeMillis(),
                workoutType = "Running"
            )
            currentSessionId = fitnessDao.insertWorkoutSession(session)
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        val request = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 3000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                handleNewLocation(location)

                CoroutineScope(Dispatchers.IO).launch {
                    val point = LocationPoint(
                        sessionId = currentSessionId,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = System.currentTimeMillis(),
                        speed = location.speed

                    )
                    Log.d("FitnessService", "onLocationResult: ${point}")

                    fitnessDao.insertLocationPoint(point)
                    uploadLocationToFirebase(point)
                    Log.d("FitnessService", "onLocationResult: ${uploadLocationToFirebase(point)}")
                }
            }
        }

        locationClient.requestLocationUpdates(
            request,
            locationCallback as LocationCallback,
            Looper.getMainLooper()
        )
    }

    //    calculate the cal and distance
    private fun handleNewLocation(location: Location): Location? {
        lastLocation?.let { prev ->
            val distance = prev.distanceTo(location)

            // meters
            if (distance > 0.5) { // filter noise
                totalDistance += distance
                totalCalories = totalDistance * 0.05 // rough formula

                Log.d("Fitnessdistance", "handleNewLocation: ${totalDistance}")
                Log.d("Fitnesscalorie", "handleNewLocation: ${totalCalories}")


                updateNotification() // ✅ Update notification when data changes
                CoroutineScope(Dispatchers.IO).launch {
                    val session =
                        fitnessDao.getAllSessions().first().find { it.id == currentSessionId }
                    session?.let {
                        val update = it.copy(
                            distance = totalDistance,
                            caloriesBurned = totalCalories
                        )
                        fitnessDao.updateWorkoutSession(
                            update
                        )
                    }
                }

            }
        }
        lastLocation = location
        return lastLocation
    }

    private fun stopTracking() {
        locationCallback?.let { locationClient.removeLocationUpdates(it) }
        locationCallback = null

        CoroutineScope(Dispatchers.IO).launch {
            val session = fitnessDao.getAllSessions().first().find { it.id == currentSessionId }
            if (session != null) {
                val updated = session.copy(
                    endTime = System.currentTimeMillis(),
                    distance = totalDistance,      // ✅ save distance
                    caloriesBurned = totalCalories       // ✅ save calories
                )
                fitnessDao.updateWorkoutSession(updated)
            }
            stopForeground(true)
            stopSelf()
        }
    }

    private fun createTrackingNotification(distance: String, calories: String): Notification {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Workout in progress")
            .setContentText("Distance: $distance | Calories: $calories")
            .setSmallIcon(R.drawable.ic_fitness) // ✅ Make sure this exists in res/drawable
            .setOngoing(true)
            .setOnlyAlertOnce(true) // ✅ Don't make sound on every update
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        Log.d("FitnessService", "createTrackingNotification: $distance , $calories")
        return notification
    }

    private fun updateNotification() {
        val distanceKm = "%.2f".format(totalDistance / 1000)
        val caloriesStr = "%.1f".format(totalCalories)

        val notification = createTrackingNotification("$distanceKm km", "$caloriesStr kcal")
        notificationManager.notify(ONGOING_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Fitness Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracking location and distance"
                enableVibration(true)
                lightColor = android.graphics.Color.RED
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }


//       private fun uploadLocationToFirebase(point: LocationPoint) {
//
//            val locationref = firebaseDb.child("locations")
//                .child(point.sessionId.toString())
//                .child("locations")
//                .push()
//
//
//            locationref.setValue(point)
//                .addOnSuccessListener {
//                    Log.d("Firebase", "uploadToFirebase: ")                }
//                .addOnFailureListener {
//                    Log.d("Firebase", "uploadToFirebase: ")
//                }
//
//
//        }
    }

    private fun uploadLocationToFirebase(point: LocationPoint) {

        val locationref = firebaseDb.child("locations")
            .child(point.sessionId.toString())
            .child("locations")
            .push()
        Log.d("hello", "uploadLocationToFirebase: ${locationref}")
        locationref.setValue(point)
            .addOnSuccessListener {
                Log.d("Firebase", "uploadToFirebase: ")
            }
            .addOnFailureListener {
                Log.d("Firebase", "uploadToFirebase: ")
            }


    }


    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ONGOING_NOTIFICATION_ID = 1234
        const val CHANNEL_ID = "fitness_tracking_channel"
    }
}
