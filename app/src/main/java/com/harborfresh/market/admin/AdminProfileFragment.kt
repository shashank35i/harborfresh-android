package com.harborfresh.market.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.harborfresh.market.R
import com.harborfresh.market.HelpActivity
import com.harborfresh.market.auth.LoginActivity
import com.harborfresh.market.common.SessionManager

class AdminProfileFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private var tvName: TextView? = null
    private var tvEmail: TextView? = null
    private var tvRole: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_admin_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = SessionManager(requireContext())
        tvName = view.findViewById(R.id.tvAdminName)
        tvEmail = view.findViewById(R.id.tvAdminEmail)
        tvRole = view.findViewById(R.id.tvAdminRole)

        val name = arguments?.getString("admin_name") ?: sessionManager.getAdminName() ?: "Admin"
        val email = arguments?.getString("admin_email") ?: sessionManager.getAdminEmail() ?: "admin@email.com"
        tvName?.text = name
        tvEmail?.text = email
        tvRole?.text = "Role: Admin"

        view.findViewById<View>(R.id.rowLogout).setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        view.findViewById<View?>(R.id.rowHelp)?.setOnClickListener {
            startActivity(Intent(requireContext(), HelpActivity::class.java))
        }

        view.findViewById<View?>(R.id.rowSupport)?.setOnClickListener {
            startActivity(Intent(requireContext(), HelpActivity::class.java))
        }
    }
}
