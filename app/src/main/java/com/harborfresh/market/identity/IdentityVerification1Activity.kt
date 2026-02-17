package com.harborfresh.market.identity

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.harborfresh.market.R
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.auth.SignupActivity
import com.harborfresh.market.common.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class IdentityVerification1Activity : AppCompatActivity() {
    private var sellerId: Int = 0
    private lateinit var sessionManager: SessionManager
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_identity_verification1)

        sessionManager = SessionManager(this)
        sellerId = resolveSellerId(intent.getIntExtra("seller_id", 0))

        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhoneNumber)
        etPassword = findViewById(R.id.etPassword)

        onClick(R.id.btnBackTop) { finish() }
        onClick(R.id.btnBack) { finish() }
        onClick(R.id.btnBackBottom) { finish() }
        onClick(R.id.btnContinue) {
            submitPersonal()
        }
        val customerId = resources.getIdentifier("tvRegisterCustomer", "id", packageName)
        if (customerId != 0) {
            onClick(customerId) {
                startActivity(Intent(this, SignupActivity::class.java))
                finish()
            }
        }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        if (sellerId == 0) return
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getSellerStatus(sellerId)
                }
                response.data?.let { data ->
                    etFullName.setText(data.seller.full_name ?: "")
                    etEmail.setText(data.seller.business_email ?: "")
                    etPhone.setText(data.seller.phone ?: "")
                    sessionManager.saveSellerSession(data.seller.id, data.seller.status, data.seller.verification_step)
                }
            } catch (_: Exception) {
                // ignore initial load errors
            }
        }
    }

    private fun submitPersonal() {
        val name = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phoneRaw = etPhone.text.toString().trim()
        val phoneDigitsRaw = phoneRaw.filter { it.isDigit() }
        // Accept +91/0 prefixes; keep last 10 digits if longer
        val phoneDigits = if (phoneDigitsRaw.length >= 10) phoneDigitsRaw.takeLast(10) else phoneDigitsRaw
        val password = etPassword.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || phoneDigits.length < 10) {
            Toast.makeText(this, "Fill all required fields (phone needs 10 digits)", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isNotEmpty() && !password.matches(Regex("^(?=.*[A-Za-z])(?=.*\\d).{8,}\$"))) {
            Toast.makeText(this, "Password must be 8+ chars with a number", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                if (sellerId == 0) {
                    val register = withContext(Dispatchers.IO) {
                        ApiClient.apiService.registerSeller(
                            mapOf(
                                "full_name" to name,
                                "business_email" to email,
                                "phone" to phoneDigits,
                                "password" to (if (password.isNotEmpty()) password else "TempPass123")
                            )
                        )
                    }
                    if (!register.success) {
                        Toast.makeText(this@IdentityVerification1Activity, register.message ?: "Registration failed", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    sellerId = register.seller_id ?: 0
                    sessionManager.saveSellerSession(sellerId, register.status, register.verification_step)
                }

                val body = mutableMapOf(
                    "full_name" to name,
                    "business_email" to email,
                    "phone" to phoneDigits
                )
                if (sellerId > 0) body["seller_id"] = sellerId.toString()
                if (password.isNotEmpty()) {
                    body["password"] = password
                }

                val resp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.savePersonal(sellerId, body)
                }
                if (resp.success) {
                    sessionManager.saveSellerSession(sellerId, resp.status, resp.verification_step)
                    startActivity(Intent(this@IdentityVerification1Activity, IdentityVerification2Activity::class.java).putExtra("seller_id", sellerId))
                } else {
                    Toast.makeText(this@IdentityVerification1Activity, resp.message ?: "Unable to save", Toast.LENGTH_LONG).show()
                }
            } catch (e: HttpException) {
                if (e.code() == 409) {
                    Toast.makeText(this@IdentityVerification1Activity, "Phone or email already registered. Please log in or use another number.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@IdentityVerification1Activity, "Server error ${e.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@IdentityVerification1Activity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

