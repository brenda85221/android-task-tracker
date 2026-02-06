package com.taskflow.taskflow2.ui.dialog

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import com.taskflow.taskflow2.data.local.TaskColor
import com.taskflow.taskflow2.data.local.TaskDatabase
import com.taskflow.taskflow2.data.local.TaskEntity
import com.taskflow.taskflow2.databinding.DialogCreateTaskBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

class CreateTaskDialogFragment : DialogFragment() {

    private val taskDao by lazy { TaskDatabase.getInstance(requireContext()).taskDao() }

    private var selectedImagePath: String? = null
    private var _binding: DialogCreateTaskBinding? = null
    private val binding get() = _binding!!

    // ---------- 編輯用 ----------
    var editTask: TaskEntity? = null
    var onSave: (() -> Unit)? = null

    companion object {
        fun newInstance(task: TaskEntity? = null): CreateTaskDialogFragment {
            val fragment = CreateTaskDialogFragment()
            fragment.editTask = task
            return fragment
        }
    }

    // ---------- Photo Picker ----------
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            selectedImagePath = copyImageToAppDir(uri)
            binding.tvSelectedImage.text =
                selectedImagePath?.let { "已選擇：${File(it).name}" } ?: "圖片儲存失敗"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateTaskBinding.inflate(inflater, container, false)

        var selectedDateMillis = System.currentTimeMillis()
        var selectedTime = "09:00"

        // ---------- 編輯模式填充資料 ----------
        editTask?.let { task ->
            binding.etTitle.setText(task.title)
            binding.etDescription.setText(task.description)
            binding.etNotes.setText(task.notes)
            selectedImagePath = task.imageUri
            binding.tvSelectedImage.text = selectedImagePath?.let { "已選擇：${File(it).name}" } ?: ""
            selectedDateMillis = task.dueDate
            selectedTime = task.dueTime
            binding.btnDate.text = "%04d-%02d-%02d".format(
                Calendar.getInstance().apply { timeInMillis = task.dueDate }.get(Calendar.YEAR),
                Calendar.getInstance().apply { timeInMillis = task.dueDate }.get(Calendar.MONTH) + 1,
                Calendar.getInstance().apply { timeInMillis = task.dueDate }.get(Calendar.DAY_OF_MONTH)
            )
            binding.btnTime.text = task.dueTime
        }

        // ---------- 日期選擇 ----------
        binding.btnDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    val cal = Calendar.getInstance().apply { set(y, m, d, 0, 0) }
                    selectedDateMillis = cal.timeInMillis
                    binding.btnDate.text = "%04d-%02d-%02d".format(y, m + 1, d)
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // ---------- 時間選擇 ----------
        binding.btnTime.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(
                requireContext(),
                { _, h, m ->
                    selectedTime = "%02d:%02d".format(h, m)
                    binding.btnTime.text = selectedTime
                },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                true
            ).show()
        }

        // ---------- 提醒選單 ----------
        binding.spinnerReminder.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("一次性", "每天", "每週")
        )

        // ---------- 顏色選項 ----------
        lifecycleScope.launch {
            taskDao.getAllColors().collectLatest { colors ->
                binding.rgColors.removeAllViews()
                colors.forEach { color ->
                    binding.rgColors.addView(
                        RadioButton(requireContext()).apply {
                            text = color.colorName
                            tag = color
                            id = View.generateViewId()
                            // 如果是編輯模式，預設選中
                            if (editTask?.colorTag == color.colorTag) {
                                isChecked = true
                            }
                        }
                    )
                }
            }
        }

        // ---------- 圖片選擇 ----------
        binding.btnSelectImage.setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        // ---------- 取消 ----------
        binding.btnCancel.setOnClickListener { dismiss() }

        // ---------- 儲存 ----------
        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            if (title.isEmpty()) {
                binding.etTitle.error = "標題必填"
                return@setOnClickListener
            }

            val selectedColor =
                binding.rgColors.findViewById<RadioButton>(binding.rgColors.checkedRadioButtonId)?.tag as? TaskColor
                    ?: run {
                        Toast.makeText(requireContext(), "請選擇任務種類", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

            val reminderType = when (binding.spinnerReminder.selectedItemPosition) {
                1 -> "DAILY"
                2 -> "WEEKLY"
                else -> "ONCE"
            }

            lifecycleScope.launch {
                if (editTask != null) {
                    // 編輯模式
                    taskDao.updateTask(
                        editTask!!.copy(
                            title = title,
                            description = binding.etDescription.text.toString(),
                            notes = binding.etNotes.text.toString(),
                            dueDate = selectedDateMillis,
                            dueTime = selectedTime,
                            reminderType = reminderType,
                            colorTag = selectedColor.colorTag,
                            colorName = selectedColor.colorName,
                            imageUri = selectedImagePath
                        )
                    )
                } else {
                    // 新增模式
                    taskDao.insertTask(
                        TaskEntity(
                            title = title,
                            description = binding.etDescription.text.toString(),
                            notes = binding.etNotes.text.toString(),
                            dueDate = selectedDateMillis,
                            dueTime = selectedTime,
                            reminderType = reminderType,
                            colorTag = selectedColor.colorTag,
                            colorName = selectedColor.colorName,
                            imageUri = selectedImagePath
                        )
                    )
                }
            }

            onSave?.invoke()
            dismiss()
        }

        return binding.root
    }

    // ---------- 儲存圖片到 App 目錄 ----------
    private suspend fun copyImageToAppDir(uri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(requireContext().filesDir, "tasks").apply { mkdirs() }
                val file = File(dir, "task_${System.currentTimeMillis()}.jpg")
                requireContext().contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ---------- 調整 Dialog 大小 ----------
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
