package com.taskflow.taskflow2.ui.adapter

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

class TaskAdapter : ListAdapter<TaskEntity, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    // ✅ 3個 Callback
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

        fun bind(task: TaskEntity) {
            tvTitle.text = task.title
            tvDue.text = task.dueTime

            cbCompleted.isChecked = task.isCompleted

            cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                onToggle?.invoke(task.copy(isCompleted = isChecked))
            }

            btnEdit.setOnClickListener { onEdit?.invoke(task) }
            btnDelete.setOnClickListener { onDelete?.invoke(task.id) }
        }
    }

    // ✅ DiffUtil 避免閃爍
    class TaskDiffCallback : DiffUtil.ItemCallback<TaskEntity>() {
        override fun areItemsTheSame(old: TaskEntity, new: TaskEntity) =
            old.id == new.id

        override fun areContentsTheSame(old: TaskEntity, new: TaskEntity) =
            old == new
    }
}
