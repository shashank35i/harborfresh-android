package com.harborfresh.market.model

data class TrackOrderResponse(
    val success: Boolean,
    val message: String? = null,
    val order_code: String? = null,
    val status: String? = null,
    val is_live: Boolean? = null,
    val map: TrackOrderMap? = null,
    val delivery_partner: TrackOrderPartner? = null,
    val seller: TrackOrderSeller? = null,
    val timeline: List<TrackOrderTimeline> = emptyList()
)

data class TrackOrderMap(
    val delivery_lat: Double? = null,
    val delivery_lng: Double? = null,
    val customer_lat: Double? = null,
    val customer_lng: Double? = null,
    val seller_lat: Double? = null,
    val seller_lng: Double? = null,
    val seller_location: String? = null
)

data class TrackOrderPartner(
    val name: String? = null,
    val phone: String? = null,
    val rating: String? = null
)

data class TrackOrderSeller(
    val name: String? = null,
    val city: String? = null
)

data class TrackOrderTimeline(
    val status: String? = null,
    val message: String? = null,
    val created_at: String? = null
)
