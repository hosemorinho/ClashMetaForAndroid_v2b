package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import com.github.kr328.clash.design.databinding.DesignCheckoutBinding
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import com.github.kr328.clash.service.v2board.PaymentMethod
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CheckoutDesign(context: Context) : Design<CheckoutDesign.Request>(context) {
    sealed class Request {
        data class Checkout(val period: String, val couponCode: String?, val paymentMethodId: Int) : Request()
    }

    private val binding = DesignCheckoutBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    private var selectedPeriod: String = "month_price"
    private var selectedPaymentMethodId: Int = 0
    private val periodMap = mutableMapOf<Int, String>()
    private val paymentMap = mutableMapOf<Int, Int>()

    init {
        binding.self = this
    }

    fun requestCheckout() {
        val coupon = binding.couponInput.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
        requests.trySend(Request.Checkout(selectedPeriod, coupon, selectedPaymentMethodId))
    }

    suspend fun setLoading(loading: Boolean) {
        withContext(Dispatchers.Main) {
            binding.loading = loading
        }
    }

    suspend fun setPlanName(name: String) {
        withContext(Dispatchers.Main) {
            binding.planName = name
        }
    }

    suspend fun setGeneralError(error: String?) {
        withContext(Dispatchers.Main) {
            binding.generalError = error
        }
    }

    suspend fun setPeriods(periods: List<Pair<String, String>>) {
        withContext(Dispatchers.Main) {
            binding.periodChips.removeAllViews()
            periodMap.clear()
            periods.forEachIndexed { index, (key, label) ->
                val chip = Chip(context).apply {
                    text = label
                    isCheckable = true
                    id = View.generateViewId()
                }
                periodMap[chip.id] = key
                binding.periodChips.addView(chip)
                if (index == 0) {
                    chip.isChecked = true
                    selectedPeriod = key
                    binding.selectedPeriod = label
                }
            }
            binding.periodChips.setOnCheckedStateChangeListener { _, checkedIds ->
                if (checkedIds.isNotEmpty()) {
                    val checkedId = checkedIds[0]
                    periodMap[checkedId]?.let { selectedPeriod = it }
                    val chip = binding.periodChips.findViewById<Chip>(checkedId)
                    binding.selectedPeriod = chip?.text?.toString()
                }
            }
        }
    }

    suspend fun setPaymentMethods(methods: List<PaymentMethod>) {
        withContext(Dispatchers.Main) {
            binding.paymentChips.removeAllViews()
            paymentMap.clear()
            methods.forEachIndexed { index, method ->
                val chip = Chip(context).apply {
                    text = method.name
                    isCheckable = true
                    id = View.generateViewId()
                }
                paymentMap[chip.id] = method.id
                binding.paymentChips.addView(chip)
                if (index == 0) {
                    chip.isChecked = true
                    selectedPaymentMethodId = method.id
                }
            }
            binding.paymentChips.setOnCheckedStateChangeListener { _, checkedIds ->
                if (checkedIds.isNotEmpty()) {
                    paymentMap[checkedIds[0]]?.let { selectedPaymentMethodId = it }
                }
            }
        }
    }
}
