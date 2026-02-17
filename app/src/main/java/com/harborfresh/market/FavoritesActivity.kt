package com.harborfresh.market

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.harborfresh.market.common.FavoritesManager
import com.harborfresh.market.ui.products.FishProductsAdapter

class FavoritesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fish)

        val rv = findViewById<RecyclerView>(R.id.rvProducts)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvSubtitle = findViewById<TextView>(R.id.tvSubtitle)

        findViewById<ImageButton?>(R.id.btnBack)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val favorites = FavoritesManager.getFavorites(this)
        tvTitle.text = "Favorites"
        tvSubtitle.text = "${favorites.size} items"

        rv.layoutManager = GridLayoutManager(this, 2)
        if (favorites.isNotEmpty()) {
            rv.adapter = FishProductsAdapter(favorites) { product ->
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
            rv.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
        } else {
            rv.visibility = View.GONE
            tvEmpty.text = "No favorites yet"
            tvEmpty.visibility = View.VISIBLE
        }
    }
}
