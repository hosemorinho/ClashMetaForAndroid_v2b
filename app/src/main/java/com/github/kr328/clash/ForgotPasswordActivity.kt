package com.github.kr328.clash

import com.github.kr328.clash.design.ForgotPasswordDesign
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.service.v2board.ApiResult
import com.github.kr328.clash.service.v2board.V2BoardRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class ForgotPasswordActivity : BaseActivity<ForgotPasswordDesign>() {
    private val repository by lazy { V2BoardRepository(this) }

    override suspend fun main() {
        val design = ForgotPasswordDesign(this)
        setContentDesign(design)

        while (isActive) {
            select<Unit> {
                events.onReceive { }
                design.requests.onReceive {
                    when (it) {
                        is ForgotPasswordDesign.Request.ResetPassword -> {
                            handleResetPassword(design, it.email, it.password, it.emailCode)
                        }
                        is ForgotPasswordDesign.Request.SendEmailCode -> {
                            handleSendEmailCode(design, it.email)
                        }
                        ForgotPasswordDesign.Request.OpenLogin -> {
                            finish()
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleResetPassword(
        design: ForgotPasswordDesign,
        email: String,
        password: String,
        emailCode: String
    ) {
        design.setEmailError(null)
        design.setPasswordError(null)
        design.setGeneralError(null)

        if (email.isBlank()) {
            design.setEmailError(getString(com.github.kr328.clash.design.R.string.email_required))
            return
        }
        if (emailCode.isBlank()) {
            design.setGeneralError(getString(com.github.kr328.clash.design.R.string.email_code_required))
            return
        }
        if (password.isBlank()) {
            design.setPasswordError(getString(com.github.kr328.clash.design.R.string.password_required))
            return
        }
        if (password != design.getConfirmPassword()) {
            design.setPasswordError(getString(com.github.kr328.clash.design.R.string.passwords_not_match))
            return
        }

        design.setLoading(true)

        when (val result = repository.forgotPassword(email, password, emailCode)) {
            is ApiResult.Success -> {
                design.showToast(
                    com.github.kr328.clash.design.R.string.password_reset_success,
                    ToastDuration.Long
                )
                finish()
            }
            is ApiResult.ValidationError -> {
                result.errors["email"]?.firstOrNull()?.let { design.setEmailError(it) }
                result.errors["password"]?.firstOrNull()?.let { design.setPasswordError(it) }
                if (result.errors.isEmpty()) {
                    design.setGeneralError(result.message)
                }
                design.setLoading(false)
            }
            is ApiResult.AuthError -> {
                design.setGeneralError(result.message)
                design.setLoading(false)
            }
            is ApiResult.NetworkError -> {
                design.setGeneralError(getString(com.github.kr328.clash.design.R.string.network_error))
                design.setLoading(false)
            }
            is ApiResult.ServerError -> {
                design.setGeneralError(result.message)
                design.setLoading(false)
            }
        }
    }

    private suspend fun handleSendEmailCode(design: ForgotPasswordDesign, email: String) {
        if (email.isBlank()) {
            design.setEmailError(getString(com.github.kr328.clash.design.R.string.email_required))
            return
        }

        design.setLoading(true)

        when (val result = repository.sendEmailVerify(email)) {
            is ApiResult.Success -> {
                design.setLoading(false)
                launch {
                    for (i in 60 downTo 0) {
                        design.setEmailCodeCooldown(i)
                        delay(1000)
                    }
                }
            }
            is ApiResult.ValidationError -> {
                design.setGeneralError(result.message)
                design.setLoading(false)
            }
            else -> {
                design.setGeneralError(getString(com.github.kr328.clash.design.R.string.send_code_failed))
                design.setLoading(false)
            }
        }
    }

    override fun shouldDisplayHomeAsUpEnabled() = true
}
