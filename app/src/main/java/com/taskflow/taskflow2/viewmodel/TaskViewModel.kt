package com.taskflow.taskflow2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskflow.taskflow2.data.local.TaskEntity
import com.taskflow.taskflow2.data.local.TaskWithColor
import com.taskflow.taskflow2.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    // ---------------- Tasks & Colors ----------------
    val tasks: StateFlow<List<TaskWithColor>> = repository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allColors: StateFlow<List<com.taskflow.taskflow2.data.local.TaskColor>> = repository.getAllColors()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ---------------- Task 操作 ----------------

    fun insertTask(task: TaskEntity) = viewModelScope.launch {
        repository.insertTask(task)
    }

    fun updateTask(task: TaskEntity) = viewModelScope.launch {
        repository.updateTask(task)
    }

    fun deleteTask(task: TaskEntity) = viewModelScope.launch {
        repository.deleteTask(task)
    }

    fun toggleTaskCompleted(taskWithColor: TaskWithColor, isCompleted: Boolean) {
        viewModelScope.launch {
            val updated = taskWithColor.task.copy(isCompleted = isCompleted)
            repository.updateTask(updated)
        }
    }

    // ---------------- getTaskById 封裝 Coroutine ----------------
    private val _currentTask = MutableStateFlow<TaskWithColor?>(null)
    val currentTask: StateFlow<TaskWithColor?> = _currentTask

    fun loadTaskById(id: Long) {
        viewModelScope.launch {
            val task = repository.getTaskById(id) // suspend call
            _currentTask.value = task
        }
    }
}

