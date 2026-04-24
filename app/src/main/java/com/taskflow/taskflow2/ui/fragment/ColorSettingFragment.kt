package com.taskflow.taskflow2.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taskflow.taskflow2.R
import com.taskflow.taskflow2.data.local.TaskColor
import com.taskflow.taskflow2.data.local.TaskDatabase
import com.taskflow.taskflow2.data.local.TaskWithColor
import com.taskflow.taskflow2.data.repository.TaskRepository
import com.taskflow.taskflow2.ui.adapter.ColorGridAdapter
import com.taskflow.taskflow2.util.TaskSwipeCallback
import com.taskflow.taskflow2.viewmodel.ColorViewModel
import com.taskflow.taskflow2.viewmodel.ColorViewModelFactory
import kotlinx.coroutines.flow.collectLatest

class ColorSettingFragment : Fragment() {

    // ---------------- ViewModel ----------------
    private val viewModel: ColorViewModel by viewModels {
        ColorViewModelFactory(TaskRepository(TaskDatabase.getInstance(requireContext()).taskDao()))
    }

    // ---------------- UI ----------------
    private lateinit var colorAdapter: ColorGridAdapter

    private val predefinedColors = listOf(
        "#F8BBD0", "#F48FB1", "#CE93D8", "#B39DDB",
        "#9FA8DA", "#90CAF9", "#81D4FA", "#80DEEA",
        "#A5D6A7", "#C5E1A5", "#E6EE9C", "#FFEE99",
        "#FFE082", "#FFCC80", "#FFAB91", "#CD853F"
    )
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_color_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvColors = view.findViewById<RecyclerView>(R.id.rvColors)
        rvColors.layoutManager = LinearLayoutManager(requireContext())
        colorAdapter = ColorGridAdapter { selectedColor ->
            showColorDialog(selectedColor)
        }
        rvColors.adapter = colorAdapter

        // ---------------- Observe Colors ----------------
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.allColors.collectLatest { colors ->
                colorAdapter.submitList(colors)
            }
        }

        view.findViewById<View>(R.id.btnAddColor)?.setOnClickListener {
            showColorDialog()
        }
        //Swipe Delete
        val swipeHandler = TaskSwipeCallback { position ->
            val color = colorAdapter.currentList[position]

            showDeleteColorConfirmDialog(color, position)
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(rvColors)
    }

    // ---------------- Color Dialog ----------------
    private fun showColorDialog(color: TaskColor? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_color, null)
        val etColorName = dialogView.findViewById<EditText>(R.id.etColorName)
        val gridColors = dialogView.findViewById<GridLayout>(R.id.gridColors)

        val isEditMode = color != null
        etColorName.setText(color?.colorName ?: "")
        var selectedHex: String? = color?.colorTag

        gridColors.removeAllViews()
        predefinedColors.forEach { hex ->
            val colorView = View(requireContext())
            val size = (resources.displayMetrics.density * 40).toInt()
            val margin = (resources.displayMetrics.density * 4).toInt()
            val params = GridLayout.LayoutParams().apply {
                width = size
                height = size
                setMargins(margin, margin, margin, margin)
            }
            colorView.layoutParams = params
            try { colorView.setBackgroundColor(Color.parseColor(hex)) } catch (_: Exception) { colorView.setBackgroundColor(Color.GRAY) }
            colorView.tag = hex

            // 標示已選顏色
            if (hex == selectedHex) {
                colorView.foreground = requireContext().getDrawable(R.drawable.bg_color_selected)
            }

            colorView.setOnClickListener {
                selectedHex = hex
                for (i in 0 until gridColors.childCount) {
                    val v = gridColors.getChildAt(i)
                    try { v.foreground = null } catch (_: Exception) {}
                }
                colorView.foreground = requireContext().getDrawable(R.drawable.bg_color_selected)
            }

            gridColors.addView(colorView)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEditMode) "編輯類別" else "新增類別")
            .setView(dialogView)
            .setPositiveButton(if (isEditMode) "保存" else "新增") { _, _ ->
                val name = etColorName.text.toString().trim()
                val hex = selectedHex
                if (name.isNotEmpty() && !hex.isNullOrEmpty()) {
                    if (isEditMode) {
                        viewModel.updateColor(color!!.copy(colorName = name, colorTag = hex))
                        Toast.makeText(requireContext(), "更新成功", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.insertColor(TaskColor(colorName = name, colorTag = hex, isDefault = false))
                        Toast.makeText(requireContext(), "新增成功", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "請輸入名稱並選擇顏色", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ---------------- Delete Confirm ----------------
    private fun showDeleteColorConfirmDialog(color: TaskColor, position: Int) {

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("刪除種類")
            .setMessage("確定刪除「${color.colorName}」嗎？刪除後任務將歸為「無分類」，可重新設定分類")
            .setPositiveButton("刪除") { _, _ ->
                viewModel.deleteColor(color)
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.setOnDismissListener {
            // 👉 不管怎麼關閉，都恢復 UI
            colorAdapter.notifyItemChanged(position)
        }

        dialog.show()
    }
}