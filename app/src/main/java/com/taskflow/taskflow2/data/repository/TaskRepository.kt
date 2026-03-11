package com.taskflow.taskflow2.data.repository

import com.taskflow.taskflow2.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepository(private val taskDao: TaskDao) {

    // ---------------- Tasks ----------------

    // 回傳所有任務（Flow）
    fun getAllTasks(): Flow<List<TaskWithColor>> = taskDao.getAllTasks()

    // 根據顏色過濾任務
    fun getTasksByColor(colorTag: String): Flow<List<TaskWithColor>> =
        taskDao.getAllTasks().map { tasks ->
            tasks.filter { it.color?.colorTag == colorTag }
        }

    // 根據 ID 查任務
    suspend fun getTaskById(id: Long): TaskWithColor? = taskDao.getTaskById(id)

    // 新增任務
    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)

    // 更新任務
    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)

    // 刪除任務
    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)

    // ---------------- Colors ----------------

    // 取得所有顏色
    fun getAllColors(): Flow<List<TaskColor>> = taskDao.getAllColors()

    // 根據 ID 查顏色
    suspend fun getColorById(id: Int): TaskColor? = taskDao.getColorById(id)

    // 新增顏色
    suspend fun insertColor(color: TaskColor): Long = taskDao.insertColor(color)

    // 更新顏色
    suspend fun updateColor(color: TaskColor) = taskDao.updateColor(color)

    // 刪除顏色
    suspend fun deleteColor(color: TaskColor) = taskDao.deleteColor(color)
}