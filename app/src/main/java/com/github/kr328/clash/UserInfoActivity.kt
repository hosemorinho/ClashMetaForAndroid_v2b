package com.github.kr328.clash

import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.design.UserInfoDesign
import com.github.kr328.clash.service.v2board.ApiResult
import com.github.kr328.clash.service.v2board.V2BoardRepository
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import java.text.SimpleDateFormat
import java.util.*

class UserInfoActivity : BaseActivity<UserInfoDesign>() {
    private val repository by lazy { V2BoardRepository(this) }

    override suspend fun main() {
        val design = UserInfoDesign(this)
        setContentDesign(design)

        design.setLoading(true)
        fetchUserInfo(design)
        design.setLoading(false)

        while (isActive) {
            select<Unit> {
                events.onReceive { }
                design.requests.onReceive {
                    when (it) {
                        UserInfoDesign.Request.Logout -> {
                            repository.logout()
                            val result = startActivityForResult(
                                androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                                LoginActivity::class.intent
                            )
                            if (result.resultCode == RESULT_OK) {
                                design.setLoading(true)
                                fetchUserInfo(design)
                                design.setLoading(false)
                            } else {
                                finishAffinity()
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun fetchUserInfo(design: UserInfoDesign) {
        design.setEmail(repository.getUserEmail())

        when (val result = repository.getUserInfo()) {
            is ApiResult.Success -> {
                val info = result.data
                val used = info.u + info.d
                val total = info.transferEnable

                design.setDataUsage(formatBytes(used), formatBytes(total))
                design.setExpiryDate(info.expiredAt?.let { formatTimestamp(it) })
                design.setBalance(formatCurrency(info.balance))

                if (info.planId != null) {
                    when (val plansResult = repository.getPlans()) {
                        is ApiResult.Success -> {
                            val plan = plansResult.data.find { it.id == info.planId }
                            design.setPlanName(plan?.name)
                        }
                        else -> {}
                    }
                }
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

    private fun formatCurrency(cents: Long): String {
        return String.format("%.2f", cents / 100.0)
    }
}
