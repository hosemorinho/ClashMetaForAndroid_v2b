package com.github.kr328.clash

import android.app.AlertDialog
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.design.LoginDesign
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.service.store.AuthStore
import com.github.kr328.clash.service.v2board.ApiResult
import com.github.kr328.clash.service.v2board.V2BoardRepository
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class LoginActivity : BaseActivity<LoginDesign>() {
    private val repository by lazy { V2BoardRepository(this) }
    private val authStore by lazy { AuthStore(this) }

    private val languageOptions = arrayOf(
        "system" to "Follow System",
        "en" to "English",
        "zh" to "简体中文",
        "zh-TW" to "繁體中文",
        "ja" to "日本語",
        "ko" to "한국어",
        "ru" to "Русский",
        "vi" to "Tiếng Việt"
    )

    override suspend fun main() {
        val design = LoginDesign(this)
        setContentDesign(design)

        design.setLanguageLabel(getLanguageLabel())

        while (isActive) {
            select<Unit> {
                events.onReceive { }
                design.requests.onReceive {
                    when (it) {
                        is LoginDesign.Request.Login -> {
                            handleLogin(design, it.email, it.password)
                        }
                        LoginDesign.Request.OpenRegister -> {
                            startActivity(RegisterActivity::class.intent)
                        }
                        LoginDesign.Request.OpenForgotPassword -> {
                            startActivity(ForgotPasswordActivity::class.intent)
                        }
                        LoginDesign.Request.ChangeLanguage -> {
                            handleChangeLanguage()
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleLogin(design: LoginDesign, email: String, password: String) {
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

        design.setLoading(true)

        when (val result = repository.login(email, password)) {
            is ApiResult.Success -> {
                importSubscription(design)
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

    private suspend fun importSubscription(design: LoginDesign) {
        when (val subResult = repository.getSubscribeUrl()) {
            is ApiResult.Success -> {
                val subscribeUrl = subResult.data.subscribeUrl
                if (subscribeUrl.isNotBlank()) {
                    try {
                        withProfile {
                            val uuid = create(
                                Profile.Type.Url,
                                "V2Board",
                                subscribeUrl
                            )
                            commit(uuid)
                            setActive(queryByUUID(uuid)!!)
                        }
                    } catch (e: Exception) {
                        // Profile import failed but login succeeded
                    }
                }
                setResult(RESULT_OK)
                finish()
            }
            else -> {
                // Subscription fetch failed but login succeeded
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private suspend fun handleChangeLanguage() {
        val labels = languageOptions.map { it.second }.toTypedArray()
        val currentLang = authStore.language.ifBlank { "system" }
        val currentIndex = languageOptions.indexOfFirst { it.first == currentLang }.coerceAtLeast(0)

        withContext(Dispatchers.Main) {
            AlertDialog.Builder(this@LoginActivity)
                .setTitle(com.github.kr328.clash.design.R.string.select_language)
                .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                    val selectedLang = languageOptions[which].first
                    authStore.language = if (selectedLang == "system") "" else selectedLang
                    dialog.dismiss()
                    recreate()
                }
                .show()
        }
    }

    private fun getLanguageLabel(): String {
        val lang = authStore.language.ifBlank { "system" }
        return languageOptions.firstOrNull { it.first == lang }?.second ?: "Follow System"
    }

    override fun shouldDisplayHomeAsUpEnabled() = false
}
