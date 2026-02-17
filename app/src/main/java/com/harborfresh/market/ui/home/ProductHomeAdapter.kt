package com.harborfresh.market.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.harborfresh.market.R
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.model.Product

class ProductHomeAdapter(
    private val items: List<Product>,
    private val onClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductHomeAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val iv: ImageView = v.findViewById(R.id.ivProduct)
        val tvName: TextView = v.findViewById(R.id.tvProductName)
        val tvPrice: TextView = v.findViewById(R.id.tvPrice)
        val tvRating: TextView = v.findViewById(R.id.tvRating)
        val tvReviews: TextView = v.findViewById(R.id.tvReviews)
        val tvBadge: TextView = v.findViewById(R.id.tvBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_home, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvPrice.text = "₹${item.price}"
        val rating = item.rating?.takeIf { it.isNotBlank() } ?: "5.0"
        holder.tvRating.text = rating
        holder.tvReviews.text = "(${fakeReviews(item.id)})"
        holder.tvBadge.text = item.freshness ?: "Fresh"
        val imagePath = item.imageUrl?.takeIf { it.isNotBlank() }
        val fullUrl = imagePath?.let {
            if (it.startsWith("http", true)) it else ApiClient.BASE_URL + it
        }
        Glide.with(holder.iv.context)
            .load(fullUrl)
            .placeholder(R.drawable.bg_image_placeholder)
            .into(holder.iv)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    private fun fakeReviews(id: Int): Int {
        return 20 + (id * 37 % 180)
    }
}
