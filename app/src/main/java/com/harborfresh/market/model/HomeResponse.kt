package com.harborfresh.market.model

data class HomeResponse(
    val success: Boolean,
    val location: Location?,
    val today_banner: Banner?,
    val categories: List<Category>?,
    val popular_today: List<Product>?
) {
    data class Location(
        val address: String?,
        val city: String?
    )
    data class Banner(
        val title: String?,
        val subtitle: String?
    )
}
