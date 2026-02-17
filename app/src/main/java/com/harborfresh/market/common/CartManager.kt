package com.harborfresh.market.common

import android.content.Context
import com.harborfresh.market.model.CartItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CartManager {
    private const val PREF_NAME = "cart_prefs"
    private const val KEY_CART = "cart_items"
    private val gson = Gson()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getCart(context: Context): MutableList<CartItem> {
        val json = prefs(context).getString(KEY_CART, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<CartItem>>() {}.type
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    private fun saveCart(context: Context, items: List<CartItem>) {
        prefs(context).edit()
            .putString(KEY_CART, gson.toJson(items))
            .apply()
    }

    fun addItem(context: Context, item: CartItem) {
        val items = getCart(context)
        val existing = items.indexOfFirst { it.id == item.id && it.name == item.name }
        if (existing >= 0) {
            val updated = items[existing]
            updated.qty += item.qty
            items[existing] = updated
        } else {
            items.add(item)
        }
        saveCart(context, items)
    }

    fun updateQty(context: Context, productId: Int, name: String, qty: Int) {
        val items = getCart(context)
        val idx = items.indexOfFirst { it.id == productId && it.name == name }
        if (idx >= 0) {
            if (qty <= 0) {
                items.removeAt(idx)
            } else {
                items[idx].qty = qty
            }
            saveCart(context, items)
        }
    }

    fun clear(context: Context) {
        saveCart(context, emptyList())
    }
}
