package com.harborfresh.market

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.common.CartManager
import com.harborfresh.market.common.FavoritesManager
import com.harborfresh.market.model.Product
import com.harborfresh.market.model.CartItem
import com.harborfresh.market.ui.cart.CartAdapter
import com.harborfresh.market.ui.products.SellerMoreAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddToCartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addtocart)

        val name = intent.getStringExtra("name") ?: ""
        val productId = intent.getIntExtra("id", -1)
        val price = intent.getStringExtra("price") ?: ""
        val freshness = intent.getStringExtra("freshness") ?: ""
        val rating = intent.getStringExtra("rating") ?: ""
        val desc = intent.getStringExtra("description") ?: ""
        val imageUrl = intent.getStringExtra("imageUrl") ?: ""
        val sellerId = intent.getIntExtra("seller_id", -1)
        var qty = 1
        var currentPrice = price.toDoubleOrNull() ?: 0.0
        var sellerMoreAdapter: SellerMoreAdapter? = null
        val rvSellerMore = findViewById<RecyclerView?>(R.id.rvSellerMore)
        val tvSellerMoreEmpty = findViewById<TextView?>(R.id.tvSellerMoreEmpty)
        val rvCartItems = findViewById<RecyclerView?>(R.id.rvCartItems)
        val tvCartEmpty = findViewById<TextView?>(R.id.tvCartEmpty)
        val cartItems = CartManager.getCart(this)
        val cartAdapter = CartAdapter(cartItems) { item ->
            CartManager.updateQty(this, item.id, item.name, item.qty)
            updateCartSection(cartItems, tvCartEmpty)
        }
        rvCartItems?.layoutManager = LinearLayoutManager(this)
        rvCartItems?.adapter = cartAdapter
        updateCartSection(cartItems, tvCartEmpty)

        findViewById<View?>(R.id.btnBackWrap)?.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        findViewById<TextView?>(R.id.tvName)?.text = name
        findViewById<TextView?>(R.id.tvPrice)?.text = "\u20B9${price}"
        findViewById<TextView?>(R.id.tvPerKg)?.text = "/kg"
        findViewById<TextView?>(R.id.tvAbout)?.text = desc.ifBlank { freshness.ifBlank { "Fresh catch, ready to cook." } }
        findViewById<TextView?>(R.id.tvRatingValue)?.text = if (rating.isNotBlank()) rating else "5.0"
        findViewById<TextView?>(R.id.tvSellerRating)?.text = if (rating.isNotBlank()) rating else "5.0"
        findViewById<TextView?>(R.id.tvReviewCount)?.text = "(${fakeReviews(productId)})"
        findViewById<TextView?>(R.id.tvTotalPrice)?.text = "\u20B9${price}"
        findViewById<TextView?>(R.id.tvTotalLabel)?.text = "Total price"
        findViewById<TextView?>(R.id.tvLocationValue)?.text = "Location not set"

        val tvQtyValue = findViewById<TextView?>(R.id.tvQtyValue)
        val tvTotalPrice = findViewById<TextView?>(R.id.tvTotalPrice)
        fun updateTotals() {
            tvQtyValue?.text = qty.toString()
            val total = currentPrice * qty
            tvTotalPrice?.text = "\u20B9" + "%.2f".format(total)
        }
        updateTotals()

        // Set freshness pill text if we can find the inner text view
        val chipFresh = findViewById<View?>(R.id.chipFresh)
        if (chipFresh is ViewGroup && chipFresh.childCount > 0) {
            val tv = chipFresh.getChildAt(0) as? TextView
            tv?.text = freshness.ifBlank { "Fresh" }
        }

        val ivHero = findViewById<ImageView?>(R.id.ivProductHero)
        val fullUrl = imageUrl.takeIf { it.isNotBlank() }?.let {
            if (it.startsWith("http", true)) it else ApiClient.BASE_URL + it
        }
        Glide.with(this)
            .load(fullUrl)
            .placeholder(R.drawable.bg_image_placeholder)
            .into(ivHero ?: return)

        findViewById<ImageView?>(R.id.btnPlus)?.setOnClickListener {
            qty += 1
            updateTotals()
        }
        findViewById<ImageView?>(R.id.btnMinus)?.setOnClickListener {
            if (qty > 1) {
                qty -= 1
                updateTotals()
            }
        }

        val ivFav = findViewById<ImageView?>(R.id.ivFav)
        var isFav = if (productId > 0) FavoritesManager.isFavorite(this, productId) else false
        updateFavIcon(ivFav, isFav)
        ivFav?.setOnClickListener {
            if (productId <= 0) return@setOnClickListener
            val product = Product(
                id = productId,
                name = name,
                price = price,
                freshness = freshness,
                description = desc,
                rating = rating,
                imageUrl = fullUrl,
                sellerId = if (sellerId > 0) sellerId else null
            )
            isFav = FavoritesManager.toggleFavorite(this, product)
            updateFavIcon(ivFav, isFav)
            val msg = if (isFav) "Added to favorites" else "Removed from favorites"
            android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView?>(R.id.ivShare)?.setOnClickListener {
            val text = "$name • \u20B9${price}/kg"
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(android.content.Intent.EXTRA_TEXT, text)
            startActivity(android.content.Intent.createChooser(intent, "Share product"))
        }

        findViewById<View?>(R.id.btnAddToCart)?.setOnClickListener {
            val priceDouble = currentPrice
            val item = CartItem(
                id = productId,
                name = name,
                price = priceDouble,
                imageUrl = fullUrl,
                qty = qty
            )
            CartManager.addItem(this, item)
            refreshCart(cartItems, cartAdapter, tvCartEmpty)
            sellerMoreAdapter?.notifyDataSetChanged()
            android.widget.Toast.makeText(this, "Added to cart", android.widget.Toast.LENGTH_SHORT).show()
            findViewById<TextView?>(R.id.btnAddToCart)?.text = "Added"
        }

        findViewById<View?>(R.id.btnProceedCheckout)?.setOnClickListener {
            startActivity(Intent(this, DeliverySlotActivity::class.java))
        }

        // If we have product id, fetch latest details (including seller info/rating) from seller products backend
        if (productId > 0) {
            lifecycleScope.launch {
                try {
                    val resp = withContext(Dispatchers.IO) { ApiClient.apiService.getSellerProductDetails(productId) }
                    resp.product?.let { p ->
                        findViewById<TextView?>(R.id.tvName)?.text = p.name
                        findViewById<TextView?>(R.id.tvPrice)?.text = "\u20B9${p.price}"
                        findViewById<TextView?>(R.id.tvRatingValue)?.text = p.rating ?: rating.ifBlank { "5.0" }
                        findViewById<TextView?>(R.id.tvSellerRating)?.text = p.rating ?: rating.ifBlank { "5.0" }
                        findViewById<TextView?>(R.id.tvReviewCount)?.text = "(${fakeReviews(p.id)})"
                        findViewById<TextView?>(R.id.tvLocationValue)?.text =
                            p.locationName?.takeIf { it.isNotBlank() } ?: "Location not set"
                        currentPrice = p.price.toDoubleOrNull() ?: currentPrice
                        updateTotals()
                    }
                    resp.seller?.let { s ->
                        findViewById<TextView?>(R.id.tvSellerName)?.text = s.name ?: "Seller"
                        findViewById<TextView?>(R.id.tvSellerRating)?.text = s.rating ?: "5.0"
                        val locationFallback = s.city?.takeIf { it.isNotBlank() } ?: "Location not set"
                        val tvLocation = findViewById<TextView?>(R.id.tvLocationValue)
                        if (tvLocation?.text.isNullOrBlank() || tvLocation?.text.toString() == "Location not set") {
                            tvLocation?.text = locationFallback
                        }
                        val sellerImage = s.image?.takeIf { it.isNotBlank() }?.let {
                            if (it.startsWith("http", true)) it else ApiClient.BASE_URL + it
                        }
                        val ivSeller = findViewById<ImageView?>(R.id.ivSeller)
                        Glide.with(this@AddToCartActivity)
                            .load(sellerImage)
                            .placeholder(R.drawable.bg_image_placeholder)
                            .into(ivSeller ?: return@let)
                        val aboutTv = findViewById<TextView?>(R.id.tvAbout)
                        val base = aboutTv?.text?.toString()?.trim().orEmpty()
                        val line = if (base.isNotEmpty()) "$base\nSeller: ${s.name ?: "N/A"}" else "Seller: ${s.name ?: "N/A"}"
                        aboutTv?.text = line
                    }
                    if (resp.otherProducts.isNotEmpty()) {
                        rvSellerMore?.layoutManager = LinearLayoutManager(this@AddToCartActivity, RecyclerView.HORIZONTAL, false)
                        sellerMoreAdapter = SellerMoreAdapter(resp.otherProducts) { product ->
                            val intent = android.content.Intent(this@AddToCartActivity, AddToCartActivity::class.java)
                            intent.putExtra("id", product.id)
                            intent.putExtra("name", product.name)
                            intent.putExtra("price", product.price)
                            intent.putExtra("freshness", product.freshness ?: "")
                            intent.putExtra("rating", product.rating ?: "")
                            intent.putExtra("imageUrl", product.imageUrl ?: "")
                            product.sellerId?.let { intent.putExtra("seller_id", it) }
                            startActivity(intent)
                        }
                        rvSellerMore?.adapter = sellerMoreAdapter
                        rvSellerMore?.visibility = View.VISIBLE
                        tvSellerMoreEmpty?.visibility = View.GONE
                    } else {
                        rvSellerMore?.visibility = View.GONE
                        tvSellerMoreEmpty?.visibility = View.VISIBLE
                    }
                } catch (_: Exception) {
                    // Ignore and keep intent data
                }
            }
        }
    }

    private fun fakeReviews(id: Int): Int {
        val safeId = if (id > 0) id else 1
        return 20 + (safeId * 37 % 180)
    }

    private fun refreshCart(
        items: MutableList<CartItem>,
        adapter: CartAdapter,
        tvEmpty: TextView?
    ) {
        val latest = CartManager.getCart(this)
        items.clear()
        items.addAll(latest)
        adapter.notifyDataSetChanged()
        updateCartSection(items, tvEmpty)
    }

    private fun updateCartSection(items: List<CartItem>, tvEmpty: TextView?) {
        val hasItems = items.isNotEmpty()
        tvEmpty?.visibility = if (hasItems) View.GONE else View.VISIBLE
    }

    private fun updateFavIcon(view: ImageView?, isFav: Boolean) {
        if (view == null) return
        if (isFav) {
            view.setImageResource(R.drawable.ic_heart)
            view.setColorFilter(0xFFEF4444.toInt())
        } else {
            view.setImageResource(android.R.drawable.btn_star_big_off)
            view.setColorFilter(0xFF111827.toInt())
        }
    }
}
