package com.harborfresh.market

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.model.CategoryItem
import com.harborfresh.market.model.CategoryResponse
import com.harborfresh.market.ui.categories.CategoryListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoriesFragment : Fragment() {

    private var rv: RecyclerView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.activity_categories, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rv = view.findViewById(R.id.rvCategories)
        rv?.layoutManager = GridLayoutManager(requireContext(), 2)

        view.findViewById<View?>(R.id.ivBack)?.visibility = View.GONE

        fetchCategories()
    }

    private fun fetchCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp: CategoryResponse = withContext(Dispatchers.IO) { ApiClient.apiService.getCategories() }
                if (!isAdded) return@launch
                if (resp.success) {
                    val items = resp.categories
                    rv?.adapter = CategoryListAdapter(items) { item ->
                        openCategory(item.name)
                    }
                } else {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "No categories found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                if (isAdded) {
                    val msg = if (e is retrofit2.HttpException) {
                        val body = e.response()?.errorBody()?.string()
                        "Server error (${e.code()}): ${body ?: "Unknown"}"
                    } else {
                        "Failed to load categories"
                    }
                    android.util.Log.e("CategoriesFragment", "Load categories failed", e)
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openCategory(name: String) {
        val intent = Intent(requireContext(), FishActivity::class.java)
        intent.putExtra("category", name)
        startActivity(intent)
    }
}
