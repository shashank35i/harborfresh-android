package com.harborfresh.market.model

data class SellerProductDetail(
    val success: Boolean,
    val product: Product?,
    val seller: SellerBrief?,
    @com.google.gson.annotations.SerializedName("other_products")
    val otherProducts: List<Product> = emptyList()
)

data class SellerBrief(
    val id: Int?,
    val name: String?,
    val rating: String?,
    val business: String? = null,
    val city: String? = null,
    val image: String? = null
)
