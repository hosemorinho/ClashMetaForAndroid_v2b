package com.github.kr328.clash

import android.content.Intent
import android.net.Uri
import com.github.kr328.clash.design.CheckoutDesign
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.service.v2board.ApiResult
import com.github.kr328.clash.service.v2board.Plan
import com.github.kr328.clash.service.v2board.V2BoardRepository
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class CheckoutActivity : BaseActivity<CheckoutDesign>() {
    private val repository by lazy { V2BoardRepository(this) }

    override suspend fun main() {
        val design = CheckoutDesign(this)
        setContentDesign(design)

        val planId = intent.getIntExtra("plan_id", 0)
        val planName = intent.getStringExtra("plan_name") ?: ""

        design.setPlanName(planName)
        design.setLoading(true)

        // Fetch plan details for available periods
        var selectedPlan: Plan? = null
        when (val result = repository.getPlans()) {
            is ApiResult.Success -> {
                selectedPlan = result.data.find { it.id == planId }
                if (selectedPlan != null) {
                    design.setPeriods(buildPeriodList(selectedPlan))
                }
            }
            else -> {}
        }

        // Fetch payment methods
        when (val result = repository.getPaymentMethods()) {
            is ApiResult.Success -> {
                design.setPaymentMethods(result.data)
            }
            else -> {}
        }

        design.setLoading(false)

        while (isActive) {
            select<Unit> {
                events.onReceive { }
                design.requests.onReceive {
                    when (it) {
                        is CheckoutDesign.Request.Checkout -> {
                            handleCheckout(design, planId, it.period, it.couponCode, it.paymentMethodId)
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleCheckout(
        design: CheckoutDesign,
        planId: Int,
        period: String,
        couponCode: String?,
        paymentMethodId: Int
    ) {
        design.setGeneralError(null)
        design.setLoading(true)

        // Create order
        when (val orderResult = repository.createOrder(planId, period, couponCode)) {
            is ApiResult.Success -> {
                val tradeNo = orderResult.data
                // Checkout order
                when (val checkoutResult = repository.checkoutOrder(tradeNo, paymentMethodId)) {
                    is ApiResult.Success -> {
                        val result = checkoutResult.data
                        if (result.type == 1 && !result.data.isNullOrBlank()) {
                            // Open payment URL in browser
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.data)))
                            } catch (e: Exception) {
                                design.setGeneralError(getString(com.github.kr328.clash.design.R.string.unable_to_open_payment))
                            }
                        } else if (result.type == -1 || result.type == 0) {
                            // Payment completed (balance payment) or no payment needed
                            design.showToast(
                                com.github.kr328.clash.design.R.string.payment_success,
                                ToastDuration.Long
                            )
                            finish()
                        }
                        design.setLoading(false)
                    }
                    is ApiResult.ValidationError -> {
                        design.setGeneralError(checkoutResult.message)
                        design.setLoading(false)
                    }
                    else -> {
                        design.setGeneralError(getString(com.github.kr328.clash.design.R.string.checkout_failed))
                        design.setLoading(false)
                    }
                }
            }
            is ApiResult.ValidationError -> {
                design.setGeneralError(orderResult.message)
                design.setLoading(false)
            }
            is ApiResult.NetworkError -> {
                design.setGeneralError(getString(com.github.kr328.clash.design.R.string.network_error))
                design.setLoading(false)
            }
            else -> {
                design.setGeneralError(getString(com.github.kr328.clash.design.R.string.order_failed))
                design.setLoading(false)
            }
        }
    }

    private fun buildPeriodList(plan: Plan): List<Pair<String, String>> {
        val periods = mutableListOf<Pair<String, String>>()
        plan.monthPrice?.let {
            periods.add("month_price" to getString(com.github.kr328.clash.design.R.string.monthly) + " - " + formatCurrency(it))
        }
        plan.quarterPrice?.let {
            periods.add("quarter_price" to getString(com.github.kr328.clash.design.R.string.quarterly) + " - " + formatCurrency(it))
        }
        plan.halfYearPrice?.let {
            periods.add("half_year_price" to getString(com.github.kr328.clash.design.R.string.semi_annually) + " - " + formatCurrency(it))
        }
        plan.yearPrice?.let {
            periods.add("year_price" to getString(com.github.kr328.clash.design.R.string.annually) + " - " + formatCurrency(it))
        }
        plan.twoYearPrice?.let {
            periods.add("two_year_price" to getString(com.github.kr328.clash.design.R.string.two_years) + " - " + formatCurrency(it))
        }
        plan.threeYearPrice?.let {
            periods.add("three_year_price" to getString(com.github.kr328.clash.design.R.string.three_years) + " - " + formatCurrency(it))
        }
        plan.onetimePrice?.let {
            periods.add("onetime_price" to getString(com.github.kr328.clash.design.R.string.one_time) + " - " + formatCurrency(it))
        }
        return periods
    }

    private fun formatCurrency(cents: Long): String {
        return String.format("$%.2f", cents / 100.0)
    }
}
