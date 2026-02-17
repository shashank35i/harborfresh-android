package com.harborfresh.market

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.harborfresh.market.auth.LoginActivity
import com.harborfresh.market.common.SessionManager
import com.harborfresh.market.onboarding.Onboarding1Activity
import com.harborfresh.market.identity.*

class SplashActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        setContentView(R.layout.activity_splash) // ✅ add this

        sessionManager = SessionManager(this)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = when {
                sessionManager.isLoggedIn() -> loggedInIntent()
                sessionManager.isOnboardingComplete() -> Intent(this, LoginActivity::class.java)
                else -> Intent(this, Onboarding1Activity::class.java)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }, 900)
    }


    private fun loggedInIntent(): Intent {
        return when (sessionManager.getRole()) {
            "seller" -> {
                val step = sessionManager.getSellerStep()
                val sellerId = sessionManager.getSellerId()
                val status = sessionManager.getSellerStatus()
                val goDashboard = status.equals("approved", true) || status.equals("submitted", true) || step >= 10
                val target = if (goDashboard) com.harborfresh.market.seller.SellerDashboardActivity::class.java else nextScreenForStep(step)
                Intent(this, target).apply { putExtra("seller_id", sellerId) }
            }
            "admin" -> Intent(this, com.harborfresh.market.admin.AdminDashboardActivity::class.java)
            else -> Intent(this, HomeActivity::class.java)
        }
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
