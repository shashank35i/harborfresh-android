package com.harborfresh.market.model

data class UserAddress(
    val id: String,
    val label: String,
    val name: String,
    val line1: String,
    val line2: String,
    val city: String,
    val pin: String,
    val isDefault: Boolean
)
