package com.github.kr328.clash.design

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.design.adapter.ServerNodeAdapter
import com.github.kr328.clash.design.databinding.DesignDashboardBinding
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import com.github.kr328.clash.service.v2board.ServerNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DashboardDesign(context: Context) : Design<DashboardDesign.Request>(context) {
    enum class Request {
        OpenPurchase,
    }

    private val binding = DesignDashboardBinding
        .inflate(context.layoutInflater, context.root, false)

    private val adapter = ServerNodeAdapter(context)

    override val root: View
        get() = binding.root

    init {
        binding.self = this
        binding.serverList.layoutManager = LinearLayoutManager(context)
        binding.serverList.adapter = adapter
    }

    fun requestPurchase() {
        requests.trySend(Request.OpenPurchase)
    }

    suspend fun setLoading(loading: Boolean) {
        withContext(Dispatchers.Main) {
            binding.loading = loading
        }
    }

    suspend fun setPlanName(name: String?) {
        withContext(Dispatchers.Main) {
            binding.planName = name
        }
    }

    suspend fun setDataUsage(used: String, total: String, percent: Int) {
        withContext(Dispatchers.Main) {
            binding.dataUsed = used
            binding.dataTotal = total
            binding.dataPercent = percent
        }
    }

    suspend fun setExpiryDate(date: String?) {
        withContext(Dispatchers.Main) {
            binding.expiryDate = date
        }
    }

    suspend fun setServerNodes(nodes: List<ServerNode>) {
        withContext(Dispatchers.Main) {
            adapter.nodes = nodes
            binding.serverCount = nodes.size
            adapter.notifyDataSetChanged()
        }
    }
}
