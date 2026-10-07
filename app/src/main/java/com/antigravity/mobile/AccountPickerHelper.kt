package com.antigravity.mobile

import android.accounts.AccountManager
import android.content.Intent
import android.os.Build

object AccountPickerHelper {

    fun createChooseAccountIntent(selectedEmail: String? = null): Intent {
        val googleAccountTypes = arrayOf("com.google")
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AccountManager.newChooseAccountIntent(
                if (!selectedEmail.isNullOrBlank()) android.accounts.Account(selectedEmail, "com.google") else null,
                null,
                googleAccountTypes,
                null,
                null,
                null,
                null
            )
        } else {
            @Suppress("DEPRECATION")
            AccountManager.newChooseAccountIntent(
                if (!selectedEmail.isNullOrBlank()) android.accounts.Account(selectedEmail, "com.google") else null,
                null,
                googleAccountTypes,
                false,
                null,
                null,
                null,
                null
            )
        }
    }
}
