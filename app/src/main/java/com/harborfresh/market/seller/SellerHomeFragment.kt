package com.harborfresh.market.seller

import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.harborfresh.market.R
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.common.SessionManager
import com.harborfresh.market.model.SellerOrderItem
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.concurrent.atomic.AtomicBoolean
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

class SellerHomeFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private var tvName: TextView? = null
    private var tvLoc: TextView? = null
    private var tvOrders: TextView? = null
    private var tvRevenue: TextView? = null
    private var tvPending: TextView? = null
    private lateinit var recentAdapter: SellerOrdersAdapter
    private var emptyRecent: View? = null
    private var rvRecent: androidx.recyclerview.widget.RecyclerView? = null
    private var tvSetLocation: TextView? = null

    private val cacheLoaded = AtomicBoolean(false)
    private var cachedOrders: List<SellerOrderItem> = emptyList()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchAndSaveLocation()
        } else {
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_seller_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        tvName = view.findViewById(R.id.tvSellerName)
        tvLoc = view.findViewById(R.id.tvSellerLoc)
        tvOrders = view.findViewById(R.id.tvCard1Value)
        tvRevenue = view.findViewById(R.id.tvCard2Value)
        tvPending = view.findViewById(R.id.tvCard3Value)
        rvRecent = view.findViewById(R.id.rvRecentOrders)
        emptyRecent = view.findViewById(R.id.emptyRecent)
        recentAdapter = SellerOrdersAdapter { item ->
            val orderId = item.id ?: return@SellerOrdersAdapter
            showOrderSheet(orderId)
        }
        rvRecent?.layoutManager = LinearLayoutManager(requireContext())
        rvRecent?.adapter = recentAdapter
        tvSetLocation = view.findViewById(R.id.tvSetLocation)

        view.findViewById<View?>(R.id.btnAddCatch)?.setOnClickListener {
            startActivity(Intent(requireContext(), SellerAddProductActivity::class.java))
        }
        view.findViewById<View?>(R.id.btnViewOrders)?.setOnClickListener {
            (activity as? SellerDashboardActivity)?.let { dash ->
                dash.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.sellerBottomNav)
                    ?.selectedItemId = R.id.nav_seller_orders
            }
        }

        tvSetLocation?.setOnClickListener { requestLocation() }

        loadDashboard()
        loadRecentOrders()
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun loadDashboard() {
        val sellerId = sessionManager.getSellerId()
        if (sellerId == 0) {
            Toast.makeText(requireContext(), "Seller session missing", Toast.LENGTH_SHORT).show()
            return
        }
        val currency = NumberFormat.getCurrencyInstance().apply { maximumFractionDigits = 0 }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = ApiClient.apiService.getSellerDashboard(sellerId)
                if (!isAdded) return@launch
                if (resp.success) {
                    tvName?.text = resp.seller?.full_name ?: resp.seller?.business_name ?: "Seller"
                    val storedLoc = sessionManager.getSellerLocation()
                    tvLoc?.text = storedLoc ?: (resp.seller?.city ?: "")
                    tvOrders?.text = (resp.stats?.orders ?: 0).toString()
                    tvPending?.text = (resp.stats?.pending ?: 0).toString()
                    tvRevenue?.text = currency.format(resp.stats?.revenue ?: 0.0)
                } else {
                    // Show graceful fallback without noisy toasts
                    tvName?.text = "Seller"
                    tvLoc?.text = ""
                    tvOrders?.text = "0"
                    tvPending?.text = "0"
                    tvRevenue?.text = currency.format(0)
                }
            } catch (_: Exception) {
                if (!isAdded) return@launch
                tvName?.text = "Seller"
                tvLoc?.text = sessionManager.getSellerLocation() ?: ""
                tvOrders?.text = "0"
                tvPending?.text = "0"
                tvRevenue?.text = currency.format(0)
            }
        }
    }

    private fun loadRecentOrders() {
        val sellerId = sessionManager.getSellerId()
        if (sellerId == 0) {
            emptyRecent?.visibility = View.VISIBLE
            rvRecent?.visibility = View.GONE
            return
        }

        // serve cached list immediately if available
        if (cacheLoaded.get()) {
            bindOrders(cachedOrders)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = ApiClient.apiService.getSellerOrders(sellerId)
                if (!isAdded) return@launch
                if (resp.success) {
                    cachedOrders = resp.orders
                    cacheLoaded.set(true)
                    bindOrders(resp.orders)
                } else {
                    bindOrders(emptyList())
                }
            } catch (_: Exception) {
                if (isAdded) bindOrders(emptyList())
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
                    loadRecentOrders()
                    loadDashboard()
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

    private fun bindOrders(list: List<SellerOrderItem>) {
        if (list.isEmpty()) {
            rvRecent?.visibility = View.GONE
            emptyRecent?.visibility = View.VISIBLE
        } else {
            rvRecent?.visibility = View.VISIBLE
            emptyRecent?.visibility = View.GONE
            recentAdapter.submit(list.take(5))
        }
    }

    private fun requestLocation() {
        val ctx = context ?: return
        val permission = android.Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(ctx, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fetchAndSaveLocation()
        } else {
            locationPermissionLauncher.launch(permission)
        }
    }

    private fun fetchAndSaveLocation() {
        val lm = context?.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager ?: return
        val loc: Location? = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstNotNullOfOrNull { provider ->
                try {
                    lm.getLastKnownLocation(provider)
                } catch (_: SecurityException) {
                    null
                }
            }
        if (loc != null) {
            val geocoder = Geocoder(requireContext())
            val address = try { geocoder.getFromLocation(loc.latitude, loc.longitude, 1)?.firstOrNull() } catch (_: Exception) { null }
            val locality = listOfNotNull(address?.locality, address?.adminArea, address?.countryName).joinToString(", ")
                .ifEmpty { "${"%.4f".format(loc.latitude)}, ${"%.4f".format(loc.longitude)}" }
            sessionManager.saveSellerLocation(locality, loc.latitude, loc.longitude)
            tvLoc?.text = locality
            Toast.makeText(requireContext(), "Location updated", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Unable to fetch location", Toast.LENGTH_SHORT).show()
        }
    }
}
