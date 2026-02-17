package com.harborfresh.market.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.databinding.ActivityAdminAllSellersBinding
import com.harborfresh.market.model.AdminSellerSummary
import kotlinx.coroutines.launch

class AdminAllSellersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminAllSellersBinding
    private var sellers: List<AdminSellerSummary> = emptyList()
    private var filter: String = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminAllSellersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        filter = intent.getStringExtra("filter") ?: "all"

        setSupportActionBar(binding.toolbarAdminAll)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarAdminAll.setNavigationOnClickListener { finish() }

        binding.rvSellers.layoutManager = LinearLayoutManager(this)

        loadSellers()
    }

    private fun loadSellers() {
        lifecycleScope.launch {
            try {
                val resp = ApiClient.apiService.getAllSellers()
                if (resp.success) {
                    sellers = when (filter.lowercase()) {
                        "pending" -> resp.sellers.filter { (it.verification_status ?: it.status ?: "pending").lowercase() !in listOf("approved", "rejected") }
                        "approved" -> resp.sellers.filter { (it.verification_status ?: it.status ?: "").lowercase() == "approved" }
                        "rejected" -> resp.sellers.filter { (it.verification_status ?: it.status ?: "").lowercase() == "rejected" }
                        else -> resp.sellers
                    }
                    renderCounts()
                    val adapter = AdminSellerAdapter(sellers) { seller ->
                        val intent = Intent(this@AdminAllSellersActivity, AdminSellerDetailActivity::class.java)
                        intent.putExtra("seller_id", seller.id)
                        startActivity(intent)
                    }
                    binding.rvSellers.adapter = adapter
                } else {
                    Toast.makeText(this@AdminAllSellersActivity, resp.message ?: "Unable to load sellers", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminAllSellersActivity, "Error loading sellers", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun renderCounts() {
        val pending = sellers.count { (it.verification_status ?: it.status ?: "pending").lowercase() !in listOf("approved", "rejected") }
        val approved = sellers.count { (it.verification_status ?: it.status ?: "").lowercase() == "approved" }
        val rejected = sellers.count { (it.verification_status ?: it.status ?: "").lowercase() == "rejected" }
        binding.tvPendingValue.text = pending.toString()
        binding.tvApprovedValue.text = approved.toString()
        binding.tvRejectedValue.text = rejected.toString()
        binding.tvTotal.text = "${sellers.size} total"
    }
}

