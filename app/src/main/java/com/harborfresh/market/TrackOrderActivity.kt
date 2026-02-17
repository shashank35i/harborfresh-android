package com.harborfresh.market

import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.model.TrackOrderResponse
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class TrackOrderActivity : AppCompatActivity() {

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val logTag = "TrackOrderActivity"
    private var mapView: MapView? = null
    private var googleMap: GoogleMap? = null
    private var pendingLatLng: LatLng? = null
    private var pendingLabel: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_order)

        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState?.getBundle("map_view"))
        mapView?.getMapAsync { map ->
            googleMap = map
            map.uiSettings.isMapToolbarEnabled = false
            pendingLatLng?.let { latLng ->
                updateMap(latLng, pendingLabel)
                pendingLatLng = null
                pendingLabel = null
            }
        }

        findViewById<View?>(R.id.btnBack)?.setOnClickListener { finish() }
        findViewById<View?>(R.id.btnNeedHelp)?.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        val orderId = intent.getIntExtra("order_id", 0)
        if (orderId <= 0) {
            Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadOrder(orderId)
    }

    private fun loadOrder(orderId: Int) {
        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { ApiClient.apiService.trackOrder(orderId) }
                if (resp.success) {
                    bindOrder(resp, orderId)
                } else {
                    Log.e(logTag, "trackOrder failed: ${resp.message}")
                    Toast.makeText(this@TrackOrderActivity, resp.message ?: "Unable to track order", Toast.LENGTH_LONG).show()
                }
            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                Log.e(logTag, "trackOrder http ${e.code()} ${e.message()} body=$body")
                Toast.makeText(this@TrackOrderActivity, "Unable to track order", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(logTag, "trackOrder exception", e)
                Toast.makeText(this@TrackOrderActivity, "Unable to track order", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun bindOrder(resp: TrackOrderResponse, orderId: Int) {
        findViewById<TextView?>(R.id.tvOrderId)?.text =
            resp.order_code?.takeIf { it.isNotBlank() } ?: "ORD-$orderId"

        val rawStatus = resp.status?.ifBlank { null } ?: "Order Placed"
        val status = if (rawStatus.equals("Ready", true)) "Ready" else rawStatus
        val statusView = findViewById<TextView?>(R.id.tvOnTime)
        statusView?.text = status
        statusView?.setTextColor(
            when {
                status.equals("Delivered", true) -> 0xFF22C55E.toInt()
                status.equals("Out for Delivery", true) || status.equals("Ready", true) -> 0xFF0B2A43.toInt()
                else -> 0xFF22C55E.toInt()
            }
        )

        bindMap(resp)
        bindPartner(resp)
        bindTimeline(resp)

        val map = resp.map
        val sellerLat = map?.seller_lat
        val sellerLng = map?.seller_lng
        val fallbackLat = sellerLat ?: map?.delivery_lat
        val fallbackLng = sellerLng ?: map?.delivery_lng
        findViewById<View?>(R.id.btnRight)?.setOnClickListener {
            if (fallbackLat != null && fallbackLng != null) {
                val uri = Uri.parse("geo:$fallbackLat,$fallbackLng?q=$fallbackLat,$fallbackLng")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } else if (!map?.seller_location.isNullOrBlank()) {
                val query = Uri.encode(map?.seller_location ?: "")
                val uri = Uri.parse("geo:0,0?q=$query")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } else if (!resp.seller?.city.isNullOrBlank()) {
                val query = Uri.encode(resp.seller?.city ?: "")
                val uri = Uri.parse("geo:0,0?q=$query")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindMap(resp: TrackOrderResponse) {
        val tvDistance = findViewById<TextView?>(R.id.tvDistance)
        val tvMapHint = findViewById<TextView?>(R.id.tvMapHint)
        val chipLive = findViewById<View?>(R.id.chipLive)
        val map = resp.map

        val sellerLat = map?.seller_lat
        val sellerLng = map?.seller_lng
        val deliveryLat = map?.delivery_lat
        val deliveryLng = map?.delivery_lng
        val customerLat = map?.customer_lat
        val customerLng = map?.customer_lng
        val sellerCity = resp.seller?.city
        val sellerLabel = resp.seller?.name?.ifBlank { null } ?: "Seller"
        val sellerLocation = map?.seller_location?.ifBlank { null }

        val primaryLat = sellerLat ?: deliveryLat
        val primaryLng = sellerLng ?: deliveryLng

        if (primaryLat != null && primaryLng != null && customerLat != null && customerLng != null) {
            val dist = haversine(customerLat, customerLng, primaryLat, primaryLng)
            tvDistance?.text = String.format(Locale.getDefault(), "%.1f km away", dist)
            tvMapHint?.text = sellerLocation ?: "Seller location"
            updateMap(LatLng(primaryLat, primaryLng), sellerLabel)
        } else if (primaryLat != null && primaryLng != null) {
            tvDistance?.text = "Seller location"
            tvMapHint?.text = sellerLocation ?: "Seller location"
            updateMap(LatLng(primaryLat, primaryLng), sellerLabel)
        } else if (!sellerCity.isNullOrBlank()) {
            tvDistance?.text = "Seller location"
            tvMapHint?.text = sellerCity
            resolveCityLatLng(sellerCity, sellerLabel)
        } else {
            tvDistance?.text = "Live location unavailable"
            tvMapHint?.text = "Location not available"
        }

        chipLive?.visibility = if (resp.is_live == true) View.VISIBLE else View.GONE
    }

    private fun bindPartner(resp: TrackOrderResponse) {
        val partner = resp.delivery_partner
        val name = partner?.name?.takeIf { it.isNotBlank() } ?: "Assigning partner"
        val phone = partner?.phone?.takeIf { it.isNotBlank() }

        findViewById<TextView?>(R.id.tvPartnerName)?.text = name
        findViewById<TextView?>(R.id.tvPartnerRole)?.text = phone ?: "Delivery Partner"
        findViewById<TextView?>(R.id.tvRating)?.text = partner?.rating?.takeIf { it.isNotBlank() } ?: "-"

        val callBtn = findViewById<View?>(R.id.btnCall)
        callBtn?.alpha = if (phone.isNullOrBlank()) 0.5f else 1f
        callBtn?.setOnClickListener {
            if (!phone.isNullOrBlank()) {
                val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                startActivity(dial)
            } else {
                Toast.makeText(this, "Phone not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bindTimeline(resp: TrackOrderResponse) {
        val timeline = resp.timeline
        val timelineMap = timeline.associateBy { status ->
            val key = status.status ?: ""
            if (key.equals("Ready", true)) "Out for Delivery" else key
        }

        val steps = listOf(
            "Order Placed" to Triple(R.id.tvS1Title, R.id.tvS1Sub, R.id.tvS1Time),
            "Preparing" to Triple(R.id.tvS2Title, R.id.tvS2Sub, R.id.tvS2Time),
            "Out for Delivery" to Triple(R.id.tvS3Title, R.id.tvS3Sub, R.id.tvS3Time),
            "Delivered" to Triple(R.id.tvS4Title, R.id.tvS4Sub, R.id.tvS4Sub)
        )

        steps.forEach { (label, ids) ->
            val entry = timelineMap[label]
            val title = findViewById<TextView?>(ids.first)
            val sub = findViewById<TextView?>(ids.second)
            val timeId = ids.third
            val timeView = if (timeId == R.id.tvS4Sub) null else findViewById<TextView?>(timeId)
            title?.text = label
            sub?.text = entry?.message ?: defaultMessage(label)
            timeView?.text = entry?.created_at?.let { formatTime(it) } ?: ""
        }

        val currentStatusRaw = resp.status?.takeIf { it.isNotBlank() } ?: timeline.lastOrNull()?.status ?: "Order Placed"
        val currentStatus = if (currentStatusRaw.equals("Ready", true)) "Out for Delivery" else currentStatusRaw
        val statusOrder = listOf("Order Placed", "Preparing", "Out for Delivery", "Delivered")
        val currentIndex = statusOrder.indexOfFirst { it.equals(currentStatus, true) }.coerceAtLeast(0)

        updateDot(findViewById(R.id.dot1), findViewById(R.id.dot1Check), currentIndex >= 0, currentIndex == 0)
        updateDot(findViewById(R.id.dot2), findViewById(R.id.dot2Check), currentIndex >= 1, currentIndex == 1)
        updateDot(findViewById(R.id.dot3), null, currentIndex >= 2, currentIndex == 2)
        updateDot(findViewById(R.id.dot4), null, currentIndex >= 3, currentIndex == 3)
    }

    private fun updateDot(card: MaterialCardView?, check: ImageView?, done: Boolean, current: Boolean) {
        if (card == null) return
        val doneColor = 0xFF22C55E.toInt()
        val currentColor = 0xFF0B2A43.toInt()
        val pendingColor = 0xFFE5E7EB.toInt()

        when {
            done -> {
                card.setCardBackgroundColor(doneColor)
                check?.visibility = View.VISIBLE
            }
            current -> {
                card.setCardBackgroundColor(currentColor)
                check?.visibility = View.GONE
            }
            else -> {
                card.setCardBackgroundColor(pendingColor)
                check?.visibility = View.GONE
            }
        }
    }

    private fun defaultMessage(status: String): String {
        return when (status) {
            "Order Placed" -> "Your order has been confirmed"
            "Preparing" -> "Fresh catch being cleaned and packed"
            "Out for Delivery" -> "On the way to your location"
            "Delivered" -> "Enjoy your dock-fresh order"
            else -> ""
        }
    }

    private fun formatTime(createdAt: String): String {
        return try {
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dt = df.parse(createdAt)
            if (dt != null) timeFormat.format(dt) else ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun resolveCityLatLng(city: String, label: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@TrackOrderActivity, Locale.getDefault())
                val results = geocoder.getFromLocationName(city, 1)
                val first = results?.firstOrNull()
                if (first != null) {
                    val latLng = LatLng(first.latitude, first.longitude)
                    withContext(Dispatchers.Main) {
                        updateMap(latLng, label)
                    }
                }
            } catch (e: Exception) {
                Log.e(logTag, "Geocode failed for seller city", e)
            }
        }
    }

    private fun updateMap(latLng: LatLng, label: String?) {
        val map = googleMap
        if (map == null) {
            pendingLatLng = latLng
            pendingLabel = label
            return
        }
        map.clear()
        map.addMarker(MarkerOptions().position(latLng).title(label ?: "Seller"))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onDestroy() {
        mapView?.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val mapBundle = outState.getBundle("map_view") ?: Bundle().also {
            outState.putBundle("map_view", it)
        }
        mapView?.onSaveInstanceState(mapBundle)
    }
}
