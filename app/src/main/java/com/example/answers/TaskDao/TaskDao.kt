package com.example.answers.TaskDao

import com.example.answers.TaskEntity.TaskEntity
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Query("SELECT * FROM tasks WHERE kimNumber = :kim AND taskNumber =:task LIMIT 1")
    suspend fun getTask(kim: String, task: String): TaskEntity?

    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<TaskEntity>>


    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getCount(): Int

    @Query("SELECT * FROM tasks WHERE kimNumber = :kimNumber")
    suspend fun getTasksByKim(kimNumber: String): List<TaskEntity>
}