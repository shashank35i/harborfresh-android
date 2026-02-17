package com.harborfresh.market

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.common.SessionManager
import com.harborfresh.market.model.AddressItem
import com.harborfresh.market.model.UserAddress
import com.harborfresh.market.ui.address.AddressAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DeliveryAddressActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var rvAddresses: RecyclerView
    private lateinit var adapter: AddressAdapter
    private lateinit var btnDeliverHere: View
    private var fromCheckout: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_address)

        sessionManager = SessionManager(this)
        btnDeliverHere = findViewById(R.id.btnDeliverHere)
        fromCheckout = intent.getBooleanExtra("from_checkout", false)

        findViewById<android.view.View?>(R.id.btnBack)?.setOnClickListener { finish() }
        findViewById<TextView?>(R.id.btnAddAddress)?.setOnClickListener { openAddEdit(null) }
        btnDeliverHere.setOnClickListener {
            val selectedId = sessionManager.getSelectedAddressId()
            if (selectedId.isNullOrBlank()) return@setOnClickListener
            if (fromCheckout) {
                openPayment()
            } else {
                android.widget.Toast.makeText(this, "Default address updated", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        rvAddresses = findViewById(R.id.rvAddresses)
        rvAddresses.layoutManager = LinearLayoutManager(this)
        adapter = AddressAdapter(
            onSelect = { address ->
                saveSelectedAddress(address)
            },
            onEdit = { address ->
                openAddEdit(address)
            }
        )
        rvAddresses.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        fetchAddresses()
    }

    private fun saveSelectedAddress(address: UserAddress) {
        sessionManager.saveSelectedAddressId(address.id)
        val loc = listOf(address.line1, address.line2, address.city, address.pin)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        sessionManager.saveUserLocation(loc, null, null)
        updateDeliverButton(address.id)
        adapter.submit(loadAddresses(), address.id)
    }

    private fun openAddEdit(address: UserAddress?) {
        val intent = Intent(this, DeliveryAddressEditActivity::class.java)
        address?.let {
            intent.putExtra("address_id", it.id)
        }
        startActivity(intent)
    }

    private fun fetchAddresses() {
        val userId = sessionManager.getUserId()
        if (userId <= 0) {
            adapter.submit(emptyList(), null)
            updateDeliverButton(null)
            return
        }
        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { ApiClient.apiService.getAddresses(userId) }
                if (resp.success) {
                    val list = resp.addresses.map { it.toUserAddress() }
                    persistAddresses(list)
                    val selectedId = ensureSelectedId(list)
                    adapter.submit(list, selectedId)
                    updateDeliverButton(selectedId)
                } else {
                    val list = loadAddresses()
                    val selectedId = sessionManager.getSelectedAddressId()
                    adapter.submit(list, selectedId)
                    updateDeliverButton(selectedId)
                }
            } catch (_: Exception) {
                val list = loadAddresses()
                val selectedId = sessionManager.getSelectedAddressId()
                adapter.submit(list, selectedId)
                updateDeliverButton(selectedId)
            }
        }
    }

    private fun ensureSelectedId(list: List<UserAddress>): String? {
        val current = sessionManager.getSelectedAddressId()
        if (current != null && list.any { it.id == current }) return current
        val def = list.firstOrNull { it.isDefault } ?: list.firstOrNull()
        return def?.id?.also { sessionManager.saveSelectedAddressId(it) }
    }

    private fun AddressItem.toUserAddress(): UserAddress {
        val addressText = address?.trim().orEmpty()
        val split = if (addressText.contains(",")) {
            addressText.split(",", limit = 2).map { it.trim() }
        } else {
            listOf(addressText, "")
        }
        return UserAddress(
            id = id.toString(),
            label = label ?: "Address",
            name = full_name ?: "",
            line1 = split.getOrNull(0) ?: "",
            line2 = split.getOrNull(1) ?: "",
            city = city ?: "",
            pin = pincode ?: "",
            isDefault = (is_default ?: 0) == 1
        )
    }

    private fun persistAddresses(list: List<UserAddress>) {
        val arr = JSONArray()
        list.forEach { addr ->
            val o = JSONObject()
            o.put("id", addr.id)
            o.put("label", addr.label)
            o.put("name", addr.name)
            o.put("line1", addr.line1)
            o.put("line2", addr.line2)
            o.put("city", addr.city)
            o.put("pin", addr.pin)
            o.put("isDefault", addr.isDefault)
            arr.put(o)
        }
        sessionManager.saveAddresses(arr.toString())
    }

    private fun loadAddresses(): List<UserAddress> {
        val raw = sessionManager.getAddresses() ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = ArrayList<UserAddress>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    UserAddress(
                        id = o.optString("id"),
                        label = o.optString("label"),
                        name = o.optString("name"),
                        line1 = o.optString("line1"),
                        line2 = o.optString("line2"),
                        city = o.optString("city"),
                        pin = o.optString("pin"),
                        isDefault = o.optBoolean("isDefault", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun updateDeliverButton(selectedId: String?) {
        btnDeliverHere.visibility = if (selectedId.isNullOrBlank()) View.GONE else View.VISIBLE
        val btnText = btnDeliverHere as? TextView
        if (btnText != null) {
            btnText.text = if (fromCheckout) "Deliver Here" else "Set as Default"
        }
    }

    private fun openPayment() {
        startActivity(Intent(this, PaymentActivity::class.java))
    }
}
