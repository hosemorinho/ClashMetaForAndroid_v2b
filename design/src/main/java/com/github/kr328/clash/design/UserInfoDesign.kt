package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import com.github.kr328.clash.design.databinding.DesignUserInfoBinding
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserInfoDesign(context: Context) : Design<UserInfoDesign.Request>(context) {
    enum class Request {
        Logout,
    }

    private val binding = DesignUserInfoBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    init {
        binding.self = this
    }

    fun requestLogout() {
        requests.trySend(Request.Logout)
    }

    suspend fun setLoading(loading: Boolean) {
        withContext(Dispatchers.Main) {
            binding.loading = loading
        }
    }

    suspend fun setEmail(email: String?) {
        withContext(Dispatchers.Main) {
            binding.email = email
        }
    }

    suspend fun setPlanName(name: String?) {
        withContext(Dispatchers.Main) {
            binding.planName = name
        }
    }

    suspend fun setDataUsage(used: String, total: String) {
        withContext(Dispatchers.Main) {
            binding.dataUsed = used
            binding.dataTotal = total
        }
    }

    suspend fun setExpiryDate(date: String?) {
        withContext(Dispatchers.Main) {
            binding.expiryDate = date
        }
    }

    suspend fun setBalance(balance: String) {
        withContext(Dispatchers.Main) {
            binding.balance = balance
        }
    }
}
