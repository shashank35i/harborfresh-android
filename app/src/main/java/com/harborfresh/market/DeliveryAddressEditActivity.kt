package com.harborfresh.market

import android.Manifest
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.common.SessionManager
import com.harborfresh.market.model.UserAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class DeliveryAddressEditActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var editingId: String? = null

    private lateinit var etName: EditText
    private lateinit var etLine1: EditText
    private lateinit var etLine2: EditText
    private lateinit var etCity: EditText
    private lateinit var etPin: EditText
    private lateinit var tvSave: TextView
    private lateinit var btnUseCurrent: TextView
    private lateinit var chipHome: com.google.android.material.chip.Chip
    private lateinit var chipOffice: com.google.android.material.chip.Chip
    private lateinit var chipOther: com.google.android.material.chip.Chip

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) fillFromCurrentLocation()
            else Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_address_edit)

        sessionManager = SessionManager(this)
        editingId = intent.getStringExtra("address_id")

        findViewById<TextView?>(R.id.btnBackEdit)?.setOnClickListener { finish() }

        etName = findViewById(R.id.etName)
        etLine1 = findViewById(R.id.etLine1)
        etLine2 = findViewById(R.id.etLine2)
        etCity = findViewById(R.id.etCity)
        etPin = findViewById(R.id.etPin)
        tvSave = findViewById(R.id.btnSaveAddress)
        btnUseCurrent = findViewById(R.id.btnUseCurrentLocation)
        chipHome = findViewById(R.id.chipHome)
        chipOffice = findViewById(R.id.chipOffice)
        chipOther = findViewById(R.id.chipOther)

        if (editingId.isNullOrBlank()) {
            chipHome.isChecked = true
        }

        editingId?.let { id ->
            loadAddresses().firstOrNull { it.id == id }?.let { addr ->
                when (addr.label.lowercase(Locale.getDefault())) {
                    "home" -> chipHome.isChecked = true
                    "office" -> chipOffice.isChecked = true
                    else -> chipOther.isChecked = true
                }
                etName.setText(addr.name)
                etLine1.setText(addr.line1)
                etLine2.setText(addr.line2)
                etCity.setText(addr.city)
                etPin.setText(addr.pin)
            }
        }

        tvSave.setOnClickListener { saveAddress() }
        btnUseCurrent.setOnClickListener { requestLocation() }
    }

    private fun saveAddress() {
        val rawLabel = selectedLabel()
        val name = etName.text.toString().trim()
        val line1 = etLine1.text.toString().trim()
        val line2 = etLine2.text.toString().trim()
        val city = etCity.text.toString().trim()
        val pin = etPin.text.toString().trim()

        if (rawLabel.isEmpty() || name.isEmpty() || line1.isEmpty() || city.isEmpty() || pin.isEmpty()) {
            if (rawLabel.isEmpty()) {
                Toast.makeText(this, "Select a label", Toast.LENGTH_SHORT).show()
            }
            if (name.isEmpty()) etName.error = "Required"
            if (line1.isEmpty()) etLine1.error = "Required"
            if (city.isEmpty()) etCity.error = "Required"
            if (pin.isEmpty()) etPin.error = "Required"
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!name.matches(Regex("[a-zA-Z ]+"))) {
            etName.error = "Only letters allowed"
            etName.requestFocus()
            return
        }

        if (!city.matches(Regex("[a-zA-Z ]+"))) {
            etCity.error = "Only letters allowed"
            etCity.requestFocus()
            return
        }

        if (line1.length < 4) {
            etLine1.error = "Enter a valid address"
            etLine1.requestFocus()
            return
        }

        if (!pin.matches(Regex("\\d{6}"))) {
            etPin.error = "Enter 6-digit pincode"
            etPin.requestFocus()
            return
        }

        val userId = sessionManager.getUserId()
        if (userId <= 0) {
            Toast.makeText(this, "Please log in to save address", Toast.LENGTH_SHORT).show()
            return
        }

        val list = loadAddresses().toMutableList()
        val existing = editingId?.let { id -> list.firstOrNull { it.id == id } }
        val isDefault = existing?.isDefault ?: list.isEmpty()
        val addrText = buildAddressText(line1, line2)
        val label = normalizeLabel(rawLabel)

        lifecycleScope.launch {
            try {
                val payload = mapOf(
                    "user_id" to userId,
                    "label" to label,
                    "full_name" to name,
                    "phone" to "",
                    "address" to addrText,
                    "city" to city,
                    "pincode" to pin,
                    "is_default" to if (isDefault) 1 else 0
                )
                val resp = withContext(Dispatchers.IO) {
                    if (editingId != null) {
                        val id = editingId?.toIntOrNull() ?: return@withContext null
                        ApiClient.apiService.updateAddress(payload + mapOf("id" to id))
                    } else {
                        ApiClient.apiService.addAddress(payload)
                    }
                }
                if (resp?.success == true) {
                    finish()
                } else {
                    Toast.makeText(this@DeliveryAddressEditActivity, resp?.message ?: "Unable to save address", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@DeliveryAddressEditActivity, "Unable to save address", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestLocation() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) fillFromCurrentLocation() else requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun fillFromCurrentLocation() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val last =
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (last != null) {
            applyLocation(last)
            return
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                applyLocation(location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        try {
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper())
        } catch (_: Exception) {
            try {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper())
            } catch (_: Exception) {
                Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyLocation(location: Location) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val list = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val first = list?.firstOrNull()
            if (first != null) {
                if (etLine1.text.isNullOrBlank()) etLine1.setText(first.thoroughfare ?: first.featureName ?: "")
                if (etLine2.text.isNullOrBlank()) etLine2.setText(first.subLocality ?: "")
                if (etCity.text.isNullOrBlank()) etCity.setText(first.locality ?: first.adminArea ?: "")
                if (etPin.text.isNullOrBlank()) etPin.setText(first.postalCode ?: "")
                if (etLine1.text.isNullOrBlank()) {
                    etLine1.setText("${location.latitude}, ${location.longitude}")
                }
            } else {
                if (etLine1.text.isNullOrBlank()) {
                    etLine1.setText("${location.latitude}, ${location.longitude}")
                }
                Toast.makeText(this, "Address not found, filled coordinates", Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            if (etLine1.text.isNullOrBlank()) {
                etLine1.setText("${location.latitude}, ${location.longitude}")
            }
            Toast.makeText(this, "Unable to decode location, filled coordinates", Toast.LENGTH_SHORT).show()
        }
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

    private fun saveAddresses(list: List<UserAddress>) {
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

    private fun buildAddressText(line1: String, line2: String): String {
        return if (line2.isNotBlank()) {
            "$line1, $line2"
        } else {
            line1
        }
    }

    private fun normalizeLabel(label: String): String {
        val clean = label.trim().lowercase(Locale.getDefault())
        return when {
            clean.contains("home") -> "Home"
            clean.contains("office") -> "Office"
            else -> "Other"
        }
    }

    private fun selectedLabel(): String {
        return when {
            chipHome.isChecked -> "Home"
            chipOffice.isChecked -> "Office"
            chipOther.isChecked -> "Other"
            else -> ""
        }
    }
}
