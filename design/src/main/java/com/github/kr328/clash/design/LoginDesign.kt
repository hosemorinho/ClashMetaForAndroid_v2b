package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import com.github.kr328.clash.design.databinding.DesignLoginBinding
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginDesign(context: Context) : Design<LoginDesign.Request>(context) {
    sealed class Request {
        data class Login(val email: String, val password: String) : Request()
        object OpenRegister : Request()
        object OpenForgotPassword : Request()
    }

    private val binding = DesignLoginBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    init {
        binding.self = this
    }

    fun requestLogin() {
        val email = binding.emailInput.text?.toString()?.trim() ?: ""
        val password = binding.passwordInput.text?.toString() ?: ""
        requests.trySend(Request.Login(email, password))
    }

    fun requestRegister() {
        requests.trySend(Request.OpenRegister)
    }

    fun requestForgotPassword() {
        requests.trySend(Request.OpenForgotPassword)
    }

    suspend fun setLoading(loading: Boolean) {
        withContext(Dispatchers.Main) {
            binding.loading = loading
        }
    }

    suspend fun setEmailError(error: String?) {
        withContext(Dispatchers.Main) {
            binding.emailError = error
        }
    }

    suspend fun setPasswordError(error: String?) {
        withContext(Dispatchers.Main) {
            binding.passwordError = error
        }
    }

    suspend fun setGeneralError(error: String?) {
        withContext(Dispatchers.Main) {
            binding.generalError = error
        }
    }
}
