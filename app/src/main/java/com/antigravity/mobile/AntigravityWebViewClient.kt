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
    private val prefs: PreferencesManager,
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

        // Allow Antigravity and Google Auth domains in-app
        if (isInternalDomain(host)) {
            return false
        }

        // External links open in system browser
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
            
            // Persist cookies to disk in background without blocking UI
            backgroundExecutor.execute {
                try {
                    CookieManager.getInstance().flush()
                } catch (_: Exception) {}
            }

            // Handle Account Chooser automation on accounts.google.com
            if (url.contains("accounts.google.com")) {
                injectAccountAutoSelector(view)
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

    private fun injectAccountAutoSelector(view: WebView?) {
        val preferred = prefs.preferredEmail.replace("\"", "\\\"")
        val autoSkip = prefs.autoSkipAccountChooser

        val script = """
            (function() {
                var preferred = "$preferred";
                var autoSkip = $autoSkip;

                function attachListeners() {
                    var items = document.querySelectorAll('[data-identifier], [data-email], li[role="link"]');
                    for (var i = 0; i < items.length; i++) {
                        (function(el) {
                            el.addEventListener('click', function() {
                                var id = el.getAttribute('data-identifier') || el.getAttribute('data-email') || el.innerText;
                                if (id && id.indexOf('@') !== -1 && window.AntigravityNative) {
                                    window.AntigravityNative.onAccountSelected(id.trim());
                                }
                            });
                        })(items[i]);
                    }
                }
                attachListeners();

                if (!autoSkip) return;

                setTimeout(function() {
                    if (preferred && preferred.length > 3) {
                        var match = document.querySelector('[data-identifier="' + preferred + '"], [data-email="' + preferred + '"]');
                        if (match) {
                            match.click();
                            return;
                        }
                        var all = document.querySelectorAll('li, div[role="button"]');
                        for (var j = 0; j < all.length; j++) {
                            if (all[j].textContent && all[j].textContent.indexOf(preferred) !== -1) {
                                all[j].click();
                                return;
                            }
                        }
                    } else {
                        var accounts = document.querySelectorAll('[data-identifier], [data-email]');
                        if (accounts.length === 1) {
                            var email = accounts[0].getAttribute('data-identifier') || accounts[0].getAttribute('data-email');
                            if (email && window.AntigravityNative) {
                                window.AntigravityNative.onAccountSelected(email);
                            }
                            accounts[0].click();
                        }
                    }
                }, 250);
            })();
        """.trimIndent()

        view?.evaluateJavascript(script, null)
    }

    private fun isInternalDomain(host: String): Boolean {
        if (host == "antigravity.google" || host.endsWith(".antigravity.google")) return true
        if (host == "antigravity.google.com" || host.endsWith(".antigravity.google.com")) return true
        if (host == "google.com" || host.endsWith(".google.com")) return true
        if (host == "gstatic.com" || host.endsWith(".gstatic.com")) return true
        if (host == "googleusercontent.com" || host.endsWith(".googleusercontent.com")) return true
        return false
    }
}
