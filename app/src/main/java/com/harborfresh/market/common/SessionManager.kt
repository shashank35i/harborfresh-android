package com.harborfresh.market.common

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("HarborFreshPrefs", Context.MODE_PRIVATE)

    companion object {
        const val IS_LOGGED_IN = "isLoggedIn"
        const val ONBOARDING_COMPLETE = "onboardingComplete"
        const val USER_ID = "userId"
        const val SELLER_ID = "sellerId"
        const val SELLER_STATUS = "sellerStatus"
        const val SELLER_STEP = "sellerStep"
        const val ROLE = "role"
        const val ADMIN_NAME = "adminName"
        const val ADMIN_EMAIL = "adminEmail"
        const val SELLER_LOCATION = "sellerLocation"
        const val SELLER_LAT = "sellerLat"
        const val SELLER_LNG = "sellerLng"
        const val USER_LOCATION = "userLocation"
        const val USER_LAT = "userLat"
        const val USER_LNG = "userLng"
        const val USER_ADDRESSES = "userAddresses"
        const val USER_ADDRESS_SELECTED = "userAddressSelected"
        const val DELIVERY_SLOT_ID = "deliverySlotId"
        const val DELIVERY_SLOT_LABEL = "deliverySlotLabel"
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        prefs.edit().putBoolean(IS_LOGGED_IN, isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(IS_LOGGED_IN, false)
    }

    fun setRole(role: String) {
        prefs.edit().putString(ROLE, role).apply()
    }

    fun getRole(): String? = prefs.getString(ROLE, null)

    fun saveUserId(id: Int) {
        prefs.edit().putInt(USER_ID, id).apply()
    }

    fun getUserId(): Int = prefs.getInt(USER_ID, 0)

    fun setOnboardingComplete(isComplete: Boolean) {
        prefs.edit().putBoolean(ONBOARDING_COMPLETE, isComplete).apply()
    }

    fun isOnboardingComplete(): Boolean {
        return prefs.getBoolean(ONBOARDING_COMPLETE, false)
    }

    fun saveSellerSession(id: Int, status: String?, step: Int?) {
        prefs.edit()
            .putInt(SELLER_ID, id)
            .putString(SELLER_STATUS, status)
            .putInt(SELLER_STEP, step ?: 0)
            .putString(ROLE, "seller")
            .putBoolean(IS_LOGGED_IN, true)
            .apply()
    }

    fun getSellerId(): Int {
        return prefs.getInt(SELLER_ID, 0)
    }

    fun getSellerStatus(): String? = prefs.getString(SELLER_STATUS, null)
    fun getSellerStep(): Int = prefs.getInt(SELLER_STEP, 0)

    fun saveAdminInfo(name: String?, email: String?) {
        prefs.edit().apply {
            name?.let { putString(ADMIN_NAME, it) }
            email?.let { putString(ADMIN_EMAIL, it) }
        }.apply()
    }

    fun getAdminName(): String? = prefs.getString(ADMIN_NAME, null)
    fun getAdminEmail(): String? = prefs.getString(ADMIN_EMAIL, null)

    fun saveSellerLocation(loc: String, lat: Double?, lng: Double?) {
        prefs.edit().apply {
            putString(SELLER_LOCATION, loc)
            lat?.let { putString(SELLER_LAT, it.toString()) }
            lng?.let { putString(SELLER_LNG, it.toString()) }
        }.apply()
    }

    fun getSellerLocation(): String? = prefs.getString(SELLER_LOCATION, null)
    fun getSellerLat(): Double? = prefs.getString(SELLER_LAT, null)?.toDoubleOrNull()
    fun getSellerLng(): Double? = prefs.getString(SELLER_LNG, null)?.toDoubleOrNull()

    fun saveUserLocation(loc: String, lat: Double?, lng: Double?) {
        prefs.edit().apply {
            putString(USER_LOCATION, loc)
            lat?.let { putString(USER_LAT, it.toString()) }
            lng?.let { putString(USER_LNG, it.toString()) }
        }.apply()
    }

    fun getUserLocation(): String? = prefs.getString(USER_LOCATION, null)
    fun getUserLat(): Double? = prefs.getString(USER_LAT, null)?.toDoubleOrNull()
    fun getUserLng(): Double? = prefs.getString(USER_LNG, null)?.toDoubleOrNull()

    fun saveAddresses(json: String) {
        prefs.edit().putString(USER_ADDRESSES, json).apply()
    }

    fun getAddresses(): String? = prefs.getString(USER_ADDRESSES, null)

    fun saveSelectedAddressId(id: String) {
        prefs.edit().putString(USER_ADDRESS_SELECTED, id).apply()
    }

    fun getSelectedAddressId(): String? = prefs.getString(USER_ADDRESS_SELECTED, null)

    fun saveDeliverySlot(id: Int, label: String) {
        prefs.edit()
            .putInt(DELIVERY_SLOT_ID, id)
            .putString(DELIVERY_SLOT_LABEL, label)
            .apply()
    }

    fun getDeliverySlotId(): Int = prefs.getInt(DELIVERY_SLOT_ID, 0)
    fun getDeliverySlotLabel(): String? = prefs.getString(DELIVERY_SLOT_LABEL, null)

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
