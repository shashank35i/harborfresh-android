package com.harborfresh.market

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.common.SessionManager
import com.harborfresh.market.model.HomeResponse
import com.harborfresh.market.model.Product
import com.harborfresh.market.ui.home.ProductHomeAdapter
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private var tvLocation: TextView? = null
    private var tvFreshSubtitle: TextView? = null
    private var rvPopular: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        tvLocation = view.findViewById(R.id.tvLocation)
        val savedLoc = sessionManager.getUserLocation()
        tvLocation?.text = savedLoc ?: "Not set"
        tvFreshSubtitle = findTextViewWithExactText(view, "Caught at 4:30 AM â€¢ 12 varieties")
        rvPopular = view.findViewById(R.id.rvPopular)

        rvPopular?.layoutManager = GridLayoutManager(requireContext(), 2)

        fun openCategories() {
            val navCategories = activity?.findViewById<View?>(R.id.navCategories)
            if (navCategories != null) {
                navCategories.performClick()
            } else {
                val intent = Intent(requireContext(), HomeActivity::class.java)
                intent.putExtra("open_tab", "categories")
                startActivity(intent)
            }
        }
        view.findViewById<View?>(R.id.tvCategories)?.setOnClickListener { openCategories() }
        view.findViewById<View?>(R.id.btnSeeAllCategories)?.setOnClickListener { openCategories() }
        view.findViewById<View?>(R.id.btnSeeAllPopular)?.setOnClickListener {
            startActivity(Intent(requireContext(), FishActivity::class.java))
        }
        view.findViewById<View?>(R.id.btnViewPlans)?.setOnClickListener {
            startActivity(Intent(requireContext(), SubscriptionActivity::class.java))
        }

        val etSearch = view.findViewById<TextView?>(R.id.etSearch)
        val btnSearch = view.findViewById<View?>(R.id.btnSearch)
        fun doSearch() {
            val query = etSearch?.text?.toString()?.trim().orEmpty()
            if (query.isBlank()) {
                Toast.makeText(requireContext(), "Enter a search term", Toast.LENGTH_SHORT).show()
                return
            }
            val intent = Intent(requireContext(), FishActivity::class.java)
            intent.putExtra("search", query)
            startActivity(intent)
        }
        btnSearch?.setOnClickListener { doSearch() }
        etSearch?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch()
                true
            } else {
                false
            }
        }

        bindTileByTitle(view, "Fish") {
            openCategory("Fish")
        }

        view.findViewById<View?>(R.id.cardCatFish)?.setOnClickListener { openCategory("Fish") }
        view.findViewById<View?>(R.id.cardCatPrawns)?.setOnClickListener { openCategory("Prawns") }
        view.findViewById<View?>(R.id.cardCatCrab)?.setOnClickListener { openCategory("Crabs") }
        view.findViewById<View?>(R.id.cardCatLobster)?.setOnClickListener { openCategory("Lobster") }
        view.findViewById<View?>(R.id.cardCatShellfish)?.setOnClickListener { openCategory("Shellfish") }
        view.findViewById<View?>(R.id.cardCatSquid)?.setOnClickListener { openCategory("Squid") }

        tvLocation?.setOnClickListener {
            startActivity(Intent(requireContext(), DeliveryAddressActivity::class.java))
        }

        fetchHome()
    }

    override fun onResume() {
        super.onResume()
        val savedLoc = sessionManager.getUserLocation()
        tvLocation?.text = savedLoc ?: "Not set"
    }

    private fun fetchHome() {
        lifecycleScope.launch {
            val userId = sessionManager.getUserId().takeIf { it > 0 } ?: run {
                Toast.makeText(requireContext(), "Please log in to load your home data", Toast.LENGTH_SHORT).show()
                return@launch
            }
            try {
                val resp = withContext(Dispatchers.IO) { ApiClient.apiService.getHome(userId) }
                if (resp.success) applyHome(resp)
            } catch (_: Exception) {
                // keep UI as-is if call fails
            }
        }
    }

    private fun applyHome(resp: HomeResponse) {
        val savedLoc = sessionManager.getUserLocation()
        if (savedLoc.isNullOrBlank()) {
            val locText = listOfNotNull(resp.location?.address, resp.location?.city)
                .joinToString(", ")
                .ifBlank { null }
            locText?.let { tvLocation?.text = it }
        }
        resp.today_banner?.subtitle?.let { tvFreshSubtitle?.text = it }

        val popular = resp.popular_today ?: emptyList()
        rvPopular?.adapter = ProductHomeAdapter(popular) { product ->
            openProduct(product)
        }
    }

    private fun openCategory(name: String?) {
        val intent = Intent(requireContext(), FishActivity::class.java)
        name?.let { intent.putExtra("category", it) }
        startActivity(intent)
    }

    private fun openProduct(product: Product) {
        val intent = Intent(requireContext(), AddToCartActivity::class.java)
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

    private fun bindTileByTitle(root: View, title: String, onClick: () -> Unit) {
        val tv = findTextViewWithExactText(root, title) ?: return
        val card = findParentCard(tv)
        if (card != null) {
            card.isClickable = true
            card.isFocusable = true
            card.setOnClickListener { onClick() }
        } else {
            tv.isClickable = true
            tv.setOnClickListener { onClick() }
        }
    }

    private fun findTextViewWithExactText(v: View, text: String): TextView? {
        if (v is TextView) {
            if (v.text?.toString()?.trim().equals(text, ignoreCase = false)) return v
        }
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                val found = findTextViewWithExactText(v.getChildAt(i), text)
                if (found != null) return found
            }
        }
        return null
    }

    private fun findParentCard(v: View): MaterialCardView? {
        var p = v.parent
        while (p is View) {
            if (p is MaterialCardView) return p
            p = p.parent
        }
        return null
    }
}
