package com.harborfresh.market.admin

import android.content.Intent
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
import kotlinx.coroutines.launch

class AdminHomeFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private var tvName: TextView? = null
    private var tvEmail: TextView? = null
    private var tvRole: TextView? = null
    private var tvPending: TextView? = null
    private var tvApproved: TextView? = null
    private var tvRejected: TextView? = null
    private var pendingListRoot: ViewGroup? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_admin_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        tvName = view.findViewById(R.id.tvAdminName)
        tvEmail = view.findViewById(R.id.tvAdminEmail)
        tvRole = view.findViewById(R.id.tvAdminRole)
        tvPending = view.findViewById(R.id.tvPendingValue)
        tvApproved = view.findViewById(R.id.tvApprovedValue)
        tvRejected = view.findViewById(R.id.tvRejectedValue)
        pendingListRoot = view.findViewById(R.id.pendingList)

        view.findViewById<View?>(R.id.btnViewPending)?.setOnClickListener {
            startActivity(Intent(requireContext(), AdminAllSellersActivity::class.java).putExtra("filter", "pending"))
        }
        view.findViewById<View?>(R.id.btnViewAll)?.setOnClickListener {
            startActivity(Intent(requireContext(), AdminAllSellersActivity::class.java))
        }

        val name = sessionManager.getAdminName() ?: "Admin"
        val email = sessionManager.getAdminEmail() ?: "admin@email.com"
        tvName?.text = name
        tvEmail?.text = email
        tvRole?.text = "Role: Admin"

        loadPending()
    }

    private fun loadPending() {
        viewLifecycleOwner.lifecycleScope.launch {
            val ctx = context ?: return@launch
            try {
                val resp = ApiClient.apiService.getPendingVerifications()
                if (!isAdded) return@launch
                if (resp.success) {
                    val counts = resp.counts
                    tvPending?.text = counts?.pending?.toString() ?: "0"
                    tvApproved?.text = counts?.approved?.toString() ?: "0"
                    tvRejected?.text = counts?.rejected?.toString() ?: "0"

                    pendingListRoot?.removeAllViews()
                    resp.pending.take(5).forEach { seller ->
                        val item = LayoutInflater.from(ctx).inflate(R.layout.view_pending_seller_item, pendingListRoot, false)
                        item.findViewById<TextView?>(R.id.tvPendingName)?.text = seller.full_name ?: "Seller"
                        item.findViewById<TextView?>(R.id.tvPendingEmail)?.text = seller.business_email ?: "—"
                        item.findViewById<TextView?>(R.id.tvPendingStatus)?.text = (seller.police_verification_status ?: "pending").replaceFirstChar { c -> c.uppercaseChar() }
                        item.setOnClickListener {
                            startActivity(
                                Intent(requireContext(), AdminSellerDetailActivity::class.java)
                                    .putExtra("seller_id", seller.id)
                            )
                        }
                        pendingListRoot?.addView(item)
                    }
                }
            } catch (_: Exception) {
                if (isAdded) {
                    Toast.makeText(ctx, "Unable to load pending verifications", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
