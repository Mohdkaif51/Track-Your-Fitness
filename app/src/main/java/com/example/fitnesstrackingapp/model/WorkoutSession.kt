package com.example.fitnesstrackingapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var startTime: Long,
    var endTime: Long? = null,
    var workoutType: String,
    var distance: Double = 0.0  ,     // Added to match UI usage
    var caloriesBurned: Double= 0.0

   ,     // Added to match UI usage
// Added to match UI usage
)

@Entity(tableName = "location_points")
data class LocationPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var sessionId: Long,
    var latitude: Double,
    var longitude: Double,
    var timestamp: Long,
    var speed: Float
)
