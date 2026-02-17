package com.harborfresh.market.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import android.text.InputType
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.databinding.ActivitySignupBinding
import com.harborfresh.market.identity.IdentityVerification1Activity
import com.harborfresh.market.common.SessionManager
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var sessionManager: SessionManager
    private var passwordVisible = false
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sessionManager = SessionManager(this)

        binding.btnRegisterSeller.setOnClickListener { showRoleSheet() }

        binding.tvSignIn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnEye.setOnClickListener { togglePassword() }

        applyMode()

        binding.btnSignup.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhoneNumber.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (!validateCommon(name, email, phone, password, seller = false)) return@setOnClickListener
            registerCustomerFlow(name, email, phone, password)
        }
    }

    private fun showRoleSheet() {
        val sheet = BottomSheetDialog(this)
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        fun buildButton(text: String, onClick: () -> Unit): MaterialButton {
            return MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 12 }
                this.text = text
                this.isAllCaps = false
                setOnClickListener {
                    sheet.dismiss()
                    onClick()
                }
            }
        }
        container.addView(buildButton("Register as Seller") {
            startActivity(Intent(this, IdentityVerification1Activity::class.java))
        })
        container.addView(buildButton("Register as Admin") {
            registerAdmin()
        })
        sheet.setContentView(container)
        sheet.show()
    }

    private fun registerAdmin() {
        val name = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhoneNumber.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        if (!validateCommon(name, email, phone, password, seller = false)) return

        lifecycleScope.launch {
            showLoading(true)
            try {
                val signupResponse = ApiClient.apiService.signup(
                    mapOf(
                        "full_name" to name,
                        "email" to email,
                        "phone" to phone,
                        "password" to password,
                        "role" to "admin"
                    )
                )
                if (signupResponse.success) {
                    Toast.makeText(this@SignupActivity, "Admin created. Please login as admin.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@SignupActivity, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@SignupActivity, signupResponse.message ?: "Admin signup failed", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SignupActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun registerCustomerFlow(name: String, email: String, phone: String, password: String) {
        lifecycleScope.launch {
            showLoading(true)
            try {
                val signupResponse = ApiClient.apiService.signup(
                    mapOf(
                        "full_name" to name,
                        "email" to email,
                        "phone" to phone,
                        "password" to password,
                        "role" to "customer"
                    )
                )

                if (signupResponse.success) {
                    // Fire-and-forget OTP; user already gets email even if backend replies error
                    try { ApiClient.apiService.sendOtp(mapOf("email" to email)) } catch (_: Exception) { }
                    val intent = Intent(this@SignupActivity, OtpVerificationActivity::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@SignupActivity, signupResponse.message ?: "Signup failed", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SignupActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun validateCommon(name: String, email: String, phone: String, password: String, seller: Boolean): Boolean {
        if (name.isEmpty() || !name.matches(Regex("[a-zA-Z ]+"))) {
            binding.etFullName.error = "A valid name is required"
            binding.etFullName.requestFocus()
            return false
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "A valid email is required"
            binding.etEmail.requestFocus()
            return false
        }
        if (phone.isEmpty() || phone.length != 10) {
            binding.etPhoneNumber.error = "A valid 10-digit phone number is required"
            binding.etPhoneNumber.requestFocus()
            return false
        }
        if (password.isEmpty() || password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            binding.etPassword.requestFocus()
            return false
        }
        return true
    }

    private fun applyMode() {
        binding.tvTitle.text = "Create account"
        binding.tvSubtitle.text = "Create your HarborFresh account for dock-to-door delivery"
        binding.tvEmailLabel.text = "Email"
        binding.btnSignup.text = "Create Account"
        binding.btnRegisterSeller.text = "Register as seller"
    }

    private fun togglePassword() {
        passwordVisible = !passwordVisible
        val selection = binding.etPassword.selectionEnd
        binding.etPassword.inputType = if (passwordVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        binding.etPassword.setSelection(selection)
    }

    private fun showLoading(show: Boolean) {
        isLoading = show
        binding.progressSignup.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnSignup.isEnabled = !show
        binding.btnRegisterSeller.isEnabled = !show
        binding.btnSignup.text = if (show) "Creating..." else "Create Account"
    }
}
