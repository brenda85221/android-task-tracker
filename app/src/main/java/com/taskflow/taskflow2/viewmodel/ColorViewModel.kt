package com.taskflow.taskflow2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskflow.taskflow2.data.local.TaskColor
import com.taskflow.taskflow2.data.local.TaskEntity
import com.taskflow.taskflow2.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ColorViewModel(private val repository: TaskRepository) : ViewModel() {

    // 監控顏色列表
    val allColors: StateFlow<List<TaskColor>> = repository.getAllColors()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 插入顏色
    fun insertColor(color: TaskColor) {
        viewModelScope.launch {
            repository.insertColor(color)
        }
    }

    // 更新顏色
    fun updateColor(color: TaskColor) {
        viewModelScope.launch {
            repository.updateColor(color)
        }
    }

    fun deleteColor(color: TaskColor) {
        viewModelScope.launch {
            repository.deleteColor(color)
        }
    }
}

// Factory
class ColorViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ColorViewModel::class.java) -> ColorViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}