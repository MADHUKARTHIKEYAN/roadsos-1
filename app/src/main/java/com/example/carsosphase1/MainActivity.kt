package com.example.carsosphase1

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    // EDIT YOUR NUMBER HERE
    private val EMERGENCY_NUMBER = "112"

    private lateinit var tvStatusTitle: TextView
    private lateinit var tvStatusSubtitle: TextView
    private lateinit var tvTimer: TextView
    private lateinit var ivStatusIcon: ImageView
    private lateinit var btnStart: Button
    private lateinit var btnThemeToggle: ImageButton
    
    private var countDownTimer: CountDownTimer? = null
    private var vibrator: Vibrator? = null
    private var ringtone: Ringtone? = null
    
    private var isCrashDetected = false
    private var isMonitoring = false

    private var speechRecognizer: SpeechRecognizer? = null

    private val crashReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            startEmergencyCountdown()
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            startSafetySystems()
        } else {
            Toast.makeText(this, "Permissions required for all safety features", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeComponents()

        if (checkAllPermissions()) {
            startSafetySystems()
        } else {
            updateUI(State.INACTIVE)
        }
    }

    private fun initializeComponents() {
        tvStatusTitle = findViewById(R.id.tvStatusTitle)
        tvStatusSubtitle = findViewById(R.id.tvStatusSubtitle)
        tvTimer = findViewById(R.id.tvTimer)
        ivStatusIcon = findViewById(R.id.ivStatusIcon)
        btnStart = findViewById(R.id.btnStart)
        btnThemeToggle = findViewById(R.id.btnThemeToggle)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(applicationContext, notificationUri)

        btnStart.setOnClickListener {
            when {
                isCrashDetected -> cancelEmergency()
                isMonitoring -> stopSafetySystems()
                else -> checkAndStartServices()
            }
        }

        // Feature icon listeners
        findViewById<LinearLayout>(R.id.featureIcons).getChildAt(2).setOnClickListener {
            startActivity(Intent(this, TriageActivity::class.java))
        }

        btnThemeToggle.setOnClickListener {
            val currentMode = AppCompatDelegate.getDefaultNightMode()
            AppCompatDelegate.setDefaultNightMode(
                if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) AppCompatDelegate.MODE_NIGHT_NO 
                else AppCompatDelegate.MODE_NIGHT_YES
            )
        }
    }

    private fun checkAllPermissions(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun checkAndStartServices() {
        if (checkAllPermissions()) {
            startSafetySystems()
        } else {
            val permissions = mutableListOf(
                Manifest.permission.CALL_PHONE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            requestPermissionsLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun startSafetySystems() {
        startCrashService()
        startVoiceDetection()
    }

    private fun stopSafetySystems() {
        stopCrashService()
        speechRecognizer?.stopListening()
        isMonitoring = false
        updateUI(State.INACTIVE)
    }

    private fun startVoiceDetection() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches?.any { it.contains("help", ignoreCase = true) } == true) {
                    startEmergencyCountdown()
                }
                if (isMonitoring) speechRecognizer?.startListening(speechIntent)
            }
            override fun onError(error: Int) {
                if (isMonitoring) speechRecognizer?.startListening(speechIntent)
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(speechIntent)
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(CrashDetectionService.ACTION_CRASH_DETECTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(crashReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(crashReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(crashReceiver)
        stopAlerts()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }

    private fun startCrashService() {
        val intent = Intent(this, CrashDetectionService::class.java)
        startForegroundService(intent)
        isMonitoring = true
        updateUI(State.MONITORING)
    }

    private fun stopCrashService() {
        val intent = Intent(this, CrashDetectionService::class.java)
        stopService(intent)
    }

    private fun startEmergencyCountdown() {
        if (isCrashDetected) return
        isCrashDetected = true
        updateUI(State.CRASH)
        startAlerts()

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000) + 1
                tvTimer.text = seconds.toString()
            }

            override fun onFinish() {
                tvTimer.text = "0"
                makeEmergencyCall()
                // Now, only dial. Triage AI is manual or requested via the button below.
                Toast.makeText(this@MainActivity, "Emergency Dialed. Tap 'AI Triage' icon for hospital routing.", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun cancelEmergency() {
        countDownTimer?.cancel()
        stopAlerts()
        isCrashDetected = false
        updateUI(State.MONITORING)
        Toast.makeText(this, "Emergency Cancelled", Toast.LENGTH_SHORT).show()
    }

    private fun makeEmergencyCall() {
        stopAlerts()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$EMERGENCY_NUMBER"))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } else {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$EMERGENCY_NUMBER"))
            startActivity(intent)
        }
    }

    private fun startAlerts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200), 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 500, 200), 0)
        }
        if (ringtone?.isPlaying == false) ringtone?.play()
    }

    private fun stopAlerts() {
        vibrator?.cancel()
        if (ringtone?.isPlaying == true) ringtone?.stop()
    }

    enum class State { INACTIVE, MONITORING, CRASH }

    private fun updateUI(state: State) {
        when (state) {
            State.INACTIVE -> {
                tvStatusTitle.text = "RoadSoS Inactive"
                tvStatusTitle.setTextColor(Color.GRAY)
                tvStatusSubtitle.text = "Background protection is off"
                ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_info)
                ivStatusIcon.setColorFilter(Color.GRAY)
                btnStart.text = "Start Protection"
                tvTimer.visibility = View.GONE
            }
            State.MONITORING -> {
                tvStatusTitle.text = "Protection Active"
                tvStatusTitle.setTextColor(Color.parseColor("#4CAF50"))
                tvStatusSubtitle.text = "Sensors & Voice Monitoring in background"
                ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_info)
                ivStatusIcon.setColorFilter(Color.parseColor("#4CAF50"))
                btnStart.text = "Stop Monitoring"
                tvTimer.visibility = View.GONE
            }
            State.CRASH -> {
                tvStatusTitle.text = "CRASH DETECTED!"
                tvStatusTitle.setTextColor(Color.RED)
                tvStatusSubtitle.text = "Calling $EMERGENCY_NUMBER automatically in..."
                ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                ivStatusIcon.setColorFilter(Color.RED)
                btnStart.text = "I AM SAFE (CANCEL)"
                tvTimer.visibility = View.VISIBLE
            }
        }
    }
}
