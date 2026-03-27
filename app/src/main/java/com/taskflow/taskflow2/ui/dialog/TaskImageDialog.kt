package com.taskflow.taskflow2.ui.dialog

import android.app.Dialog
import android.content.Context
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import com.bumptech.glide.Glide
import com.taskflow.taskflow2.R
import com.taskflow.taskflow2.data.local.TaskEntity
import com.taskflow.taskflow2.data.local.TaskDatabase
import com.taskflow.taskflow2.databinding.DialogTaskImageBinding
import kotlinx.coroutines.launch

object TaskImageDialog {

    /**
     * 顯示任務圖片 Dialog
     * @param context 呼叫的 Context
     * @param task 要顯示的 TaskEntity
     * @param lifecycleScope 用來啟動 coroutine 更新資料庫
     * @param onDismiss optional，Dialog 被關閉後 callback
     */
    fun show(
        context: Context,
        task: TaskEntity,
        lifecycleScope: LifecycleCoroutineScope,
        onDismiss: (() -> Unit)? = null
    ) {
        if (task.imageUri.isNullOrEmpty()) return

        val binding = DialogTaskImageBinding.inflate(android.view.LayoutInflater.from(context))
        val dialog = Dialog(context)
        dialog.setContentView(binding.root)

        // 顯示圖片
        Glide.with(context)
            .load(task.imageUri)
            .placeholder(R.drawable.ic_image_placeholder)
            .into(binding.ivPreview)

        // switch 狀態與文字
        binding.switchUsed.isChecked = task.isCompleted
        binding.switchUsed.text = if (task.isCompleted) "已使用" else "未使用"

        binding.switchUsed.setOnCheckedChangeListener { _, isChecked ->
            binding.switchUsed.text = if (isChecked) "已使用" else "未使用"
            lifecycleScope.launch {
                TaskDatabase.getInstance(context).taskDao()
                    .updateTask(task.copy(isCompleted = isChecked))
            }
        }

        binding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.setOnDismissListener { onDismiss?.invoke() }

        // 先 show()，再設定大小
        dialog.show()
        val width = (context.resources.displayMetrics.widthPixels * 0.95).toInt()
        val height = (context.resources.displayMetrics.heightPixels * 0.75).toInt()
        dialog.window?.setLayout(width, height)
    }
}
