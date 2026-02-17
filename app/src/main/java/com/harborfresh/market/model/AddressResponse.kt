package com.harborfresh.market.model

data class AddressResponse(
    val success: Boolean,
    val message: String? = null,
    val addresses: List<AddressItem> = emptyList()
)

data class AddressItem(
    val id: Int,
    val user_id: Int,
    val label: String?,
    val full_name: String?,
    val phone: String?,
    val address: String?,
    val city: String?,
    val pincode: String?,
    val is_default: Int?
)
