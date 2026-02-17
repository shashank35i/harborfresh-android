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

class IdentityVerification2Activity : AppCompatActivity() {
    private var sellerId: Int = 0
    private lateinit var sessionManager: SessionManager
    private lateinit var etBusiness: EditText
    private lateinit var etLocation: EditText
    private lateinit var etYears: EditText
    private lateinit var etSpeciality: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_identity_verification2)

        sessionManager = SessionManager(this)
        sellerId = resolveSellerId(intent.getIntExtra("seller_id", 0))
        if (sellerId == 0) {
            Toast.makeText(this, "Missing seller session", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        etBusiness = findViewById(R.id.etBusinessName)
        etLocation = findViewById(R.id.etLocation)
        etYears = findViewById(R.id.etYears)
        etSpeciality = findViewById(R.id.etSpeciality)

        onClick(R.id.btnBackTop) { finish() }
        onClick(R.id.btnBack) { finish() }
        onClick(R.id.btnContinue) { submitBusiness() }

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
            } catch (_: Exception) {
            }
        }
    }

    private fun submitBusiness() {
        val businessName = etBusiness.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val yearsText = etYears.text.toString().trim()
        val speciality = etSpeciality.text.toString().trim()

        if (businessName.isEmpty() || location.isEmpty() || yearsText.isEmpty() || speciality.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val years = yearsText.toIntOrNull() ?: 0
        if (years <= 0) {
            Toast.makeText(this, "Enter valid experience years", Toast.LENGTH_SHORT).show()
            return
        }

        val body = mapOf(
            "business_name" to businessName,
            "location" to location,
            "experience_years" to years.toString(),
            "specialty" to speciality,
            "city" to location,
            "seller_id" to sellerId.toString()
        )

        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.saveBusiness(sellerId, body)
                }
                if (resp.success) {
                    startActivity(Intent(this@IdentityVerification2Activity, IdentityVerification3Activity::class.java).putExtra("seller_id", sellerId))
                } else {
                    Toast.makeText(this@IdentityVerification2Activity, resp.message ?: "Unable to save", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@IdentityVerification2Activity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
