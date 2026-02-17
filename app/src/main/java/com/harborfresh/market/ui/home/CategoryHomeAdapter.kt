package com.harborfresh.market.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.harborfresh.market.R

class CategoryHomeAdapter(
    private val items: List<Pair<String, Int>>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryHomeAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvCategoryName)
        val count: TextView = v.findViewById(R.id.tvCategoryCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_home, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (name, count) = items[position]
        holder.name.text = name
        holder.count.text = if (count > 0) "$count items" else ""
        holder.itemView.setOnClickListener { onClick(name) }
    }
}
