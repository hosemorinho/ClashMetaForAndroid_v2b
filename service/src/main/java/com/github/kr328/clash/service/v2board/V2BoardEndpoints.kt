package com.github.kr328.clash.service.v2board

object V2BoardEndpoints {
    const val LOGIN = "/passport/auth/login"
    const val REGISTER = "/passport/auth/register"
    const val FORGOT_PASSWORD = "/passport/auth/forget"
    const val SEND_EMAIL_VERIFY = "/passport/comm/sendEmailVerify"
    const val GET_CONFIG = "/guest/comm/config"
    const val GET_USER_INFO = "/user/getUserInfo"
    const val GET_SUBSCRIBE = "/user/subscribe"
    const val GET_DASHBOARD = "/user/dashbord"
    const val GET_PLANS = "/user/plan/fetch"
    const val GET_PUBLIC_PLANS = "/guest/plan/fetch"
    const val CREATE_ORDER = "/user/order/save"
    const val CHECKOUT_ORDER = "/user/order/checkout"
    const val GET_ORDERS = "/user/order/fetch"
    const val GET_PAYMENT_METHODS = "/user/order/getPaymentMethod"
    const val GET_SERVERS = "/user/server/fetch"
}
