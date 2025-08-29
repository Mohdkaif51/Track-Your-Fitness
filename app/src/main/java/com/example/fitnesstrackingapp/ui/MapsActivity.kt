package com.example.fitnesstrackingapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.example.fitnesstrackingapp.R
import com.example.fitnesstrackingapp.model.FitnessDataBase
import com.example.fitnesstrackingapp.model.FitnessRepository
import com.example.fitnesstrackingapp.model.LocationPoint
import com.example.fitnesstrackingapp.viewmodel.FitnessViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val polylineOptions = PolylineOptions().width(8f).color(Color.BLUE)

    private lateinit var viewModel: FitnessViewModel
    private lateinit var repository: FitnessRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        viewModel = ViewModelProvider(this)[FitnessViewModel::class.java]
        repository = FitnessRepository(
            FitnessDataBase.getDatabase(this).fitnessDao()
        )

        viewModel.allSessions.observe(this) { sessions ->
            val lastSession = sessions.firstOrNull()
            lastSession?.id?.let { sessionId ->
                CoroutineScope(Dispatchers.IO).launch {
                    val points = repository.getSessionWithPoints(sessionId).points
                    runOnUiThread { drawRoute(points) }
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
        }
    }

    private fun drawRoute(points: List<LocationPoint>) {
        val latLngs = points.map { LatLng(it.latitude, it.longitude) }
        if (latLngs.isEmpty()) return

        // Reset polylineOptions each time to avoid endlessly growing polyline
        val options = PolylineOptions().width(8f).color(Color.BLUE).addAll(latLngs)
        mMap.addPolyline(options)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngs.first(), 15f))
    }
}
