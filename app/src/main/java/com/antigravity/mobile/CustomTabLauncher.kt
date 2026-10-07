package com.antigravity.mobile

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat

object CustomTabLauncher {

    fun launchAntigravity(
        context: Context,
        baseUrl: String = PreferencesManager.DEFAULT_URL,
        email: String = "",
        authUserIndex: Int = 0
    ) {
        val targetUri = buildTargetUri(baseUrl, email, authUserIndex)

        val darkParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(context, R.color.brand_background))
            .build()

        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(true)
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
            .setDefaultColorSchemeParams(darkParams)
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .build()

        intent.launchUrl(context, targetUri)
    }

    private fun buildTargetUri(baseUrl: String, email: String, authUserIndex: Int): Uri {
        val parsed = Uri.parse(baseUrl)
        val builder = parsed.buildUpon()

        // Append authuser to bypass Google AccountChooser loop
        if (email.isNotBlank()) {
            builder.appendQueryParameter("authuser", email.trim())
        } else if (authUserIndex >= 0) {
            builder.appendQueryParameter("authuser", authUserIndex.toString())
        }

        return builder.build()
    }
}
