package com.example.carsosphase1

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class CrashDetectionService : Service(), SensorEventListener, LocationListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private var accelerometer: Sensor? = null
    
    // Threshold: 20.0f for testing, 40.0f for real crash.
    private val IMPACT_THRESHOLD = 20.0f 
    private var impactDetected = false

    companion object {
        const val ACTION_CRASH_DETECTED = "com.example.carsosphase1.CRASH_DETECTED"
        const val ACTION_BLACK_SPOT_WARNING = "com.example.carsosphase1.BLACK_SPOT_WARNING"
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        // Use SENSOR_DELAY_NORMAL for better battery, or UI for a balance.
        // For actual crash detection, SENSOR_DELAY_GAME is often preferred.
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        
        setupLocationMonitoring()
        startForegroundService()
    }

    @SuppressLint("MissingPermission")
    private fun setupLocationMonitoring() {
        try {
            // BATTERY EFFICIENT: Update every 30 seconds or 100 meters
            // This is enough for "Black Spot" detection without draining battery
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                30000L, 
                100f, 
                this
            )
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                30000L, 
                100f, 
                this
            )
        } catch (e: Exception) {
            Log.e("CrashService", "Location updates failed: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            if (magnitude > IMPACT_THRESHOLD && !impactDetected) {
                impactDetected = true
                triggerEmergencyResponse()
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        // MOCK BLACK SPOT LOGIC: Check if current lat/lng is near a known accident zone
        // In a real app, you'd compare with a database or use Google Maps API Key here.
        checkBlackSpot(location.latitude, location.longitude)
    }

    private fun checkBlackSpot(lat: Double, lng: Double) {
        // Dummy check for simulation
        // For example: if (isNearBlackSpot(lat, lng)) { ... }
        Log.d("CrashService", "Checking black spots at $lat, $lng")
    }

    private fun triggerEmergencyResponse() {
        Log.e("CrashService", "!!! CRASH DETECTED !!!")
        
        val broadcastIntent = Intent(ACTION_CRASH_DETECTED)
        sendBroadcast(broadcastIntent)

        val activityIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        startActivity(activityIntent)

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            impactDetected = false
        }, 30000)
    }

    private fun startForegroundService() {
        val channelId = "crash_detection_channel"
        val channel = NotificationChannel(channelId, "RoadSoS Active", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("RoadSoS Protection Active")
            .setContentText("Monitoring sensors & location for your safety...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
