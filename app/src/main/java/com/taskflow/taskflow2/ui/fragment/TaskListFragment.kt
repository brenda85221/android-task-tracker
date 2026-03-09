package com.taskflow.taskflow2.ui.fragment

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
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

class TaskListFragment : Fragment(R.layout.fragment_task_list) {

    private val db by lazy { TaskDatabase.getInstance(requireContext()) }
    private val taskDao by lazy { db.taskDao() }

    private lateinit var taskAdapter: TaskAdapter
    private var selectedColorFilter: String? = null

    private lateinit var rvTasks: RecyclerView
    private lateinit var colorFilterContainer: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvTasks = view.findViewById(R.id.rvTasks)
        colorFilterContainer = view.findViewById(R.id.colorFilterContainer)

        setupRecyclerView()
        setupColorFilter()
        observeTasks()
    }

    // ---------------- RecyclerView ----------------

    private fun setupRecyclerView() {

        taskAdapter = TaskAdapter()

        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        rvTasks.adapter = taskAdapter

        // 點擊圖片
        taskAdapter.onItemClick = { taskWithColor ->
            TaskImageDialog.show(
                requireContext(),
                taskWithColor.task,
                viewLifecycleOwner.lifecycleScope
            ) {
                observeTasks()
            }
        }

        // 編輯任務
        taskAdapter.onEdit = { taskWithColor ->
            val dialog = CreateTaskDialogFragment.newInstance(taskWithColor)
            dialog.onSave = { observeTasks() }
            dialog.show(parentFragmentManager, "EditTaskDialog")
        }

        // 切換完成
        taskAdapter.onToggle = { taskId, isCompleted ->
            lifecycleScope.launch {
                val current = taskDao.getTaskById(taskId)?.task?.copy(isCompleted = isCompleted)
                current?.let { taskDao.updateTask(it) }
            }
        }

        // -------- Swipe Delete --------
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

    // ---------------- Task Flow ----------------

    private fun observeTasks() {

        lifecycleScope.launch {

            taskDao.getAllTasks().collectLatest { tasks ->

                val filtered = selectedColorFilter?.let { filter ->
                    tasks.filter { it.color?.colorTag == filter }
                } ?: tasks

                taskAdapter.submitList(filtered)
            }
        }
    }

    // ---------------- Color Filter ----------------

    private fun setupColorFilter() {

        lifecycleScope.launch {

            taskDao.getAllColors().collectLatest { colors ->

                colorFilterContainer.removeAllViews()

                addFilterButton("全部", "#F5F5F5", true) {
                    selectedColorFilter = null
                }

                colors.forEach { color ->
                    addFilterButton(color.colorName, color.colorTag, false) {
                        selectedColorFilter = color.colorTag
                    }
                }
            }
        }
    }

    private fun addFilterButton(
        text: String,
        colorTag: String,
        isAll: Boolean,
        onClick: () -> Unit
    ) {

        val button = Button(requireContext())

        val buttonWidth = (resources.displayMetrics.density * 100).toInt()
        val buttonHeight = (resources.displayMetrics.density * 48).toInt()

        button.layoutParams = LinearLayout.LayoutParams(buttonWidth, buttonHeight).apply {
            setMargins((resources.displayMetrics.density * 8).toInt(), 0, (resources.displayMetrics.density * 8).toInt(), 0)
        }

        button.text = text

        val bgColor = try {
            Color.parseColor(colorTag)
        } catch (e: Exception) {
            Color.GRAY
        }

        val luminance =
            (Color.red(bgColor) * 0.299 +
                    Color.green(bgColor) * 0.587 +
                    Color.blue(bgColor) * 0.114) / 255

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