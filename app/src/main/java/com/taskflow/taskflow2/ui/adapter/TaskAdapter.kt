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
    var onToggle: ((Long, Boolean) -> Unit)? = null  // ✅ id + 新狀態

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
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        private val tvImageHint: TextView = itemView.findViewById(R.id.tvImageHint)

        fun bind(taskWithColor: TaskWithColor) {
            val task = taskWithColor.task
            val colorTag = taskWithColor.color?.colorTag ?: "#2196F3"

            tvTitle.text = task.title
            tvDate.text = task.dueAt.toFormattedDate()

            // ✅ 移除舊 listener，避免亂勾選
            cbCompleted.setOnCheckedChangeListener(null)
            cbCompleted.isChecked = task.isCompleted
            cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != task.isCompleted) {
                    onToggle?.invoke(task.id, isChecked)
                }
            }

            // 顯示是否有圖片的提示
            tvImageHint.visibility = if (!task.imageUri.isNullOrEmpty()) View.VISIBLE else View.GONE

            // 背景色 & 文字顏色對比
            try {
                val bgColor = Color.parseColor(colorTag)
                itemView.setBackgroundColor(bgColor)
                val luminance =
                    (Color.red(bgColor) * 0.299 + Color.green(bgColor) * 0.587 + Color.blue(bgColor) * 0.114) / 255
                val textColor = if (luminance > 0.5) Color.BLACK else Color.WHITE
                tvTitle.setTextColor(textColor)
                tvDate.setTextColor(textColor)
            } catch (e: Exception) {
                tvTitle.setTextColor(Color.BLACK)
                tvDate.setTextColor(Color.BLACK)
            }

            btnEdit.setOnClickListener { onEdit?.invoke(taskWithColor) }
            btnDelete.setOnClickListener { onDelete?.invoke(taskWithColor) }

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