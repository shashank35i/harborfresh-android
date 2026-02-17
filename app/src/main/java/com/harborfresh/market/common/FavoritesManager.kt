package com.harborfresh.market.common

import android.content.Context
import com.harborfresh.market.model.Product
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object FavoritesManager {
    private const val PREFS = "HarborFreshPrefs"
    private const val KEY = "favoriteProducts"
    private val gson = Gson()

    fun getFavorites(context: Context): MutableList<Product> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<Product>>() {}.type
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun isFavorite(context: Context, productId: Int): Boolean {
        return getFavorites(context).any { it.id == productId }
    }

    fun toggleFavorite(context: Context, product: Product): Boolean {
        val list = getFavorites(context)
        val existing = list.indexOfFirst { it.id == product.id }
        val isFav = if (existing >= 0) {
            list.removeAt(existing)
            false
        } else {
            list.add(product)
            true
        }
        saveFavorites(context, list)
        return isFav
    }

    fun removeFavorite(context: Context, productId: Int) {
        val list = getFavorites(context).filterNot { it.id == productId }
        saveFavorites(context, list.toMutableList())
    }

    private fun saveFavorites(context: Context, list: MutableList<Product>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY, gson.toJson(list)).apply()
    }
}
