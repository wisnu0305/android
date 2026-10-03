package com.example.projectorremote.ui.debug

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.projectorremote.data.debug.DebugLogEntity
import com.example.projectorremote.databinding.ItemDebugLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DebugLogAdapter(
    private val dateFormat: SimpleDateFormat
) : ListAdapter<DebugLogEntity, DebugLogAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDebugLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemDebugLogBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DebugLogEntity) {
            binding.debugTimestamp.text = dateFormat.format(Date(item.timestamp))
            binding.debugRawSpeech.text = item.rawSpeech.ifEmpty { "—" }
            binding.debugNormalized.text = item.normalizedText.ifEmpty { "—" }
            binding.debugCommand.text = item.matchedCommand
            binding.debugEsp32Code.text = item.esp32Code.ifEmpty { "—" }
            binding.debugRequestUrl.text = item.requestUrl ?: "—"
            binding.debugResponse.text = item.responseBody ?: item.errorMessage ?: "—"
            binding.debugSuccess.text = if (item.success) "Ya" else "Tidak"
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<DebugLogEntity>() {
        override fun areItemsTheSame(a: DebugLogEntity, b: DebugLogEntity) = a.id == b.id
        override fun areContentsTheSame(a: DebugLogEntity, b: DebugLogEntity) = a == b
    }
}
