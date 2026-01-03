package com.example.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import com.example.wear.presentation.theme.NextUpTheme
import com.nextup.core.TodoItem
import com.nextup.core.TodoManager
import com.nextup.core.Priority
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forward
import androidx.wear.compose.foundation.lazy.AutoCenteringParams

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            val todoManager = remember { TodoManager() }
            WearApp(todoManager)
        }
    }
}

@Composable
fun WearApp(todoManager: TodoManager) {
    val todos by todoManager.todos.collectAsState()
    val listState = rememberScalingLazyListState()
    var selectedTodo by remember { mutableStateOf<TodoItem?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    NextUpTheme {
        Scaffold(
            timeText = { TimeText() },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
            positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                autoCentering = AutoCenteringParams(itemIndex = 0)
            ) {
                item {
                    ListHeader {
                        Text("NextUp Tasks")
                    }
                }
                items(todos) { todo ->
                    TodoChip(
                        todo = todo,
                        onClick = { todoManager.toggleTodo(todo.id) },
                        onLongClick = {
                            selectedTodo = todo
                            showDialog = true
                        }
                    )
                }
            }

            Dialog(
                showDialog = showDialog,
                onDismissRequest = { showDialog = false }
            ) {
                Alert(
                    title = { Text(selectedTodo?.title ?: "Task", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    positiveButton = {
                        Button(onClick = {
                            selectedTodo?.let { todoManager.deferTodo(it.id) }
                            showDialog = false
                        }) {
                            Icon(Icons.Default.Forward, contentDescription = "Defer")
                        }
                    },
                    negativeButton = {
                        Button(onClick = {
                            selectedTodo?.let { todoManager.deleteTodo(it.id) }
                            showDialog = false
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                ) {
                    Text("Action?")
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TodoChip(
    todo: TodoItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        ToggleChip(
            onCheckedChange = { },
            enabled = true,
            modifier = Modifier.fillMaxWidth(),
            checked = todo.isDone,
            label = {
                Text(
                    text = todo.title.ifBlank { "Empty ${todo.priority}" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = if (todo.isDone) LocalTextStyle.current.copy(
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                        color = Color.Gray
                    ) else LocalTextStyle.current
                )
            },
            secondaryLabel = {
                Text(text = todo.priority.name.lowercase(), color = when(todo.priority) {
                    Priority.HIGH -> Color(0xFFE57373)
                    Priority.MEDIUM -> Color(0xFFFFF176)
                    Priority.LOW -> Color(0xFF81C784)
                })
            },
            toggleControl = {
                ToggleChipDefaults.checkboxIcon(checked = todo.isDone)
            }
        )
    }
}