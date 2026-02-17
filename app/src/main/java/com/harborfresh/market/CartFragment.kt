package com.harborfresh.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.content.Intent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.harborfresh.market.common.CartManager
import com.harborfresh.market.model.CartItem
import com.harborfresh.market.ui.cart.CartAdapter

class CartFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.activity_cart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvCart)
        val tvEmpty = view.findViewById<View>(R.id.tvCartEmpty)
        val tvSubtotal = view.findViewById<android.widget.TextView>(R.id.tvSubtotalValue)
        view.findViewById<View?>(R.id.btnBackCart)?.visibility = View.GONE

        val items: MutableList<CartItem> = CartManager.getCart(requireContext())
        rv.layoutManager = LinearLayoutManager(requireContext())
        val adapter = CartAdapter(items) { updated ->
            CartManager.updateQty(requireContext(), updated.id, updated.name, updated.qty)
            updateTotals(items, tvSubtotal, tvEmpty, rv)
        }
        rv.adapter = adapter

        view.findViewById<View?>(R.id.btnCheckout)?.setOnClickListener {
            if (items.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "Your cart is empty", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(requireContext(), DeliverySlotActivity::class.java))
            }
        }

        updateTotals(items, tvSubtotal, tvEmpty, rv)
    }

    private fun updateTotals(
        items: MutableList<CartItem>,
        tvSubtotal: android.widget.TextView,
        tvEmpty: View,
        rv: RecyclerView
    ) {
        val total = items.sumOf { it.price * it.qty }
        tvSubtotal.text = "\u20B9" + "%.2f".format(total)
        val isEmpty = items.isEmpty()
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rv.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}
