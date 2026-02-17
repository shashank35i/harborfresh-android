package com.harborfresh.market.seller

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.harborfresh.market.R
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.common.SessionManager
import android.util.Log
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class SellerAddProductActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var etName: EditText
    private lateinit var etPrice: EditText
    private lateinit var etWeight: EditText
    private lateinit var etDescription: EditText
    private lateinit var tvLocation: TextView
    private lateinit var tvPhotoStatus: TextView
    private lateinit var tvPhotoHint: TextView
    private lateinit var photoPlaceholder: View
    private lateinit var ivPhotoPreview: ImageView
    private lateinit var btnRemovePhoto: ImageButton
    private lateinit var cardPhoto: FrameLayout
    private lateinit var cardFresh: View
    private lateinit var cardChilled: View
    private lateinit var cardFrozen: View
    private var imageUri: Uri? = null

    private var selectedCategory: String = "Fish"
    private var selectedFreshness: String = "Fresh"

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            ivPhotoPreview.setImageURI(uri)
            updatePhotoState(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_add_product)

        sessionManager = SessionManager(this)
        etName = findViewById(R.id.etProductName)
        etPrice = findViewById(R.id.etPrice)
        etWeight = findViewById(R.id.etWeight)
        etDescription = findViewById(R.id.etDescription)
        tvLocation = findViewById(R.id.tvLocationValue)
        tvPhotoStatus = findViewById(R.id.tvPhotoStatus)
        tvPhotoHint = findViewById(R.id.tvPhotoHint)
        photoPlaceholder = findViewById(R.id.photoPlaceholder)
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview)
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto)
        cardPhoto = findViewById(R.id.cardPhoto)
        cardFresh = findViewById(R.id.cardFresh)
        cardChilled = findViewById(R.id.cardChilled)
        cardFrozen = findViewById(R.id.cardFrozen)

        val locName = sessionManager.getSellerLocation()
        tvLocation.text = locName ?: "Set location in dashboard"

        wireCategoryCards()
        wireFreshnessCards()

        cardPhoto.setOnClickListener {
            pickImage.launch("image/*")
        }
        btnRemovePhoto.setOnClickListener {
            imageUri = null
            ivPhotoPreview.setImageDrawable(null)
            updatePhotoState(false)
        }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnAddToInventory).setOnClickListener { submitProduct() }

        updatePhotoState(false)
    }

    private fun wireCategoryCards() {
        val categoryMap = mapOf(
            R.id.cardCatFish to "Fish",
            R.id.cardCatPrawns to "Prawns",
            R.id.cardCatCrabs to "Crabs",
            R.id.cardCatLobster to "Lobster",
            R.id.cardCatShellfish to "Shellfish",
            R.id.cardCatOthers to "Others"
        )
        categoryMap.keys.forEach { id ->
            findViewById<View>(id).setOnClickListener {
                selectedCategory = categoryMap[id] ?: "Fish"
                updateSelection(categoryMap.keys, id)
            }
        }
        updateSelection(categoryMap.keys, R.id.cardCatFish)
    }

    private fun wireFreshnessCards() {
        val map = mapOf(
            R.id.cardFresh to "Fresh",
            R.id.cardChilled to "Chilled",
            R.id.cardFrozen to "Frozen"
        )
        map.keys.forEach { id ->
            findViewById<View>(id).setOnClickListener {
                selectedFreshness = map[id] ?: "Fresh"
                updateSelection(map.keys, id)
            }
        }
        updateSelection(map.keys, R.id.cardFresh)
    }

    private fun updatePhotoState(hasPhoto: Boolean) {
        if (hasPhoto) {
            photoPlaceholder.visibility = View.GONE
            ivPhotoPreview.visibility = View.VISIBLE
            btnRemovePhoto.visibility = View.VISIBLE
            tvPhotoStatus.text = "Photo selected"
            tvPhotoStatus.setTextColor(0xFF22C55E.toInt())
            tvPhotoHint.text = "Tap to change"
            cardPhoto.setBackgroundResource(R.drawable.bg_scan_verified)
        } else {
            photoPlaceholder.visibility = View.VISIBLE
            ivPhotoPreview.visibility = View.GONE
            btnRemovePhoto.visibility = View.GONE
            tvPhotoStatus.text = "Tap to add a photo"
            tvPhotoStatus.setTextColor(0xFF6B7280.toInt())
            tvPhotoHint.text = "Supported: JPG, PNG"
            cardPhoto.setBackgroundResource(R.drawable.bg_input_outline)
        }
    }

    private fun updateSelection(ids: Set<Int>, selectedId: Int) {
        ids.forEach { id ->
            val card = findViewById<View>(id)
            val isSel = id == selectedId
            card.setBackgroundResource(if (isSel) R.drawable.bg_category_selected else R.drawable.bg_category_unselected)
        }
    }

    private fun submitProduct() {
        val sellerId = sessionManager.getSellerId()
        if (sellerId == 0) {
            Log.w("SellerAddProduct", "Missing seller session")
            Toast.makeText(this, "Seller session missing", Toast.LENGTH_SHORT).show()
            return
        }
        val name = etName.text.toString().trim()
        val price = etPrice.text.toString().trim()
        val qty = etWeight.text.toString().trim()
        val desc = etDescription.text.toString().trim()
        val locName = sessionManager.getSellerLocation()
        val lat = sessionManager.getSellerLat()
        val lng = sessionManager.getSellerLng()

        if (name.isEmpty() || price.isEmpty() || qty.isEmpty() || desc.isEmpty()) {
            Log.w("SellerAddProduct", "Validation failed name:$name price:$price qty:$qty desc:$desc")
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (imageUri == null) {
            Log.w("SellerAddProduct", "No image selected")
            Toast.makeText(this, "Add a product photo", Toast.LENGTH_SHORT).show()
            return
        }

        findViewById<View>(R.id.btnAddToInventory).isEnabled = false

        lifecycleScope.launch {
            try {
                Log.d("SellerAddProduct", "Preparing upload sellerId=$sellerId name=$name price=$price qty=$qty category=$selectedCategory fresh=$selectedFreshness loc=$locName lat=$lat lng=$lng uri=$imageUri")
                val imagePart = imageUri?.let { uri ->
                    val tempFile = File.createTempFile("upload", ".jpg", cacheDir)
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                    }
                    val body = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("image", tempFile.name, body)
                }

                val resp = ApiClient.apiService.uploadSellerProduct(
                    sellerId = sellerId.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                    productName = name.toRequestBody("text/plain".toMediaTypeOrNull()),
                    quantity = qty.toRequestBody("text/plain".toMediaTypeOrNull()),
                    price = price.toRequestBody("text/plain".toMediaTypeOrNull()),
                    category = selectedCategory.toRequestBody("text/plain".toMediaTypeOrNull()),
                    freshness = selectedFreshness.toRequestBody("text/plain".toMediaTypeOrNull()),
                    locationName = (locName ?: "").toRequestBody("text/plain".toMediaTypeOrNull()),
                    latitude = (lat?.toString() ?: "").toRequestBody("text/plain".toMediaTypeOrNull()),
                    longitude = (lng?.toString() ?: "").toRequestBody("text/plain".toMediaTypeOrNull()),
                    image = imagePart
                )
                Log.d("SellerAddProduct", "Upload response success=${resp.success} message=${resp.message}")
                if (resp.success) {
                    Toast.makeText(this@SellerAddProductActivity, "Product added", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@SellerAddProductActivity, resp.message ?: "Unable to add product", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("SellerAddProduct", "Upload error", e)
                Toast.makeText(this@SellerAddProductActivity, "Error adding product", Toast.LENGTH_LONG).show()
            } finally {
                findViewById<View>(R.id.btnAddToInventory).isEnabled = true
            }
        }
    }
}
