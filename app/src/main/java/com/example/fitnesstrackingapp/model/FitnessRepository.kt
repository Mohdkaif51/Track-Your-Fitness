package com.example.fitnesstrackingapp.model

import kotlinx.coroutines.flow.Flow

class FitnessRepository(private val fitnessDao: FitnessDao) {

    val allSessions: Flow<List<WorkoutSession>> = fitnessDao.getAllSessions()

    suspend fun getSessionWithPoints(sessionId: Long): SessionWithPoints {
        val session = fitnessDao.getSessionById(sessionId)
        val points = fitnessDao.getLocationPointsForSession(sessionId)
        return SessionWithPoints(session, points)
    }

    suspend fun deleteSession(session: WorkoutSession) {
        fitnessDao.deleteWorkoutSession(session)
        fitnessDao.deleteLocationPointsForSession(session.id)
    }

    suspend fun deleteSessionById(sessionId: Long) {
        fitnessDao.deleteLocationPointsForSession(sessionId)
        fitnessDao.getSessionById(sessionId)?.let {
            fitnessDao.deleteWorkoutSession(it)
        }
    }

    suspend fun insertSession(session: WorkoutSession): Long {
        return fitnessDao.insertWorkoutSession(session)
    }

    data class SessionWithPoints(
        val session: WorkoutSession?,
        val points: List<LocationPoint>
    )
}
