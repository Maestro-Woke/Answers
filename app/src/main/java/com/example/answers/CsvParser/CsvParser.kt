package com.example.answers

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.answers.TaskEntity.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class CsvParser(private val context: Context) {

    suspend fun parseFromAssets(fileName: String): List<TaskEntity> = withContext(Dispatchers.IO) {
        val tasks = mutableListOf<TaskEntity>()
        try {
            context.assets.open(fileName).use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                parseTasks(reader, tasks)
            }
            Log.d("CsvParser", "Загружено из assets: ${tasks.size} задач")
        } catch (e: Exception) {
            Log.e("CsvParser", "Ошибка assets: ${e.message}", e)
        }
        tasks
    }

    suspend fun parseFromUri(context: Context, uri: Uri): List<TaskEntity> = withContext(Dispatchers.IO) {
        val tasks = mutableListOf<TaskEntity>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                parseTasks(reader, tasks)
            }
            Log.d("CsvParser", "Загружено из файла: ${tasks.size} задач")
        } catch (e: Exception) {
            Log.e("CsvParser", "Ошибка файла: ${e.message}", e)
        }
        tasks
    }

    private fun parseTasks(reader: BufferedReader, tasks: MutableList<TaskEntity>) {
        try {
            // Читаем первую строку (заголовки)
            val headerLine = readCsvRecord(reader) ?: return
            Log.d("CsvParser", "📋 Заголовок (первые 10): ${headerLine.take(10)}")

            // Первая колонка - "¹" или "№", остальные - номера КИМов
            val kimHeaders = headerLine.drop(1) // Пропускаем первую колонку

            // Берём только ПЕРВУЮ строку (до переноса) из каждого заголовка
            val kimNumbers = kimHeaders.map { header ->
                header.split("\n").first().trim()
            }

            Log.d("CsvParser", "🎯 Найдено ${kimNumbers.size} КИМов")
            Log.d("CsvParser", "📝 Первые 10 КИМов: ${kimNumbers.take(10)}")

            var lineNum = 1
            // Читаем остальные строки
            while (true) {
                val cells = readCsvRecord(reader) ?: break
                lineNum++

                if (cells.isEmpty()) continue

                // Первая колонка - номер задачи (берём только число, без ссылок)
                val taskNumberRaw = cells[0].trim()
                val taskNumber = taskNumberRaw.split("\n").first().trim()

                if (taskNumber.isEmpty()) continue

                Log.d("CsvParser", "📍 Обрабатываем задачу №$taskNumber")

                // Остальные колонки - ответы для каждого КИМа
                for (i in 1 until cells.size) {
                    if (i - 1 >= kimNumbers.size) break

                    val kimNumber = kimNumbers[i - 1]
                    val cellContent = cells[i].trim()

                    if (cellContent.isEmpty() || kimNumber.isEmpty()) continue

                    // Извлекаем только ответ (строки без http/https)
                    val lines = cellContent.split("\n").map { it.trim() }

                    // Берём только строки, которые НЕ содержат http/https
                    val answerLines = lines.filter {
                        it.isNotEmpty() &&
                                !it.contains("http://", ignoreCase = true) &&
                                !it.contains("https://", ignoreCase = true) &&
                                !it.contains("youtu.be", ignoreCase = true) &&
                                !it.contains("vk.com", ignoreCase = true)
                    }

                    val answer = answerLines.joinToString(" ").trim()

                    // Ищем первую ссылку (если есть)
                    val link = lines.firstOrNull {
                        it.startsWith("http://", ignoreCase = true) ||
                                it.startsWith("https://", ignoreCase = true)
                    } ?: ""

                    if (answer.isNotEmpty()) {
                        tasks.add(
                            TaskEntity(
                                taskNumber = taskNumber,
                                kimNumber = kimNumber,
                                answer = answer,
                                videoLink = link
                            )
                        )

                        // Отладка для КИМ=4, Задача=12
                        if (kimNumber == "4" && taskNumber == "12") {
                            Log.d("CsvParser", "НАЙДЕНО! КИМ=4, Задача=12")
                            Log.d("CsvParser", "   taskNumber='$taskNumber'")
                            Log.d("CsvParser", "   kimNumber='$kimNumber'")
                            Log.d("CsvParser", "   answer='$answer'")
                            Log.d("CsvParser", "   link='$link'")
                        }
                    }
                }
            }

            Log.d("CsvParser", "📊 Всего задач: ${tasks.size}")

        } catch (e: Exception) {
            Log.e("CsvParser", "💥 Ошибка парсинга: ${e.message}", e)
            e.printStackTrace()
        }
    }

    /**
     * Читает одну запись CSV (может быть многострочной из-за кавычек)
     */
    private fun readCsvRecord(reader: BufferedReader): List<String>? {
        val cells = mutableListOf<String>()
        val currentCell = StringBuilder()
        var inQuotes = false

        while (true) {
            val line = reader.readLine() ?: return if (cells.isEmpty()) null else cells

            var i = 0
            while (i < line.length) {
                val char = line[i]

                when {
                    // Кавычка - переключаем режим
                    char == '"' -> {
                        inQuotes = !inQuotes
                    }
                    // Запятая вне кавычек - конец ячейки
                    char == ',' && !inQuotes -> {
                        cells.add(currentCell.toString())
                        currentCell.clear()
                    }
                    // Обычный символ
                    else -> {
                        currentCell.append(char)
                    }
                }
                i++
            }

            // Если мы внутри кавычек - добавляем перенос строки и читаем дальше
            if (inQuotes) {
                currentCell.append('\n')
            } else {
                // Строка закончена
                cells.add(currentCell.toString())
                return cells
            }
        }
    }
}