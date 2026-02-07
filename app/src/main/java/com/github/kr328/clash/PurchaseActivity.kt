package com.github.kr328.clash

import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.design.PurchaseDesign
import com.github.kr328.clash.service.v2board.ApiResult
import com.github.kr328.clash.service.v2board.V2BoardRepository
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class PurchaseActivity : BaseActivity<PurchaseDesign>() {
    private val repository by lazy { V2BoardRepository(this) }

    override suspend fun main() {
        val design = PurchaseDesign(this)
        setContentDesign(design)

        design.setLoading(true)

        when (val result = repository.getPlans()) {
            is ApiResult.Success -> {
                design.setPlans(result.data)
            }
            else -> {}
        }

        design.setLoading(false)

        while (isActive) {
            select<Unit> {
                events.onReceive { }
                design.requests.onReceive {
                    when (it) {
                        is PurchaseDesign.Request.SelectPlan -> {
                            startActivity(
                                CheckoutActivity::class.intent.apply {
                                    putExtra("plan_id", it.plan.id)
                                    putExtra("plan_name", it.plan.name)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
