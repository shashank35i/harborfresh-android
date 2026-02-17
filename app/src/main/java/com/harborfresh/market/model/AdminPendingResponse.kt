package com.harborfresh.market.model

data class AdminPendingResponse(
    val success: Boolean,
    val pending: List<PendingSeller> = emptyList(),
    val counts: PendingCounts? = null,
    val message: String? = null
)

data class PendingSeller(
    val id: Int,
    val full_name: String?,
    val business_email: String?,
    val phone: String?,
    val police_verification_status: String?,
    val verification_status: String?,
    val face_match_verified: Int?,
    val liveness_verified: Int?,
    val aadhaar_verified: Int?
)

data class PendingCounts(
    val pending: Int = 0,
    val approved: Int = 0,
    val rejected: Int = 0
)
