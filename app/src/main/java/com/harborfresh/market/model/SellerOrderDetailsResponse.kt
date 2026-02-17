package com.harborfresh.market.model

data class SellerOrderDetailsResponse(
    val success: Boolean,
    val message: String? = null,
    val order: SellerOrderDetails? = null,
    val items: List<SellerOrderLine> = emptyList(),
    val total_amount: Double? = null
)

data class SellerOrderDetails(
    val order_code: String? = null,
    val customer_name: String? = null,
    val customer_phone: String? = null,
    val delivery_address: String? = null,
    val status: String? = null
)

data class SellerOrderLine(
    val product_name: String? = null,
    val quantity: Int? = null,
    val price: Double? = null
)
