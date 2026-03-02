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

        var selectedCalendar = Calendar.getInstance()

        // ---------- 編輯模式填充資料 ----------
        editTask?.let { task ->
            binding.etTitle.setText(task.title)
            binding.etNotes.setText(task.notes)

            selectedImagePath = task.imageUri
            binding.tvSelectedImage.text =
                selectedImagePath?.let { "已選擇：${File(it).name}" } ?: ""

            selectedCalendar.timeInMillis = task.dueAt

            val y = selectedCalendar.get(Calendar.YEAR)
            val m = selectedCalendar.get(Calendar.MONTH) + 1
            val d = selectedCalendar.get(Calendar.DAY_OF_MONTH)
            val h = selectedCalendar.get(Calendar.HOUR_OF_DAY)
            val min = selectedCalendar.get(Calendar.MINUTE)

            binding.btnDate.text = "%04d-%02d-%02d".format(y, m, d)
            binding.btnTime.text = "%02d:%02d".format(h, min)
        }

        // ---------- 日期選擇 ----------
        binding.btnTime.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, h, m ->
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, h)
                    selectedCalendar.set(Calendar.MINUTE, m)

                    binding.btnTime.text = "%02d:%02d".format(h, m)
                },
                selectedCalendar.get(Calendar.HOUR_OF_DAY),
                selectedCalendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        // ---------- 時間選擇 ----------
        binding.btnDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    selectedCalendar.set(Calendar.YEAR, y)
                    selectedCalendar.set(Calendar.MONTH, m)
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, d)

                    binding.btnDate.text = "%04d-%02d-%02d".format(y, m + 1, d)
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
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
                            notes = binding.etNotes.text.toString(),
                            dueAt = selectedCalendar.timeInMillis,
                            reminderType = reminderType,
                            colorTag = selectedColor.colorTag,
                            categoryName = selectedColor.colorName,
                            imageUri = selectedImagePath
                        )
                    )
                } else {
                    // 新增模式
                    taskDao.insertTask(
                        TaskEntity(
                            title = title,
                            notes = binding.etNotes.text.toString(),
                            dueAt = selectedCalendar.timeInMillis,
                            reminderType = reminderType,
                            colorTag = selectedColor.colorTag,
                            categoryName = selectedColor.colorName,
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
