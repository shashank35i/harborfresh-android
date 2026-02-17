package com.harborfresh.market.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.databinding.ActivityAdminSellerDetailBinding
import kotlinx.coroutines.launch

class AdminSellerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminSellerDetailBinding
    private var sellerId: Int = 0
    private val baseUrl = ApiClient.BASE_URL // match ApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminSellerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sellerId = intent.getIntExtra("seller_id", 0)

        setSupportActionBar(binding.toolbarSellerDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarSellerDetail.setNavigationOnClickListener { finish() }

        binding.btnApprove.setOnClickListener { updateStatus("approved") }
        binding.btnReject.setOnClickListener { updateStatus("rejected") }

        loadDetail()
    }

    private fun loadDetail() {
        if (sellerId == 0) {
            Toast.makeText(this, "Missing seller", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        lifecycleScope.launch {
            try {
                val resp = ApiClient.apiService.getSellerDetail(sellerId)
                if (resp.success && resp.data != null) {
                    val seller = resp.data.seller
                    val identity = resp.data.identity
                    binding.tvSellerName.text = seller.full_name ?: "Seller"
                    binding.tvSellerEmail.text = seller.business_email ?: "—"
                    binding.tvSellerPhone.text = seller.phone ?: "—"
                    binding.statusChip.text = seller.status ?: identity.verification_status ?: "pending"

                    binding.tvAadhaar.text = "Aadhaar: " + (if ((identity.aadhaar_verified ?: 0) == 1) "Verified" else "Pending")
                    binding.tvSelfie.text = "Selfie: " + (if ((identity.liveness_verified ?: 0) == 1) "Verified" else "Pending")
                    binding.tvFace.text = "Face match: " + (if ((identity.face_match_verified ?: 0) == 1) "Verified" else "Pending")
                    binding.tvPolice.text = "Police: " + (identity.police_verification_status ?: "pending").replaceFirstChar { it.uppercaseChar() }

                    binding.btnViewAadhaar.setOnClickListener {
                        if (identity.aadhaar_doc.isNullOrBlank()) {
                            Toast.makeText(this@AdminSellerDetailActivity, "No Aadhaar document uploaded", Toast.LENGTH_SHORT).show()
                        } else {
                            openDoc(identity.aadhaar_doc!!)
                        }
                    }
                    binding.btnViewSelfie.setOnClickListener {
                        if (identity.selfie_image.isNullOrBlank()) {
                            Toast.makeText(this@AdminSellerDetailActivity, "No selfie uploaded", Toast.LENGTH_SHORT).show()
                        } else {
                            openDoc(identity.selfie_image!!)
                        }
                    }
                } else {
                    Toast.makeText(this@AdminSellerDetailActivity, resp.message ?: "Unable to load", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminSellerDetailActivity, "Error loading seller", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openDoc(path: String) {
        val url = if (path.startsWith("http")) path else baseUrl + path.trimStart('/')
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, "Unable to open document", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatus(status: String) {
        if (sellerId == 0) return
        lifecycleScope.launch {
            try {
                val resp = ApiClient.apiService.updateSellerStatus(sellerId, status)
                if (resp.success) {
                    Toast.makeText(this@AdminSellerDetailActivity, "Seller $status", Toast.LENGTH_SHORT).show()
                    loadDetail()
                } else {
                    Toast.makeText(this@AdminSellerDetailActivity, resp.message ?: "Unable to update", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminSellerDetailActivity, "Error updating status", Toast.LENGTH_LONG).show()
            }
        }
    }
}
