package com.harborfresh.market.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.harborfresh.market.R
import com.harborfresh.market.databinding.ActivityAdminDashboardBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.adminBottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_admin_home -> {
                    showFragment(AdminHomeFragment())
                    true
                }
                R.id.nav_admin_profile -> {
                    showFragment(AdminProfileFragment())
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            binding.adminBottomNav.selectedItemId = R.id.nav_admin_home
            showFragment(AdminHomeFragment())
        }
    }

    private fun showFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.adminFragmentContainer, fragment)
            .commit()
    }
}


