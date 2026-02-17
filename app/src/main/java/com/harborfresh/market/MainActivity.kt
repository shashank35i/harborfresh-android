package com.harborfresh.market

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.harborfresh.market.onboarding.Onboarding1Activity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivity(Intent(this, Onboarding1Activity::class.java))
        finish()
    }
}
