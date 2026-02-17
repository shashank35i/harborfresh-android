package com.harborfresh.market.identity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.harborfresh.market.R
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.common.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IdentityVerification5Activity : AppCompatActivity() {
    private var sellerId: Int = 0
    private lateinit var sessionManager: SessionManager
    private var documentsReady: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_identity_verification5)

        sessionManager = SessionManager(this)
        sellerId = resolveSellerId(intent.getIntExtra("seller_id", 0))
        if (sellerId == 0) {
            Toast.makeText(this, "Missing seller session", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        onClick(R.id.btnBackTop) { finish() }
        onClick(R.id.btnBack) { finish() }
        onClick(R.id.btnContinue) { proceed() }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { ApiClient.apiService.getSellerStatus(sellerId) }
                resp.data?.let { data ->
                    documentsReady = data.documents.fishing_license && data.documents.government_id && data.documents.address_proof
                    sessionManager.saveSellerSession(data.seller.id, data.seller.status, data.seller.verification_step)
                }
            } catch (_: Exception) { documentsReady = false }
        }
    }

    private fun proceed() {
        if (!documentsReady) {
            Toast.makeText(this, "Please upload all documents first", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(this, IdentityBigVerification1Activity::class.java)
                .putExtra("seller_id", sellerId)
        )
        finish()
    }
}
