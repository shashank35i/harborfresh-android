package com.harborfresh.market.ui.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.harborfresh.market.R
import com.harborfresh.market.model.CartItem

class CartAdapter(
    private val items: MutableList<CartItem>,
    private val onQtyChanged: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val iv: ImageView = v.findViewById(R.id.ivThumb)
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvPrice: TextView = v.findViewById(R.id.tvPrice)
        val tvQty: TextView = v.findViewById(R.id.tvQty)
        val tvLine: TextView = v.findViewById(R.id.tvLineTotal)
        val btnPlus: ImageView = v.findViewById(R.id.btnPlus)
        val btnMinus: ImageView = v.findViewById(R.id.btnMinus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvPrice.text = "\u20B9${item.price}"
        holder.tvQty.text = item.qty.toString()
        holder.tvLine.text = "\u20B9${"%.2f".format(item.price * item.qty)}"
        Glide.with(holder.iv.context)
            .load(item.imageUrl)
            .placeholder(R.drawable.bg_image_placeholder)
            .into(holder.iv)

        holder.btnPlus.setOnClickListener {
            item.qty += 1
            notifyItemChanged(position)
            onQtyChanged(item)
        }
        holder.btnMinus.setOnClickListener {
            if (item.qty > 1) {
                item.qty -= 1
                notifyItemChanged(position)
                onQtyChanged(item)
            } else {
                val idx = holder.bindingAdapterPosition
                if (idx != RecyclerView.NO_POSITION) {
                    items.removeAt(idx)
                    notifyItemRemoved(idx)
                    onQtyChanged(item.copy(qty = 0))
                }
            }
        }
    }
}
