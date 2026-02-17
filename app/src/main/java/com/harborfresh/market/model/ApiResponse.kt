package com.harborfresh.market.model

data class ApiResponse(
    val success: Boolean,
    val message: String?,
    val seller_id: Int? = null,
    val status: String? = null,
    val verification_step: Int? = null
)
