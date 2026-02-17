package com.harborfresh.market.model

data class SellerDashboardResponse(
    val success: Boolean,
    val seller: SellerInfo?,
    val stats: SellerStats?,
    val message: String? = null
)

data class SellerInfo(
    val full_name: String?,
    val business_name: String?,
    val city: String?,
    val profile_image: String?
)

data class SellerStats(
    val products: Int = 0,
    val orders: Int = 0,
    val pending: Int = 0,
    val revenue: Double = 0.0
)
