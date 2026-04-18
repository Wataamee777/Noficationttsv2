package com.example.notificationttsv2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notificationttsv2.databinding.ItemAppBinding

/**
 * 表示用のアプリ情報モデル。
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable,
    val isSystemApp: Boolean,
    val enabled: Boolean
)

/**
 * アプリ一覧を表示するRecyclerViewアダプター。
 */
class AppAdapter(
    private val onToggleChanged: (AppInfo, Boolean) -> Unit
) : ListAdapter<AppInfo, AppAdapter.AppViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppViewHolder(
        private val binding: ItemAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppInfo) {
            binding.iconView.setImageDrawable(item.icon)
            binding.nameView.text = item.appName

            binding.switchView.setOnCheckedChangeListener(null)
            binding.switchView.isChecked = item.enabled
            binding.switchView.setOnCheckedChangeListener { _, isChecked ->
                onToggleChanged(item, isChecked)
            }

            // 行自体をタップしてもON/OFFしやすくする。
            binding.root.setOnClickListener {
                binding.switchView.toggle()
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem == newItem
        }
    }
}
