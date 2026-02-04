package com.taskflow.taskflow2.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TaskListFragment : Fragment() {

    private val db by lazy { TaskDatabase.getInstance(requireContext()) }
    private val taskDao by lazy { db.taskDao() }
    private lateinit var taskAdapter: TaskAdapter
    private var selectedColorFilter: String? = null  // ← 篩選用

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvTasks = view.findViewById<RecyclerView>(R.id.rvTasks)
        val colorFilterContainer = view.findViewById<LinearLayout>(R.id.colorFilterContainer)

        taskAdapter = TaskAdapter()
        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        rvTasks.adapter = taskAdapter

        // 監聽顏色變化，動態生成篩選按鈕
        lifecycleScope.launch {
            taskDao.getAllColors().collectLatest { colors ->
                colorFilterContainer.removeAllViews()

                // "全部" 按鈕
                Button(requireContext()).apply {
                    text = "全部"
                    setOnClickListener {
                        selectedColorFilter = null
                        refreshTaskList()
                    }
                    colorFilterContainer.addView(this)
                }

                // 各顏色篩選按鈕
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
                val filtered = if (selectedColorFilter != null) {
                    tasks.filter { it.colorTag == selectedColorFilter }
                } else {
                    tasks
                }
                // 依 dueDate 由近到遠排序
                taskAdapter.submitList(filtered.sortedBy { it.dueDate })
            }
        }
    }
}
