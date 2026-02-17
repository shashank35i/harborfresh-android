package com.harborfresh.market.model

data class PlaceOrderResponse(
    val success: Boolean,
    val message: String? = null,
    val order_id: Int? = null,
    val order_code: String? = null,
    val total_amount: Double? = null,
    val total_items: Int? = null
)
