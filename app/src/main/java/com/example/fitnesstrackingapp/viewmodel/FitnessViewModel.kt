package com.example.fitnesstrackingapp.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.fitnesstrackingapp.model.FitnessDataBase
import com.example.fitnesstrackingapp.model.FitnessRepository
import com.example.fitnesstrackingapp.model.WorkoutSession
import com.example.fitnesstrackingapp.service.FitnessTrackingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FitnessViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FitnessRepository
    val allSessions: LiveData<List<WorkoutSession>>

    init {
        val fitnessDao = FitnessDataBase.getDatabase(application).fitnessDao()
        repository = FitnessRepository(fitnessDao)
        allSessions = repository.allSessions.asLiveData()
    }

    fun insertSession(session: WorkoutSession) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSession(session)
        }
    }

    fun startTracking(context: Context) {
        val intent = Intent(context, FitnessTrackingService::class.java).apply {
            action = FitnessTrackingService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopTracking(context: Context) {
        val intent = Intent(context, FitnessTrackingService::class.java).apply {
            action = FitnessTrackingService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun deleteSession(session: WorkoutSession) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSession(session)
        }
    }

    fun deleteSessionById(sessionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSessionById(sessionId)
        }
    }






}
