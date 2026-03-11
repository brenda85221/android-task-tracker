package com.taskflow.taskflow2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taskflow.taskflow2.data.local.TaskEntity
import com.taskflow.taskflow2.data.local.TaskWithColor
import com.taskflow.taskflow2.data.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class CalendarViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _selectedColorFilter = MutableStateFlow<String?>(null)
    private val _currentMonth = MutableStateFlow(Calendar.getInstance())

    val tasksInMonth: StateFlow<List<TaskWithColor>> =
        combine(repository.getAllTasks(), _selectedColorFilter, _currentMonth) { tasks, colorFilter, monthCal ->
            val monthStart = Calendar.getInstance().apply {
                time = monthCal.time
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val monthEnd = Calendar.getInstance().apply {
                time = monthCal.time
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            tasks.filter { task ->
                val inMonth = task.task.dueAt in monthStart.timeInMillis..monthEnd.timeInMillis
                val colorMatch = colorFilter?.let { task.color?.colorTag == it } ?: true
                inMonth && colorMatch
            }.sortedWith(compareBy<TaskWithColor> { it.task.isCompleted }.thenBy { it.task.dueAt })
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setColorFilter(colorTag: String?) {
        _selectedColorFilter.value = colorTag
    }

    fun setMonth(calendar: Calendar) {
        _currentMonth.value = calendar
    }

    fun updateTask(task: TaskEntity) = viewModelScope.launch { repository.updateTask(task) }

    fun deleteTask(task: TaskEntity) = viewModelScope.launch { repository.deleteTask(task) }

    fun toggleTaskCompleted(taskWithColor: TaskWithColor, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateTask(taskWithColor.task.copy(isCompleted = isCompleted))
        }
    }
}

// ---------------- Factory ----------------
class CalendarViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(CalendarViewModel::class.java) -> CalendarViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}