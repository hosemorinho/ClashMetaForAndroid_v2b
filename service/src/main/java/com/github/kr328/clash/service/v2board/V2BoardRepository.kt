package com.github.kr328.clash.service.v2board

import android.content.Context
import com.github.kr328.clash.service.store.AuthStore
import kotlinx.serialization.json.*

class V2BoardRepository(context: Context) {
    private val api = V2BoardApi(context)
    private val authStore = AuthStore(context)

    val isLoggedIn: Boolean get() = authStore.isLoggedIn

    suspend fun login(email: String, password: String): ApiResult<LoginResponse> {
        val body = buildJsonObject {
            put("email", email)
            put("password", password)
        }
        return when (val result = api.post(V2BoardEndpoints.LOGIN, body)) {
            is ApiResult.Success -> {
                val response = api.decode<LoginResponse>(result.data)
                val authData = response.authData ?: response.token ?: ""
                if (authData.isNotBlank()) {
                    authStore.authData = authData
                    authStore.token = response.token ?: ""
                    authStore.userEmail = email
                }
                ApiResult.Success(response)
            }
            is ApiResult.ValidationError -> result
            is ApiResult.AuthError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.ServerError -> result
        }
    }

    suspend fun register(
        email: String,
        password: String,
        inviteCode: String? = null,
        emailCode: String? = null
    ): ApiResult<LoginResponse> {
        val body = buildJsonObject {
            put("email", email)
            put("password", password)
            inviteCode?.let { put("invite_code", it) }
            emailCode?.let { put("email_code", it) }
        }
        return when (val result = api.post(V2BoardEndpoints.REGISTER, body)) {
            is ApiResult.Success -> {
                val response = api.decode<LoginResponse>(result.data)
                val authData = response.authData ?: response.token ?: ""
                if (authData.isNotBlank()) {
                    authStore.authData = authData
                    authStore.token = response.token ?: ""
                    authStore.userEmail = email
                }
                ApiResult.Success(response)
            }
            is ApiResult.ValidationError -> result
            is ApiResult.AuthError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.ServerError -> result
        }
    }

