package com.harborfresh.market

import android.os.Bundle
import android.widget.ImageView
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.common.CartManager
import com.harborfresh.market.common.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class PaymentActivity : AppCompatActivity(), PaymentResultListener {

    private val razorpayKey = "rzp_test_1DP5mmOlF5G5ag"
    private lateinit var sessionManager: SessionManager
    private lateinit var btnPlaceOrder: MaterialButton
    private var rowUpi: MaterialCardView? = null
    private var rowCard: MaterialCardView? = null
    private var rowWallet: MaterialCardView? = null
    private var cardCOD: MaterialCardView? = null

    private var selectedMethod: String = "UPI"
    private var pendingOrderId: Int? = null
    private var pendingOrderCode: String? = null
    private var pendingAmount: Double = 0.0
    private var pendingItems: Int = 0
    private var summarySubtotal: Double = 0.0
    private var summaryDeliveryFee: Double = 0.0
    private var summaryDiscount: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        sessionManager = SessionManager(this)
        Checkout.preload(applicationContext)

        findViewById<ImageView?>(R.id.btnBackPayment)?.setOnClickListener { finish() }

        rowUpi = findViewById(R.id.rowUpi)
        rowCard = findViewById(R.id.rowCard)
        rowWallet = findViewById(R.id.rowWallet)
        cardCOD = findViewById(R.id.cardCOD)
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder)

        rowUpi?.setOnClickListener { setMethod("UPI") }
        rowCard?.setOnClickListener { setMethod("CARD") }
        rowWallet?.setOnClickListener { setMethod("WALLET") }
        cardCOD?.setOnClickListener { setMethod("COD") }

        setMethod("UPI")
        bindOrderSummary()
        btnPlaceOrder.setOnClickListener { placeOrderAndPay() }
    }

    private fun setMethod(method: String) {
        selectedMethod = method
        updateSelectionUi()
        Toast.makeText(this, "$method selected", Toast.LENGTH_SHORT).show()
    }

    private fun updateSelectionUi() {
        val selectedColor = 0xFF0B2A43.toInt()
        val normalStroke = 0xFFE7EDF4.toInt()
        val selectedStroke = 0xFF0B2A43.toInt()

        rowUpi?.strokeWidth = if (selectedMethod == "UPI") 2 else 1
        rowUpi?.strokeColor = if (selectedMethod == "UPI") selectedStroke else normalStroke
        rowCard?.strokeWidth = if (selectedMethod == "CARD") 2 else 1
        rowCard?.strokeColor = if (selectedMethod == "CARD") selectedStroke else normalStroke
        rowWallet?.strokeWidth = if (selectedMethod == "WALLET") 2 else 1
        rowWallet?.strokeColor = if (selectedMethod == "WALLET") selectedStroke else normalStroke

        val codSelected = selectedMethod == "COD"
        cardCOD?.setCardBackgroundColor(if (codSelected) selectedColor else 0xFF0B2239.toInt())
    }

    private fun placeOrderAndPay() {
        val userId = sessionManager.getUserId()
        val address = sessionManager.getUserLocation().orEmpty()
        val slotId = sessionManager.getDeliverySlotId()

        if (userId <= 0) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            return
        }
        if (address.isBlank()) {
            Toast.makeText(this, "Select delivery address", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedMethod == "COD") {
            Toast.makeText(this, "Select online payment method", Toast.LENGTH_SHORT).show()
            return
        }

        val cart = CartManager.getCart(this)
        if (cart.isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val items = cart.map {
            mapOf(
                "product_id" to it.id,
                "quantity" to it.qty,
                "price" to it.price
            )
        }

        val payload = HashMap<String, Any>()
        payload["user_id"] = userId
        payload["delivery_address"] = address
        payload["items"] = items
        if (slotId > 0) {
            payload["delivery_slot_id"] = slotId
        }

        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { ApiClient.apiService.placeOrderApp(payload) }
                if (resp.success && resp.order_id != null) {
                    pendingOrderId = resp.order_id
                    pendingOrderCode = resp.order_code
                    pendingAmount = resp.total_amount ?: 0.0
                    pendingItems = resp.total_items ?: 0

                    openRazorpay()
                } else {
                    Toast.makeText(this@PaymentActivity, resp.message ?: "Order failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("PaymentActivity", "Place order failed", e)
                Toast.makeText(this@PaymentActivity, e.localizedMessage ?: "Order failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openRazorpay() {
        val orderId = pendingOrderId ?: return
        val amountPaise = (pendingAmount * 100).toInt().coerceAtLeast(1)
        val options = JSONObject()
        options.put("name", "HarborFresh")
        options.put("description", "Order ${pendingOrderCode ?: orderId}")
        options.put("currency", "INR")
        options.put("amount", amountPaise)

        val checkout = Checkout()
        try {
            checkout.setKeyID(razorpayKey)
            checkout.open(this, options)
        } catch (e: Exception) {
            Log.e("PaymentActivity", "Razorpay open failed", e)
            Toast.makeText(this, e.localizedMessage ?: "Razorpay failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentID: String?) {
        finalizePayment(selectedMethod)
    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, response ?: "Payment failed", Toast.LENGTH_SHORT).show()
    }

    private fun finalizePayment(method: String) {
        val orderId = pendingOrderId ?: return
        val methodToSend = when (method) {
            "COD" -> "COD"
            "CARD" -> "CARD"
            "WALLET" -> "WALLET"
            else -> "UPI"
        }
        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.placePayment(
                        HashMap<String, Any>().apply {
                            put("order_id", orderId)
                            put("payment_method", methodToSend)
                        }
                    )
                }
                if (resp.success) {
                    CartManager.clear(this@PaymentActivity)
                    openConfirmed()
                } else {
                    Toast.makeText(this@PaymentActivity, resp.message ?: "Payment failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("PaymentActivity", "Payment finalize failed", e)
                Toast.makeText(this@PaymentActivity, e.localizedMessage ?: "Payment failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openConfirmed() {
        val intent = android.content.Intent(this, OrderConfirmedActivity::class.java)
        intent.putExtra("order_id", pendingOrderId ?: 0)
        intent.putExtra("order_code", pendingOrderCode ?: "")
        intent.putExtra("address", sessionManager.getUserLocation().orEmpty())
        intent.putExtra("slot", sessionManager.getDeliverySlotLabel().orEmpty())
        intent.putExtra("total_amount", pendingAmount)
        intent.putExtra("total_items", pendingItems)
        startActivity(intent)
        finish()
    }

    private fun bindOrderSummary() {
        val cart = CartManager.getCart(this)
        summarySubtotal = cart.sumOf { it.price * it.qty }
        summaryDeliveryFee = 0.0
        summaryDiscount = 0.0
        val total = (summarySubtotal + summaryDeliveryFee - summaryDiscount).coerceAtLeast(0.0)

        findViewById<TextView?>(R.id.tvSubtotalValue)?.text =
            "Rs " + String.format("%.0f", summarySubtotal)
        findViewById<TextView?>(R.id.tvDeliveryFeeValue)?.text =
            "Rs " + String.format("%.0f", summaryDeliveryFee)
        findViewById<TextView?>(R.id.tvDiscountValue)?.text =
            "- Rs " + String.format("%.0f", summaryDiscount)
        findViewById<TextView?>(R.id.tvTotalValue)?.text =
            "Rs " + String.format("%.0f", total)
    }
}
