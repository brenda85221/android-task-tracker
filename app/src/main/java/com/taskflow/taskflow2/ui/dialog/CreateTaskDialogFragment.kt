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
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.taskflow.taskflow2.data.local.TaskColor
import com.taskflow.taskflow2.data.local.TaskDatabase
import com.taskflow.taskflow2.data.local.TaskEntity
import com.taskflow.taskflow2.data.local.TaskWithColor
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

    private var selectedCalendar = Calendar.getInstance()
    private var dateSelected = false
    private var timeSelected = false

    // ---------- 編輯用 ----------
    var editTask: TaskWithColor? = null
    var onSave: (() -> Unit)? = null

    companion object {
        fun newInstance(task: TaskWithColor? = null): CreateTaskDialogFragment {
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

            selectedImagePath?.let {
                binding.ivPreviewImage.setImageURI(Uri.fromFile(File(it)))
                binding.layoutImagePreview.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = DialogCreateTaskBinding.inflate(inflater, container, false)

        setupEditMode()
        setupDatePicker()
        setupTimePicker()
        setupReminderSpinner()
        setupColorOptions()
        setupImagePicker()
        setupButtons()

        return binding.root
    }

    // ---------- Edit Mode ----------
    private fun setupEditMode() {

        val taskWithColor = editTask ?: return
        val task = taskWithColor.task

        binding.etTitle.setText(task.title)
        binding.etNotes.setText(task.notes)

        selectedImagePath = task.imageUri
        selectedImagePath?.let {
            binding.ivPreviewImage.setImageURI(Uri.fromFile(File(it)))
            binding.layoutImagePreview.visibility = View.VISIBLE
        }

        selectedCalendar.timeInMillis = task.dueAt

        updateDateButton()
        updateTimeButton()

        dateSelected = true
        timeSelected = true
    }

    // ---------- Date Picker ----------
    private fun setupDatePicker() {

        binding.btnDate.setOnClickListener {

            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->

                    dateSelected = true

                    selectedCalendar.set(Calendar.YEAR, y)
                    selectedCalendar.set(Calendar.MONTH, m)
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, d)

                    updateDateButton()
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    // ---------- Time Picker ----------
    private fun setupTimePicker() {

        binding.btnTime.setOnClickListener {

            TimePickerDialog(
                requireContext(),
                { _, h, m ->

                    timeSelected = true

                    selectedCalendar.set(Calendar.HOUR_OF_DAY, h)
                    selectedCalendar.set(Calendar.MINUTE, m)

                    updateTimeButton()
                },
                selectedCalendar.get(Calendar.HOUR_OF_DAY),
                selectedCalendar.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    // ---------- Reminder Spinner ----------
    private fun setupReminderSpinner() {

        binding.spinnerReminder.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("一次性", "每天", "每週")
        )
    }

    // ---------- Color Options ----------
    private fun setupColorOptions() {

        lifecycleScope.launch {
            taskDao.getAllColors().collectLatest { colors ->

                binding.rgColors.removeAllViews()

                colors.forEach { color ->

                    val radio = RadioButton(requireContext()).apply {

                        text = color.colorName
                        tag = color
                        id = View.generateViewId()

                        if (editTask?.task?.colorId == color.id) {
                            isChecked = true
                        }
                    }

                    binding.rgColors.addView(radio)
                }
            }
        }
    }

    // ---------- Image Picker ----------
    private fun setupImagePicker() {

        binding.btnSelectImage.setOnClickListener { openImagePicker() }

        binding.ivPreviewImage.setOnClickListener { openImagePicker() }

        binding.btnRemoveImage.setOnClickListener {
            selectedImagePath = null
            binding.layoutImagePreview.visibility = View.GONE
        }
    }

    private fun openImagePicker() {

        pickImageLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // ---------- Buttons ----------
    private fun setupButtons() {

        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnSave.setOnClickListener { saveTask() }
    }

    // ---------- Save Task ----------
    private fun saveTask() {

        val title = binding.etTitle.text.toString().trim()

        if (title.isEmpty()) {
            binding.etTitle.error = "標題必填"
            return
        }

        val selectedColor =
            binding.rgColors.findViewById<RadioButton>(
                binding.rgColors.checkedRadioButtonId
            )?.tag as? TaskColor
                ?: run {
                    Toast.makeText(requireContext(), "請選擇任務種類", Toast.LENGTH_SHORT).show()
                    return
                }

        if (!timeSelected) {
            selectedCalendar.set(Calendar.HOUR_OF_DAY, 23)
            selectedCalendar.set(Calendar.MINUTE, 59)
        }

        val reminderType = when (binding.spinnerReminder.selectedItemPosition) {
            1 -> "DAILY"
            2 -> "WEEKLY"
            else -> "ONCE"
        }

        val dueAt = if (dateSelected) selectedCalendar.timeInMillis else 0L

        lifecycleScope.launch {

            val notes = binding.etNotes.text.toString()

            if (editTask != null) {

                val oldTask = editTask!!.task

                taskDao.updateTask(
                    oldTask.copy(
                        title = title,
                        notes = notes,
                        dueAt = dueAt,
                        reminderType = reminderType,
                        colorId = selectedColor.id,
                        imageUri = selectedImagePath
                    )
                )

            } else {

                taskDao.insertTask(
                    TaskEntity(
                        title = title,
                        notes = notes,
                        dueAt = dueAt,
                        reminderType = reminderType,
                        colorId = selectedColor.id,
                        imageUri = selectedImagePath
                    )
                )
            }

            onSave?.invoke()
            dismiss()
        }
    }

    // ---------- Update UI ----------
    private fun updateDateButton() {

        val y = selectedCalendar.get(Calendar.YEAR)
        val m = selectedCalendar.get(Calendar.MONTH) + 1
        val d = selectedCalendar.get(Calendar.DAY_OF_MONTH)

        binding.btnDate.text = "%04d-%02d-%02d".format(y, m, d)
    }

    private fun updateTimeButton() {

        val h = selectedCalendar.get(Calendar.HOUR_OF_DAY)
        val m = selectedCalendar.get(Calendar.MINUTE)

        binding.btnTime.text = "%02d:%02d".format(h, m)
    }

    // ---------- Save Image ----------
    private suspend fun copyImageToAppDir(uri: Uri): String? =
        withContext(Dispatchers.IO) {

            try {

                val dir = File(requireContext().filesDir, "tasks").apply { mkdirs() }

                val file = File(dir, "task_${System.currentTimeMillis()}.jpg")

                requireContext().contentResolver.openInputStream(uri)?.use { input ->
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ---------- Dialog Size ----------
    override fun onStart() {
        super.onStart()

        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}