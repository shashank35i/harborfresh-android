package com.harborfresh.market.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.harborfresh.market.R
import com.harborfresh.market.model.Product

class ProductAdapter(private var list: List<Product>) :
    RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgProduct)
        val name: TextView = view.findViewById(R.id.txtName)
        val price: TextView = view.findViewById(R.id.txtPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.name.text = item.name
        holder.price.text = "₹${item.price} / kg" // Correctly format the price

        // Load image from URL using Coil
        holder.img.load(item.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.placeholder_image)
            error(R.drawable.placeholder_image)
        }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<Product>) {
        list = newList
        notifyDataSetChanged()
    }
}
