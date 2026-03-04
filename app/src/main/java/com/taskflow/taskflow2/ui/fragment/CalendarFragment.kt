package com.taskflow.taskflow2.ui.fragment

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taskflow.taskflow2.R
import com.taskflow.taskflow2.data.local.TaskDatabase
import com.taskflow.taskflow2.data.local.TaskWithColor
import com.taskflow.taskflow2.ui.adapter.TaskAdapter
import com.taskflow.taskflow2.ui.dialog.TaskImageDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    private val db by lazy { TaskDatabase.getInstance(requireContext()) }
    private val taskDao by lazy { db.taskDao() }
    private lateinit var taskAdapter: TaskAdapter
    private var currentDate = Calendar.getInstance()
    private var selectedColorFilter: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvMonthYear = view.findViewById<TextView>(R.id.tvMonthYear)
        val btnPrevMonth = view.findViewById<Button>(R.id.btnPrevMonth)
        val btnNextMonth = view.findViewById<Button>(R.id.btnNextMonth)
        val rvTasks = view.findViewById<RecyclerView>(R.id.rvTasksInMonth)

        taskAdapter = TaskAdapter()
        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        rvTasks.adapter = taskAdapter

        // ---------------- TaskAdapter Callback ----------------
        taskAdapter.onItemClick = { taskWithColor ->
            val task = taskWithColor.task
            if (!task.imageUri.isNullOrEmpty()) {
                TaskImageDialog.show(requireContext(), task, viewLifecycleOwner.lifecycleScope) {
                    refreshTaskList()
                }
            }
        }

        taskAdapter.onEdit = { /* TODO: 可呼叫 CreateTaskDialogFragment 編輯 */ }

        taskAdapter.onDelete = { taskWithColor ->
            lifecycleScope.launch {
                taskDao.deleteTask(taskWithColor.task)
                refreshTaskList()
            }
        }

        // 切換完成 / 未完成
        taskAdapter.onToggle = { taskId, isCompleted ->
            lifecycleScope.launch {
                val currentTask = taskDao.getTaskById(taskId)?.task?.copy(isCompleted = isCompleted)
                currentTask?.let { taskDao.updateTask(it) }
                // ✅ 滾動到最後一個項目（已完成任務移到底）
                rvTasks.post {
                    val lastIndex = taskAdapter.itemCount - 1
                    if (lastIndex >= 0) {
                        (rvTasks.layoutManager as? LinearLayoutManager)?.scrollToPosition(lastIndex)
                    }
                }
            }
        }

        // ---------------- 月份切換 ----------------
        btnPrevMonth.setOnClickListener {
            currentDate.add(Calendar.MONTH, -1)
            updateMonthView(tvMonthYear)
            refreshTaskList()
        }

        btnNextMonth.setOnClickListener {
            currentDate.add(Calendar.MONTH, 1)
            updateMonthView(tvMonthYear)
            refreshTaskList()
        }

        updateMonthView(tvMonthYear)
        refreshTaskList()
    }

    private fun updateMonthView(tv: TextView) {
        val sdf = java.text.SimpleDateFormat("yyyy 年 MM 月", Locale.getDefault())
        tv.text = sdf.format(currentDate.time)
    }

    private fun refreshTaskList() {
        lifecycleScope.launch {
            taskDao.getAllTasks().collectLatest { tasks ->
                val monthStart = Calendar.getInstance().apply {
                    time = currentDate.time
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val monthEnd = Calendar.getInstance().apply {
                    time = currentDate.time
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }

                val filtered = tasks.filter { taskWithColor ->
                    val task = taskWithColor.task
                    val inMonth = task.dueAt in monthStart.timeInMillis..monthEnd.timeInMillis
                    val colorMatch = selectedColorFilter?.let { filter -> taskWithColor.color?.colorTag == filter } ?: true
                    inMonth && colorMatch
                }
                    // ✅ 未完成任務在前，已完成任務在後，保持 dueAt 排序
                    .sortedWith(compareBy<TaskWithColor> { it.task.isCompleted }.thenBy { it.task.dueAt })

                taskAdapter.submitList(filtered)
            }
        }
    }
}