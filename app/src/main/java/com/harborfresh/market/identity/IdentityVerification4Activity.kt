package com.harborfresh.market.identity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.harborfresh.market.R
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.common.SessionManager
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class IdentityVerification4Activity : AppCompatActivity() {

    private var sellerId: Int = 0
    private lateinit var sessionManager: SessionManager
    private var uriLicense: Uri? = null
    private var uriId: Uri? = null
    private var uriAddress: Uri? = null
    private lateinit var tvLicense: TextView
    private lateinit var tvId: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var btnLicense: AppCompatButton
    private lateinit var btnId: AppCompatButton
    private lateinit var btnAddress: AppCompatButton
    private var uploadedLicense = false
    private var uploadedId = false
    private var uploadedAddress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_identity_verification4)

        sessionManager = SessionManager(this)
        sellerId = resolveSellerId(intent.getIntExtra("seller_id", 0))
        if (sellerId == 0) {
            Toast.makeText(this, "Missing seller session", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        tvLicense = findViewById(R.id.tvLicenseStatus)
        tvId = findViewById(R.id.tvIdStatus)
        tvAddress = findViewById(R.id.tvAddressStatus)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        btnLicense = findViewById(R.id.btnUploadLicense)
        btnId = findViewById(R.id.btnUploadId)
        btnAddress = findViewById(R.id.btnUploadAddress)

        // Default UI state
        setDocStatus(tvLicense, false)
        setDocStatus(tvId, false)
        setDocStatus(tvAddress, false)
        updateSubtitle()

        val pickLicense = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                uriLicense = it
                uploadedLicense = true
                setDocStatus(tvLicense, true, fileName(it) ?: "Selected")
                updateSubtitle()
            }
        }
        val pickId = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                uriId = it
                uploadedId = true
                setDocStatus(tvId, true, fileName(it) ?: "Selected")
                updateSubtitle()
            }
        }
        val pickAddress = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                uriAddress = it
                uploadedAddress = true
                setDocStatus(tvAddress, true, fileName(it) ?: "Selected")
                updateSubtitle()
            }
        }

        onClick(R.id.btnUploadLicense) { pickLicense.launch("*/*") }
        onClick(R.id.btnUploadId) { pickId.launch("*/*") }
        onClick(R.id.btnUploadAddress) { pickAddress.launch("*/*") }

        onClick(R.id.btnBack) { finish() }
        onClick(R.id.btnBackBottom) { finish() }
        onClick(R.id.btnContinue) {
            if (uriLicense == null || uriId == null || uriAddress == null) {
                Toast.makeText(this, "Please upload all documents", Toast.LENGTH_SHORT).show()
                return@onClick
            }

            lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        ApiClient.apiService.uploadSellerDocuments(
                            sellerId,
                            toPart("fishing_license_doc", uriLicense!!),
                            toPart("government_id_doc", uriId!!),
                            toPart("address_proof_doc", uriAddress!!)
                        )
                    }
                    if (response.success) {
                        sessionManager.saveSellerSession(sellerId, response.status, response.verification_step)
                        uploadedLicense = true
                        uploadedId = true
                        uploadedAddress = true
                        setDocStatus(tvLicense, true)
                        setDocStatus(tvId, true)
                        setDocStatus(tvAddress, true)
                        updateSubtitle()
                        startActivity(Intent(this@IdentityVerification4Activity, IdentityVerification5Activity::class.java).putExtra("seller_id", sellerId))
                    } else {
                        Toast.makeText(this@IdentityVerification4Activity, response.message ?: "Upload failed", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@IdentityVerification4Activity, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { ApiClient.apiService.getSellerStatus(sellerId) }
                resp.data?.let { data ->
                    sessionManager.saveSellerSession(data.seller.id, data.seller.status, data.seller.verification_step)
                    // Merge server state with any locally selected files so UI does not revert to "Not uploaded"
                    uploadedLicense = data.documents.fishing_license || uriLicense != null || uploadedLicense
                    uploadedId = data.documents.government_id || uriId != null || uploadedId
                    uploadedAddress = data.documents.address_proof || uriAddress != null || uploadedAddress
                    setDocStatus(tvLicense, uploadedLicense)
                    setDocStatus(tvId, uploadedId)
                    setDocStatus(tvAddress, uploadedAddress)
                    updateSubtitle()
                }
            } catch (_: Exception) {
                uploadedLicense = false
                uploadedId = false
                uploadedAddress = false
                setDocStatus(tvLicense, false)
                setDocStatus(tvId, false)
                setDocStatus(tvAddress, false)
                updateSubtitle()
            }
        }
    }

    private fun fileName(uri: Uri): String? {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIdx >= 0 && cursor.moveToFirst()) cursor.getString(nameIdx) else null
        }
    }

    private fun toPart(field: String, uri: Uri): MultipartBody.Part {
        val input = contentResolver.openInputStream(uri) ?: error("Cannot open uri")
        val ext = (contentResolver.getType(uri)?.substringAfterLast('/') ?: "bin")
        val tempFile = File.createTempFile("upload_${field}_", ".${ext}", cacheDir)
        FileOutputStream(tempFile).use { out -> input.copyTo(out) }
        val mediaType = contentResolver.getType(uri)?.toMediaTypeOrNull() ?: "application/octet-stream".toMediaTypeOrNull()
        val body = tempFile.asRequestBody(mediaType)
        return MultipartBody.Part.createFormData(field, tempFile.name, body)
    }

    private fun setDocStatus(tv: TextView, uploaded: Boolean, label: String? = null) {
        tv.text = label ?: if (uploaded) "Uploaded" else "Not uploaded"
        tv.setTextColor(if (uploaded) Color.parseColor("#22C55E") else Color.parseColor("#9CA3AF"))
        val button = when (tv.id) {
            R.id.tvLicenseStatus -> btnLicense
            R.id.tvIdStatus -> btnId
            R.id.tvAddressStatus -> btnAddress
            else -> null
        }
        val btnText = label ?: if (uploaded) "Uploaded" else "Choose file"
        button?.text = btnText
        button?.setTextColor(if (uploaded) Color.parseColor("#22C55E") else Color.parseColor("#0B2A43"))
    }

    private fun updateSubtitle() {
        val done = listOf(uploadedLicense || uriLicense != null, uploadedId || uriId != null, uploadedAddress || uriAddress != null).count { it }
        tvSubtitle.text = when (done) {
            3 -> "All documents ready to upload"
            2 -> "2/3 documents ready"
            1 -> "1/3 documents ready"
            else -> "Select all required documents"
        }
    }
}
