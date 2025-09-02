package com.example.nfcpaydemo

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.ToneGenerator
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class NFCPaymentActivity : AppCompatActivity() {

    private lateinit var nfcAdapter: NfcAdapter
    private var paymentStartTime = 0L
    
    private lateinit var nfcTapScreen: LinearLayout
    private lateinit var processingScreen: LinearLayout
    private lateinit var pinScreen: LinearLayout
    private lateinit var successScreen: LinearLayout
    private lateinit var failureScreen: LinearLayout
    
    private lateinit var btnBack: Button
    private lateinit var btnDone: Button
    private lateinit var btnTryAgain: Button
    private lateinit var btnSubmitPIN: Button
    private lateinit var etPIN: android.widget.EditText
    private lateinit var tvTimeTaken: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc_payment)
        
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        initViews()
        setupClickListeners()
        startNFCPayment()
    }

    private fun initViews() {
        nfcTapScreen = findViewById(R.id.nfcTapScreen)
        processingScreen = findViewById(R.id.processingScreen)
        pinScreen = findViewById(R.id.pinScreen)
        successScreen = findViewById(R.id.successScreen)
        failureScreen = findViewById(R.id.failureScreen)
        
        btnBack = findViewById(R.id.btnBack)
        btnDone = findViewById(R.id.btnDone)
        btnTryAgain = findViewById(R.id.btnTryAgain)
        btnSubmitPIN = findViewById(R.id.btnSubmitPIN)
        etPIN = findViewById(R.id.etPIN)
        tvTimeTaken = findViewById(R.id.tvTimeTaken)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }
        btnDone.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
        btnTryAgain.setOnClickListener {
            showNFCTapScreen()
            startNFCPayment()
        }
        btnSubmitPIN.setOnClickListener {
            val pin = etPIN.text.toString().trim()
            if (pin == "2580") {
                showProcessingScreen()
                Handler(Looper.getMainLooper()).postDelayed({
                    val duration = (System.currentTimeMillis() - paymentStartTime) / 1000.0
                    paymentStartTime = 0L
                    showSuccessScreen(duration)
                }, 2000)
            } else {
                android.widget.Toast.makeText(this, "Incorrect PIN", android.widget.Toast.LENGTH_SHORT).show()
                etPIN.text.clear()
            }
        }
    }

    private fun startNFCPayment() {
        paymentStartTime = System.currentTimeMillis()
        showNFCTapScreen()
    }

    private fun showNFCTapScreen() {
        nfcTapScreen.visibility = View.VISIBLE
        processingScreen.visibility = View.GONE
        pinScreen.visibility = View.GONE
        successScreen.visibility = View.GONE
        failureScreen.visibility = View.GONE
    }
    
    private fun showPINScreen() {
        nfcTapScreen.visibility = View.GONE
        processingScreen.visibility = View.GONE
        pinScreen.visibility = View.VISIBLE
        successScreen.visibility = View.GONE
        failureScreen.visibility = View.GONE
        etPIN.text.clear()
    }

    private fun showProcessingScreen() {
        nfcTapScreen.visibility = View.GONE
        processingScreen.visibility = View.VISIBLE
        pinScreen.visibility = View.GONE
        successScreen.visibility = View.GONE
        failureScreen.visibility = View.GONE
    }

    private fun showSuccessScreen(duration: Double) {
        nfcTapScreen.visibility = View.GONE
        processingScreen.visibility = View.GONE
        pinScreen.visibility = View.GONE
        successScreen.visibility = View.VISIBLE
        failureScreen.visibility = View.GONE
        
        tvTimeTaken.text = "Time taken: ${String.format("%.2f", duration)} seconds"
        playTone(ToneGenerator.TONE_PROP_ACK)
    }

    private fun showFailureScreen() {
        nfcTapScreen.visibility = View.GONE
        processingScreen.visibility = View.GONE
        pinScreen.visibility = View.GONE
        successScreen.visibility = View.GONE
        failureScreen.visibility = View.VISIBLE
        
        playTone(ToneGenerator.TONE_PROP_NACK)
    }

    private fun playTone(toneType: Int) {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGenerator.startTone(toneType, 200)
            Handler(Looper.getMainLooper()).postDelayed({
                toneGenerator.release()
            }, 300)
        } catch (e: Exception) {
            // Ignore if sound fails
        }
    }

    override fun onResume() {
        super.onResume()
        if (::nfcAdapter.isInitialized) {
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
            val filters = arrayOf(
                IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                    addDataType("text/plain")
                },
                IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
            )
            val techLists = arrayOf(
                arrayOf("android.nfc.tech.Ndef"),
                arrayOf("android.nfc.tech.NfcA"),
                arrayOf("android.nfc.tech.IsoDep")
            )
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, techLists)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::nfcAdapter.isInitialized) {
            nfcAdapter.disableForegroundDispatch(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        
        if (paymentStartTime == 0L) return
        
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        tag?.let {
            showProcessingScreen()
            
            Handler(Looper.getMainLooper()).postDelayed({
                val duration = (System.currentTimeMillis() - paymentStartTime) / 1000.0
                
                try {
                    val ndef = Ndef.get(it)
                    if (ndef != null) {
                        ndef.connect()
                        val message: NdefMessage? = ndef.ndefMessage
                        val isValidPayment = message?.records?.firstOrNull()?.let { record ->
                            val payload = record.payload
                            if (payload.size > 3) {
                                val text = String(payload, 3, payload.size - 3, Charsets.UTF_8)
                                text.trim().equals("PAY", ignoreCase = true)
                            } else false
                        } ?: false
                        
                        ndef.close()
                        
                        if (isValidPayment) {
                            showPINScreen()
                        } else {
                            showFailureScreen()
                            paymentStartTime = System.currentTimeMillis()
                        }
                    } else {
                        showFailureScreen()
                        paymentStartTime = System.currentTimeMillis()
                    }
                } catch (e: Exception) {
                    showFailureScreen()
                    paymentStartTime = System.currentTimeMillis()
                }
            }, 2000) // 2 second processing delay
        }
    }
}