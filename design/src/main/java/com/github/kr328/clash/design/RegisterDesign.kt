package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import com.github.kr328.clash.design.databinding.DesignRegisterBinding
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import com.github.kr328.clash.service.v2board.SystemConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RegisterDesign(context: Context) : Design<RegisterDesign.Request>(context) {
    sealed class Request {
        data class Register(
            val email: String,
            val password: String,
            val inviteCode: String?,
            val emailCode: String?
        ) : Request()
        data class SendEmailCode(val email: String) : Request()
        object OpenLogin : Request()
    }

    private val binding = DesignRegisterBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    init {
        binding.self = this
    }

    fun requestRegisterAction() {
        val email = binding.emailInput.text?.toString()?.trim() ?: ""
        val password = binding.passwordInput.text?.toString() ?: ""
        val inviteCode = binding.inviteCodeInput.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
        val emailCode = binding.emailCodeInput.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
        requests.trySend(Request.Register(email, password, inviteCode, emailCode))
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

    suspend fun setConfig(config: SystemConfig) {
        withContext(Dispatchers.Main) {
            binding.showInviteCode = config.isInviteForce
            binding.showEmailVerify = config.isEmailVerify
        }
    }

    suspend fun setEmailCodeCooldown(seconds: Int) {
        withContext(Dispatchers.Main) {
            binding.emailCodeCooldown = seconds
        }
    }
}
