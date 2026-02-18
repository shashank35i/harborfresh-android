package com.harborfresh.market

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat

class HomeActivity : AppCompatActivity() {

    private val COLOR_OFF by lazy { ContextCompat.getColor(this, R.color.hf_on_surface_variant) }
    private val COLOR_ON by lazy { ContextCompat.getColor(this, R.color.hf_primary) }

    private lateinit var ivHome: ImageView
    private lateinit var ivCategories: ImageView
    private lateinit var ivOrders: ImageView
    private lateinit var ivCart: ImageView
    private lateinit var ivProfile: ImageView

    private lateinit var tvHome: TextView
    private lateinit var tvCategoriesTab: TextView
    private lateinit var tvOrders: TextView
    private lateinit var tvCart: TextView
    private lateinit var tvProfile: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        ivHome = findViewById(R.id.ivHome)
        ivCategories = findViewById(R.id.ivCategories)
        ivOrders = findViewById(R.id.ivOrders)
        ivCart = findViewById(R.id.ivCart)
        ivProfile = findViewById(R.id.ivProfile)

        tvHome = findViewById(R.id.tvHome)
        tvCategoriesTab = findViewById(R.id.tvCategoriesTab)
        tvOrders = findViewById(R.id.tvOrders)
        tvCart = findViewById(R.id.tvCart)
        tvProfile = findViewById(R.id.tvProfile)

        // ✅ First screen: HomeFragment
        if (savedInstanceState == null) {
            when (intent.getStringExtra("open_tab")) {
                "orders" -> {
                    openFragment(OrdersFragment())
                    selectTab(Tab.ORDERS)
                }
                "cart" -> {
                    openFragment(CartFragment())
                    selectTab(Tab.CART)
                }
                "profile" -> {
                    openFragment(ProfileFragment())
                    selectTab(Tab.PROFILE)
                }
                "categories" -> {
                    openFragment(CategoriesFragment())
                    selectTab(Tab.CATEGORIES)
                }
                else -> {
                    openFragment(HomeFragment())
                    selectTab(Tab.HOME)
                }
            }
        }

        findViewById<android.view.View>(R.id.navHome).setOnClickListener {
            openFragment(HomeFragment())
            selectTab(Tab.HOME)
        }

        findViewById<android.view.View>(R.id.navCategories).setOnClickListener {
            openFragment(CategoriesFragment())
            selectTab(Tab.CATEGORIES)
        }

        findViewById<android.view.View>(R.id.navOrders).setOnClickListener {
            openFragment(OrdersFragment())
            selectTab(Tab.ORDERS)
        }

        findViewById<android.view.View>(R.id.navCart).setOnClickListener {
            openFragment(CartFragment())
            selectTab(Tab.CART)
        }

        findViewById<android.view.View>(R.id.navProfile).setOnClickListener {
            openFragment(ProfileFragment())
            selectTab(Tab.PROFILE)
        }
    }

    private enum class Tab { HOME, CATEGORIES, ORDERS, CART, PROFILE }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun selectTab(tab: Tab) {
        setItem(ivHome, tvHome, false)
        setItem(ivCategories, tvCategoriesTab, false)
        setItem(ivOrders, tvOrders, false)
        setItem(ivCart, tvCart, false)
        setItem(ivProfile, tvProfile, false)

        when (tab) {
            Tab.HOME -> setItem(ivHome, tvHome, true)
            Tab.CATEGORIES -> setItem(ivCategories, tvCategoriesTab, true)
            Tab.ORDERS -> setItem(ivOrders, tvOrders, true)
            Tab.CART -> setItem(ivCart, tvCart, true)
            Tab.PROFILE -> setItem(ivProfile, tvProfile, true)
        }
    }

    private fun setItem(icon: ImageView, label: TextView, selected: Boolean) {
        val c = if (selected) COLOR_ON else COLOR_OFF
        icon.setColorFilter(c)
        label.setTextColor(c)
    }
}
