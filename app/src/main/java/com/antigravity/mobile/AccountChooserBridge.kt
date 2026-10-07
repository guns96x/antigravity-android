package com.antigravity.mobile

import android.webkit.JavascriptInterface

class AccountChooserBridge(
    private val onAccountDetected: (String) -> Unit
) {
    @JavascriptInterface
    fun onAccountSelected(email: String) {
        if (email.isNotBlank()) {
            onAccountDetected(email.trim())
        }
    }
}
