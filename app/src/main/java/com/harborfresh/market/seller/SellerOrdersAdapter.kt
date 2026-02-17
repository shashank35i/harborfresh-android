package com.harborfresh.market.seller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.harborfresh.market.R
import com.harborfresh.market.model.SellerOrderItem
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class SellerOrdersAdapter(
    private val onOrderClick: ((SellerOrderItem) -> Unit)? = null
) : RecyclerView.Adapter<SellerOrdersAdapter.OrderVH>() {

    private val items = mutableListOf<SellerOrderItem>()

    fun submit(list: List<SellerOrderItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_seller_order_item, parent, false)
        return OrderVH(view)
    }

    override fun onBindViewHolder(holder: OrderVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class OrderVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderCode: TextView = itemView.findViewById(R.id.tvOrderCode)
        private val tvStatusPill: TextView = itemView.findViewById(R.id.tvStatusPill)
        private val tvTimeAgo: TextView = itemView.findViewById(R.id.tvTimeAgo)
        private val tvInitials: TextView = itemView.findViewById(R.id.tvInitials)
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvCustomerPhone: TextView = itemView.findViewById(R.id.tvCustomerPhone)
        private val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvItemPrice: TextView = itemView.findViewById(R.id.tvItemPrice)
        private val tvSlotText: TextView = itemView.findViewById(R.id.tvSlotText)
        private val tvTotalPrice: TextView = itemView.findViewById(R.id.tvTotalPrice)

        fun bind(item: SellerOrderItem) {
            val status = item.status?.ifBlank { null } ?: "Pending"
            tvOrderCode.text = item.order_code ?: "Order"
            tvStatusPill.text = status
            applyStatusStyle(status)
            tvTimeAgo.text = formatRelative(item.created_at)

            val name = item.customer_name?.ifBlank { null } ?: "Customer"
            tvCustomerName.text = name
            tvCustomerPhone.text = item.customer_phone?.ifBlank { null } ?: "Phone not available"
            tvInitials.text = initialsFor(name)

            val qty = item.quantity ?: 0
            val product = item.product_name?.ifBlank { null } ?: "Item"
            tvItemName.text = if (qty > 0) {
                val suffix = if (qty == 1) "item" else "items"
                "$product ($qty $suffix)"
            } else {
                product
            }

            val price = item.total_price ?: 0.0
            val formatted = "Rs " + String.format(Locale.getDefault(), "%.0f", price)
            tvItemPrice.text = formatted
            tvTotalPrice.text = formatted

            val slotDay = item.slot_day?.ifBlank { null }
            val slotRange = item.slot_time_range?.ifBlank { null }
            tvSlotText.text = if (slotDay != null && slotRange != null) {
                "$slotDay, $slotRange"
            } else {
                "Delivery slot pending"
            }

            itemView.setOnClickListener { onOrderClick?.invoke(item) }
        }

        private fun formatRelative(raw: String?): String {
            if (raw.isNullOrBlank()) return ""
            return try {
                val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val date = parser.parse(raw)
                if (date == null) return raw
                val diff = System.currentTimeMillis() - date.time
                val mins = diff / (60 * 1000)
                when {
                    mins < 60 -> "${mins} mins ago"
                    mins < 1440 -> "${mins / 60} hours ago"
                    else -> SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
                }
            } catch (_: ParseException) {
                raw
            }
        }

        private fun initialsFor(name: String): String {
            val parts = name.trim().split(" ").filter { it.isNotBlank() }
            val first = parts.getOrNull(0)?.firstOrNull()?.uppercaseChar()
            val second = parts.getOrNull(1)?.firstOrNull()?.uppercaseChar()
            return listOfNotNull(first, second).joinToString("")
        }

        private fun applyStatusStyle(status: String) {
            when {
                status.equals("Confirmed", true) -> {
                    tvStatusPill.setBackgroundResource(R.drawable.bg_status_pill_confirmed)
                    tvStatusPill.setTextColor(0xFF1D4ED8.toInt())
                }
                status.equals("Preparing", true) -> {
                    tvStatusPill.setBackgroundResource(R.drawable.bg_status_pill_preparing)
                    tvStatusPill.setTextColor(0xFF6D28D9.toInt())
                }
                status.equals("Ready", true) -> {
                    tvStatusPill.setBackgroundResource(R.drawable.bg_status_pill_out_for_delivery)
                    tvStatusPill.setTextColor(0xFF0284C7.toInt())
                }
                status.equals("Out for Delivery", true) -> {
                    tvStatusPill.setBackgroundResource(R.drawable.bg_status_pill_out_for_delivery)
                    tvStatusPill.setTextColor(0xFF0284C7.toInt())
                }
                status.equals("Pending", true) -> {
                    tvStatusPill.setBackgroundResource(R.drawable.bg_status_pill_pending)
                    tvStatusPill.setTextColor(0xFFB45309.toInt())
                }
                status.equals("Declined", true) -> {
                    tvStatusPill.setBackgroundResource(R.drawable.bg_status_pill_pending)
                    tvStatusPill.setTextColor(0xFFB91C1C.toInt())
                }
                else -> {
                    tvStatusPill.setBackgroundResource(R.drawable.bg_status_pill_pending)
                    tvStatusPill.setTextColor(0xFFB45309.toInt())
                }
            }
        }
    }
}
