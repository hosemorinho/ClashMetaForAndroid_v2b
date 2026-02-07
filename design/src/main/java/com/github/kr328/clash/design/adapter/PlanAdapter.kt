package com.github.kr328.clash.design.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.databinding.AdapterPlanBinding
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.service.v2board.Plan

class PlanAdapter(
    private val context: Context,
    private val onClicked: (Plan) -> Unit
) : RecyclerView.Adapter<PlanAdapter.Holder>() {
    class Holder(val binding: AdapterPlanBinding) : RecyclerView.ViewHolder(binding.root)

    var plans: List<Plan> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            AdapterPlanBinding
                .inflate(context.layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val plan = plans[position]
        holder.binding.plan = plan
        holder.binding.bandwidthText = formatBandwidth(plan.transferEnable)
        holder.binding.priceText = formatPrice(plan)
        holder.binding.root.setOnClickListener { onClicked(plan) }
    }

    override fun getItemCount(): Int = plans.size

    private fun formatBandwidth(bytes: Long): String {
        val gb = bytes.toDouble() / (1024 * 1024 * 1024)
        return if (gb >= 1) {
            String.format("%.0f GB", gb)
        } else {
            val mb = bytes.toDouble() / (1024 * 1024)
            String.format("%.0f MB", mb)
        }
    }

    private fun formatPrice(plan: Plan): String {
        val prices = mutableListOf<String>()
        plan.monthPrice?.let { prices.add(context.getString(R.string.format_price_month, formatCurrency(it))) }
        plan.quarterPrice?.let { prices.add(context.getString(R.string.format_price_quarter, formatCurrency(it))) }
        plan.yearPrice?.let { prices.add(context.getString(R.string.format_price_year, formatCurrency(it))) }
        plan.onetimePrice?.let { prices.add(context.getString(R.string.format_price_onetime, formatCurrency(it))) }
        return prices.firstOrNull() ?: context.getString(R.string.free)
    }

    private fun formatCurrency(cents: Long): String {
        return String.format("%.2f", cents / 100.0)
    }
}
