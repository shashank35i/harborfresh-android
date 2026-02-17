package com.harborfresh.market.seller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.harborfresh.market.R
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.common.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class SellerOrdersFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: SellerOrdersAdapter
    private var rv: RecyclerView? = null
    private var emptyView: View? = null
    private var progress: ProgressBar? = null
    private var tvSubtitle: android.widget.TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_seller_orders, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        adapter = SellerOrdersAdapter { item ->
            val orderId = item.id ?: return@SellerOrdersAdapter
            showOrderSheet(orderId)
        }
        rv = view.findViewById(R.id.rvSellerOrders)
        emptyView = view.findViewById(R.id.emptyState)
        progress = view.findViewById(R.id.progressOrders)
        tvSubtitle = view.findViewById(R.id.tvSubtitle)

        rv?.layoutManager = LinearLayoutManager(requireContext())
        rv?.adapter = adapter

        loadOrders()
    }

    private fun loadOrders() {
        val sellerId = sessionManager.getSellerId()
        if (sellerId == 0) {
            Toast.makeText(requireContext(), "Seller session missing", Toast.LENGTH_SHORT).show()
            return
        }
        progress?.visibility = View.VISIBLE
        emptyView?.visibility = View.GONE
        rv?.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = ApiClient.apiService.getSellerOrders(sellerId)
                if (!isAdded) return@launch
                progress?.visibility = View.GONE
                if (resp.success && resp.orders.isNotEmpty()) {
                    rv?.visibility = View.VISIBLE
                    emptyView?.visibility = View.GONE
                    adapter.submit(resp.orders)
                    tvSubtitle?.text = "${resp.orders.size} active orders"
                } else {
                    rv?.visibility = View.GONE
                    emptyView?.visibility = View.VISIBLE
                    tvSubtitle?.text = "0 active orders"
                }
            } catch (_: Exception) {
                if (isAdded) {
                    progress?.visibility = View.GONE
                    rv?.visibility = View.GONE
                    emptyView?.visibility = View.VISIBLE
                    tvSubtitle?.text = "0 active orders"
                }
            }
        }
    }

    private fun showOrderSheet(orderId: Int) {
        val sellerId = sessionManager.getSellerId()
        if (sellerId == 0) {
            Toast.makeText(requireContext(), "Seller session missing", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottomup_order_details, null)
        dialog.setContentView(sheetView)

        val tvCustomerName = sheetView.findViewById<TextView>(R.id.tvCustomerName)
        val tvCustomerPhone = sheetView.findViewById<TextView>(R.id.tvCustomerPhone)
        val tvAddress = sheetView.findViewById<TextView>(R.id.tvAddress)
        val tvItemName = sheetView.findViewById<TextView>(R.id.tvItemName)
        val tvItemMeta = sheetView.findViewById<TextView>(R.id.tvItemMeta)
        val tvItemPrice = sheetView.findViewById<TextView>(R.id.tvItemPrice)
        val btnAccept = sheetView.findViewById<View>(R.id.btnAccept)
        val btnDecline = sheetView.findViewById<View>(R.id.btnDecline)
        val tvClose = sheetView.findViewById<View>(R.id.tvClose)

        tvClose.setOnClickListener { dialog.dismiss() }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = ApiClient.apiService.getSellerOrderDetails(orderId, sellerId)
                if (!isAdded) return@launch
                if (resp.success && resp.order != null) {
                    val order = resp.order
                    tvCustomerName.text = order.customer_name ?: "Customer"
                    tvCustomerPhone.text = order.customer_phone ?: "Phone not available"
                    tvAddress.text = order.delivery_address ?: "Address not available"

                    val items = resp.items
                    val totalQty = items.sumOf { it.quantity ?: 0 }
                    val firstName = items.firstOrNull()?.product_name ?: "Items"
                    tvItemName.text = firstName
                    tvItemMeta.text = if (totalQty > 0) {
                        "$totalQty items"
                    } else {
                        "Items"
                    }

                    val total = resp.total_amount ?: items.sumOf { it.price ?: 0.0 }
                    tvItemPrice.text = "Rs " + String.format(java.util.Locale.getDefault(), "%.0f", total)

                    val status = order.status ?: "Pending"
                    configureActions(status, orderId, dialog, btnAccept, btnDecline)
                } else {
                    Toast.makeText(requireContext(), resp.message ?: "Unable to load order", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            } catch (_: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Unable to load order", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun updateOrderStatus(orderId: Int, action: String, dialog: BottomSheetDialog) {
        val sellerId = sessionManager.getSellerId()
        if (sellerId == 0) return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = when (action) {
                    "primary" -> {
                        ApiClient.apiService.acceptSellerOrder(orderId, sellerId)
                    }
                    "prepare" -> {
                        ApiClient.apiService.markSellerOrderPreparing(orderId, sellerId)
                    }
                    "ready" -> {
                        ApiClient.apiService.markSellerOrderReady(orderId, sellerId)
                    }
                    else -> {
                        ApiClient.apiService.declineSellerOrder(orderId, sellerId)
                    }
                }
                if (!isAdded) return@launch
                if (resp.success) {
                    dialog.dismiss()
                    loadOrders()
                } else {
                    Toast.makeText(requireContext(), resp.message ?: "Update failed", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun configureActions(
        status: String,
        orderId: Int,
        dialog: BottomSheetDialog,
        btnAccept: View,
        btnDecline: View
    ) {
        val acceptText = (btnAccept as? TextView)
        val declineText = (btnDecline as? TextView)
        btnAccept.alpha = 1f
        btnDecline.alpha = 1f
        btnAccept.isEnabled = true
        btnDecline.isEnabled = true

        when {
            status.equals("Pending", true) -> {
                acceptText?.text = "Accept"
                declineText?.text = "Decline"
                btnDecline.visibility = View.VISIBLE
                setActionLayout(single = false, btnAccept = btnAccept, btnDecline = btnDecline)
                btnAccept.setOnClickListener { updateOrderStatus(orderId, "primary", dialog) }
                btnDecline.setOnClickListener { updateOrderStatus(orderId, "decline", dialog) }
            }
            status.equals("Confirmed", true) -> {
                acceptText?.text = "Mark as Preparing"
                btnDecline.visibility = View.GONE
                setActionLayout(single = true, btnAccept = btnAccept, btnDecline = btnDecline)
                btnAccept.setOnClickListener { updateOrderStatus(orderId, "prepare", dialog) }
            }
            status.equals("Preparing", true) -> {
                acceptText?.text = "Mark as Ready"
                btnDecline.visibility = View.GONE
                setActionLayout(single = true, btnAccept = btnAccept, btnDecline = btnDecline)
                btnAccept.setOnClickListener { updateOrderStatus(orderId, "ready", dialog) }
            }
            else -> {
                acceptText?.text = "Updated"
                btnDecline.visibility = View.GONE
                setActionLayout(single = true, btnAccept = btnAccept, btnDecline = btnDecline)
                btnAccept.isEnabled = false
                btnAccept.alpha = 0.6f
            }
        }
    }

    private fun setActionLayout(single: Boolean, btnAccept: View, btnDecline: View) {
        val parent = btnAccept.parent as? LinearLayout
        val height = dp(52)
        if (single) {
            val lp = LinearLayout.LayoutParams(dp(220), height)
            lp.gravity = android.view.Gravity.CENTER
            btnAccept.layoutParams = lp
            parent?.gravity = android.view.Gravity.CENTER
        } else {
            val lpAccept = LinearLayout.LayoutParams(0, height, 1f)
            lpAccept.marginEnd = dp(10)
            val lpDecline = LinearLayout.LayoutParams(0, height, 1f)
            lpDecline.marginStart = dp(10)
            btnAccept.layoutParams = lpAccept
            btnDecline.layoutParams = lpDecline
            parent?.gravity = android.view.Gravity.CENTER
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
    }
}
