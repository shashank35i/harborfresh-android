package com.harborfresh.market.model

import com.google.gson.annotations.SerializedName

data class CategoryResponse(
    val success: Boolean,
    val categories: List<CategoryItem>
)

data class CategoryItem(
    val name: String,
    val icon: String?,
    @SerializedName("total_items")
    val totalItems: Int
)
