package com.taskflow.taskflow2

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.taskflow.taskflow2.data.local.TaskDatabase
import com.taskflow.taskflow2.data.local.TaskEntity
import com.taskflow.taskflow2.databinding.ActivityMainBinding
import com.taskflow.taskflow2.util.NotificationUtils
import kotlinx.coroutines.launch
import com.taskflow.taskflow2.ui.adapter.TaskAdapter
import java.util.Calendar
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val db by lazy { TaskDatabase.getInstance(this) }
    private val taskDao by lazy { db.taskDao() }
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "onCreate called")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 建立通知頻道
        NotificationUtils.createNotificationChannel(this)

        // 測試通知按鈕
        binding.btnTestNotification?.setOnClickListener {
            NotificationUtils.showTaskReminder(
                context = this,
                taskId = 1,
                taskTitle = "TaskFlow 測試",
                taskDescription = "這是一則手動觸發的通知"
            )
        }

        // 新增任務按鈕
        binding.btnAddTask.setOnClickListener {
            showCreateTaskDialog()
        }

        // 初始化 RecyclerView
        // ✅ 初始化改這行
        taskAdapter = TaskAdapter()  // 不用 emptyList()
        binding.rvTasks.layoutManager = LinearLayoutManager(this)
        binding.rvTasks.adapter = taskAdapter

//        Log.d("TaskFlow", "Binding & ContentView OK")
        // 監聽資料變化
        lifecycleScope.launch {
            taskDao.getAllTasks().collectLatest { tasks ->
                taskAdapter.submitList(tasks)
            }
        }

        // 設定 callbacks
        taskAdapter.onToggle = { task ->
            lifecycleScope.launch {
                taskDao.updateTask(task.copy(isCompleted = !task.isCompleted))
            }
        }

        taskAdapter.onDelete = { taskId: Long ->
            lifecycleScope.launch {
                val task = taskDao.getTaskById(taskId) ?: return@launch
                taskDao.deleteTask(task)
            }
        }

        taskAdapter.onEdit = { task ->
//            Toast.makeText(this, "編輯 ${task.title}", Toast.LENGTH_SHORT).show()
            showEditTaskDialog(task)
        }
    }

    private fun showCreateTaskDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_task, null)

        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)
        val btnDate = dialogView.findViewById<Button>(R.id.btnDate)
        val btnTime = dialogView.findViewById<Button>(R.id.btnTime)
        val spinnerReminder = dialogView.findViewById<Spinner>(R.id.spinnerReminder)
        val rgColors = dialogView.findViewById<RadioGroup>(R.id.rgColors)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        var selectedDateMillis = System.currentTimeMillis()
        var selectedTime = "09:00"

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

        // 預設顏色：藍色
        rgColors.check(R.id.rbBlue)

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
            val (colorTag, colorName) = when (checkedId) {
                R.id.rbRed -> "#F44336" to "Red"
                R.id.rbGreen -> "#4CAF50" to "Green"
                else -> "#2196F3" to "Blue"
            }

            lifecycleScope.launch {
                val task = TaskEntity(
                    title = title,
                    description = description,
                    notes = notes,
                    dueDate = selectedDateMillis,
                    dueTime = selectedTime,
                    reminderType = reminderType,
                    colorTag = colorTag,
                    colorName = colorName
                )
                taskDao.insertTask(task)
            }

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditTaskDialog(task: TaskEntity) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_task, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)
        val btnDate = dialogView.findViewById<Button>(R.id.btnDate)
        val btnTime = dialogView.findViewById<Button>(R.id.btnTime)
        val spinnerReminder = dialogView.findViewById<Spinner>(R.id.spinnerReminder)
        val rgColors = dialogView.findViewById<RadioGroup>(R.id.rgColors)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        // 預填現有資料
        etTitle.setText(task.title)
        etDescription.setText(task.description)
        etNotes.setText(task.notes)
        btnDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(task.dueDate))
        btnTime.text = task.dueTime
        // 設定 spinner 和顏色（依 task.reminderType 和 task.colorTag 調整索引）
        when (task.reminderType) {
            "DAILY" -> spinnerReminder.setSelection(1)
            "WEEKLY" -> spinnerReminder.setSelection(2)
            else -> spinnerReminder.setSelection(0)
        }
        when (task.colorTag) {
            "F44336" -> rgColors.check(R.id.rbRed)
            "4CAF50" -> rgColors.check(R.id.rbGreen)
            else -> rgColors.check(R.id.rbBlue)
        }

        // 日期/時間 picker 邏輯同 showCreateTaskDialog，但用 task.dueDate 初始
        var selectedDateMillis = task.dueDate
        var selectedTime = task.dueTime.toIntOrNull() ?: 900
        // ... (複製日期/時間 setOnClickListener，從 showCreateTaskDialog)

        val dialog = AlertDialog.Builder(this)
            .setTitle("編輯任務")
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val updatedTask = task.copy(
                title = etTitle.text.toString().trim(),
                description = etDescription.text.toString().trim(),
                notes = etNotes.text.toString().trim(),
                dueDate = selectedDateMillis,
                dueTime = String.format("%02d%02d", selectedTime / 100, selectedTime % 100),
                reminderType = when (spinnerReminder.selectedItemPosition) {
                    1 -> "DAILY"
                    2 -> "WEEKLY"
                    else -> "ONCE"
                },
                colorTag = when (rgColors.checkedRadioButtonId) {
                    R.id.rbRed -> "F44336"
                    R.id.rbGreen -> "4CAF50"
                    else -> "2196F3"
                },
                colorName = when (rgColors.checkedRadioButtonId) {
                    R.id.rbRed -> "Red"
                    R.id.rbGreen -> "Green"
                    else -> "Blue"
                }
            )
            lifecycleScope.launch {
                taskDao.updateTask(updatedTask)
            }
            dialog.dismiss()
        }
        dialog.show()
    }

}
