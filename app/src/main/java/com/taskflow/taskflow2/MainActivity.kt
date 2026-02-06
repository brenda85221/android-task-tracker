package com.taskflow.taskflow2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.taskflow.taskflow2.databinding.ActivityMainBinding
import com.taskflow.taskflow2.ui.dialog.CreateTaskDialogFragment
import com.taskflow.taskflow2.ui.fragment.CalendarFragment
import com.taskflow.taskflow2.ui.fragment.ColorSettingFragment
import com.taskflow.taskflow2.ui.fragment.TaskListFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 預設顯示任務列表
        if (savedInstanceState == null) {
            loadFragment(TaskListFragment())
        }

        // 底部導覽
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_calendar -> loadFragment(CalendarFragment())
                R.id.nav_list -> loadFragment(TaskListFragment())
                R.id.nav_colors -> loadFragment(ColorSettingFragment())
                else -> false
            }
        }

        // 浮動新增按鈕
        binding.fabAddTask.setOnClickListener {
            CreateTaskDialogFragment().show(
                supportFragmentManager,
                "CreateTaskDialog"
            )
        }
    }

    private fun loadFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        return true
    }
}
