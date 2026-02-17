package com.harborfresh.market.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.harborfresh.market.R
import com.harborfresh.market.model.AdminSellerSummary

class AdminSellerAdapter(
    private val items: List<AdminSellerSummary>,
    private val onClick: (AdminSellerSummary) -> Unit
) : RecyclerView.Adapter<AdminSellerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView? = view.findViewById(R.id.tvPendingName)
        val email: TextView? = view.findViewById(R.id.tvPendingEmail)
        val status: TextView? = view.findViewById(R.id.tvPendingStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_pending_seller_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name?.text = item.full_name ?: "Seller"
        holder.email?.text = item.business_email ?: "�"
        val statusText = (item.verification_status ?: item.status ?: "pending")
            .replaceFirstChar { it.uppercaseChar() }
        holder.status?.text = statusText
        holder.itemView.setOnClickListener { onClick(item) }
    }
}
