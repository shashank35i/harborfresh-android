package com.harborfresh.market

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.google.android.material.button.MaterialButton

class SubscriptionActivity : AppCompatActivity(), PurchasesUpdatedListener {

    private lateinit var btnSubscribe: MaterialButton
    private lateinit var btnSkipForNow: MaterialButton
    private lateinit var tvPrice: TextView
    private lateinit var tvSubPriceHint: TextView
    private lateinit var progress: ProgressBar

    private lateinit var billingClient: BillingClient
    private var productDetails: ProductDetails? = null

    companion object {
        private const val TAG = "SubscriptionActivity"

        // TODO: Replace with your Play Console SUBS product ID
        private const val REAL_SUBS_PRODUCT_ID = "seafood_premium_subscription"

        // Optional fallback (for quick internal testing)
        private const val TEST_INAPP_PRODUCT_ID = "android.test.purchased"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        bindViews()
        setupClickListeners()

        setLoading(true)
        setupBillingClient()
    }

    private fun bindViews() {
        btnSubscribe = findViewById(R.id.btnSubscribe)
        btnSkipForNow = findViewById(R.id.btnSkipForNow)
        tvPrice = findViewById(R.id.tvPrice)
        tvSubPriceHint = findViewById(R.id.tvSubPriceHint)
        progress = findViewById(R.id.progress)
    }

    private fun setupClickListeners() {
        btnSkipForNow.setOnClickListener {
            goNext()
        }
        btnSubscribe.setOnClickListener {
            launchBillingFlow()
        }
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    querySubscription()
                } else {
                    Log.e(TAG, "Billing setup failed: ${result.debugMessage}")
                    setLoading(false)
                    showToast("Billing not available: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected")
            }
        })
    }

    private fun querySubscription() {
        queryProduct(REAL_SUBS_PRODUCT_ID, BillingClient.ProductType.SUBS) { ok ->
            if (!ok) {
                Log.w(TAG, "SUBS product not found. Trying test INAPP.")
                queryProduct(TEST_INAPP_PRODUCT_ID, BillingClient.ProductType.INAPP) { testOk ->
                    if (!testOk) {
                        setLoading(false)
                        tvPrice.text = "Premium unavailable"
                        tvSubPriceHint.text = "No products found in Play Console"
                        showToast("No products found in Play Console")
                    }
                }
            }
        }
    }

    private fun queryProduct(productId: String, type: String, callback: (Boolean) -> Unit) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(type)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { result, list ->
            val ok = (result.responseCode == BillingClient.BillingResponseCode.OK && list.isNotEmpty())
            if (ok) {
                productDetails = list[0]
                applyPriceToUI(list[0])
                setLoading(false)
                callback(true)
            } else {
                Log.e(TAG, "queryProductDetails failed: ${result.debugMessage}")
                callback(false)
            }
        }
    }

    private fun applyPriceToUI(details: ProductDetails) {
        // Default placeholders
        tvPrice.text = "Premium"
        tvSubPriceHint.text = "Unlock all benefits"

        if (details.productType == BillingClient.ProductType.SUBS) {
            // Choose first offer token (you can improve selection logic later)
            val offer = details.subscriptionOfferDetails?.firstOrNull()
            val phases = offer?.pricingPhases?.pricingPhaseList

            // Try to find the recurring price phase
            val recurring = phases?.firstOrNull { it.billingPeriod != null && it.formattedPrice.isNotBlank() }
            val trial = phases?.firstOrNull { it.priceAmountMicros == 0L } // free trial phase often has 0

            if (recurring != null) {
                tvPrice.text = "${recurring.formattedPrice} / ${prettyPeriod(recurring.billingPeriod ?: "")}"
            } else {
                tvPrice.text = "Premium Subscription"
            }

            tvSubPriceHint.text = when {
                trial != null && trial.billingPeriod != null -> "Free trial for ${prettyPeriod(trial.billingPeriod)} • Cancel anytime"
                else -> "Cancel anytime • Priority delivery benefits"
            }
        } else {
            // INAPP fallback (test)
            val oneTime = details.oneTimePurchaseOfferDetails
            if (oneTime != null) {
                tvPrice.text = oneTime.formattedPrice
                tvSubPriceHint.text = "One-time purchase (test)"
            }
        }

        btnSubscribe.isEnabled = true
    }

    private fun prettyPeriod(period: String): String {
        // ISO-8601 like P1W, P1M, P3M, P1Y
        return when (period) {
            "P1W" -> "week"
            "P1M" -> "month"
            "P3M" -> "3 months"
            "P6M" -> "6 months"
            "P1Y" -> "year"
            else -> period.removePrefix("P").lowercase()
        }
    }

    private fun launchBillingFlow() {
        val details = productDetails ?: run {
            showToast("Product not loaded yet")
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        // For subscriptions, you MUST pass an offerToken
        if (details.productType == BillingClient.ProductType.SUBS) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken.isNullOrBlank()) {
                showToast("No subscription offer available")
                return
            }
            productParams.setOfferToken(offerToken)
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams.build()))
            .build()

        billingClient.launchBillingFlow(this, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    showToast("No purchase found")
                    return
                }
                purchases.forEach { handlePurchase(it) }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                showToast("Purchase canceled")
            }

            else -> {
                Log.e(TAG, "Purchase failed: ${result.debugMessage}")
                showToast("Purchase failed: ${result.debugMessage}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        Log.d(TAG, "Purchase success: ${purchase.products}")

        // (Optional) verify purchase.signature server-side if you have backend

        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.acknowledgePurchase(params) { ack ->
                if (ack.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase acknowledged")
                    showToast("Premium Activated!")
                    goNext()
                } else {
                    Log.e(TAG, "Acknowledge failed: ${ack.debugMessage}")
                    showToast("Activation pending: ${ack.debugMessage}")
                    goNext()
                }
            }
        } else {
            showToast("Premium Activated!")
            goNext()
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnSubscribe.isEnabled = !loading
        btnSubscribe.alpha = if (loading) 0.6f else 1f
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun goNext() {
        // TODO: change to your next screen (login/home)
        startActivity(Intent(this, Class.forName("com.harborfresh.market.ui.auth.page2")))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::billingClient.isInitialized) billingClient.endConnection()
        } catch (_: Exception) { }
    }
}
