package com.taskflow.taskflow2.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taskflow.taskflow2.R
import com.taskflow.taskflow2.data.local.TaskDatabase
import com.taskflow.taskflow2.ui.adapter.TaskAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class CalendarFragment : Fragment() {

    private val db by lazy { TaskDatabase.getInstance(requireContext()) }
    private val taskDao by lazy { db.taskDao() }
    private lateinit var taskAdapter: TaskAdapter
    private var currentDate = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvMonthYear = view.findViewById<TextView>(R.id.tvMonthYear)
        val btnPrevMonth = view.findViewById<Button>(R.id.btnPrevMonth)
        val btnNextMonth = view.findViewById<Button>(R.id.btnNextMonth)
        val rvTasks = view.findViewById<RecyclerView>(R.id.rvTasksInMonth)

        taskAdapter = TaskAdapter()
        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        rvTasks.adapter = taskAdapter

        // 月份切換
        btnPrevMonth.setOnClickListener {
            currentDate.add(Calendar.MONTH, -1)
            updateMonthView(tvMonthYear)
            loadTasksForMonth()
        }

        btnNextMonth.setOnClickListener {
            currentDate.add(Calendar.MONTH, 1)
            updateMonthView(tvMonthYear)
            loadTasksForMonth()
        }

        updateMonthView(tvMonthYear)
        loadTasksForMonth()
    }

    private fun updateMonthView(tv: TextView) {
        val sdf = java.text.SimpleDateFormat("yyyy 年 MM 月", Locale.getDefault())
        tv.text = sdf.format(currentDate.time)
    }

    private fun loadTasksForMonth() {
        lifecycleScope.launch {
            taskDao.getAllTasks().collectLatest { tasks ->
                val cal = Calendar.getInstance()
                val monthStart = Calendar.getInstance().apply {
                    time = currentDate.time
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val monthEnd = Calendar.getInstance().apply {
                    time = currentDate.time
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                }

                val filtered = tasks.filter { task ->
                    val taskCal = Calendar.getInstance()
                    taskCal.timeInMillis = task.dueDate
                    task.dueDate in monthStart.timeInMillis..monthEnd.timeInMillis
                }.sortedBy { it.dueDate }

                taskAdapter.submitList(filtered)
            }
        }
    }
}
