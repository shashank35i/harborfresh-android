package com.harborfresh.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.common.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class OrdersFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private var containerOrders: LinearLayout? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_customer_my_orders, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        containerOrders = view.findViewById(R.id.content)
        view.findViewById<View?>(R.id.btnBack)?.visibility = View.GONE
        loadOrders()
    }

    private fun loadOrders() {
        val userId = sessionManager.getUserId()
        if (userId <= 0) {
            Toast.makeText(requireContext(), "Please log in to view orders", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { ApiClient.apiService.getOrders(userId) }
                if (resp.success) {
                    renderOrders(resp.orders)
                } else {
                    Toast.makeText(requireContext(), resp.message ?: "Unable to load orders", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading orders", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun renderOrders(orders: List<com.harborfresh.market.model.CustomerOrder>) {
        containerOrders?.let { layout ->
            layout.removeAllViews()
            if (orders.isEmpty()) {
                val tvEmpty = TextView(requireContext()).apply {
                    text = "No orders yet"
                    setPadding(12, 24, 12, 24)
                }
                layout.addView(tvEmpty)
                return
            }
            orders.forEach { order ->
                val card = LayoutInflater.from(requireContext()).inflate(R.layout.view_order_item, layout, false)
                card.findViewById<TextView?>(R.id.tvOrderId)?.text = order.order_code ?: "Order"
                val statusText = order.status ?: "Pending"
                val statusView = card.findViewById<TextView?>(R.id.tvStatus)
                statusView?.text = statusText
                statusView?.setBackgroundResource(if (statusText.equals("Delivered", true)) R.drawable.bg_status_pill_green else R.drawable.bg_status_pill_gray)
                card.findViewById<TextView?>(R.id.tvDate)?.text = order.created_at ?: ""
                val items = order.items ?: 0
                val amount = order.total_amount ?: 0.0
                val price = String.format(Locale.getDefault(), "%.0f", amount)
                card.findViewById<TextView?>(R.id.tvMeta)?.text = "$items items • \u20B9$price"
                card.setOnClickListener {
                    val intent = android.content.Intent(requireContext(), TrackOrderActivity::class.java)
                    intent.putExtra("order_id", order.id)
                    startActivity(intent)
                }
                layout.addView(card)
            }
        }
    }
}
