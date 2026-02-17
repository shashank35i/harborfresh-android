package com.harborfresh.market.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.model.Product
import com.harborfresh.market.model.CategoryItem
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class ProductViewModel : ViewModel() {

    // LiveData for UI
    val products: LiveData<List<Product>> get() = _products
    private val _products = MutableLiveData<List<Product>>()

    val categories: LiveData<List<CategoryItem>> get() = _categories
    private val _categories = MutableLiveData<List<CategoryItem>>()

    val popularProducts: LiveData<List<Product>> get() = _popularProducts
    private val _popularProducts = MutableLiveData<List<Product>>()

    val isLoading: LiveData<Boolean> get() = _isLoading
    private val _isLoading = MutableLiveData<Boolean>()

    val error: LiveData<String> get() = _error
    private val _error = MutableLiveData<String>()

    // --- Robust Loading State Management ---
    private val loadingCounter = AtomicInteger()

    private fun onLoadingStarted() {
        if (loadingCounter.getAndIncrement() == 0) {
            _isLoading.postValue(true)
        }
    }

    private fun onLoadingFinished() {
        if (loadingCounter.decrementAndGet() == 0) {
            _isLoading.postValue(false)
        }
    }

    // --- API Calls ---
    fun loadCategories() {
        viewModelScope.launch {
            onLoadingStarted()
            try {
                val response = ApiClient.apiService.getCategories()
                if (response.success) {
                    _categories.value = response.categories
                } else {
                    _error.value = "Failed to load categories"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                onLoadingFinished()
            }
        }
    }

    fun loadPopularProducts() {
        viewModelScope.launch {
            onLoadingStarted()
            try {
                val response = ApiClient.apiService.getPopularProducts()
                if (response.success) {
                    _popularProducts.value = response.products
                } else {
                    _error.value = "Failed to load popular products"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                onLoadingFinished()
            }
        }
    }

    fun loadProductsByCategory(category: String) {
        viewModelScope.launch {
            onLoadingStarted()
            try {
                val response = ApiClient.apiService.getProductsByCategory(category)
                if (response.success) {
                    _products.value = response.products
                } else {
                    _products.value = emptyList()
                    _error.value = "No products found in this category"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                onLoadingFinished()
            }
        }
    }
}
