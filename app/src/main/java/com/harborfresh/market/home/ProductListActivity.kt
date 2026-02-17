package com.harborfresh.market.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.harborfresh.market.databinding.ActivityProductListBinding
import com.harborfresh.market.viewmodel.ProductViewModel

class ProductListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductListBinding
    private lateinit var viewModel: ProductViewModel
    private lateinit var adapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val category = intent.getStringExtra("CATEGORY_NAME") ?: return
        title = category // Set the activity title

        // 1. Setup RecyclerView with an empty adapter
        adapter = ProductAdapter(emptyList())
        binding.rvProducts.layoutManager = GridLayoutManager(this, 2) // Using a grid layout
        binding.rvProducts.adapter = adapter

        // 2. Initialize ViewModel
        viewModel = ViewModelProvider(this)[ProductViewModel::class.java]

        // 3. Observe LiveData for product changes
        viewModel.products.observe(this) { products ->
            adapter.updateList(products)
        }

        // 4. Call the API to fetch products
        viewModel.loadProductsByCategory(category)
    }
}
