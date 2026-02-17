package com.harborfresh.market.model

data class PlacePaymentResponse(
    val success: Boolean,
    val message: String? = null,
    val order_id: Int? = null,
    val payment_method: String? = null,
    val payment_status: String? = null,
    val transaction_id: String? = null,
    val amount: Double? = null
)
