package com.taskflow.taskflow2

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.taskflow.taskflow2.data.local.TaskColor
import com.taskflow.taskflow2.data.local.TaskDatabase
import com.taskflow.taskflow2.data.local.TaskEntity
import com.taskflow.taskflow2.databinding.ActivityMainBinding
import com.taskflow.taskflow2.ui.fragment.CalendarFragment
import com.taskflow.taskflow2.ui.fragment.ColorSettingFragment
import com.taskflow.taskflow2.ui.fragment.TaskListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import androidx.activity.result.PickVisualMediaRequest

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val taskDao by lazy {
        TaskDatabase.getInstance(this).taskDao()
    }

    /** 使用者選到的圖片（App 私有路徑） */
    private var selectedImagePath: String? = null

    /** Photo Picker */
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri ?: return@registerForActivityResult

            lifecycleScope.launch {
                selectedImagePath = copyImageToAppDir(uri)
                currentDialogImageText?.text =
                    selectedImagePath?.let { "已選擇：${File(it).name}" } ?: "圖片儲存失敗"
            }
        }

    /** 對話框內的 Image TextView 參考 */
    private var currentDialogImageText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            loadFragment(TaskListFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_calendar -> loadFragment(CalendarFragment())
                R.id.nav_list -> loadFragment(TaskListFragment())
                R.id.nav_colors -> loadFragment(ColorSettingFragment())
                else -> false
            }
        }

        binding.fabAddTask.setOnClickListener {
            showCreateTaskDialog()
        }
    }

    private fun loadFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        return true
    }

    private fun showCreateTaskDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_create_task, null)

        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val etDescription = view.findViewById<EditText>(R.id.etDescription)
        val etNotes = view.findViewById<EditText>(R.id.etNotes)
        val btnDate = view.findViewById<Button>(R.id.btnDate)
        val btnTime = view.findViewById<Button>(R.id.btnTime)
        val spinnerReminder = view.findViewById<Spinner>(R.id.spinnerReminder)
        val rgColors = view.findViewById<RadioGroup>(R.id.rgColors)
        val tvSelectedImage = view.findViewById<TextView>(R.id.tvSelectedImage)
        val btnSelectImage = view.findViewById<Button>(R.id.btnSelectImage)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        currentDialogImageText = tvSelectedImage
        selectedImagePath = null

        var selectedDateMillis = System.currentTimeMillis()
        var selectedTime = "09:00"

        btnDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    val cal = Calendar.getInstance().apply {
                        set(y, m, d, 0, 0)
                    }
                    selectedDateMillis = cal.timeInMillis
                    btnDate.text = "%04d-%02d-%02d".format(y, m + 1, d)
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnTime.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, h, m ->
                    selectedTime = "%02d:%02d".format(h, m)
                    btnTime.text = selectedTime
                },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                true
            ).show()
        }

        spinnerReminder.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("一次性", "每天", "每週")
        )

        lifecycleScope.launch {
            taskDao.getAllColors().collectLatest { colors ->
                rgColors.removeAllViews()
                colors.forEach { color ->
                    rgColors.addView(RadioButton(this@MainActivity).apply {
                        text = color.colorName
                        tag = color
                        id = View.generateViewId()
                    })
                }
            }
        }

        btnSelectImage.setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }



        val dialog = AlertDialog.Builder(this)
            .setTitle("新增任務")
            .setView(view)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) {
                etTitle.error = "標題必填"
                return@setOnClickListener
            }

            val selectedColor =
                rgColors.findViewById<RadioButton>(rgColors.checkedRadioButtonId)
                    ?.tag as? TaskColor
                    ?: run {
                        Toast.makeText(this, "請選擇任務種類", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

            val reminderType = when (spinnerReminder.selectedItemPosition) {
                1 -> "DAILY"
                2 -> "WEEKLY"
                else -> "ONCE"
            }

            lifecycleScope.launch {
                taskDao.insertTask(
                    TaskEntity(
                        title = title,
                        description = etDescription.text.toString(),
                        notes = etNotes.text.toString(),
                        dueDate = selectedDateMillis,
                        dueTime = selectedTime,
                        reminderType = reminderType,
                        colorTag = selectedColor.colorTag,
                        colorName = selectedColor.colorName,
                        imageUri = selectedImagePath
                    )
                )
            }

            dialog.dismiss()
        }

        dialog.show()
    }

    private suspend fun copyImageToAppDir(uri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(filesDir, "tasks").apply { mkdirs() }
                val file = File(dir, "task_${System.currentTimeMillis()}.jpg")

                contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}
