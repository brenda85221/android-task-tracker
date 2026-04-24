package com.taskflow.taskflow2.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.taskflow.taskflow2.R
import com.taskflow.taskflow2.data.local.TaskWithColor
import com.taskflow.taskflow2.util.toFormattedDate

class TaskAdapter : ListAdapter<TaskWithColor, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    var onItemClick: ((TaskWithColor) -> Unit)? = null
    var onEdit: ((TaskWithColor) -> Unit)? = null
    var onDelete: ((TaskWithColor) -> Unit)? = null
    var onToggle: ((Long, Boolean) -> Unit)? = null  // id + 新狀態

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val cbCompleted: CheckBox = itemView.findViewById(R.id.cbCompleted)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val tvImageHint: TextView = itemView.findViewById(R.id.tvImageHint)
        private val colorIndicator: View = itemView.findViewById(R.id.colorIndicator) // 新增左邊條

        fun bind(taskWithColor: TaskWithColor) {
            val task = taskWithColor.task
            val colorTag = taskWithColor.color?.colorTag ?: "#D3D3D3" //任務種類為null時，為灰色

            tvTitle.text = task.title
            tvDate.text = task.dueAt.toFormattedDate()

            // ✅ 避免 RecyclerView 重用導致亂勾選
            cbCompleted.setOnCheckedChangeListener(null)
            cbCompleted.isChecked = task.isCompleted
            cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != task.isCompleted) {
                    onToggle?.invoke(task.id, isChecked)
                }
            }

            // 顯示是否有圖片提示
            tvImageHint.visibility = if (!task.imageUri.isNullOrEmpty()) View.VISIBLE else View.GONE

            // 只改左邊條顏色，不影響整個 item 背景
            try {
                val indicatorColor = Color.parseColor(colorTag)
                colorIndicator.setBackgroundColor(indicatorColor)

                // 文字顏色保持黑色，背景透明
                tvTitle.setTextColor(Color.BLACK)
                tvDate.setTextColor(Color.DKGRAY)
            } catch (e: Exception) {
                colorIndicator.setBackgroundColor(Color.GRAY)
                tvTitle.setTextColor(Color.BLACK)
                tvDate.setTextColor(Color.DKGRAY)
            }

            // 編輯按鈕
            btnEdit.setOnClickListener { onEdit?.invoke(taskWithColor) }

            // item click → 只有有圖片才觸發
            itemView.setOnClickListener {
                if (!task.imageUri.isNullOrEmpty()) {
                    onItemClick?.invoke(taskWithColor)
                }
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<TaskWithColor>() {
        override fun areItemsTheSame(old: TaskWithColor, new: TaskWithColor) =
            old.task.id == new.task.id

        override fun areContentsTheSame(old: TaskWithColor, new: TaskWithColor) =
            old == new
    }
}