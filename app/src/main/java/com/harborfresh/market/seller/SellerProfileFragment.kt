package com.harborfresh.market.seller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.harborfresh.market.R
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.common.SessionManager
import com.harborfresh.market.auth.LoginActivity
import kotlinx.coroutines.launch

class SellerProfileFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private var tvName: TextView? = null
    private var tvEmail: TextView? = null
    private var tvPhone: TextView? = null
    private var tvStatusPill: TextView? = null
    private var tvStatusValue: TextView? = null
    private var tvStepValue: TextView? = null
    private var tvVerifiedValue: TextView? = null
    private var tvBusinessName: TextView? = null
    private var tvBusinessEmail: TextView? = null
    private var tvBusinessLocation: TextView? = null
    private var tvAadhaarValue: TextView? = null
    private var tvSelfieValue: TextView? = null
    private var tvFaceValue: TextView? = null
    private var tvPoliceValue: TextView? = null
    private var btnLogout: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_seller_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())

        tvName = view.findViewById(R.id.tvSellerName)
        tvEmail = view.findViewById(R.id.tvSellerEmail)
        tvPhone = view.findViewById(R.id.tvSellerPhone)
        tvStatusPill = view.findViewById(R.id.tvSellerStatusPill)
        tvStatusValue = view.findViewById(R.id.tvStatusValue)
        tvStepValue = view.findViewById(R.id.tvStepValue)
        tvVerifiedValue = view.findViewById(R.id.tvVerifiedValue)
        tvBusinessName = view.findViewById(R.id.tvBusinessName)
        tvBusinessEmail = view.findViewById(R.id.tvBusinessEmail)
        tvBusinessLocation = view.findViewById(R.id.tvBusinessLocation)
        tvAadhaarValue = view.findViewById(R.id.tvAadhaarValue)
        tvSelfieValue = view.findViewById(R.id.tvSelfieValue)
        tvFaceValue = view.findViewById(R.id.tvFaceValue)
        tvPoliceValue = view.findViewById(R.id.tvPoliceValue)
        btnLogout = view.findViewById(R.id.btnSellerLogout)

        val sellerId = sessionManager.getSellerId()
        if (sellerId == 0) {
            Toast.makeText(requireContext(), "Seller session missing", Toast.LENGTH_SHORT).show()
            return
        }

        btnLogout?.setOnClickListener {
            sessionManager.clearSession()
            val intent = android.content.Intent(requireContext(), LoginActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        loadProfile(sellerId)
    }

    private fun loadProfile(sellerId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = ApiClient.apiService.getSellerStatus(sellerId)
                val seller = resp.data?.seller
                val identity = resp.data?.identity
                val sellerStatus = seller?.status

                seller?.let {
                    tvName?.text = it.full_name ?: "Seller"
                    tvEmail?.text = it.business_email ?: "—"
                    tvPhone?.text = it.phone ?: "—"

                    val statusText = (it.status ?: "draft").replaceFirstChar { c -> c.uppercaseChar() }
                    tvStatusPill?.text = statusText
                    tvStatusPill?.setBackgroundResource(
                        if (statusText.equals("approved", true)) R.drawable.bg_status_pill_green else R.drawable.bg_status_pill_gray
                    )
                    tvStatusValue?.text = statusText
                    tvStepValue?.text = (it.verification_step).toString()
                    tvVerifiedValue?.text = if (it.is_verified == 1) "Verified" else "Pending"
                }

                identity?.let { id ->
                    tvAadhaarValue?.text = if (!id.aadhaar_doc.isNullOrEmpty()) "Uploaded" else "Pending"
                    tvSelfieValue?.text = if (id.liveness_verified == 1) "Verified" else "Pending"
                    tvFaceValue?.text = if (id.face_match_verified == 1) "Verified" else "Pending"
                    val policeStatusRaw = id.police_verification_status?.lowercase()
                    val policeText = when {
                        policeStatusRaw == "verified" && sellerStatus?.lowercase() == "approved" -> "Verified"
                        policeStatusRaw == "rejected" -> "Rejected"
                        else -> "Pending"
                    }
                    tvPoliceValue?.text = policeText
                }

                // Business info falls back to seller basics if detailed table not fetched
                tvBusinessName?.text = seller?.full_name ?: "Not provided"
                tvBusinessEmail?.text = seller?.business_email ?: "Not provided"
                tvBusinessLocation?.text = sessionManager.getSellerStatus() ?: "Location pending"

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Unable to load profile", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
