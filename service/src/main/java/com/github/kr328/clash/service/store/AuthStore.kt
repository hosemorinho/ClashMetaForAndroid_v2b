package com.github.kr328.clash.service.store

import android.content.Context
import com.github.kr328.clash.common.store.Store
import com.github.kr328.clash.common.store.asStoreProvider

class AuthStore(context: Context) {
    private val store = Store(
        context
            .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            .asStoreProvider()
    )

    var authData: String by store.string(
        key = "auth_data",
        defaultValue = ""
    )

    var token: String by store.string(
        key = "token",
        defaultValue = ""
    )

    var userEmail: String by store.string(
        key = "user_email",
        defaultValue = ""
    )

    var subscribeUrl: String by store.string(
        key = "subscribe_url",
        defaultValue = ""
    )

    var language: String by store.string(
        key = "language",
        defaultValue = ""
    )

    val isLoggedIn: Boolean get() = authData.isNotBlank()

    fun clear() {
        authData = ""
        token = ""
        userEmail = ""
        subscribeUrl = ""
    }

    companion object {
        private const val PREFERENCE_NAME = "v2board_auth"
    }
}
