package com.taskflow.taskflow2.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taskflow.taskflow2.R
import com.taskflow.taskflow2.data.local.TaskColor
import com.taskflow.taskflow2.data.local.TaskDatabase
import com.taskflow.taskflow2.ui.adapter.ColorAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ColorSettingFragment : Fragment() {

    private val db by lazy { TaskDatabase.getInstance(requireContext()) }
    private val taskDao by lazy { db.taskDao() }
    private lateinit var colorAdapter: ColorAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_color_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvColors = view.findViewById<RecyclerView>(R.id.rvColors)
        val btnAddColor = view.findViewById<Button>(R.id.btnAddColor)

        colorAdapter = ColorAdapter { color ->
            showEditColorDialog(color)
        }
        rvColors.layoutManager = LinearLayoutManager(requireContext())
        rvColors.adapter = colorAdapter

        // 監聽顏色列表
        lifecycleScope.launch {
            taskDao.getAllColors().collectLatest { colors ->
                colorAdapter.submitList(colors)
            }
        }

        // 新增顏色按鈕
        btnAddColor.setOnClickListener {
            showAddColorDialog()
        }
    }

    private fun showAddColorDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_color, null)
        val etColorName = dialogView.findViewById<EditText>(R.id.etColorName)
        val etColorTag = dialogView.findViewById<EditText>(R.id.etColorTag)

        AlertDialog.Builder(requireContext())
            .setTitle("新增顏色")
            .setView(dialogView)
            .setPositiveButton("新增") { _, _ ->
                val name = etColorName.text.toString().trim()
                val tag = etColorTag.text.toString().trim()
                if (name.isNotEmpty() && tag.isNotEmpty()) {
                    lifecycleScope.launch {
                        taskDao.insertColor(TaskColor(
                            colorName = name,
                            colorTag = tag,
                            isDefault = false
                        ))
                        Toast.makeText(requireContext(), "新增成功", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditColorDialog(color: TaskColor) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_color, null)
        val etColorName = dialogView.findViewById<EditText>(R.id.etColorName)
        val etColorTag = dialogView.findViewById<EditText>(R.id.etColorTag)

        etColorName.setText(color.colorName)
        etColorTag.setText(color.colorTag)

        AlertDialog.Builder(requireContext())
            .setTitle("編輯顏色")
            .setView(dialogView)
            .setPositiveButton("更新") { _, _ ->
                val name = etColorName.text.toString().trim()
                val tag = etColorTag.text.toString().trim()
                if (name.isNotEmpty() && tag.isNotEmpty()) {
                    lifecycleScope.launch {
                        taskDao.updateColor(color.copy(
                            colorName = name,
                            colorTag = tag
                        ))
                        Toast.makeText(requireContext(), "更新成功", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
