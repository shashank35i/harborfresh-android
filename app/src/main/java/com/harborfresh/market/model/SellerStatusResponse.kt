package com.harborfresh.market.model

data class SellerStatusResponse(
    val success: Boolean,
    val message: String? = null,
    val data: OnboardingStatus? = null
)

data class OnboardingStatus(
    val seller: SellerStatus,
    val personal_done: Boolean,
    val business_done: Boolean,
    val legal_done: Boolean,
    val documents: DocumentStatus,
    val identity: IdentityStatus
)

data class SellerStatus(
    val id: Int,
    val full_name: String?,
    val business_email: String?,
    val phone: String?,
    val status: String?,
    val verification_step: Int,
    val is_verified: Int
)

data class DocumentStatus(
    val fishing_license: Boolean,
    val government_id: Boolean,
    val address_proof: Boolean
)

data class IdentityStatus(
    val aadhaar_number: String? = null,
    val aadhaar_name: String? = null,
    val aadhaar_doc: String? = null,
    val selfie_image: String? = null,
    val aadhaar_verified: Int,
    val liveness_verified: Int,
    val face_match_verified: Int,
    val face_match_score: Double?,
    val police_verification_status: String?,
    val verification_status: String?
)
