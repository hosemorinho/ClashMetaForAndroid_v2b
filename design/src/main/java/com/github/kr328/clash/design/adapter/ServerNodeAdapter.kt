package com.github.kr328.clash.design.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.design.databinding.AdapterServerNodeBinding
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.service.v2board.ServerNode

class ServerNodeAdapter(
    private val context: Context
) : RecyclerView.Adapter<ServerNodeAdapter.Holder>() {
    class Holder(val binding: AdapterServerNodeBinding) : RecyclerView.ViewHolder(binding.root)

    var nodes: List<ServerNode> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            AdapterServerNodeBinding
                .inflate(context.layoutInflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.node = nodes[position]
    }

    override fun getItemCount(): Int = nodes.size
}
