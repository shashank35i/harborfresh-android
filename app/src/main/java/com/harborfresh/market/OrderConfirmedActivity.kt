package com.harborfresh.market

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class OrderConfirmedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_confirmed)

        val orderCode = intent.getStringExtra("order_code").orEmpty()
        val orderId = intent.getIntExtra("order_id", 0)
        val address = intent.getStringExtra("address").orEmpty()
        val slot = intent.getStringExtra("slot").orEmpty()
        val totalAmount = intent.getDoubleExtra("total_amount", 0.0)
        val totalItems = intent.getIntExtra("total_items", 0)

        findViewById<TextView?>(R.id.tvOrderId)?.text =
            if (orderCode.isNotBlank()) orderCode else "ORD-$orderId"
        findViewById<TextView?>(R.id.tvDeliveringValue)?.text =
            if (address.isNotBlank()) address else "Address not set"
        findViewById<TextView?>(R.id.tvEtaValue)?.text =
            if (slot.isNotBlank()) slot else "Delivery slot pending"
        findViewById<TextView?>(R.id.tvItemsValue)?.text =
            if (totalItems > 0) {
                "%d items - Rs %.2f".format(totalItems, totalAmount)
            } else {
                "Total: Rs %.2f".format(totalAmount)
            }

        findViewById<android.view.View?>(R.id.btnTrackOrder)?.setOnClickListener {
            val intent = Intent(this, TrackOrderActivity::class.java)
            intent.putExtra("order_id", orderId)
            startActivity(intent)
            finish()
        }

        findViewById<TextView?>(R.id.tvBackHome)?.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("open_tab", "home")
            startActivity(intent)
            finish()
        }
    }
}
