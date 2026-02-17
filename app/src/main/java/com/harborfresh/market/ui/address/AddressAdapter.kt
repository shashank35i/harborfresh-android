package com.harborfresh.market.ui.address

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.harborfresh.market.R
import com.harborfresh.market.model.UserAddress
import com.google.android.material.card.MaterialCardView

class AddressAdapter(
    private val onSelect: (UserAddress) -> Unit,
    private val onEdit: (UserAddress) -> Unit
) : RecyclerView.Adapter<AddressAdapter.AddressViewHolder>() {

    private val items = mutableListOf<UserAddress>()
    private var selectedId: String? = null

    fun submit(list: List<UserAddress>, selectedId: String?) {
        items.clear()
        items.addAll(list)
        this.selectedId = selectedId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_delivery_address, parent, false)
        return AddressViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, item.id == selectedId, onSelect, onEdit)
    }

    override fun getItemCount(): Int = items.size

    class AddressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card = itemView.findViewById<MaterialCardView>(R.id.cardAddress)
        private val tvLabel = itemView.findViewById<TextView>(R.id.tvLabel)
        private val tvName = itemView.findViewById<TextView>(R.id.tvName)
        private val tvAddr = itemView.findViewById<TextView>(R.id.tvAddr)
        private val tvCity = itemView.findViewById<TextView>(R.id.tvCity)
        private val tvEdit = itemView.findViewById<TextView>(R.id.tvEdit)
        private val tvSelected = itemView.findViewById<TextView>(R.id.tvSelected)
        private val chipDefault = itemView.findViewById<TextView>(R.id.tvDefault)

        fun bind(
            item: UserAddress,
            isSelected: Boolean,
            onSelect: (UserAddress) -> Unit,
            onEdit: (UserAddress) -> Unit
        ) {
            val selectedBg = 0xFF0B2A43.toInt()
            val normalBg = 0xFFFFFFFF.toInt()
            val selectedPrimary = 0xFFFFFFFF.toInt()
            val normalPrimary = 0xFF111827.toInt()
            val selectedSecondary = 0xFF9FB1C1.toInt()
            val normalSecondary = 0xFF6B7280.toInt()

            card.setCardBackgroundColor(if (isSelected) selectedBg else normalBg)
            tvLabel.text = item.label
            tvName.text = item.name
            tvAddr.text = listOf(item.line1, item.line2).filter { it.isNotBlank() }.joinToString(", ")
            tvCity.text = listOf(item.city, item.pin).filter { it.isNotBlank() }.joinToString(" - ")
            chipDefault.visibility = if (item.isDefault) View.VISIBLE else View.GONE
            tvSelected.visibility = if (isSelected) View.VISIBLE else View.GONE

            tvLabel.setTextColor(if (isSelected) selectedPrimary else normalPrimary)
            tvName.setTextColor(if (isSelected) selectedSecondary else normalSecondary)
            tvAddr.setTextColor(if (isSelected) selectedSecondary else normalSecondary)
            tvCity.setTextColor(if (isSelected) selectedSecondary else normalSecondary)
            tvEdit.setTextColor(if (isSelected) selectedSecondary else normalSecondary)

            card.setOnClickListener { onSelect(item) }
            tvEdit.setOnClickListener { onEdit(item) }
        }
    }
}
