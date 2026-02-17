package com.harborfresh.market.model

data class AdminSellerSummary(
    val id: Int,
    val full_name: String?,
    val business_email: String?,
    val phone: String?,
    val status: String?,
    val verification_status: String?,
    val police_verification_status: String?,
    val face_match_verified: Int?,
    val liveness_verified: Int?,
    val aadhaar_verified: Int?
)

data class AdminAllSellersResponse(
    val success: Boolean,
    val message: String? = null,
    val sellers: List<AdminSellerSummary> = emptyList()
)
