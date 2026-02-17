package com.harborfresh.market.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.harborfresh.market.auth.LoginActivity
import com.harborfresh.market.common.SessionManager
import com.harborfresh.market.databinding.ActivityOnboarding3Binding

class Onboarding3Activity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboarding3Binding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboarding3Binding.inflate(layoutInflater)
        setContentView(binding.root)
        sessionManager = SessionManager(this)

        binding.btnGetStarted.setOnClickListener {
            // Mark onboarding as complete
            sessionManager.setOnboardingComplete(true)
            
            // Proceed to Login and clear the onboarding history
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
