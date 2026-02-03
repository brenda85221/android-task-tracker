package com.taskflow.taskflow2.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.taskflow.taskflow2.util.NotificationUtils

class TaskReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // 測試用：顯示一則固定內容的通知
        NotificationUtils.createNotificationChannel(applicationContext)
        NotificationUtils.showTaskReminder(
            context = applicationContext,
            taskId = 1,
            taskTitle = "TaskFlow 測試提醒",
            taskDescription = "這是一則背景任務測試通知。"
        )
        return Result.success()
    }
}
