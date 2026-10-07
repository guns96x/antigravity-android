package com.antigravity.mobile

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.concurrent.Executors

class AntigravityWebViewClient(
    private val context: Context,
    private val onPageStartedCallback: (String) -> Unit,
    private val onPageFinishedCallback: (String) -> Unit,
    private val onErrorCallback: (String) -> Unit,
    private val onRendererCrashCallback: () -> Unit
) : WebViewClient() {

    private val backgroundExecutor = Executors.newSingleThreadExecutor()

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        val scheme = uri.scheme?.lowercase() ?: ""
        val host = uri.host?.lowercase() ?: ""

        // Non-web schemes (mailto, tel, intent, etc.)
        if (scheme != "http" && scheme != "https") {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
            } catch (_: Exception) {}
            return true
        }

        // Keep all Antigravity and Google auth domains inside our in-app WebView
        if (isInternalDomain(host)) {
            return false
        }

        // For strictly external 3rd-party sites (e.g. GitHub, external docs), open in browser
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (_: Exception) {}
        return true
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (url != null) {
            onPageStartedCallback(url)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (url != null) {
            onPageFinishedCallback(url)
            
            // Persist cookies to disk in background so sessions never drop
            backgroundExecutor.execute {
                try {
                    CookieManager.getInstance().flush()
                } catch (_: Exception) {}
            }
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            val description = error?.description?.toString() ?: "Connection error"
            onErrorCallback(description)
        }
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        onRendererCrashCallback()
        return true
    }

    private fun isInternalDomain(host: String): Boolean {
        val h = host.lowercase()
        if (h == "antigravity.google" || h.endsWith(".antigravity.google")) return true
        if (h == "antigravity.google.com" || h.endsWith(".antigravity.google.com")) return true
        if (h == "google.com" || h.endsWith(".google.com")) return true
        if (h.contains(".google.")) return true
        if (h == "gstatic.com" || h.endsWith(".gstatic.com")) return true
        if (h == "googleusercontent.com" || h.endsWith(".googleusercontent.com")) return true
        if (h == "googleapis.com" || h.endsWith(".googleapis.com")) return true
        if (h == "youtube.com" || h.endsWith(".youtube.com")) return true
        return false
    }
}
