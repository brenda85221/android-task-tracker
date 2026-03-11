package com.taskflow.taskflow2.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taskflow.taskflow2.R
import com.taskflow.taskflow2.data.local.TaskDatabase
import com.taskflow.taskflow2.data.local.TaskWithColor
import com.taskflow.taskflow2.data.repository.TaskRepository
import com.taskflow.taskflow2.ui.adapter.TaskAdapter
import com.taskflow.taskflow2.ui.dialog.CreateTaskDialogFragment
import com.taskflow.taskflow2.ui.dialog.TaskImageDialog
import com.taskflow.taskflow2.util.TaskSwipeCallback
import com.taskflow.taskflow2.viewmodel.TaskViewModel
import com.taskflow.taskflow2.viewmodel.TaskViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import android.graphics.drawable.GradientDrawable
import android.app.AlertDialog

class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    // ---------------- ViewModel ----------------
    private val viewModel: TaskViewModel by viewModels {
        TaskViewModelFactory(
            TaskRepository(
                TaskDatabase.getInstance(requireContext()).taskDao()
            )
        )
    }

    // ---------------- UI ----------------
    private lateinit var rvTasks: RecyclerView
    private lateinit var tvMonthYear: TextView
    private lateinit var btnPrevMonth: Button
    private lateinit var btnNextMonth: Button
    private lateinit var colorFilterContainer: LinearLayout

    private lateinit var taskAdapter: TaskAdapter
    private var currentDate = Calendar.getInstance()
    private var selectedColorFilter: String? = null

    // ---------------- Lifecycle ----------------
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvTasks = view.findViewById(R.id.rvTasksInMonth)
        tvMonthYear = view.findViewById(R.id.tvMonthYear)
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth)
        btnNextMonth = view.findViewById(R.id.btnNextMonth)
        colorFilterContainer = view.findViewById(R.id.colorFilterContainer)

        setupRecyclerView()
        setupMonthNavigation()
        setupColorFilter()
        updateMonthView()
        observeTasks()
    }

    // ---------------- RecyclerView ----------------
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
            val taskWithColor = taskAdapter.currentList.find { it.task.id == taskId }
            taskWithColor?.let { viewModel.toggleTaskCompleted(it, isCompleted) }
        }

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
                viewLifecycleOwner.lifecycleScope.launch { viewModel.deleteTask(task.task) }
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

    // ---------------- Observe Tasks ----------------
    private fun observeTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tasks.collectLatest { tasks ->
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
                    val colorMatch =
                        selectedColorFilter?.let { filter -> taskWithColor.color?.colorTag == filter } ?: true
                    inMonth && colorMatch
                }.sortedWith(compareBy<TaskWithColor> { it.task.isCompleted }.thenBy { it.task.dueAt })

                taskAdapter.submitList(filtered)
            }
        }
    }

    // ---------------- Color Filter ----------------
    private fun setupColorFilter() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allColors.collectLatest { colors ->
                colorFilterContainer.removeAllViews()
                addFilterButton("全部", "#F5F5F5", true) { selectedColorFilter = null }
                colors.forEach { color ->
                    addFilterButton(color.colorName, color.colorTag, false) { selectedColorFilter = color.colorTag }
                }
            }
        }
    }

    private fun addFilterButton(text: String, colorTag: String, isAll: Boolean, onClick: () -> Unit) {
        val button = Button(requireContext())
        val buttonWidth = (resources.displayMetrics.density * 100).toInt()
        val buttonHeight = (resources.displayMetrics.density * 48).toInt()
        button.layoutParams = LinearLayout.LayoutParams(buttonWidth, buttonHeight).apply {
            setMargins(
                (resources.displayMetrics.density * 8).toInt(),
                0,
                (resources.displayMetrics.density * 8).toInt(),
                0
            )
        }
        button.text = text

        val bgColor = try {
            Color.parseColor(colorTag)
        } catch (e: Exception) {
            Color.GRAY
        }

        val luminance =
            (Color.red(bgColor) * 0.299 + Color.green(bgColor) * 0.587 + Color.blue(bgColor) * 0.114) / 255

        button.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setColor(bgColor)
        }

        button.setTextColor(if (luminance > 0.5) Color.BLACK else Color.WHITE)

        button.setOnClickListener {
            onClick()
            observeTasks()
        }

        colorFilterContainer.addView(button)
    }
}