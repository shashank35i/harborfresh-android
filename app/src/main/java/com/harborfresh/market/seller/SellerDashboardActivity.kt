package com.harborfresh.market.seller

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.harborfresh.market.R
import com.harborfresh.market.common.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class SellerDashboardActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_dashboard)

        sessionManager = SessionManager(this)
        bottomNav = findViewById(R.id.sellerBottomNav)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_seller_home -> {
                    openFragment(SellerHomeFragment())
                    true
                }
                R.id.nav_seller_orders -> {
                    openFragment(SellerOrdersFragment())
                    true
                }
                R.id.nav_seller_profile -> {
                    openFragment(SellerProfileFragment())
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_seller_home
        }
    }

    private fun openFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.sellerFragmentContainer, fragment)
            .commit()
    }
}
