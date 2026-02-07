package com.github.kr328.clash

import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.design.DashboardDesign
import com.github.kr328.clash.service.v2board.ApiResult
import com.github.kr328.clash.service.v2board.V2BoardRepository
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : BaseActivity<DashboardDesign>() {
    private val repository by lazy { V2BoardRepository(this) }

    override suspend fun main() {
        val design = DashboardDesign(this)
        setContentDesign(design)

        design.setLoading(true)
        fetchDashboardData(design)
        design.setLoading(false)

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ActivityStart -> {
                            design.setLoading(true)
                            fetchDashboardData(design)
                            design.setLoading(false)
                        }
                        else -> Unit
                    }
                }
                design.requests.onReceive {
                    when (it) {
                        DashboardDesign.Request.OpenPurchase ->
                            startActivity(PurchaseActivity::class.intent)
                    }
                }
            }
        }
    }

    private suspend fun fetchDashboardData(design: DashboardDesign) {
        // Fetch subscription info
        when (val result = repository.getSubscribeUrl()) {
            is ApiResult.Success -> {
                val data = result.data
                val used = data.u + data.d
                val total = data.transferEnable
                val percent = if (total > 0) ((used.toDouble() / total) * 100).toInt().coerceIn(0, 100) else 0

                design.setDataUsage(
                    formatBytes(used),
                    formatBytes(total),
                    percent
                )

                design.setExpiryDate(
                    data.expiredAt?.let { formatTimestamp(it) }
                )
            }
            else -> {}
        }

        // Fetch plans to get current plan name
        when (val userResult = repository.getUserInfo()) {
            is ApiResult.Success -> {
                val planId = userResult.data.planId
                if (planId != null) {
                    when (val plansResult = repository.getPlans()) {
                        is ApiResult.Success -> {
                            val plan = plansResult.data.find { it.id == planId }
                            design.setPlanName(plan?.name)
                        }
                        else -> {}
                    }
                }
            }
            else -> {}
        }

        // Fetch server nodes
        when (val result = repository.getServers()) {
            is ApiResult.Success -> {
                design.setServerNodes(result.data)
            }
            else -> {}
        }
    }

    private fun formatBytes(bytes: Long): String {
        val gb = bytes.toDouble() / (1024 * 1024 * 1024)
        return if (gb >= 1) {
            String.format("%.1f GB", gb)
        } else {
            val mb = bytes.toDouble() / (1024 * 1024)
            String.format("%.1f MB", mb)
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val ts = if (timestamp < 1_000_000_000_000L) timestamp * 1000 else timestamp
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(ts))
    }
}
