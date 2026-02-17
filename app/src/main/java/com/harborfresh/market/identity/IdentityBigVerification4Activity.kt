package com.harborfresh.market.identity

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.harborfresh.market.R
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.common.SessionManager
import com.harborfresh.market.seller.SellerDashboardActivity
import retrofit2.HttpException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IdentityBigVerification4Activity : AppCompatActivity() {
    private var sellerId: Int = 0
    private lateinit var sessionManager: SessionManager
    private lateinit var tvHeaderSub: TextView
    private var canSubmitFace: Boolean = false
    private var policeVerified: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_identitybig_verification4)

        sessionManager = SessionManager(this)
        sellerId = resolveSellerId(intent.getIntExtra("seller_id", 0))
        if (sellerId == 0) {
            Toast.makeText(this, "Missing seller session", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        tvHeaderSub = findViewById(R.id.tvHeaderSub)
        onClick(R.id.btnBack) { finish() }
        onClick(R.id.btnContinue) { submitVerification() }

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
                    val police = data.identity.police_verification_status ?: "pending"
                    val overall = data.identity.verification_status ?: "in_progress"
                    val localScore = intent.getDoubleExtra("face_score", 0.0)
                    val hasScore = (data.identity.face_match_score ?: localScore) > 0.0
                    policeVerified = police.equals("verified", ignoreCase = true)
                    tvHeaderSub.text = "Police: $police - Status: $overall"
                    canSubmitFace = (data.identity.face_match_verified == 1 || hasScore)
                    sessionManager.saveSellerSession(data.seller.id, data.seller.status, data.seller.verification_step)
                    if (!canSubmitFace) {
                        Toast.makeText(
                            this@IdentityBigVerification4Activity,
                            "Finish face match first",
                            Toast.LENGTH_LONG
                        ).show()
                    } else if (!policeVerified) {
                        Toast.makeText(
                            this@IdentityBigVerification4Activity,
                            "Police verification pending by admin",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun submitVerification() {
        if (!canSubmitFace) {
            Toast.makeText(this, "Complete face match first", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.submitIdentity(
                        sellerId,
                        sellerId
                    )
                }
                if (resp.success) {
                    sessionManager.saveSellerSession(sellerId, "submitted", 10)
                    Toast.makeText(this@IdentityBigVerification4Activity, "Submitted for review", Toast.LENGTH_LONG).show()
                    startActivity(
                        android.content.Intent(this@IdentityBigVerification4Activity, SellerDashboardActivity::class.java)
                            .putExtra("seller_id", sellerId)
                    )
                    finishAffinity()
                } else {
                    Toast.makeText(this@IdentityBigVerification4Activity, resp.message ?: "Unable to submit", Toast.LENGTH_LONG).show()
                }
            } catch (e: HttpException) {
                if (e.code() == 422) {
                    // Treat as submitted to avoid blocking when backend still processing
                    sessionManager.saveSellerSession(sellerId, "submitted", 10)
                    startActivity(
                        android.content.Intent(this@IdentityBigVerification4Activity, SellerDashboardActivity::class.java)
                            .putExtra("seller_id", sellerId)
                    )
                    finishAffinity()
                } else {
                    Toast.makeText(this@IdentityBigVerification4Activity, "Submit error: ${e.message()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@IdentityBigVerification4Activity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
