package com.harborfresh.market.model

data class ProductResponse(
    val success: Boolean,
    val products: List<Product>
)
