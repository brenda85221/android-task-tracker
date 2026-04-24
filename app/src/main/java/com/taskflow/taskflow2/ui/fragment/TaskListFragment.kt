package com.taskflow.taskflow2.ui.fragment

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// 用 sealed class 明確表示三種 filter 狀態
sealed class ColorFilter {
    object All : ColorFilter()
    object Uncategorized : ColorFilter()
    data class ByTag(val tag: String) : ColorFilter()
}

class TaskListFragment : Fragment(R.layout.fragment_task_list) {

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
    private lateinit var colorFilterContainer: LinearLayout
    private lateinit var taskAdapter: TaskAdapter

    // 用 StateFlow 管理 filter，變更時自動觸發 combine 重新過濾
    private val colorFilter = MutableStateFlow<ColorFilter>(ColorFilter.All)

    // ---------------- Lifecycle ----------------
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvTasks = view.findViewById(R.id.rvTasks)
        colorFilterContainer = view.findViewById(R.id.colorFilterContainer)

        setupRecyclerView()
        setupColorFilter()
        observeTasks()  // 只呼叫一次
    }

    // ---------------- RecyclerView ----------------
    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter()
        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        rvTasks.adapter = taskAdapter

        // 點擊圖片 — Flow 自動更新，不需回呼 observeTasks()
        taskAdapter.onItemClick = { taskWithColor ->
            TaskImageDialog.show(
                requireContext(),
                taskWithColor.task,
                viewLifecycleOwner.lifecycleScope
            ) { /* 不需要做任何事，DB 改變 → Flow 自動推送 */ }
        }

        // 編輯任務 — 同上
        taskAdapter.onEdit = { taskWithColor ->
            val dialog = CreateTaskDialogFragment.newInstance(taskWithColor)
            // 不需要 dialog.onSave = { observeTasks() }
            dialog.show(parentFragmentManager, "EditTaskDialog")
        }

        // 切換完成
        taskAdapter.onToggle = { taskId, isCompleted ->
            val task = taskAdapter.currentList.find { it.task.id == taskId }
            task?.let { viewModel.toggleTaskCompleted(it, isCompleted) }
        }

        // Swipe Delete
        val swipeHandler = TaskSwipeCallback { position ->
            val task = taskAdapter.currentList[position]
            showDeleteConfirmDialog(task, position)
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(rvTasks)
    }

    // ---------------- Delete Confirm ----------------
    private fun showDeleteConfirmDialog(task: TaskWithColor, position: Int) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("刪除任務")
            .setMessage("確定刪除「${task.task.title}」嗎？")
            .setPositiveButton("刪除") { _, _ ->
                viewModel.deleteTask(task.task)
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.setOnDismissListener {
            taskAdapter.notifyItemChanged(position)
        }
        dialog.show()
    }

    // ---------------- Observe Tasks（只訂閱一次）----------------
    private fun observeTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            // combine 讓任一來源變動都重新過濾，不需手動觸發
            combine(viewModel.tasks, colorFilter) { tasks, filter ->
                when (filter) {
                    is ColorFilter.All -> tasks
                    is ColorFilter.Uncategorized -> tasks.filter { it.color == null }
                    is ColorFilter.ByTag -> tasks.filter { it.color?.colorTag == filter.tag }
                }
            }.collectLatest { filtered ->
                taskAdapter.submitList(filtered)
            }
        }
    }

    // ---------------- Color Filter ----------------
    private fun setupColorFilter() {
        viewLifecycleOwner.lifecycleScope.launch {
            // combine tasks + colors，讓「無分類」按鈕能響應 tasks 變化
            combine(viewModel.tasks, viewModel.allColors) { tasks, colors ->
                Pair(tasks, colors)
            }.collectLatest { (tasks, colors) ->
                colorFilterContainer.removeAllViews()

                // 全部
                addFilterButton("全部", "#F5F5F5") {
                    colorFilter.value = ColorFilter.All
                }

                // 無分類（只在有無 colorId 的 task 時顯示）
                val hasUncategorized = tasks.any { it.task.colorId == null }
                if (hasUncategorized) {
                    addFilterButton("無分類", "#D3D3D3") {
                        colorFilter.value = ColorFilter.Uncategorized
                    }
                }

                // 各顏色
                colors.forEach { color ->
                    addFilterButton(color.colorName, color.colorTag) {
                        colorFilter.value = ColorFilter.ByTag(color.colorTag)
                    }
                }
            }
        }
    }

    private fun addFilterButton(text: String, colorTag: String, onClick: () -> Unit) {
        val button = Button(requireContext())
        val dp = resources.displayMetrics.density
        button.layoutParams = LinearLayout.LayoutParams(
            (dp * 100).toInt(),
            (dp * 48).toInt()
        ).apply {
            setMargins((dp * 8).toInt(), 0, (dp * 8).toInt(), 0)
        }
        button.text = text

        val bgColor = try { Color.parseColor(colorTag) } catch (e: Exception) { Color.GRAY }
        val luminance =
            (Color.red(bgColor) * 0.299 + Color.green(bgColor) * 0.587 + Color.blue(bgColor) * 0.114) / 255

        button.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setColor(bgColor)
        }
        button.setTextColor(if (luminance > 0.5) Color.BLACK else Color.WHITE)

        // 直接改 StateFlow，不需再呼叫 observeTasks()
        button.setOnClickListener { onClick() }
        colorFilterContainer.addView(button)
    }
}