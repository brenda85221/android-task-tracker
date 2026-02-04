package com.taskflow.taskflow2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_colors")
data class TaskColor(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val colorName: String,      // "Red", "Blue", 自訂名稱
    val colorTag: String,        // "#F44336", 十六進位
    val isDefault: Boolean = false
)
