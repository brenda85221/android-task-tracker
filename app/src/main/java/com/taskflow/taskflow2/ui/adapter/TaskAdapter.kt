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
import com.taskflow.taskflow2.data.local.TaskEntity
import com.taskflow.taskflow2.util.toFormattedDate

class TaskAdapter : ListAdapter<TaskEntity, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    var onItemClick: ((TaskEntity) -> Unit)? = null
    var onEdit: ((TaskEntity) -> Unit)? = null
    var onDelete: ((TaskEntity) -> Unit)? = null
    var onToggle: ((TaskEntity) -> Unit)? = null

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
//        private val tvDue: TextView = itemView.findViewById(R.id.tvDue)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val cbCompleted: CheckBox = itemView.findViewById(R.id.cbCompleted)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        private val tvImageHint: TextView = itemView.findViewById(R.id.tvImageHint)
        fun bind(task: TaskEntity) {
            tvTitle.text = task.title
            tvDate.text = task.dueAt.toFormattedDate()
//            tvDate.text = task.dueDate
            cbCompleted.isChecked = task.isCompleted

            // 顯示是否有圖片的提示
            tvImageHint.visibility = if (!task.imageUri.isNullOrEmpty()) View.VISIBLE else View.GONE


            // 背景色 & 文字顏色對比
            try {
                val bgColor = Color.parseColor(task.colorTag)
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

            // ----------- 事件監聽 -----------
            cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                onToggle?.invoke(task.copy(isCompleted = isChecked))
            }

            btnEdit.setOnClickListener { onEdit?.invoke(task) }
            btnDelete.setOnClickListener { onDelete?.invoke(task) }

            // item click → 只有有圖片才觸發
            itemView.setOnClickListener {
                if (!task.imageUri.isNullOrEmpty()) {
                    onItemClick?.invoke(task)
                }
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<TaskEntity>() {
        override fun areItemsTheSame(old: TaskEntity, new: TaskEntity) = old.id == new.id
        override fun areContentsTheSame(old: TaskEntity, new: TaskEntity) = old == new
    }
}
