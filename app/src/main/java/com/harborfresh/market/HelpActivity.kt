package com.harborfresh.market

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        findViewById<View?>(R.id.btnBack)?.setOnClickListener { finish() }
    }
}
