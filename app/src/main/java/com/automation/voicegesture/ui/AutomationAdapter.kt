package com.automation.voicegesture.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.automation.voicegesture.data.Automation
import com.automation.voicegesture.databinding.ItemAutomationBinding

class AutomationAdapter(
    private val onDeleteClick: (Automation) -> Unit
) : ListAdapter<Automation, AutomationAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAutomationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAutomationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(automation: Automation) {
            binding.triggerWordText.text = automation.triggerWord
            binding.deleteButton.setOnClickListener { onDeleteClick(automation) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Automation>() {
            override fun areItemsTheSame(oldItem: Automation, newItem: Automation) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Automation, newItem: Automation) =
                oldItem == newItem
        }
    }
}
