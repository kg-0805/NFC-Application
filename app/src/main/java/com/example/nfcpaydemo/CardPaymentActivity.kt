package com.example.nfcpaydemo

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CardPaymentActivity : AppCompatActivity() {

    private lateinit var cardForm: LinearLayout
    private lateinit var processingScreen: LinearLayout
    private lateinit var otpScreen: LinearLayout
    private lateinit var successScreen: LinearLayout
    
    private lateinit var etCardNumber: EditText
    private lateinit var etExpiry: EditText
    private lateinit var etCVV: EditText
    private lateinit var etCardName: EditText
    private lateinit var etOTP: EditText
    private lateinit var ivCardType: android.widget.ImageView
    
    private lateinit var btnProceed: Button
    private lateinit var btnSubmitOTP: Button
    private lateinit var btnDone: Button
    private lateinit var btnBack: Button
    
    private lateinit var tvTimeTaken: TextView
    
    private var paymentStartTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_payment)
        
        initViews()
        setupClickListeners()
        setupExpiryFormatting()
        setupCardTypeDetection()
        setupFieldValidation()
    }

    private fun initViews() {
        cardForm = findViewById(R.id.cardForm)
        processingScreen = findViewById(R.id.processingScreen)
        otpScreen = findViewById(R.id.otpScreen)
        successScreen = findViewById(R.id.successScreen)
        
        etCardNumber = findViewById(R.id.etCardNumber)
        etExpiry = findViewById(R.id.etExpiry)
        etCVV = findViewById(R.id.etCVV)
        etCardName = findViewById(R.id.etCardName)
        etOTP = findViewById(R.id.etOTP)
        ivCardType = findViewById(R.id.ivCardType)
        
        btnProceed = findViewById(R.id.btnProceed)
        btnSubmitOTP = findViewById(R.id.btnSubmitOTP)
        btnDone = findViewById(R.id.btnDone)
        btnBack = findViewById(R.id.btnBack)
        
        tvTimeTaken = findViewById(R.id.tvTimeTaken)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }
        
        btnProceed.setOnClickListener {
            if (validateCardDetails()) {
                paymentStartTime = System.currentTimeMillis()
                showProcessingScreen()
            }
        }
        
        btnSubmitOTP.setOnClickListener {
            if (validateOTP()) {
                showProcessingScreen()
                val finalProcessingTime = (1500..3000).random()
                Handler(Looper.getMainLooper()).postDelayed({
                    showSuccessScreen()
                }, finalProcessingTime.toLong())
            }
        }
        
        btnDone.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun validateCardDetails(): Boolean {
        val cardNumber = etCardNumber.text.toString().trim()
        val expiry = etExpiry.text.toString().trim()
        val cvv = etCVV.text.toString().trim()
        val name = etCardName.text.toString().trim()
        
        when {
            cardNumber.length != 16 -> {
                showToast("Please enter a valid 16-digit card number")
                return false
            }
            getCardType(cardNumber) == "Unknown" -> {
                showToast("Invalid card type")
                return false
            }
            expiry.length != 5 || !expiry.contains("/") -> {
                showToast("Please enter expiry in MM/YY format")
                return false
            }
            !isValidExpiry(expiry) -> {
                showToast("Card has expired")
                return false
            }
            cvv.length != 3 -> {
                showToast("Please enter a valid 3-digit CVV")
                return false
            }
            name.isEmpty() -> {
                showToast("Please enter cardholder name")
                return false
            }
        }
        
        val cardType = getCardType(cardNumber)
        showToast("$cardType card detected")
        return true
    }
    
    private fun getCardType(cardNumber: String): String {
        return when {
            cardNumber.startsWith("4") -> "Visa"
            cardNumber.startsWith("5") || cardNumber.startsWith("2") -> "Mastercard"
            cardNumber.startsWith("60") || cardNumber.startsWith("65") || 
            cardNumber.startsWith("81") || cardNumber.startsWith("82") -> "RuPay"
            else -> "Unknown"
        }
    }
    
    private fun isValidExpiry(expiry: String): Boolean {
        val parts = expiry.split("/")
        val month = parts[0].toIntOrNull() ?: return false
        val year = parts[1].toIntOrNull() ?: return false
        
        if (month < 1 || month > 12) return false
        
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) % 100
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
        
        return when {
            year > currentYear -> true
            year == currentYear -> month >= currentMonth
            else -> false
        }
    }
    


    private fun validateOTP(): Boolean {
        val otp = etOTP.text.toString().trim()
        when {
            otp.length != 6 -> {
                showToast("Please enter a valid 6-digit OTP")
                return false
            }
            otp != "111111" && otp != "222222" -> {
                showToast("Invalid OTP")
                return false
            }
        }
        return true
    }

    private fun showProcessingScreen() {
        cardForm.visibility = View.GONE
        otpScreen.visibility = View.GONE
        successScreen.visibility = View.GONE
        processingScreen.visibility = View.VISIBLE
        
        val processingTime = (2000..5000).random()
        Handler(Looper.getMainLooper()).postDelayed({
            if (processingScreen.visibility == View.VISIBLE) {
                showOTPScreen()
            }
        }, processingTime.toLong())
    }

    private fun showOTPScreen() {
        cardForm.visibility = View.GONE
        processingScreen.visibility = View.GONE
        successScreen.visibility = View.GONE
        otpScreen.visibility = View.VISIBLE
    }

    private fun showSuccessScreen() {
        val duration = (System.currentTimeMillis() - paymentStartTime) / 1000.0
        
        cardForm.visibility = View.GONE
        processingScreen.visibility = View.GONE
        otpScreen.visibility = View.GONE
        successScreen.visibility = View.VISIBLE
        
        tvTimeTaken.text = "Time taken: ${String.format("%.2f", duration)} seconds"
    }

    private fun setupExpiryFormatting() {
        etExpiry.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString().replace("/", "")
                if (text.length >= 2 && !s.toString().contains("/")) {
                    etExpiry.setText("${text.substring(0, 2)}/${text.substring(2)}")
                    etExpiry.setSelection(etExpiry.text.length)
                }
            }
        })
    }
    
    private fun setupCardTypeDetection() {
        etCardNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val cardNumber = s.toString()
                when {
                    cardNumber.startsWith("4") -> {
                        ivCardType.setImageResource(R.drawable.ic_visa)
                        ivCardType.visibility = View.VISIBLE
                    }
                    cardNumber.startsWith("5") || cardNumber.startsWith("2") -> {
                        ivCardType.setImageResource(R.drawable.ic_mastercard)
                        ivCardType.visibility = View.VISIBLE
                    }
                    cardNumber.startsWith("60") || cardNumber.startsWith("65") || 
                    cardNumber.startsWith("81") || cardNumber.startsWith("82") -> {
                        ivCardType.setImageResource(R.drawable.ic_rupay)
                        ivCardType.visibility = View.VISIBLE
                    }
                    cardNumber.isEmpty() -> {
                        ivCardType.visibility = View.GONE
                    }
                }
            }
        })
    }
    
    private fun setupFieldValidation() {
        etCardNumber.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val cardNumber = etCardNumber.text.toString().trim()
                when {
                    cardNumber.length != 16 -> showToast("Please enter a valid 16-digit card number")
                    getCardType(cardNumber) == "Unknown" -> showToast("Invalid card type")
                }
            }
        }
        
        etExpiry.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val expiry = etExpiry.text.toString().trim()
                when {
                    expiry.length != 5 || !expiry.contains("/") -> showToast("Please enter expiry in MM/YY format")
                    !isValidExpiry(expiry) -> showToast("Card has expired")
                }
            }
        }
        
        etCVV.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val cvv = etCVV.text.toString().trim()
                if (cvv.length != 3) {
                    showToast("Please enter a valid 3-digit CVV")
                }
            }
        }
        
        etCardName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val name = etCardName.text.toString().trim()
                if (name.isEmpty()) {
                    showToast("Please enter cardholder name")
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}