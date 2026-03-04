package com.taskflow.taskflow2.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // ---------------- Tasks ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    // 🔥 回傳關聯資料 Task + Color
    @Transaction
    @Query("""
        SELECT * FROM tasks
        ORDER BY isPinned DESC, isCompleted ASC, dueAt ASC
    """)
    fun getAllTasks(): Flow<List<TaskWithColor>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskWithColor?

    // ---------------- Colors ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColor(color: TaskColor): Long

    @Update
    suspend fun updateColor(color: TaskColor)

    @Delete
    suspend fun deleteColor(color: TaskColor)

    @Query("SELECT * FROM task_colors ORDER BY isDefault DESC")
    fun getAllColors(): Flow<List<TaskColor>>

    @Query("SELECT * FROM task_colors WHERE id = :id")
    suspend fun getColorById(id: Int): TaskColor?
}