    suspend fun forgotPassword(email: String, password: String, emailCode: String): ApiResult<Unit> {
        val body = buildJsonObject {
            put("email", email)
            put("password", password)
            put("email_code", emailCode)
        }
        return when (val result = api.post(V2BoardEndpoints.FORGOT_PASSWORD, body)) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.ValidationError -> result
            is ApiResult.AuthError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.ServerError -> result
        }
    }

    suspend fun sendEmailVerify(email: String): ApiResult<Unit> {
        val body = buildJsonObject {
            put("email", email)
        }
        return when (val result = api.post(V2BoardEndpoints.SEND_EMAIL_VERIFY, body)) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.ValidationError -> result
            is ApiResult.AuthError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.ServerError -> result
        }
    }

    suspend fun getConfig(): ApiResult<SystemConfig> {
        return when (val result = api.get(V2BoardEndpoints.GET_CONFIG)) {
            is ApiResult.Success -> ApiResult.Success(api.decode(result.data))
            is ApiResult.ValidationError -> result
            is ApiResult.AuthError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.ServerError -> result
        }
    }

    suspend fun getUserInfo(): ApiResult<UserInfo> {
        return when (val result = api.get(V2BoardEndpoints.GET_USER_INFO)) {
            is ApiResult.Success -> ApiResult.Success(api.decode(result.data))
            is ApiResult.ValidationError -> result
            is ApiResult.AuthError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.ServerError -> result
        }
    }

    suspend fun getSubscribeUrl(): ApiResult<SubscribeData> {
        return when (val result = api.get(V2BoardEndpoints.GET_SUBSCRIBE)) {
            is ApiResult.Success -> {
                val data = api.decode<SubscribeData>(result.data)
                if (data.subscribeUrl.isNotBlank()) {
                    authStore.subscribeUrl = data.subscribeUrl
                }
                ApiResult.Success(data)
            }
            is ApiResult.ValidationError -> result
            is ApiResult.AuthError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.ServerError -> result
        }
    }

    suspend fun getPlans(): ApiResult<List<Plan>> {
        return when (val result = api.get(V2BoardEndpoints.GET_PLANS)) {
            is ApiResult.Success -> {
                try {
                    val plans = api.decode<List<Plan>>(result.data)
                    ApiResult.Success(plans)
                } catch (e: Exception) {
                    ApiResult.Success(emptyList())
                }
            }
            is ApiResult.ValidationError -> result
            is ApiResult.AuthError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.ServerError -> result
        }
    }

    suspend fun getServers(): ApiResult<List<ServerNode>> {
        return when (val result = api.get(V2BoardEndpoints.GET_SERVERS)) {
            is ApiResult.Success -> {
                try {
                    val servers = api.decode<List<ServerNode>>(result.data)
                    ApiResult.Success(servers)
                } catch (e: Exception) {
                    ApiResult.Success(emptyList())
                }
            }
            is ApiResult.ValidationError -> result
            is ApiResult.AuthError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.ServerError -> result
        }
    }

    suspend fun createOrder(planId: Int, period: String, couponCode: String? = null): ApiResult<String> {
        val body = buildJsonObject {
            put("plan_id", planId)
            put("period", period)
            couponCode?.let { put("coupon_code", it) }
        }
        return when (val result = api.post(V2BoardEndpoints.CREATE_ORDER, body)) {
            is ApiResult.Success -> {
                val tradeNo = try {
                    result.data.jsonPrimitive.content
                } catch (e: Exception) {
                    result.data.toString()
                }
                ApiResult.Success(tradeNo)
            }
            is ApiResult.ValidationError -> result
            is ApiResult.AuthError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.ServerError -> result
        }
    }

    suspend fun checkoutOrder(tradeNo: String, method: Int): ApiResult<CheckoutResult> {
        val body = buildJsonObject {
            put("trade_no", tradeNo)
            put("method", method)
        }
        return when (val result = api.post(V2BoardEndpoints.CHECKOUT_ORDER, body)) {
            is ApiResult.Success -> ApiResult.Success(api.decode(result.data))
            is ApiResult.ValidationError -> result
            is ApiResult.AuthError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.ServerError -> result
        }
    }

    suspend fun getOrders(): ApiResult<List<Order>> {
        return when (val result = api.get(V2BoardEndpoints.GET_ORDERS)) {
            is ApiResult.Success -> {
                try {
                    ApiResult.Success(api.decode<List<Order>>(result.data))
                } catch (e: Exception) {
                    ApiResult.Success(emptyList())
                }
            }
            is ApiResult.ValidationError -> result
            is ApiResult.AuthError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.ServerError -> result
        }
    }

    suspend fun getPaymentMethods(): ApiResult<List<PaymentMethod>> {
        return when (val result = api.get(V2BoardEndpoints.GET_PAYMENT_METHODS)) {
            is ApiResult.Success -> {
                try {
                    ApiResult.Success(api.decode<List<PaymentMethod>>(result.data))
                } catch (e: Exception) {
                    ApiResult.Success(emptyList())
                }
            }
            is ApiResult.ValidationError -> result
            is ApiResult.AuthError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.ServerError -> result
        }
    }

    suspend fun getDashboardStats(): ApiResult<DashboardStats> {
        return when (val result = api.get(V2BoardEndpoints.GET_DASHBOARD)) {
            is ApiResult.Success -> ApiResult.Success(api.decode(result.data))
            is ApiResult.ValidationError -> result
            is ApiResult.AuthError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.ServerError -> result
        }
    }

    fun logout() {
        authStore.clear()
    }

    fun getAuthData(): String = authStore.authData
    fun getUserEmail(): String = authStore.userEmail
    fun getStoredSubscribeUrl(): String = authStore.subscribeUrl
}
