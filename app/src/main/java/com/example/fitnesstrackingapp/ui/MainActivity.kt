package com.example.fitnesstrackingapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnesstrackingapp.R
import com.example.fitnesstrackingapp.model.WorkoutSession
import com.example.fitnesstrackingapp.viewmodel.FitnessViewModel
import com.google.android.gms.common.api.internal.LifecycleActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: FitnessViewModel


    private lateinit var btnStartTracking: Button
    private lateinit var btnStopTracking: Button
    private lateinit var tvTrackingStatus: TextView
    private lateinit var tvTotalSessions: TextView
    private lateinit var tvTotalDistance: TextView
    private lateinit var tvTotalCalories: TextView
    private lateinit var rvWorkoutSessions: RecyclerView
    private lateinit var adapter: WorkoutSessionAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            viewModel.startTracking(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val startTime = System.currentTimeMillis()
        splashScreen.setKeepOnScreenCondition {
            System.currentTimeMillis() - startTime < 3000
        }



        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()

        viewModel = ViewModelProvider(this)[FitnessViewModel::class.java]

        setupClickListeners()
        setupObservers()


        val mapbtn: FloatingActionButton = findViewById(R.id.fabNewWorkout)
        mapbtn.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }
//Firebase Token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM", "Device Token: $token")
        }


    }

    private fun initViews() {
        btnStartTracking = findViewById(R.id.btnStartTracking)
        btnStopTracking = findViewById(R.id.btnStopTracking)
        tvTrackingStatus = findViewById(R.id.tvTrackingStatus)
        tvTotalSessions = findViewById(R.id.tvTotalSessions)
        tvTotalDistance = findViewById(R.id.tvTotalDistance)
        tvTotalCalories = findViewById(R.id.tvTotalCalories)
        rvWorkoutSessions = findViewById(R.id.rvWorkoutSessions)
    }

    private fun setupRecyclerView() {
        adapter = WorkoutSessionAdapter(
            onItemClick = { session -> showSessionDetails(session) },
            onDeleteClick = { session ->
                showDeleteConfirmationDialog(session) { shouldDelete ->
                    if (shouldDelete)

                        viewModel.deleteSession(session)

//                    val position = adapter.currentList.indexOfFirst { it.id == session.id }
//                    if (position != -1) {
//                       adapter.deleteItem(position)
//                    }

                }
            }
        )

        rvWorkoutSessions.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            addItemDecoration(
                DividerItemDecoration(
                    this@MainActivity,
                    LinearLayoutManager.VERTICAL
                )
            )
        }

        setupSwipeToDelete()

    }


    private fun setupClickListeners() {
        btnStartTracking.setOnClickListener {
            if (checkLocationPermissions()) {
                viewModel.startTracking(this)
                tvTrackingStatus.text = getString(R.string.tracking_started)
                btnStartTracking.isEnabled = false
                btnStartTracking.alpha = 0.5f
                btnStopTracking.isEnabled = true
                btnStopTracking.alpha = 1.0f

                Toast.makeText(this, "Tracking started", Toast.LENGTH_SHORT).show()

            } else {
                requestLocationPermissions()

            }


        }

        btnStopTracking.setOnClickListener {
            viewModel.stopTracking(this)
            tvTrackingStatus.text = getString(R.string.tracking_stopped)
            btnStopTracking.isEnabled = false
            btnStopTracking.alpha = 0.5f
            btnStartTracking.isEnabled = true
            btnStartTracking.alpha = 1.0f

            Toast.makeText(this, "Tracking stopped", Toast.LENGTH_SHORT).show()
        }


        btnStopTracking.isEnabled = false
        btnStopTracking.alpha = 0.5f
        btnStartTracking.isEnabled = true
        btnStartTracking.alpha = 1.0f
    }

    private fun setupObservers() {
        viewModel.allSessions.observe(this) { sessions ->
            adapter.submitList(sessions)
            updateStatistics(sessions)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateStatistics(sessions: List<WorkoutSession>) {
        val totalSessions = sessions.size
        val totalDistance = sessions.sumOf { it.distance }
        val totalCalories = sessions.sumOf { it.caloriesBurned }

        tvTotalSessions.text = totalSessions.toString()


        tvTotalDistance.text = "%.2f".format(totalDistance / 1000)
        tvTotalCalories.text = "%.1f".format(totalCalories)
    }

    private fun showSessionDetails(session: WorkoutSession) {
        android.widget.Toast.makeText(
            this,
            "Session: ${session.workoutType} - ${"%.2f".format(session.distance / 1000)} km",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun showDeleteConfirmationDialog(session: WorkoutSession, onResult: (Boolean) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Delete Workout")
            .setMessage("Are you sure you want to delete this workout session?")
            .setPositiveButton("Delete") { _, _ -> onResult(true) }
            .setNegativeButton("Cancel") { dialog, _ ->
                onResult(false)
                dialog.dismiss()
            }
            .setOnCancelListener { onResult(false) }
            .show()
    }


    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val session = adapter.currentList[position]
                showDeleteConfirmationDialog(session) { shouldDelete ->
                    if (shouldDelete) {
                        viewModel.deleteSession(session)
                    } else {
                        adapter.notifyItemChanged(position)
                    }
                }
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(rvWorkoutSessions)
    }

    private fun checkLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }


}
