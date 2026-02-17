package com.harborfresh.market.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.databinding.ActivityForgotPasswordBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSendOtp.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Enter a valid email"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        ApiClient.apiService.sendOtp(mapOf("email" to email))
                    }
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        response.message ?: "",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        binding.btnResetPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val otp = binding.etOtp.text.toString().trim()
            val newPassword = binding.etNewPassword.text.toString()
            val confirm = binding.etConfirmPassword.text.toString()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Enter a valid email"
                return@setOnClickListener
            }

            if (otp.length != 6) {
                binding.etOtp.error = "6-digit OTP required"
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                binding.etNewPassword.error = "Min 6 characters"
                return@setOnClickListener
            }

            if (newPassword != confirm) {
                binding.etConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        ApiClient.apiService.resetPassword(
                            mapOf(
                                "email" to email,
                                "otp" to otp,
                                "password" to newPassword
                            )
                        )
                    }

                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        response.message ?: "",
                        Toast.LENGTH_LONG
                    ).show()

                    if (response.success) {
                        startActivity(Intent(this@ForgotPasswordActivity, LoginActivity::class.java))
                        finish()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
