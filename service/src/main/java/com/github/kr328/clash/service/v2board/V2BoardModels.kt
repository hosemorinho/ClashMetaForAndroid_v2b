package com.github.kr328.clash.service.v2board

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    @SerialName("invite_code") val inviteCode: String? = null,
    @SerialName("email_code") val emailCode: String? = null
)

@Serializable
data class ForgotPasswordRequest(
    val email: String,
    val password: String,
    @SerialName("email_code") val emailCode: String
)

@Serializable
data class SendEmailVerifyRequest(
    val email: String
)

@Serializable
data class LoginResponse(
    val token: String? = null,
    @SerialName("auth_data") val authData: String? = null
)

@Serializable
data class SystemConfig(
    @SerialName("is_email_verify") val isEmailVerify: Boolean = false,
    @SerialName("is_invite_force") val isInviteForce: Boolean = false,
    @SerialName("is_recaptcha") val isRecaptcha: Boolean = false,
    @SerialName("recaptcha_site_key") val recaptchaSiteKey: String? = null,
    @SerialName("app_description") val appDescription: String? = null,
    @SerialName("app_url") val appUrl: String? = null,
    @SerialName("tos_url") val tosUrl: String? = null
)

@Serializable
data class UserInfo(
    val email: String = "",
    @SerialName("transfer_enable") val transferEnable: Long = 0,
    @SerialName("last_login_at") val lastLoginAt: Long? = null,
    @SerialName("created_at") val createdAt: Long = 0,
    val banned: Boolean = false,
    @SerialName("remind_expire") val remindExpire: Boolean = false,
    @SerialName("remind_traffic") val remindTraffic: Boolean = false,
    @SerialName("expired_at") val expiredAt: Long? = null,
    val balance: Long = 0,
    val commission_balance: Long = 0,
    @SerialName("plan_id") val planId: Int? = null,
    val discount: Int? = null,
    @SerialName("commission_rate") val commissionRate: Int? = null,
    val telegram_id: Long? = null,
    val uuid: String = "",
    val avatar_url: String? = null,
    val u: Long = 0,
    val d: Long = 0
)

@Serializable
data class SubscribeData(
    @SerialName("plan_id") val planId: Int? = null,
    val token: String = "",
    @SerialName("expired_at") val expiredAt: Long? = null,
    val u: Long = 0,
    val d: Long = 0,
    @SerialName("transfer_enable") val transferEnable: Long = 0,
    @SerialName("subscribe_url") val subscribeUrl: String = "",
    @SerialName("reset_day") val resetDay: Int? = null
)

@Serializable
data class Plan(
    val id: Int = 0,
    @SerialName("group_id") val groupId: Int = 0,
    val name: String = "",
    val content: String? = null,
    @SerialName("transfer_enable") val transferEnable: Long = 0,
    @SerialName("month_price") val monthPrice: Long? = null,
    @SerialName("quarter_price") val quarterPrice: Long? = null,
    @SerialName("half_year_price") val halfYearPrice: Long? = null,
    @SerialName("year_price") val yearPrice: Long? = null,
    @SerialName("two_year_price") val twoYearPrice: Long? = null,
    @SerialName("three_year_price") val threeYearPrice: Long? = null,
    @SerialName("onetime_price") val onetimePrice: Long? = null,
    @SerialName("reset_price") val resetPrice: Long? = null,
    val sort: Int? = null,
    val renew: Boolean = true,
    @SerialName("reset_traffic_method") val resetTrafficMethod: Int? = null
)

@Serializable
data class ServerNode(
    val id: Int = 0,
    @SerialName("group_id") val groupId: List<Int> = emptyList(),
    val name: String = "",
    @SerialName("parent_id") val parentId: Int? = null,
    val host: String = "",
    val port: Int = 0,
    @SerialName("server_port") val serverPort: Int = 0,
    val tags: List<String>? = null,
    val rate: String = "1",
    @SerialName("class") val serverClass: Int = 0,
    val online: Int = 0,
    val type: String = ""
)

@Serializable
data class Order(
    val id: Int = 0,
    @SerialName("invite_user_id") val inviteUserId: Int? = null,
    @SerialName("plan_id") val planId: Int = 0,
    val period: String = "",
    @SerialName("trade_no") val tradeNo: String = "",
    @SerialName("total_amount") val totalAmount: Long = 0,
    val status: Int = 0,
    @SerialName("commission_status") val commissionStatus: Int = 0,
    @SerialName("commission_balance") val commissionBalance: Long = 0,
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("updated_at") val updatedAt: Long = 0
)

@Serializable
data class PaymentMethod(
    val id: Int = 0,
    val name: String = "",
    val payment: String = "",
    val icon: String? = null
)

@Serializable
data class DashboardStats(
    val u: Long = 0,
    val d: Long = 0,
    @SerialName("transfer_enable") val transferEnable: Long = 0,
    @SerialName("expired_at") val expiredAt: Long? = null,
    @SerialName("plan_id") val planId: Int? = null,
    @SerialName("subscribe_url") val subscribeUrl: String? = null,
    @SerialName("reset_day") val resetDay: Int? = null
)

@Serializable
data class CreateOrderRequest(
    @SerialName("plan_id") val planId: Int,
    val period: String,
    @SerialName("coupon_code") val couponCode: String? = null
)

@Serializable
data class CheckoutOrderRequest(
    @SerialName("trade_no") val tradeNo: String,
    val method: Int
)

@Serializable
data class CheckoutResult(
    val type: Int = 0,
    val data: String? = null
)

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class ValidationError(val message: String, val errors: Map<String, List<String>> = emptyMap()) : ApiResult<Nothing>()
    data class AuthError(val message: String) : ApiResult<Nothing>()
    data class NetworkError(val message: String) : ApiResult<Nothing>()
    data class ServerError(val message: String) : ApiResult<Nothing>()
}
