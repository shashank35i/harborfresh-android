package com.harborfresh.market.identity

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.harborfresh.market.R
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.common.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IdentityVerification3Activity : AppCompatActivity() {
    private var sellerId: Int = 0
    private lateinit var sessionManager: SessionManager
    private lateinit var etLicense: EditText
    private lateinit var etGst: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_identity_verification3)

        sessionManager = SessionManager(this)
        sellerId = resolveSellerId(intent.getIntExtra("seller_id", 0))
        if (sellerId == 0) {
            Toast.makeText(this, "Missing seller session", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        etLicense = findViewById(R.id.etLicense)
        etGst = findViewById(R.id.etGst)

        onClick(R.id.btnBackTop) { finish() }
        onClick(R.id.btnBack) { finish() }
        onClick(R.id.btnContinue) { submitLegal() }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { ApiClient.apiService.getSellerStatus(sellerId) }
                resp.data?.let { data ->
                    sessionManager.saveSellerSession(data.seller.id, data.seller.status, data.seller.verification_step)
                }
            } catch (_: Exception) { }
        }
    }

    private fun submitLegal() {
        val license = etLicense.text.toString().trim()
        val gst = etGst.text.toString().trim()

        if (license.isEmpty()) {
            Toast.makeText(this, "Enter fishing license", Toast.LENGTH_SHORT).show()
            return
        }
        if (license.length < 6) {
            Toast.makeText(this, "License number must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        val body = mutableMapOf(
            "fishing_license" to license,
            "seller_id" to sellerId.toString()
        )
        if (gst.isNotEmpty()) body["gst_number"] = gst

        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.saveLegal(sellerId, body)
                }
                if (resp.success) {
                    startActivity(Intent(this@IdentityVerification3Activity, IdentityVerification4Activity::class.java).putExtra("seller_id", sellerId))
                } else {
                    Toast.makeText(this@IdentityVerification3Activity, resp.message ?: "Unable to save", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@IdentityVerification3Activity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
