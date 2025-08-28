package com.example.nfcpaydemo

import android.nfc.tech.Ndef
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.widget.TextView
import android.nfc.NdefMessage
import android.nfc.Tag
import android.view.View
import android.widget.LinearLayout
import android.media.ToneGenerator
import android.media.AudioManager
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {

    private lateinit var nfcAdapter: NfcAdapter
    private var paymentStartTime = 0L
    private val cart = mutableMapOf<String, Int>()
    private val prices = mapOf("Apple" to 50, "Banana" to 30, "Orange" to 40)
    private val paymentTimes = mutableMapOf<String, Double>()

    private lateinit var cartItems: TextView
    private lateinit var totalAmount: TextView
    private lateinit var statusMessage: TextView
    private lateinit var btnPayNow: Button
    private lateinit var btnClearCart: Button
    
    private val paymentResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            cart.clear()
            updateCartUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        cartItems = findViewById(R.id.cartItems)
        totalAmount = findViewById(R.id.totalAmount)
        btnPayNow = findViewById(R.id.btnPayNow)
        btnClearCart = findViewById(R.id.clearCart)
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.addApple).setOnClickListener {
            cart["Apple"] = (cart["Apple"] ?: 0) + 1
            updateCartUI()
        }

        findViewById<Button>(R.id.addBanana).setOnClickListener {
            cart["Banana"] = (cart["Banana"] ?: 0) + 1
            updateCartUI()
        }

        findViewById<Button>(R.id.addOrange).setOnClickListener {
            cart["Orange"] = (cart["Orange"] ?: 0) + 1
            updateCartUI()
        }

        btnClearCart.setOnClickListener {
            cart.clear()
            updateCartUI()
        }

        btnPayNow.setOnClickListener {
            if (cart.isEmpty()) {
                showStatus(getString(R.string.cart_empty), android.R.color.holo_red_light)
                return@setOnClickListener
            }
            val intent = Intent(this, PaymentOptionsActivity::class.java)
            paymentResultLauncher.launch(intent)
        }
    }

    private fun updateCartUI() {
        val items = cart.entries.joinToString("\n") { "${it.key} x ${it.value}" }
        cartItems.text = if (items.isEmpty()) "No items added" else items
        val total = cart.map { prices[it.key]!! * it.value }.sum()
        totalAmount.text = "₹$total"
        
        val isEmpty = cart.isEmpty()
        btnPayNow.isEnabled = !isEmpty
        btnClearCart.isEnabled = !isEmpty
        btnPayNow.alpha = if (isEmpty) 0.5f else 1.0f
        btnClearCart.alpha = if (isEmpty) 0.5f else 1.0f
    }

    private fun startPayment(method: String) {
        if (cart.isEmpty()) {
            showStatus(getString(R.string.cart_empty), android.R.color.holo_red_light)
            return
        }

        paymentStartTime = System.currentTimeMillis()
        when (method) {
            "NFC" -> {
                showStatus("Tap your NFC card to pay", android.R.color.holo_blue_light)
            }
        }
    }



    private fun completePayment(method: String, duration: Double) {
        paymentTimes[method] = duration
        
        // Play success tone
        playTone(ToneGenerator.TONE_PROP_ACK)
        
        showStatus("✅ Payment Successful!", android.R.color.holo_green_light)
        showSpeedPopup(duration)
        
        cart.clear()
        updateCartUI()
    }

    private fun showSpeedPopup(paymentTime: Double) {
        val message = "🏆 Payment Successful!\n\nTime taken: ${String.format("%.2f", paymentTime)} seconds"
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Payment Complete")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setIcon(android.R.drawable.ic_dialog_info)
            .show()
    }

    private fun showStatus(message: String, colorRes: Int) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
                        showStatus("Processing payment...", android.R.color.holo_orange_light)
                        Handler(Looper.getMainLooper()).postDelayed({
                            paymentStartTime = 0L
                            completePayment("NFC", duration)
                        }, 2000) // 2 second processing delay
                    } else {
                        showStatus("Processing payment...", android.R.color.holo_orange_light)
                        Handler(Looper.getMainLooper()).postDelayed({
                            playTone(ToneGenerator.TONE_PROP_NACK)
                            showStatus("❌ Payment Failed - Invalid card", android.R.color.holo_red_light)
                            paymentStartTime = System.currentTimeMillis() // Reset timer
                        }, 2000) // 2 second processing delay
                    }
                } else {
                    showStatus("Processing payment...", android.R.color.holo_orange_light)
                    Handler(Looper.getMainLooper()).postDelayed({
                        playTone(ToneGenerator.TONE_PROP_NACK)
                        showStatus("❌ Payment Failed - Invalid card", android.R.color.holo_red_light)
                        paymentStartTime = System.currentTimeMillis() // Reset timer
                    }, 2000) // 2 second processing delay
                }
            } catch (e: Exception) {
                showStatus("Processing payment...", android.R.color.holo_orange_light)
                Handler(Looper.getMainLooper()).postDelayed({
                    playTone(ToneGenerator.TONE_PROP_NACK)
                    showStatus("❌ Payment Failed - Invalid card", android.R.color.holo_red_light)
                    paymentStartTime = System.currentTimeMillis() // Reset timer
                }, 2000) // 2 second processing delay
            }
        }
    }
}

