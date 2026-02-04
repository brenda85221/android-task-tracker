package com.taskflow.taskflow2

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.taskflow.taskflow2.data.local.TaskColor
import com.taskflow.taskflow2.data.local.TaskDatabase
import com.taskflow.taskflow2.data.local.TaskEntity
import com.taskflow.taskflow2.data.local.TaskDao
import com.taskflow.taskflow2.databinding.ActivityMainBinding
import com.taskflow.taskflow2.ui.adapter.TaskAdapter
import com.taskflow.taskflow2.ui.fragment.CalendarFragment
import com.taskflow.taskflow2.ui.fragment.TaskListFragment
import com.taskflow.taskflow2.ui.fragment.ColorSettingFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db by lazy { TaskDatabase.getInstance(this) }
    private val taskDao by lazy { db.taskDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 預設顯示條列分頁
        if (savedInstanceState == null) {
            loadFragment(TaskListFragment())
        }

        // 底部導覽監聽
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_calendar -> {
                    loadFragment(CalendarFragment())
                    true
                }
                R.id.nav_list -> {
                    loadFragment(TaskListFragment())
                    true
                }
                R.id.nav_colors -> {
                    loadFragment(ColorSettingFragment())
                    true
                }
                else -> false
            }
        }

        // 🆕 浮動按鈕 - 所有分頁通用
        binding.fabAddTask.setOnClickListener {
            // 目前只有 TaskListFragment 有新增任務需求
            // 可以根據當前分頁決定行為，或統一顯示對話框
            showCreateTaskDialog()
            Toast.makeText(this, "新增任務", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showCreateTaskDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_task, null)

        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)
        val btnDate = dialogView.findViewById<Button>(R.id.btnDate)
        val btnTime = dialogView.findViewById<Button>(R.id.btnTime)
        val spinnerReminder = dialogView.findViewById<Spinner>(R.id.spinnerReminder)
        val tvColorTitle = dialogView.findViewById<TextView>(R.id.tvColorTitle)  // ← 新增標題
        val rgColors = dialogView.findViewById<RadioGroup>(R.id.rgColors)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        var selectedDateMillis = System.currentTimeMillis()
        var selectedTime = "09:00"
        var selectedColorId: Int? = null  // ← 新增：選中的顏色 ID

        // 日期選擇
        btnDate.setOnClickListener {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val day = cal.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, y, m, d ->
                val c = Calendar.getInstance()
                c.set(y, m, d, 0, 0, 0)
                selectedDateMillis = c.timeInMillis
                btnDate.text = "%04d-%02d-%02d".format(y, m + 1, d)
            }, year, month, day).show()
        }

        // 時間選擇
        btnTime.setOnClickListener {
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)

            TimePickerDialog(this, { _, h, m ->
                selectedTime = "%02d:%02d".format(h, m)
                btnTime.text = selectedTime
            }, hour, minute, true).show()
        }

        // 提醒頻率選單
        val reminderOptions = arrayOf("一次性", "每天", "每週")
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            reminderOptions
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerReminder.adapter = adapter

        // 🆕 動態載入任務顏色
        lifecycleScope.launch {
            taskDao.getAllColors().collectLatest { colors ->
                rgColors.removeAllViews()  // 清空舊的 RadioButton

                if (colors.isEmpty()) {
                    // 沒有顏色時顯示預設
                    tvColorTitle.text = "任務種類"
                    val defaultColor = TaskColor(colorName = "藍色", colorTag = "#2196F3")
                    createColorRadioButton(defaultColor, rgColors, 0)
                    return@collectLatest
                }

                tvColorTitle.text = "任務種類"  // ← 顯示標題
                colors.forEachIndexed { index, color ->
                    createColorRadioButton(color, rgColors, index)
                }
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("新增任務")
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val notes = etNotes.text.toString().trim()

            if (title.isEmpty()) {
                etTitle.error = "標題必填"
                return@setOnClickListener
            }

            val reminderType = when (spinnerReminder.selectedItemPosition) {
                1 -> "DAILY"
                2 -> "WEEKLY"
                else -> "ONCE"
            }

            val checkedId = rgColors.checkedRadioButtonId
            val selectedColor = rgColors.findViewById<RadioButton>(checkedId)?.tag as? TaskColor
            if (selectedColor == null) {
                Toast.makeText(this, "請選擇任務種類", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val task = TaskEntity(
                    title = title,
                    description = description,
                    notes = notes,
                    dueDate = selectedDateMillis,
                    dueTime = selectedTime,
                    reminderType = reminderType,
                    colorTag = selectedColor.colorTag,
                    colorName = selectedColor.colorName
                )
                taskDao.insertTask(task)
            }

            dialog.dismiss()
        }

        dialog.show()
    }

    // 🆕 建立 RadioButton 的輔助函數
    private fun createColorRadioButton(color: TaskColor, radioGroup: RadioGroup, index: Int) {
        val radioButton = RadioButton(this).apply {
            text = color.colorName
            tag = color  // ← 儲存顏色物件
            id = View.generateViewId()  // ← 產生唯一 ID
            setPadding(0, 0, 0, 0)
        }

        radioGroup.addView(radioButton)

        // 預設第一個選中
//        if (index == 0) {
//            radioButton.isChecked = true
//            selectedColorId = color.id
//        }
    }



}
