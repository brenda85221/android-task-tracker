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

class ColorGridAdapter(
    private val onClick: (TaskColor) -> Unit
) : ListAdapter<TaskColor, ColorGridAdapter.ColorViewHolder>(ColorDiffCallback()) {

    inner class ColorViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(color: TaskColor) {
            // 顯示顏色名稱
            val tvName = view.findViewById<TextView>(R.id.tvColorName)
            tvName.text = color.colorName

            // 設定色塊背景
            val colorBlock = view.findViewById<View>(R.id.viewColor)
            try {
                colorBlock.setBackgroundColor(Color.parseColor(color.colorTag))
            } catch (e: Exception) {
                colorBlock.setBackgroundColor(Color.GRAY)
            }

            // 點擊事件：直接回傳 color，外部 Fragment 處理彈窗
            view.setOnClickListener { onClick(color) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ColorDiffCallback : DiffUtil.ItemCallback<TaskColor>() {
        override fun areItemsTheSame(old: TaskColor, new: TaskColor) = old.id == new.id
        override fun areContentsTheSame(old: TaskColor, new: TaskColor) = old == new
    }
}