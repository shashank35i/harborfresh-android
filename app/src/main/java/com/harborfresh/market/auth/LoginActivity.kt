package com.harborfresh.market.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import android.text.InputType
import com.harborfresh.market.HomeActivity
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.common.SessionManager
import com.harborfresh.market.databinding.ActivityLoginBinding
import com.harborfresh.market.identity.IdentityVerification1Activity
import com.harborfresh.market.identity.IdentityVerification2Activity
import com.harborfresh.market.identity.IdentityVerification3Activity
import com.harborfresh.market.identity.IdentityVerification4Activity
import com.harborfresh.market.identity.IdentityVerification5Activity
import com.harborfresh.market.identity.IdentityBigVerification1Activity
import com.harborfresh.market.identity.IdentityBigVerification2Activity
import com.harborfresh.market.identity.IdentityBigVerification3Activity
import com.harborfresh.market.identity.IdentityBigVerification4Activity
import retrofit2.HttpException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private var mode: String = "customer" // customer, seller, admin
    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        binding.btnBack.visibility = android.view.View.GONE

        binding.btnSignIn.isEnabled = true
        binding.btnSignIn.isClickable = true
        binding.btnEye.setOnClickListener { togglePassword() }

        binding.btnSeller.setOnClickListener { cycleMode() }

        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString()?.trim().orEmpty()

            // Basic validation (still runs)
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "A valid email is required"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.etPassword.error = "Password is required"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            }

            // Disable button to prevent double taps
            binding.btnSignIn.isEnabled = false

            lifecycleScope.launch {
                try {
                    if (mode == "seller") {
                        val resp = withContext(Dispatchers.IO) {
                            ApiClient.apiService.loginSeller(
                                mapOf("business_email" to email, "password" to password)
                            )
                        }
                        if (resp.success) {
                            sessionManager.setLoggedIn(true)
                            val sellerId = resp.seller_id ?: 0
                            sessionManager.saveSellerSession(
                                sellerId,
                                resp.status,
                                resp.verification_step
                            )
                            sessionManager.setRole("seller")
                            val target = withContext(Dispatchers.IO) {
                                try {
                                    ApiClient.apiService.getSellerStatus(sellerId)
                                } catch (_: Exception) {
                                    null
                                }
                            }
                            val step = target?.data?.seller?.verification_step ?: resp.verification_step ?: 0
                            val status = target?.data?.seller?.status ?: resp.status
                            val needsOnboarding = (status == null || !status.equals("approved", ignoreCase = true)) && step < 9
                            val intent = Intent(
                                this@LoginActivity,
                                if (needsOnboarding) nextScreenForStep(step) else com.harborfresh.market.seller.SellerDashboardActivity::class.java
                            ).apply {
                                putExtra("seller_id", sellerId)
                            }
                            Toast.makeText(this@LoginActivity, "Seller login success", Toast.LENGTH_SHORT).show()
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                resp.message ?: "Login failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val resp = withContext(Dispatchers.IO) {
                            ApiClient.apiService.login(
                                mapOf("email" to email, "password" to password)
                            )
                        }
                        if (resp.success) {
                            val roleFromApi = resp.role?.lowercase()
                            // Prevent admin accounts from entering customer mode
                            if (mode == "customer" && roleFromApi == "admin") {
                                Toast.makeText(this@LoginActivity, "Please use Admin login for admin accounts", Toast.LENGTH_LONG).show()
                                return@launch
                            }

                            sessionManager.setLoggedIn(true)
                            resp.user_id?.let { sessionManager.saveUserId(it) }
                            sessionManager.setRole(if (mode == "admin") "admin" else "customer")
                            if (mode == "admin") {
                                if (roleFromApi == "admin") {
                                    sessionManager.saveAdminInfo(resp.name, email)
                                    Toast.makeText(this@LoginActivity, "Admin login success", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this@LoginActivity, com.harborfresh.market.admin.AdminDashboardActivity::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(this@LoginActivity, "Not an admin account", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(this@LoginActivity, "Login success", Toast.LENGTH_SHORT).show()
                                goHome()
                            }
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                resp.message ?: "Login failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: HttpException) {
                    if (e.code() == 401) {
                        Toast.makeText(
                            this@LoginActivity,
                            "Invalid email or password",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Server error ${e.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    Log.e("LoginActivity", "Login http error", e)
                } catch (e: Exception) {
                    Log.e("LoginActivity", "Login error", e)
                    Toast.makeText(
                        this@LoginActivity,
                        "Login error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    binding.btnSignIn.isEnabled = true
                }
            }
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        binding.tvForgot.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        applyMode()
    }

    private fun goHome() {
        val intent = Intent(this@LoginActivity, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun applyMode() {
        when (mode) {
            "seller" -> {
                binding.tvTitle.text = "Seller login"
                binding.tvSubtitle.text = "Manage listings, payouts, and orders"
                binding.tvEmailLabel.text = "Business Email"
                binding.btnSignIn.text = "Sign In as Seller"
                binding.tvFisherman.text = "Are you a customer?"
                binding.btnSeller.text = "Sign in as Admin"
            }
            "admin" -> {
                binding.tvTitle.text = "Admin login"
                binding.tvSubtitle.text = "Review seller onboarding and compliance"
                binding.tvEmailLabel.text = "Admin Email"
                binding.btnSignIn.text = "Sign In as Admin"
                binding.tvFisherman.text = "Switch to customer?"
                binding.btnSeller.text = "Sign in as Customer"
            }
            else -> {
                binding.tvTitle.text = "Welcome back"
                binding.tvSubtitle.text = "Sign in to keep dock-to-door orders moving"
                binding.tvEmailLabel.text = "Email"
                binding.btnSignIn.text = "Sign In as Customer"
                binding.tvFisherman.text = "Are you a seller?"
                binding.btnSeller.text = "Sign in as Seller"
            }
        }
    }

    private fun cycleMode() {
        mode = when (mode) {
            "customer" -> "seller"
            "seller" -> "admin"
            else -> "customer"
        }
        applyMode()
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

    private fun nextScreenForStep(step: Int): Class<*> {
        return when {
            step <= 1 -> IdentityVerification1Activity::class.java
            step == 2 -> IdentityVerification2Activity::class.java
            step == 3 -> IdentityVerification3Activity::class.java
            step == 4 -> IdentityVerification4Activity::class.java
            step == 5 -> IdentityVerification5Activity::class.java
            step == 6 -> IdentityBigVerification1Activity::class.java
            step == 7 -> IdentityBigVerification2Activity::class.java
            step == 8 -> IdentityBigVerification3Activity::class.java
            step == 9 -> IdentityBigVerification4Activity::class.java
            else -> com.harborfresh.market.seller.SellerDashboardActivity::class.java
        }
    }
}
