package com.taskflow.taskflow2.ui.fragment

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taskflow.taskflow2.R
import com.taskflow.taskflow2.data.local.TaskDatabase
import com.taskflow.taskflow2.data.local.TaskWithColor
import com.taskflow.taskflow2.ui.adapter.TaskAdapter
import com.taskflow.taskflow2.ui.dialog.CreateTaskDialogFragment
import com.taskflow.taskflow2.ui.dialog.TaskImageDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TaskListFragment : Fragment(R.layout.fragment_task_list) {

    private val db by lazy { TaskDatabase.getInstance(requireContext()) }
    private val taskDao by lazy { db.taskDao() }

    private lateinit var taskAdapter: TaskAdapter
    private var selectedColorFilter: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvTasks = view.findViewById<RecyclerView>(R.id.rvTasks)
        val colorFilterContainer = view.findViewById<LinearLayout>(R.id.colorFilterContainer)

        // Adapter 現在要使用 TaskWithColor
        taskAdapter = TaskAdapter()
        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        rvTasks.adapter = taskAdapter

        // 點擊顯示圖片
        taskAdapter.onItemClick = { taskWithColor ->
            TaskImageDialog.show(
                requireContext(),
                taskWithColor.task,
                viewLifecycleOwner.lifecycleScope
            ) {
                refreshTaskList()
            }
        }

        // 編輯任務
        taskAdapter.onEdit = { taskWithColor ->
            val dialog = CreateTaskDialogFragment.newInstance(taskWithColor)
            dialog.onSave = {
                refreshTaskList()
            }
            dialog.show(parentFragmentManager, "EditTaskDialog")
        }

        // 刪除任務
        taskAdapter.onDelete = { taskWithColor ->
            lifecycleScope.launch {
                taskDao.deleteTask(taskWithColor.task)
                refreshTaskList()
            }
        }

        // 切換完成 / 未完成
        // 切換完成 / 未完成
        taskAdapter.onToggle = { taskId: Long, isCompleted: Boolean ->
            lifecycleScope.launch {
                // 用 getTaskById 取得最新完整資料，再更新
                val currentTask = taskDao.getTaskById(taskId)?.task?.copy(isCompleted = isCompleted)
                currentTask?.let { taskDao.updateTask(it) }
                // 不需手動 refresh，Flow 會自動更新
            }
        }


        // ---------------- 顏色篩選 ----------------
        lifecycleScope.launch {
            taskDao.getAllColors().collectLatest { colors ->
                colorFilterContainer.removeAllViews()

                // 全部按鈕
                // 全部按鈕也加上圓角＋間距
                Button(requireContext()).apply {
                    text = "全部"

                    val buttonSize = (resources.displayMetrics.density * 100).toInt()
                    val buttonHeight = (resources.displayMetrics.density * 48).toInt()
                    layoutParams = LinearLayout.LayoutParams(buttonSize, buttonHeight).apply {
                        setMargins((resources.displayMetrics.density * 8).toInt(), 0, 0, 0)
                    }

                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 24f
                        setColor(android.graphics.Color.parseColor("#F5F5F5"))  // 淺灰背景
                    }
                    setTextColor(android.graphics.Color.BLACK)

                    setOnClickListener {
                        selectedColorFilter = null
                        refreshTaskList()
                    }
                    colorFilterContainer.addView(this)
                }


                // 每個顏色按鈕
                colors.forEach { color ->
                    Button(requireContext()).apply {
                        text = color.colorName

                        // 固定大小＋間距（不變）
                        val buttonSize = (resources.displayMetrics.density * 100).toInt()
                        val buttonHeight = (resources.displayMetrics.density * 48).toInt()
                        layoutParams = LinearLayout.LayoutParams(buttonSize, buttonHeight).apply {
                            setMargins((resources.displayMetrics.density * 8).toInt(), 0, (resources.displayMetrics.density * 8).toInt(), 0)
                        }

                        // ✅ 一行搞定：直接建立圓角＋顏色
                        try {
                            val bgColor = android.graphics.Color.parseColor(color.colorTag)
                            val luminance = (
                                    android.graphics.Color.red(bgColor) * 0.299 +
                                            android.graphics.Color.green(bgColor) * 0.587 +
                                            android.graphics.Color.blue(bgColor) * 0.114
                                    ) / 255

                            background = android.graphics.drawable.GradientDrawable().apply {
                                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                                cornerRadius = 24f
                                setColor(bgColor)  // ✅ 直接在這裡設定
                            }
                            setTextColor(if (luminance > 0.5) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                        } catch (e: Exception) {
                            background = android.graphics.drawable.GradientDrawable().apply {
                                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                                cornerRadius = 24f
                                setColor(android.graphics.Color.GRAY)
                            }
                            setTextColor(android.graphics.Color.BLACK)
                        }

                        setOnClickListener {
                            selectedColorFilter = color.colorTag
                            refreshTaskList()
                        }
                        colorFilterContainer.addView(this)
                    }
                }


            }
        }

        refreshTaskList()
    }

    private fun refreshTaskList() {
        lifecycleScope.launch {
            taskDao.getAllTasks().collectLatest { tasks ->
                // 篩選顏色（tasks: List<TaskWithColor>）
                val filtered = selectedColorFilter?.let { filter ->
                    tasks.filter { it.color?.colorTag == filter }
                } ?: tasks

                taskAdapter.submitList(filtered)
            }
        }
    }
}
