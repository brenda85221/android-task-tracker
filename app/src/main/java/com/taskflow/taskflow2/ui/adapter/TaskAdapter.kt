package com.taskflow.taskflow2.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.taskflow.taskflow2.R
import com.taskflow.taskflow2.data.local.TaskEntity

class TaskAdapter :
    ListAdapter<TaskEntity, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    var onToggle: ((TaskEntity) -> Unit)? = null
    var onEdit: ((TaskEntity) -> Unit)? = null
    var onDelete: ((Long) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cbCompleted: CheckBox = itemView.findViewById(R.id.cbCompleted)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDue: TextView = itemView.findViewById(R.id.tvDue)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        private val ivTaskImage: ImageView = itemView.findViewById(R.id.ivTaskImage)

        fun bind(task: TaskEntity) {
            tvTitle.text = task.title
            tvDue.text = task.dueTime
            cbCompleted.isChecked = task.isCompleted

            // ---------- 背景色 & 文字對比 ----------
            try {
                val bgColor = Color.parseColor(task.colorTag)
                itemView.setBackgroundColor(bgColor)

                val luminance =
                    (Color.red(bgColor) * 0.299 +
                            Color.green(bgColor) * 0.587 +
                            Color.blue(bgColor) * 0.114) / 255

                val textColor =
                    if (luminance > 0.5) Color.BLACK else Color.WHITE

                tvTitle.setTextColor(textColor)
                tvDue.setTextColor(textColor)
            } catch (e: Exception) {
                tvTitle.setTextColor(Color.BLACK)
                tvDue.setTextColor(Color.BLACK)
            }

            // ---------- 圖片處理（重點修正） ----------
            if (!task.imageUri.isNullOrEmpty()) {
                ivTaskImage.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(task.imageUri)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(ivTaskImage)

            } else {
                Glide.with(itemView.context).clear(ivTaskImage)
                ivTaskImage.visibility = View.GONE
            }

            // ---------- 事件 ----------
            cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                onToggle?.invoke(task.copy(isCompleted = isChecked))
            }

            btnEdit.setOnClickListener { onEdit?.invoke(task) }
            btnDelete.setOnClickListener { onDelete?.invoke(task.id) }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<TaskEntity>() {
        override fun areItemsTheSame(old: TaskEntity, new: TaskEntity) =
            old.id == new.id

        override fun areContentsTheSame(old: TaskEntity, new: TaskEntity) =
            old == new
    }
}
