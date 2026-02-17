package com.harborfresh.market.identity

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.harborfresh.market.R
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.common.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import android.widget.ImageView

class IdentityBigVerification1Activity : AppCompatActivity() {
    private var sellerId: Int = 0
    private lateinit var sessionManager: SessionManager
    private lateinit var tvAadhaarNumber: EditText
    private lateinit var tvName: EditText
    private lateinit var tvHeaderSub: TextView
    private var tvStatus: TextView? = null
    private var tvFrontLabel: TextView? = null
    private var tvBackLabel: TextView? = null
    private var frontIcon: ImageView? = null
    private var backIcon: ImageView? = null
    private var frontBox: android.widget.FrameLayout? = null
    private var backBox: android.widget.FrameLayout? = null
    private var aadhaarNumber: String = ""
    private var aadhaarName: String = ""
    private var frontUri: Uri? = null
    private var backUri: Uri? = null
    private var frontSelected: Boolean = false
    private var backSelected: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_identitybig_verification1)

        sessionManager = SessionManager(this)
        sellerId = resolveSellerId(intent.getIntExtra("seller_id", 0))
        if (sellerId == 0) {
            Toast.makeText(this, "Missing seller session", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        tvAadhaarNumber = findViewById(R.id.tvAadhaarNumber)
        tvName = findViewById(R.id.tvName)
        tvHeaderSub = findViewById(R.id.tvHeaderSub)
        tvStatus = findViewById(R.id.tvDocStatus)
        tvFrontLabel = findViewById(R.id.tvFrontLabel)
        tvBackLabel = findViewById(R.id.tvBackLabel)
        frontIcon = findViewById(R.id.frontIcon)
        backIcon = findViewById(R.id.backIcon)
        frontBox = findViewById(R.id.scanBoxFront)
        backBox = findViewById(R.id.scanBoxBack)

        val pickFront = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                frontUri = it
                frontSelected = true
                Toast.makeText(this, "Aadhaar front selected", Toast.LENGTH_SHORT).show()
                setStatus("Aadhaar front selected • pending upload", false, frontSelected = true, backSelected = backSelected)
            }
        }

        val pickBack = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                backUri = it
                backSelected = true
                Toast.makeText(this, "Aadhaar back selected", Toast.LENGTH_SHORT).show()
                setStatus("Aadhaar back selected • pending upload", false, frontSelected = frontSelected, backSelected = true)
            }
        }

        onClick(R.id.btnBack) { finish() }
        onClick(R.id.btnContinue) { submitAadhaar() }
        onClick(R.id.scanBoxFront) { pickFront.launch("*/*") }
        onClick(R.id.scanBoxBack) { pickBack.launch("*/*") }

        setStatus("Aadhaar not uploaded", false)
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
                    aadhaarNumber = data.identity.aadhaar_number ?: ""
                    aadhaarName = data.identity.aadhaar_name ?: ""
                    val uploaded = !data.identity.aadhaar_doc.isNullOrEmpty()
                    if (aadhaarNumber.isNotEmpty()) tvAadhaarNumber.setText(aadhaarNumber) else tvAadhaarNumber.text.clear()
                    if (aadhaarName.isNotEmpty()) tvName.setText(aadhaarName) else tvName.text.clear()
                    val statusLabel = when {
                        (data.identity.aadhaar_verified) == 1 -> "Aadhaar uploaded • verified"
                        uploaded -> "Aadhaar uploaded • awaiting review"
                        else -> "Aadhaar not uploaded"
                    }
                    if (uploaded || data.identity.aadhaar_verified == 1) {
                        frontSelected = true
                        backSelected = true
                    }
                    val anyFront = frontUri != null || frontSelected
                    val anyBack = backUri != null || backSelected
                    setStatus(
                        statusLabel,
                        uploaded || (data.identity.aadhaar_verified == 1),
                        frontSelected = anyFront,
                        backSelected = anyBack
                    )
                }
            } catch (_: Exception) { }
        }
    }

    private fun submitAadhaar() {
        aadhaarNumber = tvAadhaarNumber.text.toString().trim()
        aadhaarName = tvName.text.toString().trim()
        if (!aadhaarNumber.matches(Regex("^\\d{12}$"))) {
            Toast.makeText(this, "Enter a valid 12-digit Aadhaar number", Toast.LENGTH_SHORT).show()
            return
        }
        if (frontUri == null || backUri == null) {
            Toast.makeText(this, "Select Aadhaar front and back", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.uploadAadhaar(
                        sellerId,
                        aadhaarNumber.toRequestBody("text/plain".toMediaTypeOrNull()),
                        aadhaarName.toRequestBody("text/plain".toMediaTypeOrNull()),
                        toPart("aadhaar_doc", frontUri!!)
                    )
                }
                if (resp.success) {
                    sessionManager.saveSellerSession(sellerId, resp.status, resp.verification_step)
                    frontSelected = true
                    backSelected = true
                    setStatus("Aadhaar uploaded • pending verification", true)
                    startActivity(Intent(this@IdentityBigVerification1Activity, IdentityBigVerification2Activity::class.java).putExtra("seller_id", sellerId))
                } else {
                    Toast.makeText(this@IdentityBigVerification1Activity, resp.message ?: "Unable to upload", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@IdentityBigVerification1Activity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun toPart(field: String, uri: Uri): MultipartBody.Part {
        val input = contentResolver.openInputStream(uri) ?: error("Cannot open uri")
        val ext = (contentResolver.getType(uri)?.substringAfterLast('/') ?: "bin")
        val tempFile = File.createTempFile("upload_${field}_", ".${ext}", cacheDir)
        FileOutputStream(tempFile).use { out -> input.copyTo(out) }
        val mediaType = contentResolver.getType(uri)?.toMediaTypeOrNull()
        val body = tempFile.asRequestBody(mediaType)
        return MultipartBody.Part.createFormData(field, tempFile.name, body)
    }

    private fun setStatus(text: String, uploaded: Boolean, frontSelected: Boolean = false, backSelected: Boolean = false) {
        val pendingSelected = !uploaded && frontSelected && backSelected
        val displayText = when {
            uploaded -> text
            pendingSelected -> "Aadhaar selected • pending upload"
            else -> text
        }
        tvHeaderSub.text = displayText
        val color = when {
            uploaded -> Color.parseColor("#22C55E")
            pendingSelected -> Color.parseColor("#0B2A43")
            else -> Color.parseColor("#9CA3AF")
        }
        tvHeaderSub.setTextColor(color)
        tvStatus?.let {
            it.text = displayText
            it.setTextColor(color)
            it.setBackgroundResource(
                when {
                    uploaded -> R.drawable.bg_status_pill_green
                    pendingSelected -> R.drawable.bg_status_pill_gray
                    else -> R.drawable.bg_status_pill_gray
                }
            )
        }
        val frontDone = uploaded || frontSelected
        val backDone = uploaded || backSelected
        frontIcon?.setColorFilter(if (frontDone) Color.parseColor("#22C55E") else Color.parseColor("#9CA3AF"))
        backIcon?.setColorFilter(if (backDone) Color.parseColor("#22C55E") else Color.parseColor("#9CA3AF"))
        frontBox?.setBackgroundResource(if (frontDone) R.drawable.bg_scan_verified else R.drawable.bg_card_white_22)
        backBox?.setBackgroundResource(if (backDone) R.drawable.bg_scan_verified else R.drawable.bg_card_white_22)
        tvFrontLabel?.text = if (frontDone) "Aadhaar Front Uploaded" else "Tap to upload Aadhaar Front"
        tvBackLabel?.text = if (backDone) "Aadhaar Back Uploaded" else "Tap to upload Aadhaar Back"
        val frontColor = if (frontDone) Color.parseColor("#22C55E") else Color.parseColor("#6B7280")
        val backColor = if (backDone) Color.parseColor("#22C55E") else Color.parseColor("#6B7280")
        tvFrontLabel?.setTextColor(frontColor)
        tvBackLabel?.setTextColor(backColor)
    }
}
