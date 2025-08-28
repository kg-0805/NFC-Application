package com.example.nfcpaydemo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class PaymentOptionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_options)
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        findViewById<Button>(R.id.btnCardPay).setOnClickListener {
            val intent = Intent(this, CardPaymentActivity::class.java)
            startActivityForResult(intent, 1001)
        }
        
        findViewById<Button>(R.id.btnNFCPay).setOnClickListener {
            val intent = Intent(this, NFCPaymentActivity::class.java)
            startActivityForResult(intent, 1003)
        }
        
        findViewById<Button>(R.id.btnUPIPay).setOnClickListener {
            val intent = Intent(this, UPIPaymentActivity::class.java)
            startActivityForResult(intent, 1002)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            setResult(RESULT_OK)
            finish()
        }
    }
}