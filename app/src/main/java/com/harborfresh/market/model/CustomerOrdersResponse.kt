package com.harborfresh.market.model

data class CustomerOrder(
    val id: Int,
    val order_code: String?,
    val status: String?,
    val total_amount: Double?,
    val created_at: String?,
    val items: Int?
)

data class CustomerOrdersResponse(
    val success: Boolean,
    val message: String? = null,
    val orders: List<CustomerOrder> = emptyList()
)
