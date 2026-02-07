package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.design.adapter.PlanAdapter
import com.github.kr328.clash.design.databinding.DesignPurchaseBinding
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import com.github.kr328.clash.service.v2board.Plan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PurchaseDesign(context: Context) : Design<PurchaseDesign.Request>(context) {
    sealed class Request {
        data class SelectPlan(val plan: Plan) : Request()
    }

    private val binding = DesignPurchaseBinding
        .inflate(context.layoutInflater, context.root, false)

    private val adapter = PlanAdapter(context) { plan ->
        requests.trySend(Request.SelectPlan(plan))
    }

    override val root: View
        get() = binding.root

    init {
        binding.self = this
        binding.planList.layoutManager = LinearLayoutManager(context)
        binding.planList.adapter = adapter
    }

    suspend fun setLoading(loading: Boolean) {
        withContext(Dispatchers.Main) {
            binding.loading = loading
        }
    }

    suspend fun setPlans(plans: List<Plan>) {
        withContext(Dispatchers.Main) {
            adapter.plans = plans
            adapter.notifyDataSetChanged()
        }
    }
}
