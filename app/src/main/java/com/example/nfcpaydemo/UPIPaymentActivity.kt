package com.example.nfcpaydemo

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class UPIPaymentActivity : AppCompatActivity() {

    private lateinit var upiForm: LinearLayout
    private lateinit var approvalScreen: LinearLayout
    private lateinit var successScreen: LinearLayout
    
    private lateinit var etUPIAddress: EditText
    private lateinit var btnProceedUPI: Button
    private lateinit var btnDone: Button
    private lateinit var btnBack: Button
    
    private lateinit var tvCountdown: TextView
    private lateinit var tvTimeTaken: TextView
    
    private var paymentStartTime = 0L
    private var countdownHandler: Handler? = null
    private var countdownRunnable: Runnable? = null
    private var countdown = 40

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upi_payment)
        
        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        upiForm = findViewById(R.id.upiForm)
        approvalScreen = findViewById(R.id.approvalScreen)
        successScreen = findViewById(R.id.successScreen)
        
        etUPIAddress = findViewById(R.id.etUPIAddress)
        btnProceedUPI = findViewById(R.id.btnProceedUPI)
        btnDone = findViewById(R.id.btnDone)
        btnBack = findViewById(R.id.btnBack)
        
        tvCountdown = findViewById(R.id.tvCountdown)
        tvTimeTaken = findViewById(R.id.tvTimeTaken)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }
        
        btnProceedUPI.setOnClickListener {
            if (validateUPIAddress()) {
                paymentStartTime = System.currentTimeMillis()
                showApprovalScreen()
            }
        }
        
        btnDone.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun validateUPIAddress(): Boolean {
        val upiAddress = etUPIAddress.text.toString().trim()
        
        if (upiAddress.isEmpty()) {
            showToast("Please enter UPI address")
            return false
        }
        
        if (!upiAddress.contains("@")) {
            showToast("Please enter a valid UPI address")
            return false
        }
        
        return true
    }

    private fun showApprovalScreen() {
        upiForm.visibility = View.GONE
        successScreen.visibility = View.GONE
        approvalScreen.visibility = View.VISIBLE
        
        startCountdown()
        
        // Simulate approval after 30-40 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            showSuccessScreen()
        }, 35000) // 35 seconds
    }

    private fun startCountdown() {
        countdown = 40
        countdownHandler = Handler(Looper.getMainLooper())
        countdownRunnable = object : Runnable {
            override fun run() {
                if (countdown > 0) {
                    tvCountdown.text = "Waiting for approval... ${countdown}s"
                    countdown--
                    countdownHandler?.postDelayed(this, 1000)
                } else {
                    tvCountdown.text = "Processing..."
                }
            }
        }
        countdownRunnable?.let { countdownHandler?.post(it) }
    }

    private fun showSuccessScreen() {
        val duration = (System.currentTimeMillis() - paymentStartTime) / 1000.0
        
        // Stop countdown
        countdownRunnable?.let { countdownHandler?.removeCallbacks(it) }
        
        upiForm.visibility = View.GONE
        approvalScreen.visibility = View.GONE
        successScreen.visibility = View.VISIBLE
        
        tvTimeTaken.text = "Time taken: ${String.format("%.2f", duration)} seconds"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownRunnable?.let { countdownHandler?.removeCallbacks(it) }
    }
}