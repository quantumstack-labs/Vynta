package com.first_project.chronoai.ui1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.first_project.chronoai.data.CalendarRepository
import com.first_project.chronoai.data.local.dao.TaskDao

class HomeViewModelFactory(
    private val repository: CalendarRepository,
    private val taskDao: TaskDao,
    private val aiManager: com.first_project.chronoai.ai.GroqManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, taskDao, aiManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
