package com.harborfresh.market

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.ui.products.FishProductsAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FishActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fish)

        rv = findViewById(R.id.rvProducts)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        rv.layoutManager = GridLayoutManager(this, 2)

        findViewById<ImageButton?>(R.id.btnBack)?.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val category = intent.getStringExtra("category")
        val productId = intent.getIntExtra("product_id", 0)
        val searchQuery = intent.getStringExtra("search")?.trim().orEmpty()
        if (productId > 0 && intent.hasExtra("name") && intent.hasExtra("price")) {
            // Redirect to product detail when launched from a product card.
            val product = com.harborfresh.market.model.Product(
                id = productId,
                name = intent.getStringExtra("name") ?: "",
                price = intent.getStringExtra("price") ?: "",
                freshness = intent.getStringExtra("freshness"),
                description = intent.getStringExtra("description"),
                rating = intent.getStringExtra("rating"),
                imageUrl = intent.getStringExtra("imageUrl"),
                sellerId = intent.getIntExtra("seller_id", 0)
            )
            openDetail(product)
            finish()
            return
        }

        val title = category ?: "Popular Today"
        tvTitle.text = if (searchQuery.isNotBlank()) "Results" else title
        tvSubtitle.text = if (searchQuery.isNotBlank()) "Searching for \"$searchQuery\"" else ""

        if (searchQuery.isNotBlank()) {
            loadSearch(searchQuery)
        } else if (category != null) {
            loadProducts(category)
        } else {
            loadPopular()
        }
    }

    private fun loadProducts(category: String) {
        lifecycleScope.launch {
            tvEmpty.visibility = View.GONE
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getProductsByCategory(category)
                }
                if (resp.success && resp.products.isNotEmpty()) {
                    tvSubtitle.text = "${resp.products.size} items"
                    rv.adapter = FishProductsAdapter(resp.products) { product ->
                        openDetail(product)
                    }
                    rv.visibility = View.VISIBLE
                    tvEmpty.visibility = View.GONE
                } else {
                    rv.visibility = View.GONE
                    tvSubtitle.text = "0 items"
                    tvEmpty.text = "No items found for $category"
                    tvEmpty.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                android.util.Log.e("FishActivity", "Load products failed for $category", e)
                rv.visibility = View.GONE
                val msg = if (e is retrofit2.HttpException) {
                    val body = e.response()?.errorBody()?.string()
                    "Server error (${e.code()}): ${body ?: "Unknown"}"
                } else {
                    "Unable to load items. Check connection."
                }
                tvEmpty.text = msg
                tvEmpty.visibility = View.VISIBLE
                Toast.makeText(this@FishActivity, "Failed to load products", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadPopular() {
        lifecycleScope.launch {
            tvEmpty.visibility = View.GONE
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getPopularProducts()
                }
                if (resp.success && resp.products.isNotEmpty()) {
                    tvSubtitle.text = "${resp.products.size} items"
                    rv.adapter = FishProductsAdapter(resp.products) { product ->
                        openDetail(product)
                    }
                    rv.visibility = View.VISIBLE
                    tvEmpty.visibility = View.GONE
                } else {
                    rv.visibility = View.GONE
                    tvSubtitle.text = "0 items"
                    tvEmpty.text = "No popular items found"
                    tvEmpty.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                android.util.Log.e("FishActivity", "Load popular failed", e)
                rv.visibility = View.GONE
                val msg = if (e is retrofit2.HttpException) {
                    val body = e.response()?.errorBody()?.string()
                    "Server error (${e.code()}): ${body ?: "Unknown"}"
                } else {
                    "Unable to load items. Check connection."
                }
                tvEmpty.text = msg
                tvEmpty.visibility = View.VISIBLE
                Toast.makeText(this@FishActivity, "Failed to load products", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSearch(query: String) {
        lifecycleScope.launch {
            tvEmpty.visibility = View.GONE
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.apiService.searchProducts(query)
                }
                if (resp.success && resp.products.isNotEmpty()) {
                    tvSubtitle.text = "${resp.products.size} items"
                    rv.adapter = FishProductsAdapter(resp.products) { product ->
                        openDetail(product)
                    }
                    rv.visibility = View.VISIBLE
                    tvEmpty.visibility = View.GONE
                } else {
                    rv.visibility = View.GONE
                    tvSubtitle.text = "0 items"
                    tvEmpty.text = "No items found for \"$query\""
                    tvEmpty.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                android.util.Log.e("FishActivity", "Search failed for $query", e)
                rv.visibility = View.GONE
                val msg = if (e is retrofit2.HttpException) {
                    val body = e.response()?.errorBody()?.string()
                    "Server error (${e.code()}): ${body ?: "Unknown"}"
                } else {
                    "Unable to load items. Check connection."
                }
                tvEmpty.text = msg
                tvEmpty.visibility = View.VISIBLE
                Toast.makeText(this@FishActivity, "Failed to load products", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openDetail(product: com.harborfresh.market.model.Product) {
        val intent = android.content.Intent(this, AddToCartActivity::class.java)
        intent.putExtra("name", product.name)
        intent.putExtra("price", product.price)
        intent.putExtra("freshness", product.freshness ?: "")
        intent.putExtra("description", product.description ?: "")
        intent.putExtra("rating", product.rating ?: "")
        intent.putExtra("imageUrl", product.imageUrl ?: "")
        intent.putExtra("id", product.id)
        product.sellerId?.let { intent.putExtra("seller_id", it) }
        startActivity(intent)
    }
}
