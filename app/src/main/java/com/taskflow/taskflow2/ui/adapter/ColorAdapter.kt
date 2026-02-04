package com.taskflow.taskflow2.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.taskflow.taskflow2.R
import com.taskflow.taskflow2.data.local.TaskColor

class ColorAdapter(
    val onEdit: (TaskColor) -> Unit
) : ListAdapter<TaskColor, ColorAdapter.ColorViewHolder>(ColorDiffCallback()) {  // ← 加上 <TaskColor, ColorViewHolder>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvColorName = itemView.findViewById<TextView>(R.id.tvColorName)
        private val viewColor = itemView.findViewById<View>(R.id.viewColor)

        fun bind(color: TaskColor) {
            tvColorName.text = "${color.colorName} (${color.colorTag})"
            try {
                viewColor.setBackgroundColor(Color.parseColor(color.colorTag))
            } catch (e: Exception) {
                // 無效顏色代碼，保持預設顏色
                viewColor.setBackgroundColor(Color.BLUE)
            }
            itemView.setOnClickListener { onEdit(color) }
        }
    }

    class ColorDiffCallback : DiffUtil.ItemCallback<TaskColor>() {
        override fun areItemsTheSame(old: TaskColor, new: TaskColor) = old.id == new.id
        override fun areContentsTheSame(old: TaskColor, new: TaskColor) = old == new
    }
}
