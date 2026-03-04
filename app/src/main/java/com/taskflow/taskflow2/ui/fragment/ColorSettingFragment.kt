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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.taskflow.taskflow2.R
import com.taskflow.taskflow2.data.local.TaskColor
import com.taskflow.taskflow2.data.local.TaskDatabase
import com.taskflow.taskflow2.ui.adapter.ColorGridAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ColorSettingFragment : Fragment() {

    private val db by lazy { TaskDatabase.getInstance(requireContext()) }
    private val taskDao by lazy { db.taskDao() }
    private lateinit var colorAdapter: ColorGridAdapter

    private val predefinedColors = listOf(
        "#F8BBD0", // 柔粉
        "#F48FB1",
        "#CE93D8", // 淡紫
        "#B39DDB",
        "#9FA8DA", // 淡靛
        "#90CAF9", // 淡藍
        "#81D4FA",
        "#80DEEA", // 淡青
        "#A5D6A7", // 淡綠
        "#C5E1A5",
        "#E6EE9C", // 淡萊姆
        "#FFF59D", // 淡黃
        "#FFE082",
        "#FFCC80", // 淡橘
        "#FFAB91",
        "#B0BEC5"  // 柔灰藍
    )

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

        rvColors.layoutManager = LinearLayoutManager(requireContext())
        colorAdapter = ColorGridAdapter { selectedColor ->
            // 點擊色塊 → 編輯顏色
            showColorDialog(selectedColor)
        }
        rvColors.adapter = colorAdapter

        // 監聽資料庫顏色列表
        lifecycleScope.launch {
            taskDao.getAllColors().collectLatest { colors ->
                colorAdapter.submitList(colors)
            }
        }

        // 新增顏色按鈕 (示範: 直接呼叫對話框)
        view.findViewById<View>(R.id.btnAddColor)?.setOnClickListener {
            showColorDialog()
        }
    }

    private fun showColorDialog(color: TaskColor? = null) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_color, null)

        val etColorName = dialogView.findViewById<EditText>(R.id.etColorName)
        val gridColors = dialogView.findViewById<GridLayout>(R.id.gridColors)

        // 判斷初始名稱與選色
        val isEditMode = color != null
        etColorName.setText(color?.colorName ?: "")
        var selectedHex: String? = color?.colorTag

        // 建立色塊
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
            try {
                colorView.setBackgroundColor(Color.parseColor(hex))
            } catch (_: Exception) {
                colorView.setBackgroundColor(Color.GRAY)
            }
            colorView.tag = hex

            // 標示已選顏色
            if (hex == selectedHex) {
                colorView.setBackgroundColor(Color.parseColor(hex))
                colorView.foreground =
                    requireContext().getDrawable(R.drawable.bg_color_selected)
            }


            colorView.setOnClickListener {
                selectedHex = hex

                for (i in 0 until gridColors.childCount) {
                    val v = gridColors.getChildAt(i)
                    try {
                        v.setBackgroundColor(Color.parseColor(v.tag as String))
                        v.foreground = null   // 🔥 清掉舊邊框
                    } catch (_: Exception) {}
                }

                // 只用 foreground 畫邊框
                colorView.foreground =
                    requireContext().getDrawable(R.drawable.bg_color_selected)
            }

            gridColors.addView(colorView)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isEditMode) "編輯任務種類" else "新增任務種類")
            .setView(dialogView)
            .setPositiveButton(if (isEditMode) "保存" else "新增") { _, _ ->
                val name = etColorName.text.toString().trim()
                val hex = selectedHex
                if (name.isNotEmpty() && !hex.isNullOrEmpty()) {
                    lifecycleScope.launch {
                        if (isEditMode) {
                            taskDao.updateColor(
                                TaskColor(
                                    id = color!!.id,
                                    colorName = name,
                                    colorTag = hex,
                                    isDefault = color.isDefault
                                )
                            )
                            Toast.makeText(requireContext(), "更新成功", Toast.LENGTH_SHORT).show()
                        } else {
                            taskDao.insertColor(
                                TaskColor(
                                    colorName = name,
                                    colorTag = hex,
                                    isDefault = false
                                )
                            )
                            Toast.makeText(requireContext(), "新增成功", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "請輸入名稱並選擇顏色", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}