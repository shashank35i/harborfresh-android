package com.harborfresh.market.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.databinding.ActivityOtpVerificationBinding
import com.harborfresh.market.HomeActivity
import kotlinx.coroutines.launch

class OtpVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOtpVerificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email = intent.getStringExtra("email")

        binding.btnVerify.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()

            if (otp.isEmpty() || otp.length < 6) {
                binding.etOtp.error = "A valid 6-digit OTP is required"
                binding.etOtp.requestFocus()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = ApiClient.apiService.verifyOtp(mapOf("email" to email!!, "otp" to otp))

                    if (response.success) {
                        Toast.makeText(this@OtpVerificationActivity, "Verification successful! Please log in.", Toast.LENGTH_LONG).show()

                        val intent = Intent(this@OtpVerificationActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@OtpVerificationActivity, response.message ?: "OTP verification failed", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@OtpVerificationActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
