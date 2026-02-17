package com.harborfresh.market

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.harborfresh.market.api.ApiClient
import com.harborfresh.market.common.FavoritesManager
import com.harborfresh.market.common.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        wireMenu(view)
        view.findViewById<View?>(R.id.btnSignOut)?.setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(requireContext(), com.harborfresh.market.auth.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }
        fetchProfile(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { fetchProfile(it) } ?: updateFavoritesCount(view)
    }

    private fun wireMenu(root: View) {
        root.findViewById<View?>(R.id.rowHelp)?.setOnClickListener {
            startActivity(Intent(requireContext(), HelpActivity::class.java))
        }
        root.findViewById<View?>(R.id.rowSavedAddresses)?.setOnClickListener {
            startActivity(Intent(requireContext(), DeliveryAddressActivity::class.java))
        }
        root.findViewById<View?>(R.id.rowFavorites)?.setOnClickListener {
            startActivity(Intent(requireContext(), FavoritesActivity::class.java))
        }
        root.findViewById<View?>(R.id.rowNotifications)?.setOnClickListener {
            showBottomSheet(
                title = "Notifications",
                subtitle = "Manage alerts for orders and offers",
                items = listOf(
                    SheetItem(R.drawable.ic_orders, "Order updates: Enabled"),
                    SheetItem(R.drawable.ic_bell, "Promotions: Enabled"),
                    SheetItem(R.drawable.ic_clock, "Delivery alerts: Enabled")
                )
            )
        }
        root.findViewById<View?>(R.id.rowPreferences)?.setOnClickListener {
            showBottomSheet(
                title = "Preferences",
                subtitle = "Personalize your experience",
                items = listOf(
                    SheetItem(R.drawable.ic_settings, "Language: English"),
                    SheetItem(R.drawable.ic_card, "Currency: INR"),
                    SheetItem(R.drawable.ic_shield, "Privacy: Standard")
                )
            )
        }
        root.findViewById<View?>(R.id.rowRefer)?.setOnClickListener {
            showBottomSheet(
                title = "Refer and Earn",
                subtitle = "Share your code and earn rewards",
                items = listOf(
                    SheetItem(R.drawable.ic_gift, "Invite friends: Earn Rs 100"),
                    SheetItem(R.drawable.ic_info, "Share your code: FRESH100"),
                    SheetItem(R.drawable.ic_info, "Terms apply")
                )
            )
        }
        root.findViewById<View?>(R.id.rowPayment)?.setOnClickListener {
            showBottomSheet(
                title = "Payment Methods",
                subtitle = "Set your preferred payment",
                items = listOf(
                    SheetItem(R.drawable.ic_card, "Cards: Add or manage at checkout"),
                    SheetItem(R.drawable.ic_credit_card_24, "UPI: Linked"),
                    SheetItem(R.drawable.ic_card, "Cash on Delivery: Available")
                )
            )
        }
    }

    private fun fetchProfile(root: View) {
        lifecycleScope.launch {
            val userId = sessionManager.getUserId().takeIf { it > 0 } ?: run {
                Toast.makeText(requireContext(), "Please log in to view profile", Toast.LENGTH_SHORT).show()
                return@launch
            }
            root.findViewById<TextView?>(R.id.tvName)?.text = "-"
            root.findViewById<TextView?>(R.id.tvPhone)?.text = "-"
            root.findViewById<TextView?>(R.id.tvEmail)?.text = "-"
            root.findViewById<TextView?>(R.id.tvOrdersValue)?.text = "0"
            root.findViewById<TextView?>(R.id.tvSavedValue)?.text = "Rs 0"
            root.findViewById<TextView?>(R.id.tvPointsValue)?.text = "0"
            root.findViewById<TextView?>(R.id.tvAddressCount)?.text = "0"
            try {
                val resp = withContext(Dispatchers.IO) { ApiClient.apiService.getProfile(userId) }
                if (resp.success) {
                    resp.profile?.let {
                        root.findViewById<TextView?>(R.id.tvName)?.text = it.name?.ifBlank { "-" } ?: "-"
                        root.findViewById<TextView?>(R.id.tvPhone)?.text = it.phone?.ifBlank { "-" } ?: "-"
                        root.findViewById<TextView?>(R.id.tvEmail)?.text = it.email?.ifBlank { "-" } ?: "-"
                    }
                    resp.stats?.let {
                        root.findViewById<TextView?>(R.id.tvOrdersValue)?.text = (it.orders ?: 0).toString()
                        root.findViewById<TextView?>(R.id.tvSavedValue)?.text = "Rs ${String.format("%.0f", it.saved ?: 0.0)}"
                        root.findViewById<TextView?>(R.id.tvPointsValue)?.text = (it.points ?: 0).toString()
                    }
                    resp.addresses?.let {
                        root.findViewById<TextView?>(R.id.tvAddressCount)?.text = it.size.toString()
                    }
                    updateFavoritesCount(root)
                } else {
                    Toast.makeText(requireContext(), "Unable to load profile", Toast.LENGTH_SHORT).show()
                    Log.w("ProfileFragment", "Profile load failed: $resp")
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Error loading profile", e)
                Toast.makeText(requireContext(), "Unable to load profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFavoritesCount(root: View?) {
        val count = FavoritesManager.getFavorites(requireContext()).size
        root?.findViewById<TextView?>(R.id.tvFavoritesCount)?.text = count.toString()
    }

    private fun showBottomSheet(title: String, subtitle: String, items: List<SheetItem>) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_profile_action, null)
        dialog.setContentView(view)

        view.findViewById<TextView?>(R.id.tvSheetTitle)?.text = title
        view.findViewById<TextView?>(R.id.tvSheetSubtitle)?.text = subtitle

        val rowIds = listOf(R.id.item1, R.id.item2, R.id.item3, R.id.item4)
        val iconIds = listOf(R.id.item1Icon, R.id.item2Icon, R.id.item3Icon, R.id.item4Icon)
        val textIds = listOf(R.id.item1Text, R.id.item2Text, R.id.item3Text, R.id.item4Text)

        rowIds.forEachIndexed { index, rowId ->
            val row = view.findViewById<View?>(rowId)
            val icon = view.findViewById<ImageView?>(iconIds[index])
            val text = view.findViewById<TextView?>(textIds[index])
            val item = items.getOrNull(index)
            if (item == null) {
                row?.visibility = View.GONE
            } else {
                row?.visibility = View.VISIBLE
                icon?.setImageResource(item.iconRes)
                text?.text = item.text
            }
        }

        view.findViewById<View?>(R.id.tvSheetClose)?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private data class SheetItem(val iconRes: Int, val text: String)
}
