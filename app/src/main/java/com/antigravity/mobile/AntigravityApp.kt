package com.antigravity.mobile

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebView

class AntigravityApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Ensure cookies are enabled globally
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        
        // Disable WebView debugging in production release
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
    }
}
