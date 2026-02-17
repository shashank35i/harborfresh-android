package com.harborfresh.market.api

import com.harborfresh.market.model.ApiResponse
import com.harborfresh.market.model.CategoryResponse
import com.harborfresh.market.model.LoginResponse
import com.harborfresh.market.model.ProductResponse
import com.harborfresh.market.model.PlaceOrderResponse
import com.harborfresh.market.model.PlacePaymentResponse
import com.harborfresh.market.model.SellerLoginResponse
import com.harborfresh.market.model.SellerStatusResponse
import com.harborfresh.market.model.FaceMatchRequest
import com.harborfresh.market.model.AdminPendingResponse
import com.harborfresh.market.model.CustomerOrdersResponse
import com.harborfresh.market.model.AdminAllSellersResponse
import com.harborfresh.market.model.SellerOrderDetailsResponse
import com.harborfresh.market.model.AddressResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {

    // AUTH
    @POST("user/signup.php")
    suspend fun signup(@Body data: Map<String, String>): ApiResponse

    @POST("user/send_otp.php")
    suspend fun sendOtp(@Body data: Map<String, String>): ApiResponse

    @POST("user/verify_otp.php")
    suspend fun verifyOtp(@Body data: Map<String, String>): ApiResponse

    @POST("user/login.php")
    suspend fun login(@Body data: Map<String, String>): LoginResponse

    @POST("user/reset_password.php")
    suspend fun resetPassword(@Body data: Map<String, String>): ApiResponse

    // SELLER
    @POST("api/seller/register.php")
    suspend fun registerSeller(@Body data: Map<String, String>): ApiResponse

    @POST("api/seller/login.php")
    suspend fun loginSeller(@Body data: Map<String, String>): SellerLoginResponse

    @POST("api/seller/personal.php")
    suspend fun savePersonal(
        @Header("X-Seller-Id") sellerId: Int,
        @Body data: Map<String, String>
    ): ApiResponse

    @POST("api/seller/business.php")
    suspend fun saveBusiness(
        @Header("X-Seller-Id") sellerId: Int,
        @Body data: Map<String, String>
    ): ApiResponse

    @POST("api/seller/legal.php")
    suspend fun saveLegal(
        @Header("X-Seller-Id") sellerId: Int,
        @Body data: Map<String, String>
    ): ApiResponse

    @Multipart
    @POST("api/seller/documents.php")
    suspend fun uploadSellerDocuments(
        @Header("X-Seller-Id") sellerId: Int,
        @Part fishing_license_doc: MultipartBody.Part,
        @Part government_id_doc: MultipartBody.Part,
        @Part address_proof_doc: MultipartBody.Part
    ): ApiResponse

    @Multipart
    @POST("api/seller/identity/aadhaar.php")
    suspend fun uploadAadhaar(
        @Header("X-Seller-Id") sellerId: Int,
        @Part("aadhaar_number") aadhaarNumber: RequestBody,
        @Part("aadhaar_name") aadhaarName: RequestBody,
        @Part aadhaar_doc: MultipartBody.Part
    ): ApiResponse

    @Multipart
    @POST("api/seller/identity/selfie.php")
    suspend fun uploadSelfie(
        @Header("X-Seller-Id") sellerId: Int,
        @Part selfie_image: MultipartBody.Part
    ): ApiResponse

    @POST("api/seller/identity/facematch.php")
    suspend fun submitFaceMatch(
        @Header("X-Seller-Id") sellerId: Int,
        @Body body: FaceMatchRequest
    ): ApiResponse

    @FormUrlEncoded
    @POST("api/seller/identity/submit.php")
    suspend fun submitIdentity(
        @Header("X-Seller-Id") sellerId: Int,
        @Field("seller_id") sellerIdField: Int
    ): ApiResponse

    @FormUrlEncoded
    @POST("seller/police_verification.php")
    suspend fun verifyPolice(
        @Field("seller_id") sellerId: Int
    ): ApiResponse

    // ADMIN
    @GET("api/admin/pending_verifications.php")
    suspend fun getPendingVerifications(): AdminPendingResponse

    @GET("api/admin/list_sellers.php")
    suspend fun getAllSellers(): AdminAllSellersResponse

    @GET("api/admin/seller_detail.php")
    suspend fun getSellerDetail(
        @Query("seller_id") sellerId: Int
    ): SellerStatusResponse

    @FormUrlEncoded
    @POST("api/admin/update_seller_status.php")
    suspend fun updateSellerStatus(
        @Field("seller_id") sellerId: Int,
        @Field("status") status: String
    ): ApiResponse

    @GET("api/seller/onboarding/status.php")
    suspend fun getSellerStatus(
        @Header("X-Seller-Id") sellerId: Int
    ): SellerStatusResponse

    @GET("seller/dashboard.php")
    suspend fun getSellerDashboard(
        @Query("seller_id") sellerId: Int
    ): com.harborfresh.market.model.SellerDashboardResponse

    @GET("seller/get_orders.php")
    suspend fun getSellerOrders(
        @Query("seller_id") sellerId: Int
    ): com.harborfresh.market.model.SellerOrdersResponse

    @GET("seller/get_order_details.php")
    suspend fun getSellerOrderDetails(
        @Query("order_id") orderId: Int,
        @Query("seller_id") sellerId: Int
    ): SellerOrderDetailsResponse

    @FormUrlEncoded
    @POST("seller/accept_order.php")
    suspend fun acceptSellerOrder(
        @Field("order_id") orderId: Int,
        @Field("seller_id") sellerId: Int
    ): ApiResponse

    @FormUrlEncoded
    @POST("seller/decline_order.php")
    suspend fun declineSellerOrder(
        @Field("order_id") orderId: Int,
        @Field("seller_id") sellerId: Int
    ): ApiResponse

    @FormUrlEncoded
    @POST("seller/mark_order_preparing.php")
    suspend fun markSellerOrderPreparing(
        @Field("order_id") orderId: Int,
        @Field("seller_id") sellerId: Int
    ): ApiResponse

    @FormUrlEncoded
    @POST("seller/mark_order_ready.php")
    suspend fun markSellerOrderReady(
        @Field("order_id") orderId: Int,
        @Field("seller_id") sellerId: Int
    ): ApiResponse

    @Multipart
    @POST("seller/add_product.php")
    suspend fun uploadSellerProduct(
        @Part("seller_id") sellerId: RequestBody,
        @Part("product_name") productName: RequestBody,
        @Part("quantity") quantity: RequestBody,
        @Part("price") price: RequestBody,
        @Part("category") category: RequestBody? = null,
        @Part("freshness") freshness: RequestBody? = null,
        @Part("location_name") locationName: RequestBody? = null,
        @Part("latitude") latitude: RequestBody? = null,
        @Part("longitude") longitude: RequestBody? = null,
        @Part image: MultipartBody.Part? = null
    ): ApiResponse

    // CUSTOMER
    @GET("user/home.php")
    suspend fun getHome(
        @Query("user_id") userId: Int
    ): com.harborfresh.market.model.HomeResponse

    @GET("user/profile.php")
    suspend fun getProfile(
        @Query("user_id") userId: Int
    ): com.harborfresh.market.model.ProfileResponse

    @GET("user/list_orders.php")
    suspend fun getOrders(
        @Query("user_id") userId: Int
    ): CustomerOrdersResponse

    @GET("user/track_order.php")
    suspend fun trackOrder(
        @Query("order_id") orderId: Int
    ): com.harborfresh.market.model.TrackOrderResponse

    @POST("user/place_order_app.php")
    suspend fun placeOrderApp(
        @Body data: Map<String, @JvmSuppressWildcards Any>
    ): PlaceOrderResponse

    @POST("user/place_payment.php")
    suspend fun placePayment(
        @Body data: Map<String, @JvmSuppressWildcards Any>
    ): PlacePaymentResponse

    // HOME & PRODUCTS
    @GET("user/get_categories.php")
    suspend fun getCategories(): CategoryResponse

    @GET("user/get_popular_products.php")
    suspend fun getPopularProducts(): ProductResponse

    @GET("user/get_products_by_category.php")
    suspend fun getProductsByCategory(@Query("category") category: String): ProductResponse

    @GET("user/get_product_details.php")
    suspend fun getProductDetails(
        @Query("product_id") productId: Int
    ): com.harborfresh.market.model.SellerProductDetail

    @GET("user/get_addresses.php")
    suspend fun getAddresses(
        @Query("user_id") userId: Int
    ): AddressResponse

    @POST("user/add_address.php")
    suspend fun addAddress(
        @Body data: Map<String, @JvmSuppressWildcards Any>
    ): ApiResponse

    @POST("user/update_address.php")
    suspend fun updateAddress(
        @Body data: Map<String, @JvmSuppressWildcards Any>
    ): ApiResponse

    @GET("user/search_products.php")
    suspend fun searchProducts(
        @Query("q") query: String
    ): ProductResponse

    @GET("user/get_seller_product_details.php")
    suspend fun getSellerProductDetails(
        @Query("product_id") productId: Int
    ): com.harborfresh.market.model.SellerProductDetail
}


