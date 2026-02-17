package com.harborfresh.market.identity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Bitmap
import androidx.lifecycle.lifecycleScope
import com.harborfresh.market.R
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.common.SessionManager
import android.graphics.Color
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class IdentityBigVerification2Activity : AppCompatActivity() {
    private var sellerId: Int = 0
    private lateinit var sessionManager: SessionManager
    private var selfieUri: Uri? = null
    private lateinit var tvHeaderSub: TextView
    private var tvLiveStatus: TextView? = null
    private var tvLiveTitle: TextView? = null
    private var tvLiveSubtitle: TextView? = null
    private var imgLiveIcon: ImageView? = null
    private var liveBox: FrameLayout? = null
    private var selfieUploaded: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_identitybig_verification2)

        sessionManager = SessionManager(this)
        sellerId = resolveSellerId(intent.getIntExtra("seller_id", 0))
        if (sellerId == 0) {
            Toast.makeText(this, "Missing seller session", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        tvHeaderSub = findViewById(R.id.tvHeaderSub)
        tvLiveStatus = findViewById(R.id.tvLiveStatus)
        tvLiveTitle = findViewById(R.id.tvLiveTitle)
        tvLiveSubtitle = findViewById(R.id.tvLiveSubtitle)
        imgLiveIcon = findViewById(R.id.imgLiveIcon)
        liveBox = findViewById(R.id.liveBox)
        val takeSelfie = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? ->
            bmp?.let {
                selfieUri = saveBitmap(it)
                if (selfieUri != null) {
                    Toast.makeText(this, "Selfie captured", Toast.LENGTH_SHORT).show()
                    setStatus("Selfie captured - pending upload", false)
                    uploadSelfie()
                }
            } ?: Toast.makeText(this, "Camera capture failed, try again", Toast.LENGTH_SHORT).show()
        }

        onClick(R.id.btnBack) { finish() }
        onClick(R.id.btnContinue) {
            when {
                selfieUploaded -> startActivity(Intent(this@IdentityBigVerification2Activity, IdentityBigVerification3Activity::class.java).putExtra("seller_id", sellerId))
                selfieUri != null -> uploadSelfie()
                else -> takeSelfie.launch(null)
            }
        }

        val openSelfiePicker = { takeSelfie.launch(null) }
        onClick(R.id.stepsCard) { openSelfiePicker() }
        liveBox?.setOnClickListener { openSelfiePicker() }
        imgLiveIcon?.setOnClickListener { openSelfiePicker() }
        tvLiveTitle?.setOnClickListener { openSelfiePicker() }
        tvLiveSubtitle?.setOnClickListener { openSelfiePicker() }
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
                    val aadhaarOk = data.identity.aadhaar_verified == 1 || !data.identity.aadhaar_doc.isNullOrEmpty()
                    selfieUploaded = !data.identity.selfie_image.isNullOrEmpty() || data.identity.liveness_verified == 1
                    setStatus(
                        when {
                            selfieUploaded -> "Selfie uploaded"
                            aadhaarOk -> "Aadhaar verified - capture live selfie"
                            else -> "Aadhaar not uploaded"
                        },
                        selfieUploaded
                    )
                    if (selfieUploaded) {
                        startActivity(Intent(this@IdentityBigVerification2Activity, IdentityBigVerification3Activity::class.java).putExtra("seller_id", sellerId))
                        finish()
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun uploadSelfie() {
        val uri = selfieUri ?: return
        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.uploadSelfie(
                        sellerId,
                        toPart("selfie_image", uri)
                    )
                }
                if (resp.success) {
                    selfieUploaded = true
                    sessionManager.saveSellerSession(sellerId, resp.status, resp.verification_step)
                    setStatus("Selfie uploaded", true)
                    startActivity(Intent(this@IdentityBigVerification2Activity, IdentityBigVerification3Activity::class.java).putExtra("seller_id", sellerId))
                } else {
                    Toast.makeText(this@IdentityBigVerification2Activity, resp.message ?: "Upload failed", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@IdentityBigVerification2Activity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun toPart(field: String, uri: Uri): MultipartBody.Part {
        val input = contentResolver.openInputStream(uri) ?: error("Cannot open uri")
        val ext = (contentResolver.getType(uri)?.substringAfterLast('/') ?: "jpg")
        val tempFile = File.createTempFile("upload_${field}_", ".${ext}", cacheDir)
        FileOutputStream(tempFile).use { out -> input.copyTo(out) }
        val mediaType = contentResolver.getType(uri)?.toMediaTypeOrNull()
        val body = tempFile.asRequestBody(mediaType)
        return MultipartBody.Part.createFormData(field, tempFile.name, body)
    }

    private fun saveBitmap(bmp: Bitmap): Uri? {
        return try {
            val file = File.createTempFile("selfie_", ".jpg", cacheDir)
            FileOutputStream(file).use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Uri.fromFile(file)
        } catch (_: Exception) {
            null
        }
    }

    private fun setStatus(text: String, uploaded: Boolean) {
        tvHeaderSub.text = text
        tvHeaderSub.setTextColor(if (uploaded) Color.parseColor("#22C55E") else Color.parseColor("#9CA3AF"))
        tvLiveStatus?.let {
            it.text = text
            it.setTextColor(if (uploaded) Color.parseColor("#22C55E") else Color.parseColor("#9CA3AF"))
            it.setBackgroundResource(if (uploaded) R.drawable.bg_status_pill_green else R.drawable.bg_status_pill_gray)
        }
        if (uploaded) {
            liveBox?.setBackgroundResource(R.drawable.bg_live_box)
            imgLiveIcon?.setImageResource(R.drawable.ic_check)
            imgLiveIcon?.setColorFilter(Color.parseColor("#22C55E"))
            tvLiveTitle?.text = "Selfie Verified"
            tvLiveTitle?.setTextColor(Color.parseColor("#22C55E"))
            tvLiveSubtitle?.text = "Liveness check passed"
            tvLiveSubtitle?.setTextColor(Color.parseColor("#22C55E"))
        } else {
            liveBox?.setBackgroundResource(R.drawable.bg_card_white_22)
            imgLiveIcon?.setImageResource(R.drawable.ic_camera)
            imgLiveIcon?.setColorFilter(Color.parseColor("#9CA3AF"))
            tvLiveTitle?.text = "Selfie Pending"
            tvLiveTitle?.setTextColor(Color.parseColor("#6B7280"))
            tvLiveSubtitle?.text = "Tap to capture live selfie"
            tvLiveSubtitle?.setTextColor(Color.parseColor("#9CA3AF"))
        }
    }
}
