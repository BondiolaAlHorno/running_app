package com.example.running_app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.running_app.databinding.ActivityMainBinding
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.widget.Toast
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import android.os.Handler

class MainActivity : AppCompatActivity() {

    private var appSwitch:Boolean = false

    private lateinit var binding: ActivityMainBinding

    private var LOCATION_PERMISSION_REQUEST_CODE = 1001

    private var totalDistance: Float = 0f // Distancia total en metros
    private var currentSpeed: Float = 0f // Velocidad actual en km/h

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var previousLocation: Location? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { currentLocation ->
                // 1. Calcular velocidad
                currentSpeed = currentLocation.speed * 3.6f // m/s → km/h

                // 2. Calcular distancia (solo si hay ubicación previa)
                previousLocation?.let { prevLocation ->
                    totalDistance += prevLocation.distanceTo(currentLocation)
                }
                previousLocation = currentLocation

                updateUI()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Inicializar FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initTimer()

        if (!checkLocationPermission()){
            requestLocationPermission()
        }

        binding.btnStart.setOnClickListener(){
            if (!appSwitch){
                if (checkLocationPermission()){
                    resetTrackingData()
                    startTracking()
                    binding.playStop.setImageResource(R.drawable.stop)
                    resetChronometer()
                    initChronometer()
                    appSwitch=true
                }
                else{
                    Toast.makeText(this, "Se requieren los permisos para acceder a la localizacion", Toast.LENGTH_SHORT).show()
                    requestLocationPermission()
                }
            }
            else{
                stopTracking()
                binding.playStop.setImageResource(R.drawable.play)
                stopChronometer()
                updateUIavarageSpeed()
                appSwitch=false
            }
        }

    }

    // Verificar permisos (usa el contexto de la actividad)
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Solicitar permisos (usa la actividad actual)
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun isGpsEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun startTracking() {
        if (!isGpsEnabled()) {
            Toast.makeText(this, "Activa el GPS", Toast.LENGTH_SHORT).show()
            return
        }
        else {
            if (checkLocationPermission()) {
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    5000
                ).apply {
                    setWaitForAccurateLocation(false)
                }.build()
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } else {
                requestLocationPermission()
            }
        }
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun resetTrackingData() {
        totalDistance = 0f
        currentSpeed = 0f
        previousLocation = null
    }

    private fun updateUI() {
        binding.speed.text = "Velocidad: ${"%.2f".format(currentSpeed)} km/h"
        binding.distance.text = "Distancia: ${"%.2f".format(totalDistance/1000)} km"
    }

    private fun updateUIavarageSpeed(){
        if ((totalDistance.toInt() != 0) && (elapsedTimeInSeconds!=0)){
            var avaragespeed = (totalDistance/1000.0)/(elapsedTimeInSeconds.toDouble()/3600.0)
            binding.speed.text = "Velocidad promedio: ${String.format("%.2f", avaragespeed)} km/h"
        }
        else{
            binding.speed.text = "Velocidad promedio: 0 km/h"
        }

    }

    private var elapsedTimeInSeconds = 0
    private lateinit var handler: Handler
    private lateinit var timerRunnable: Runnable

    private fun initTimer() {
        handler = Handler(Looper.getMainLooper())
        timerRunnable = object : Runnable {
            override fun run() {
                updateTimerDisplay()
                elapsedTimeInSeconds++
                handler.postDelayed(this, 1000)
            }
        }
    }

    private fun updateTimerDisplay() {
        val hours = elapsedTimeInSeconds / 3600
        val minutes = (elapsedTimeInSeconds % 3600) / 60
        val seconds = elapsedTimeInSeconds % 60
        val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        binding.timeMarker.text = "Tiempo: $timeString"
    }

    private fun initChronometer(){
        handler.post(timerRunnable)
    }

    private fun resetChronometer(){
        elapsedTimeInSeconds = 0
    }

    private fun stopChronometer(){
        handler.removeCallbacks(timerRunnable)
    }
}