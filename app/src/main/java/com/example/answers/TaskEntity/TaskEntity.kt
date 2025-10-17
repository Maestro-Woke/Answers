package com.example.answers.TaskEntity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val taskNumber: String,     // Номер задачи (1, 2, 3...)
    val kimNumber: String,      // Номер КИМа (4 1, 4 2, 3 4...)
    val answer: String,         // Ответ
    val videoLink: String = ""  // Ссылка (опционально)
)

