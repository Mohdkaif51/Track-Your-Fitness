package com.example.fitnesstrackingapp.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FitnessDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(session: WorkoutSession): Long

    @Update
    suspend fun updateWorkoutSession(session: WorkoutSession)

    @Delete
    suspend fun deleteWorkoutSession(session: WorkoutSession)

    @Query("DELETE FROM location_points WHERE sessionId = :sessionId")
    suspend fun deleteLocationPointsForSession(sessionId: Long)

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): WorkoutSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationPoint(point: LocationPoint): Long

    @Query("SELECT * FROM location_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getLocationPointsForSession(sessionId: Long): List<LocationPoint>
}
