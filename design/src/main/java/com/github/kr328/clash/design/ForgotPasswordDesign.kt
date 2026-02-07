package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import com.github.kr328.clash.design.databinding.DesignForgotPasswordBinding
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ForgotPasswordDesign(context: Context) : Design<ForgotPasswordDesign.Request>(context) {
    sealed class Request {
        data class ResetPassword(val email: String, val password: String, val emailCode: String) : Request()
        data class SendEmailCode(val email: String) : Request()
        object OpenLogin : Request()
    }

    private val binding = DesignForgotPasswordBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    init {
        binding.self = this
    }

    fun requestResetPassword() {
        val email = binding.emailInput.text?.toString()?.trim() ?: ""
        val password = binding.passwordInput.text?.toString() ?: ""
        val emailCode = binding.emailCodeInput.text?.toString()?.trim() ?: ""
        requests.trySend(Request.ResetPassword(email, password, emailCode))
    }

    fun requestSendEmailCode() {
        val email = binding.emailInput.text?.toString()?.trim() ?: ""
        requests.trySend(Request.SendEmailCode(email))
    }

    fun requestOpenLogin() {
        requests.trySend(Request.OpenLogin)
    }

    fun getConfirmPassword(): String {
        return binding.confirmPasswordInput.text?.toString() ?: ""
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

    suspend fun setEmailCodeCooldown(seconds: Int) {
        withContext(Dispatchers.Main) {
            binding.emailCodeCooldown = seconds
        }
    }
}
