package com.harborfresh.market.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.harborfresh.market.R
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.common.CartManager
import com.harborfresh.market.model.Product

class SellerMoreAdapter(
    private val items: List<Product>,
    private val onClick: (Product) -> Unit
) : RecyclerView.Adapter<SellerMoreAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val iv: ImageView = view.findViewById(R.id.ivMoreImage)
        val tvName: TextView = view.findViewById(R.id.tvMoreName)
        val tvPrice: TextView = view.findViewById(R.id.tvMorePrice)
        val tvInCart: TextView = view.findViewById(R.id.tvInCart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_seller_more, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvPrice.text = "\u20B9${item.price} /kg"

        val imagePath = item.imageUrl?.takeIf { it.isNotBlank() }
        val fullUrl = imagePath?.let {
            if (it.startsWith("http", true)) it else ApiClient.BASE_URL + it
        }
        Glide.with(holder.iv.context)
            .load(fullUrl)
            .placeholder(R.drawable.bg_image_placeholder)
            .into(holder.iv)

        val cart = CartManager.getCart(holder.itemView.context)
        val inCart = cart.any { it.id == item.id && it.name == item.name }
        holder.tvInCart.visibility = if (inCart) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onClick(item) }
    }
}
