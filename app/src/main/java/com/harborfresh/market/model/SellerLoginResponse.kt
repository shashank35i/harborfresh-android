package com.harborfresh.market.model

data class SellerLoginResponse(
    val success: Boolean,
    val message: String?,
    val seller_id: Int?,
    val status: String? = null,
    val verification_step: Int? = null,
    val seller: SellerSummary? = null
)

data class SellerSummary(
    val full_name: String?,
    val business_email: String?,
    val phone: String?,
    val is_verified: Int? = 0
)
