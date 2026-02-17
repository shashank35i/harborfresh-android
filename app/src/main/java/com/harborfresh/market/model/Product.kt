package com.harborfresh.market.model

import com.google.gson.annotations.SerializedName

// This model perfectly matches the JSON from your backend
data class Product(
    val id: Int,
    val name: String,
    val description: String?,
    val rating: String?,
    val freshness: String?,
    @SerializedName("seller_id")
    val sellerId: Int? = null,

    // Use @SerializedName to map JSON keys to different variable names
    @SerializedName(value = "price_per_kg", alternate = ["price"])
    val price: String, // The JSON key is "price_per_kg", but we'll call it "price" in our app

    @SerializedName(value = "image_url", alternate = ["image"])
    val imageUrl: String?, // Supports both image_url and image keys
    @SerializedName("location_name")
    val locationName: String? = null
)
