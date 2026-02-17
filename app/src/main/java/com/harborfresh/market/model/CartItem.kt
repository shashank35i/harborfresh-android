package com.harborfresh.market.model

data class CartItem(
    val id: Int,
    val name: String,
    val price: Double,
    val imageUrl: String?,
    var qty: Int = 1
)
