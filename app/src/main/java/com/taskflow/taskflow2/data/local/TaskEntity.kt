package com.taskflow.taskflow2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val notes: String = "",

    val createdAt: Long = System.currentTimeMillis(),
    val dueAt: Long,

    val reminderType: String = "ONCE",
    val reminderDays: Int = 0,
    val isReminderEnabled: Boolean = true,

    val colorTag: String = "#2196F3",
    val categoryName: String = "General",
    val imageUri: String? = null,

    val isRecurring: Boolean = false,
    val recurringPattern: String = "",
    val recurringEndDate: Long? = null,

    val isCompleted: Boolean = false,            // ← 合併進來
    val completedAt: Long? = null,

    val priority: Int = 1,
    val isPinned: Boolean = false                // ← 有這欄位
)
