package com.harborfresh.market.model

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val user_id: Int?,
    val name: String?,
    val role: String?
)
