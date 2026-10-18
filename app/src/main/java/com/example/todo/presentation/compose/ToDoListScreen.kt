package com.example.todo.presentation.compose

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.todo.domain.model.ToDo
import com.example.todo.presentation.uistate.Screen
import com.example.todo.presentation.viewmodel.AuthViewModel
import com.example.todo.presentation.viewmodel.TodoListViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToDoListScreen(
    modifier: Modifier = Modifier,
    viewModel: TodoListViewModel,
    authViewModel: AuthViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var isInputVisible by remember { mutableStateOf(false) }
    var selectedTaskForDialog by remember { mutableStateOf<ToDo?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }

    // State for temporary date/time selection and validation alert
    var selectedDueDateMillis by remember { mutableStateOf<Long?>(null) }
    var showTimeRequiredAlert by remember { mutableStateOf(false) }

    val displayedItems = when (uiState.currentScreen) {
        Screen.HOME -> uiState.items.filter { !it.isDone }
        Screen.COMPLETED -> uiState.items.filter { it.isDone }
    }

    fun submitTask(dueTimestamp: Long? = selectedDueDateMillis) {
        if (uiState.input.trim().isEmpty()) return

        // MANDATORY CHECK: If time is not selected, prompt the user
        val finalTimestamp = dueTimestamp ?: uiState.editingTodo?.dueDateMillis
        if (finalTimestamp == null) {
            showTimeRequiredAlert = true
            return
        }

        viewModel.addOrUpdateTodo(finalTimestamp)
        isInputVisible = false
        selectedDueDateMillis = null
        focusManager.clearFocus()
    }

    fun showDateTimePickerAndSubmit() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)

                        val timeInMillis = calendar.timeInMillis
                        selectedDueDateMillis = timeInMillis
                        submitTask(timeInMillis)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    LaunchedEffect(isInputVisible, uiState.editingTodo) {
        if (isInputVisible || uiState.editingTodo != null) {
            focusRequester.requestFocus()
            if (uiState.editingTodo != null) {
                selectedDueDateMillis = uiState.editingTodo?.dueDateMillis
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.currentScreen == Screen.HOME) "✨ My Tasks" else "🏆 Completed",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(onClick = { showProfileDialog = true }) {
                        val imageUri = authState.profileImageUri
                        if (!imageUri.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = imageUri,
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                error = {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = "Fallback Profile",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isHomeActive = uiState.currentScreen == Screen.HOME
                    IconButton(onClick = { viewModel.selectScreen(Screen.HOME); isInputVisible = false }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "Home",
                                tint = if (isHomeActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                "Home",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isHomeActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    if (uiState.currentScreen == Screen.HOME) {
                        FloatingActionButton(
                            onClick = {
                                if (!isInputVisible && uiState.editingTodo == null) {
                                    isInputVisible = true
                                } else {
                                    submitTask()
                                }
                            },
                            modifier = Modifier.size(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                imageVector = if (isInputVisible || uiState.editingTodo != null) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = "Add or Save"
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(52.dp))
                    }

                    val isCompletedActive = uiState.currentScreen == Screen.COMPLETED
                    IconButton(onClick = { viewModel.selectScreen(Screen.COMPLETED); isInputVisible = false }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = if (isCompletedActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                "Completed",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isCompletedActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (uiState.currentScreen == Screen.HOME && (isInputVisible || uiState.editingTodo != null)) {
                    Column {
                        OutlinedTextField(
                            value = uiState.input,
                            onValueChange = { viewModel.onInputChanged(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            label = { Text(if (uiState.editingTodo != null) "Edit task" else "What needs to be done?") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { submitTask() }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ),
                            trailingIcon = {
                                IconButton(onClick = { showDateTimePickerAndSubmit() }) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = "Pick Date/Time",
                                        tint = if (selectedDueDateMillis != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (displayedItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (uiState.currentScreen == Screen.HOME) "🎉 No pending tasks!" else "📭 No completed tasks yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(displayedItems, key = { it.id }) { item ->
                            ToDoListRow(
                                item = item,
                                onClick = { selectedTaskForDialog = item }
                            )
                        }
                    }
                }
            }
        }

        // Time Required Alert Dialog
        if (showTimeRequiredAlert) {
            AlertDialog(
                onDismissRequest = { showTimeRequiredAlert = false },
                title = { Text("Time Required", fontWeight = FontWeight.Bold) },
                text = { Text("Please choose a due date and time for the task before adding it.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showTimeRequiredAlert = false
                            showDateTimePickerAndSubmit()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Choose Time")
                    }
                }
            )
        }

        selectedTaskForDialog?.let { task ->
            TaskActionDialog(
                item = task,
                onDismiss = { selectedTaskForDialog = null },
                onToggleComplete = { viewModel.toggleTodo(task) },
                onEdit = {
                    viewModel.startEditing(task)
                    isInputVisible = true
                },
                onDelete = { viewModel.deleteTodo(task) }
            )
        }

        if (showProfileDialog) {
            ProfileDialog(
                currentImageUri = authState.profileImageUri,
                userEmail = authState.emailInput,
                onDismiss = { showProfileDialog = false },
                onSaveProfile = { name, newImageUri ->
                    authViewModel.updateProfile(name, newImageUri)
                },
                onLogout = {
                    authViewModel.logout()
                }
            )
        }
    }
}