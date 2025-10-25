package com.example.answers

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.answers.AppDatabase.AppDatabase
import com.example.answers.TaskRepository.TaskRepository
import com.example.answers.ui.theme.AnswersTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: TaskRepository
    private lateinit var csvParser: CsvParser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = AppDatabase.getDatabase(this)
        repository = TaskRepository(database.taskDao())
        csvParser = CsvParser(this)

        setContent {
            AnswersTheme {
                MainScreen(repository, csvParser)
            }
        }
    }
}

@Composable
fun getAdaptiveSizes(): ScreenSizes {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val screenWidth = configuration.screenWidthDp

    return when {
        screenWidth > 900 -> ScreenSizes(
            titleSize = 72.sp,
            subtitleSize = 48.sp,
            inputTextSize = 50.sp,
            buttonTextSize = 44.sp,
            inputHeight = 120.dp,
            buttonHeight = 120.dp,
            padding = 64.dp,
            fieldWidth = 0.6f,
            verticalSpacing = 48.dp,
            sectionSpacing = 72.dp
        )
        screenWidth > 600 -> ScreenSizes(
            titleSize = 64.sp,
            subtitleSize = 40.sp,
            inputTextSize = 30.sp,
            buttonTextSize = 40.sp,
            inputHeight = 100.dp,
            buttonHeight = 100.dp,
            padding = 48.dp,
            fieldWidth = 0.7f,
            verticalSpacing = 40.dp,
            sectionSpacing = 60.dp
        )
        screenHeight > 800 -> ScreenSizes(
            titleSize = 48.sp,
            subtitleSize = 28.sp,
            inputTextSize = 40.sp,
            buttonTextSize = 32.sp,
            inputHeight = 80.dp,
            buttonHeight = 80.dp,
            padding = 24.dp,
            fieldWidth = 0.9f,
            verticalSpacing = 38.dp,
            sectionSpacing = 38.dp
        )
        else -> ScreenSizes(
            titleSize = 40.sp,
            subtitleSize = 24.sp,
            inputTextSize = 36.sp,
            buttonTextSize = 28.sp,
            inputHeight = 70.dp,
            buttonHeight = 70.dp,
            padding = 16.dp,
            fieldWidth = 0.95f,
            verticalSpacing = 20.dp,
            sectionSpacing = 32.dp
        )
    }
}

@Composable
fun MainScreen(repository: TaskRepository, csvParser: CsvParser) {
    var kimText by rememberSaveable { mutableStateOf("") }
    var taskText by rememberSaveable { mutableStateOf("") }
    var answerText by rememberSaveable { mutableStateOf("ответ") }
    var linkText by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var dbCount by rememberSaveable { mutableStateOf(0) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sizes = getAdaptiveSizes()

    // ДЛЯ УПРАВЛЕНИЯ КЛАВИАТУРОЙ И ФОКУСОМ
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        Log.d("MainActivity", "LaunchedEffect запущен")

        try {
            val count = repository.getCount()
            dbCount = count
            Log.d("MainActivity", "Записей в БД: $count")

            if (count == 0) {
                isLoading = true
                Log.d("MainActivity", "БД пуста, загружаем из assets...")
                try {
                    val tasks = csvParser.parseFromAssets("answers.csv")
                    Log.d("MainActivity", "Получено из assets: ${tasks.size}")
                    repository.insertTasks(tasks)
                    dbCount = tasks.size
                    Log.d("MainActivity", "Загружено: $dbCount")
                } catch (e: Exception) {
                    Log.e("MainActivity", "Ошибка assets: ${e.message}", e)
                }
                isLoading = false
            } else {
                Log.d("MainActivity", "В БД уже есть записей: $count")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Критическая ошибка: ${e.message}", e)
        }
    }

    // Launcher для выбора CSV
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isLoading = true
                Log.d("MainActivity", "Загружаем CSV из $it")

                try {
                    val tasks = csvParser.parseFromUri(context, it)
                    Log.d("MainActivity", "Получено задач: ${tasks.size}")

                    repository.deleteAll()
                    repository.insertTasks(tasks)
                    dbCount = tasks.size
                    answerText = "Загружено ${tasks.size} задач"
                    linkText = ""

                    Log.d("MainActivity", "Сохранено в БД: $dbCount")
                } catch (e: Exception) {
                    Log.e("MainActivity", "Ошибка загрузки: ${e.message}", e)
                    answerText = "Ошибка: ${e.message}"
                }

                isLoading = false
            }
        }
    }

    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0F4C75),
            Color(0xFF5B6F9E),
            Color(0xFFA88FBD)
        )
    )

    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFF6B9D),
            Color(0xFFFFA07A),
            Color(0xFFFFD54F)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(sizes.padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "КИМ",
                fontSize = sizes.titleSize,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(sizes.verticalSpacing))

            OutlinedTextField(
                value = kimText,
                onValueChange = { kimText = it },
                modifier = Modifier
                    .fillMaxWidth(sizes.fieldWidth)
                    .height(sizes.inputHeight),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = sizes.inputTextSize,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(sizes.sectionSpacing))

            Text(
                text = "задача",
                fontSize = sizes.titleSize,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(sizes.verticalSpacing))

            OutlinedTextField(
                value = taskText,
                onValueChange = { taskText = it },
                modifier = Modifier
                    .fillMaxWidth(sizes.fieldWidth)
                    .height(sizes.inputHeight),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = sizes.inputTextSize,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        // При нажатии "Готово" скрываем клавиатуру
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(sizes.sectionSpacing))

            Button(
                onClick = {
                    // СКРЫВАЕМ КЛАВИАТУРУ И УБИРАЕМ ФОКУС
                    keyboardController?.hide()
                    focusManager.clearFocus()

                    if (kimText.isNotEmpty() && taskText.isNotEmpty()) {
                        scope.launch {
                            val kim = kimText.trim()
                            val task = taskText.trim()

                            Log.d("MainActivity", "Ищем: КИМ='$kim', Задача='$task'")

                            val result = repository.getTask(kim, task)

                            if (result != null) {
                                Log.d("MainActivity", "Найдено: ${result.answer}")
                                answerText = result.answer
                                linkText = result.videoLink
                            } else {
                                Log.e("MainActivity", "Не найдено в БД")
                                answerText = "Не найдено"
                                linkText = ""
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(sizes.fieldWidth)
                    .height(sizes.buttonHeight),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp),
                enabled = !isLoading
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = buttonGradient, shape = RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLoading) "Загрузка..." else "ОТВЕТ",
                        fontSize = sizes.buttonTextSize,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A237E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(sizes.sectionSpacing))

            Text(
                text = answerText,
                fontSize = sizes.titleSize,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = sizes.titleSize,
                softWrap = true,
                maxLines = Int.MAX_VALUE
            )

            if (linkText.isNotEmpty() && linkText.startsWith("http")) {
                Spacer(modifier = Modifier.height(sizes.verticalSpacing))

                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkText))
                        context.startActivity(intent)
                    }
                ) {
                    Text(
                        text = "Открыть видео",
                        fontSize = sizes.subtitleSize,
                        color = Color.White,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                }
            }

            Spacer(modifier = Modifier.height(sizes.sectionSpacing))

            Button(
                onClick = { csvLauncher.launch("text/*") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    "Загрузить CSV ($dbCount)",
                    color = Color.White,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}