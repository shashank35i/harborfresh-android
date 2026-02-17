package com.harborfresh.market

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.harborfresh.market.common.SessionManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton

class DeliverySlotActivity : AppCompatActivity() {

    private lateinit var btnContinue: MaterialButton
    private lateinit var slot1: MaterialCardView
    private lateinit var slot2: MaterialCardView
    private lateinit var slot3: MaterialCardView
    private lateinit var tabToday: MaterialCardView
    private lateinit var tabTomorrow: MaterialCardView
    private lateinit var tvTabToday: TextView
    private lateinit var tvTabTomorrow: TextView
    private lateinit var sessionManager: SessionManager

    private var selectedSlotId: Int = 0
    private var selectedSlotLabel: String = ""
    private var isTomorrow: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_slot)

        sessionManager = SessionManager(this)

        findViewById<android.view.View?>(R.id.btnBack)?.setOnClickListener { finish() }

        btnContinue = findViewById(R.id.btnContinue)
        slot1 = findViewById(R.id.cardSlot1)
        slot2 = findViewById(R.id.cardSlot2)
        slot3 = findViewById(R.id.cardSlot3)
        tabToday = findViewById(R.id.tabToday)
        tabTomorrow = findViewById(R.id.tabTomorrow)
        tvTabToday = findViewById(R.id.tvTabToday)
        tvTabTomorrow = findViewById(R.id.tvTabTomorrow)

        slot1.setOnClickListener { selectSlot(slot1) }
        slot2.setOnClickListener { selectSlot(slot2) }
        slot3.setOnClickListener { /* unavailable */ }
        tabToday.setOnClickListener { setDay(false) }
        tabTomorrow.setOnClickListener { setDay(true) }

        btnContinue.setOnClickListener {
            if (selectedSlotId > 0 && selectedSlotLabel.isNotBlank()) {
                sessionManager.saveDeliverySlot(selectedSlotId, selectedSlotLabel)
            }
            val intent = Intent(this, DeliveryAddressActivity::class.java)
            intent.putExtra("from_checkout", true)
            startActivity(intent)
        }
    }

    private fun selectSlot(selected: MaterialCardView) {
        markSelected(slot1, slot1 == selected)
        markSelected(slot2, slot2 == selected)
        if (slot1 == selected) {
            selectedSlotId = 1
            selectedSlotLabel = if (isTomorrow) {
                "Tomorrow, 2:00 PM - 4:00 PM"
            } else {
                "Today, 2:00 PM - 4:00 PM"
            }
        } else if (slot2 == selected) {
            selectedSlotId = 2
            selectedSlotLabel = if (isTomorrow) {
                "Tomorrow, 4:00 PM - 6:00 PM"
            } else {
                "Today, 4:00 PM - 6:00 PM"
            }
        }
        btnContinue.isEnabled = true
        btnContinue.setBackgroundColor(0xFF0B2A43.toInt())
    }

    private fun markSelected(card: MaterialCardView, selected: Boolean) {
        val bg = if (selected) 0xFFEFF6FF.toInt() else 0xFFFFFFFF.toInt()
        val stroke = if (selected) 0xFF0B2A43.toInt() else 0xFFE8EDF3.toInt()
        card.setCardBackgroundColor(bg)
        card.strokeColor = stroke
        card.strokeWidth = if (selected) 2 else 1
    }

    private fun setDay(tomorrow: Boolean) {
        isTomorrow = tomorrow
        val selectedColor = 0xFF0B2A43.toInt()
        val unselectedColor = 0xFFFFFFFF.toInt()
        val unselectedStroke = 0xFFEEF2F7.toInt()
        val selectedText = 0xFFFFFFFF.toInt()
        val unselectedText = 0xFF111827.toInt()

        tabToday.setCardBackgroundColor(if (tomorrow) unselectedColor else selectedColor)
        tabToday.strokeColor = if (tomorrow) unselectedStroke else selectedColor
        tabTomorrow.setCardBackgroundColor(if (tomorrow) selectedColor else unselectedColor)
        tabTomorrow.strokeColor = if (tomorrow) selectedColor else unselectedStroke
        tvTabToday.setTextColor(if (tomorrow) unselectedText else selectedText)
        tvTabTomorrow.setTextColor(if (tomorrow) selectedText else unselectedText)

        selectedSlotId = 0
        selectedSlotLabel = ""
        btnContinue.isEnabled = false
        btnContinue.setBackgroundColor(0xFF8B97A3.toInt())
        markSelected(slot1, false)
        markSelected(slot2, false)
    }
}
