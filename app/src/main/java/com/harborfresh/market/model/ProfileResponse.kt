package com.harborfresh.market.model

data class ProfileResponse(
    val success: Boolean,
    val profile: Profile?,
    val stats: Stats?,
    val addresses: List<Address>?
) {
    data class Profile(
        val name: String?,
        val email: String?,
        val phone: String?
    )
    data class Stats(
        val orders: Int?,
        val saved: Double?,
        val points: Int?
    )
    data class Address(
        val id: Int?,
        val label: String?,
        val full_name: String?,
        val phone: String?,
        val address: String?,
        val city: String?,
        val pincode: String?,
        val is_default: Int?
    )
}
