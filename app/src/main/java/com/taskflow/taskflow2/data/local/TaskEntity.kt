package com.taskflow.taskflow2.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

//@Entity(tableName = "tasks")
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskColor::class,
            parentColumns = ["id"],
            childColumns = ["colorId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
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

    val colorId: Int? = null,

    val imageUri: String? = null,

    val isRecurring: Boolean = false,
    val recurringPattern: String = "",
    val recurringEndDate: Long? = null,

    val isCompleted: Boolean = false,            // ← 合併進來
    val completedAt: Long? = null,

    val priority: Int = 1,
    val isPinned: Boolean = false                // ← 有這欄位
)
