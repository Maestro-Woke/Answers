package com.example.answers.TaskRepository



import com.example.answers.TaskDao.TaskDao
import com.example.answers.TaskEntity.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()

    suspend fun insertTasks(tasks: List<TaskEntity>) {
        taskDao.insertAll(tasks)
    }

    suspend fun getTask(kim: String, task: String): TaskEntity? {
        return taskDao.getTask(kim, task)
    }

    suspend fun deleteAll() {
        taskDao.deleteAll()
    }

    suspend fun getCount(): Int {
        return taskDao.getCount()
    }

    // В файле TaskRepository.kt добавьте:

    suspend fun getTasksByKim(kimNumber: String): List<TaskEntity> = withContext(Dispatchers.IO) {
        taskDao.getTasksByKim(kimNumber)
    }
}