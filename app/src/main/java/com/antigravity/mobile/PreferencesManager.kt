package com.antigravity.mobile

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var canonicalUrl: String
        get() = prefs.getString(KEY_CANONICAL_URL, DEFAULT_URL) ?: DEFAULT_URL
        set(value) = prefs.edit().putString(KEY_CANONICAL_URL, value.trim()).apply()

    var preferredEmail: String
        get() = prefs.getString(KEY_PREFERRED_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PREFERRED_EMAIL, value.trim()).apply()

    var authUserIndex: Int
        get() = prefs.getInt(KEY_AUTH_USER_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_AUTH_USER_INDEX, value).apply()

    var autoLaunchOnOpen: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LAUNCH, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_LAUNCH, value).apply()

    var isDesktopMode: Boolean
        get() = prefs.getBoolean(KEY_DESKTOP_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DESKTOP_MODE, value).apply()

    var keepScreenAwake: Boolean
        get() = prefs.getBoolean(KEY_KEEP_AWAKE, false)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_AWAKE, value).apply()

    var autoSkipAccountChooser: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SKIP_CHOOSER, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SKIP_CHOOSER, value).apply()

    var hasPromptedAccountPicker: Boolean
        get() = prefs.getBoolean(KEY_PROMPTED_PICKER, false)
        set(value) = prefs.edit().putBoolean(KEY_PROMPTED_PICKER, value).apply()

    fun clearSession() {
        prefs.edit().remove(KEY_PREFERRED_EMAIL).apply()
    }

    companion object {
        private const val PREFS_NAME = "antigravity_prefs"
        const val DEFAULT_URL = "https://antigravity.google.com"

        private const val KEY_CANONICAL_URL = "canonical_url"
        private const val KEY_PREFERRED_EMAIL = "preferred_email"
        private const val KEY_AUTH_USER_INDEX = "auth_user_index"
        private const val KEY_AUTO_LAUNCH = "auto_launch"
        private const val KEY_DESKTOP_MODE = "desktop_mode"
        private const val KEY_KEEP_AWAKE = "keep_awake"
        private const val KEY_AUTO_SKIP_CHOOSER = "auto_skip_chooser"
        private const val KEY_PROMPTED_PICKER = "prompted_picker"
    }
}
