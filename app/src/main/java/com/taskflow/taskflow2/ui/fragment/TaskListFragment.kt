package com.taskflow.taskflow2.ui.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taskflow.taskflow2.R
import com.taskflow.taskflow2.data.local.TaskDatabase
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

        taskAdapter = TaskAdapter()
        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        rvTasks.adapter = taskAdapter

        taskAdapter.onItemClick = { task ->
            TaskImageDialog.show(requireContext(), task, viewLifecycleOwner.lifecycleScope) {
                // optional: Dialog 關閉後刷新列表
                refreshTaskList()
            }
        }

        taskAdapter.onEdit = { task ->
            // 呼叫 CreateTaskDialogFragment 編輯
            val dialog = CreateTaskDialogFragment.newInstance(task)
            dialog.onSave = {
                // 編輯完成後刷新列表
                refreshTaskList()
            }
            dialog.show(parentFragmentManager, "EditTaskDialog")
        }

        taskAdapter.onDelete = { taskId ->
            lifecycleScope.launch {
                taskDao.deleteTask(taskId)
                refreshTaskList()
            }
        }


        taskAdapter.onToggle = { task ->
            lifecycleScope.launch {
                taskDao.updateTask(task)
                refreshTaskList()
            }
        }

        // ---------------- 顏色篩選 ----------------
        lifecycleScope.launch {
            taskDao.getAllColors().collectLatest { colors ->
                colorFilterContainer.removeAllViews()

                Button(requireContext()).apply {
                    text = "全部"
                    setOnClickListener {
                        selectedColorFilter = null
                        refreshTaskList()
                    }
                    colorFilterContainer.addView(this)
                }

                colors.forEach { color ->
                    Button(requireContext()).apply {
                        text = color.colorName
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
                val filtered = selectedColorFilter?.let { filter ->
                    tasks.filter { it.colorTag == filter }
                } ?: tasks

                taskAdapter.submitList(filtered)
            }
        }
    }
}
