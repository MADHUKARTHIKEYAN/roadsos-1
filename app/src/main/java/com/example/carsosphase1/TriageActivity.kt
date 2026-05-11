package com.example.carsosphase1

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TriageActivity : AppCompatActivity() {

    private lateinit var tvAIResult: TextView
    private lateinit var hospitalListContainer: LinearLayout
    private lateinit var btnNavigate: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_triage)

        tvAIResult = findViewById(R.id.tvAIResult)
        hospitalListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE
        }
        
        // Find the container in XML to add the hospital list
        findViewById<LinearLayout>(R.id.questionsContainer).addView(hospitalListContainer, 5)

        btnNavigate = findViewById(R.id.btnNavigate)

        findViewById<Button>(R.id.btnAnalyze).setOnClickListener {
            performTriage()
        }

        btnNavigate.setOnClickListener {
            navigateToHospital("hospital")
        }
    }

    private fun performTriage() {
        val bleeding = findViewById<RadioGroup>(R.id.rgBleeding).checkedRadioButtonId == R.id.rbBleedingYes
        val conscious = findViewById<RadioGroup>(R.id.rgConscious).checkedRadioButtonId == R.id.rbConsciousYes
        val canMove = findViewById<RadioGroup>(R.id.rgMove).checkedRadioButtonId == R.id.rbMoveYes

        val result = when {
            bleeding || !conscious -> "URGENT: Level 1 Trauma Centre required."
            !canMove -> "SERIOUS: Specialized Orthopedic Facility recommended."
            else -> "STABLE: General Hospital or Clinic recommended."
        }

        tvAIResult.text = "AI Recommendation: $result"
        tvAIResult.visibility = View.VISIBLE
        
        showNearbyHospitals(bleeding || !conscious)
    }

    private fun showNearbyHospitals(isUrgent: Boolean) {
        hospitalListContainer.removeAllViews()
        hospitalListContainer.visibility = View.VISIBLE

        val hospitals = if (isUrgent) {
            listOf("City Trauma Centre (2.4km)", "Regional ICU Hospital (5.1km)")
        } else {
            listOf("General Health Clinic (1.2km)", "Metro City Hospital (3.8km)")
        }

        hospitals.forEach { name ->
            val btn = Button(this).apply {
                text = "Alert & Navigate to $name"
                setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
                setTextColor(android.graphics.Color.WHITE)
                setOnClickListener {
                    alertHospital(name)
                    navigateToHospital(name)
                }
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 8, 0, 8)
                layoutParams = params
            }
            hospitalListContainer.addView(btn)
        }
    }

    private fun alertHospital(name: String) {
        // Mocking an AI-driven notification to the hospital
        Toast.makeText(this, "AI: Alerting $name... Sending patient vitals & location.", Toast.LENGTH_LONG).show()
        Log.d("TriageAI", "Alert sent to $name with API Key: ${AppConfig.AI_API_KEY}")
    }

    private fun navigateToHospital(query: String) {
        val gmmIntentUri = Uri.parse("geo:0,0?q=$query")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        startActivity(mapIntent)
    }
}

// Minimal log helper
object Log {
    fun d(tag: String, msg: String) = android.util.Log.d(tag, msg)
}
