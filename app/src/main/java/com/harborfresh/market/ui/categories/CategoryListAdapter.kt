package com.harborfresh.market.ui.categories

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.harborfresh.market.R
import com.harborfresh.market.model.CategoryItem

class CategoryListAdapter(
    private val items: List<CategoryItem>,
    private val onClick: (CategoryItem) -> Unit
) : RecyclerView.Adapter<CategoryListAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvCategoryName)
        val tvCount: TextView = v.findViewById(R.id.tvCount)
        val ivImage: ImageView = v.findViewById(R.id.ivCategoryImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_list, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvCount.text = if (item.totalItems > 0) "${item.totalItems} items" else ""
        holder.ivImage.setImageResource(imageFor(item.name))
        holder.itemView.setOnClickListener { onClick(item) }
    }

    private fun imageFor(name: String): Int {
        return when (name.trim().lowercase()) {
            "fish" -> R.drawable.fishesc
            "prawns" -> R.drawable.prawnsc
            "crabs" -> R.drawable.crabsc
            "lobster" -> R.drawable.lobsterc
            "shellfish" -> R.drawable.shellfishc
            "squid" -> R.drawable.squidc
            else -> R.drawable.bg_category_tile
        }
    }
}
