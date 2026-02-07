package com.github.kr328.clash

import com.github.kr328.clash.design.RegisterDesign
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.service.v2board.ApiResult
import com.github.kr328.clash.service.v2board.V2BoardRepository
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class RegisterActivity : BaseActivity<RegisterDesign>() {
    private val repository by lazy { V2BoardRepository(this) }

    override suspend fun main() {
        val design = RegisterDesign(this)
        setContentDesign(design)

        // Fetch config to determine which fields to show
        when (val configResult = repository.getConfig()) {
            is ApiResult.Success -> design.setConfig(configResult.data)
            else -> {} // Use defaults
        }

        while (isActive) {
            select<Unit> {
                events.onReceive { }
                design.requests.onReceive {
                    when (it) {
                        is RegisterDesign.Request.Register -> {
                            handleRegister(design, it.email, it.password, it.inviteCode, it.emailCode)
                        }
                        is RegisterDesign.Request.SendEmailCode -> {
                            handleSendEmailCode(design, it.email)
                        }
                        RegisterDesign.Request.OpenLogin -> {
                            finish()
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleRegister(
        design: RegisterDesign,
        email: String,
        password: String,
        inviteCode: String?,
        emailCode: String?
    ) {
        design.setEmailError(null)
        design.setPasswordError(null)
        design.setGeneralError(null)

        if (email.isBlank()) {
            design.setEmailError(getString(com.github.kr328.clash.design.R.string.email_required))
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

        when (val result = repository.register(email, password, inviteCode, emailCode)) {
            is ApiResult.Success -> {
                // Auto-import subscription
                when (val subResult = repository.getSubscribeUrl()) {
                    is ApiResult.Success -> {
                        val subscribeUrl = subResult.data.subscribeUrl
                        if (subscribeUrl.isNotBlank()) {
                            try {
                                withProfile {
                                    val uuid = create(Profile.Type.Url, "V2Board", subscribeUrl)
                                    commit(uuid)
                                    setActive(queryByUUID(uuid)!!)
                                }
                            } catch (_: Exception) { }
                        }
                    }
                    else -> { }
                }
                setResult(RESULT_OK)
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

    private suspend fun handleSendEmailCode(design: RegisterDesign, email: String) {
        if (email.isBlank()) {
            design.setEmailError(getString(com.github.kr328.clash.design.R.string.email_required))
            return
        }

        design.setLoading(true)

        when (val result = repository.sendEmailVerify(email)) {
            is ApiResult.Success -> {
                design.setLoading(false)
                // Start cooldown timer
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
