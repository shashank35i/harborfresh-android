package com.harborfresh.market.model

data class SellerOrdersResponse(
    val success: Boolean,
    val orders: List<SellerOrderItem> = emptyList(),
    val message: String? = null
)

data class SellerOrderItem(
    val id: Int?,
    val order_code: String?,
    val customer_name: String?,
    val customer_phone: String?,
    val product_name: String?,
    val quantity: Int?,
    val total_price: Double?,
    val status: String?,
    val created_at: String?,
    val slot_day: String?,
    val slot_time_range: String?
)
