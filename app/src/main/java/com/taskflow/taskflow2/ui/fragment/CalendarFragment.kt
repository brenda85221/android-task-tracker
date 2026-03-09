package com.taskflow.taskflow2.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taskflow.taskflow2.R
import com.taskflow.taskflow2.data.local.TaskDatabase
import com.taskflow.taskflow2.data.local.TaskWithColor
import com.taskflow.taskflow2.ui.adapter.TaskAdapter
import com.taskflow.taskflow2.ui.dialog.CreateTaskDialogFragment
import com.taskflow.taskflow2.ui.dialog.TaskImageDialog
import com.taskflow.taskflow2.util.TaskSwipeCallback
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

    private lateinit var rvTasks: RecyclerView
    private lateinit var tvMonthYear: TextView
    private lateinit var btnPrevMonth: Button
    private lateinit var btnNextMonth: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvTasks = view.findViewById(R.id.rvTasksInMonth)
        tvMonthYear = view.findViewById(R.id.tvMonthYear)
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth)
        btnNextMonth = view.findViewById(R.id.btnNextMonth)

        setupRecyclerView()
        setupMonthNavigation()
        updateMonthView()
        observeTasks()
    }

    // ---------------- RecyclerView + Swipe ----------------
    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter()
        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        rvTasks.adapter = taskAdapter

        taskAdapter.onItemClick = { taskWithColor ->
            val task = taskWithColor.task
            if (!task.imageUri.isNullOrEmpty()) {
                TaskImageDialog.show(requireContext(), task, viewLifecycleOwner.lifecycleScope) {
                    observeTasks()
                }
            }
        }

        taskAdapter.onEdit = { taskWithColor ->
            val dialog = CreateTaskDialogFragment.newInstance(taskWithColor)
            dialog.onSave = { observeTasks() }
            dialog.show(parentFragmentManager, "EditTaskDialog")
        }

        taskAdapter.onToggle = { taskId, isCompleted ->
            lifecycleScope.launch {
                val current = taskDao.getTaskById(taskId)?.task?.copy(isCompleted = isCompleted)
                current?.let { taskDao.updateTask(it) }
            }
        }

        // Swipe to delete
        val swipeHandler = TaskSwipeCallback { position ->
            val task = taskAdapter.currentList[position]
            showDeleteConfirmDialog(task, position)
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(rvTasks)
    }

    // ---------------- Delete Confirm ----------------
    private fun showDeleteConfirmDialog(task: TaskWithColor, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("刪除任務")
            .setMessage("確定刪除「${task.task.title}」嗎？")
            .setPositiveButton("刪除") { _, _ ->
                lifecycleScope.launch {
                    taskDao.deleteTask(task.task)
                }
            }
            .setNegativeButton("取消") { _, _ ->
                taskAdapter.notifyItemChanged(position)
            }
            .show()
    }

    // ---------------- Month Navigation ----------------
    private fun setupMonthNavigation() {
        btnPrevMonth.setOnClickListener {
            currentDate.add(Calendar.MONTH, -1)
            updateMonthView()
            observeTasks()
        }

        btnNextMonth.setOnClickListener {
            currentDate.add(Calendar.MONTH, 1)
            updateMonthView()
            observeTasks()
        }
    }

    private fun updateMonthView() {
        val sdf = java.text.SimpleDateFormat("yyyy 年 MM 月", Locale.getDefault())
        tvMonthYear.text = sdf.format(currentDate.time)
    }

    // ---------------- Task Flow ----------------
    private fun observeTasks() {
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
                }.sortedWith(compareBy<TaskWithColor> { it.task.isCompleted }.thenBy { it.task.dueAt })

                taskAdapter.submitList(filtered)
            }
        }
    }
}