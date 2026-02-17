package com.harborfresh.market.identity

import androidx.appcompat.app.AppCompatActivity
import android.view.View
import com.harborfresh.market.common.SessionManager

/** Safe click binding that skips if the view id is missing in a given layout. */
fun AppCompatActivity.onClick(viewId: Int, action: () -> Unit) {
    findViewById<View?>(viewId)?.setOnClickListener { action() }
}

fun AppCompatActivity.pushStep(step: Int = 0, sellerId: Int = 0) {
    // progression is now handled by dedicated API calls per screen
}

fun AppCompatActivity.resolveSellerId(intentSellerId: Int = 0): Int {
    val sessionId = SessionManager(this).getSellerId()
    return when {
        intentSellerId > 0 -> intentSellerId
        sessionId > 0 -> sessionId
        else -> 0
    }
}
