package com.example.nextup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.nextup.ui.theme.NextUpTheme
import com.nextup.core.Priority
import com.nextup.core.TodoItem
import com.nextup.core.TodoManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val todoManager = remember { TodoManager() }
            NextUpTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        Button(
                            onClick = { todoManager.nextDayMigration() },
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Text("Next Day")
                        }
                    }
                ) { innerPadding ->
                    TodoList(
                        todoManager = todoManager,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodoList(todoManager: TodoManager, modifier: Modifier = Modifier) {
    val todos by todoManager.todos.collectAsState()
    val listState = rememberLazyListState()
    
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(todos) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        listState.layoutInfo.visibleItemsInfo
                            .find { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
                            ?.let { item ->
                                if (item.key is Int) {
                                    draggedItemIndex = item.index
                                }
                            }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        draggingOffset += dragAmount.y
                        
                        val draggedIndex = draggedItemIndex ?: return@detectDragGesturesAfterLongPress
                        val currentItems = listState.layoutInfo.visibleItemsInfo
                        val draggedItem = currentItems.find { it.index == draggedIndex } ?: return@detectDragGesturesAfterLongPress
                        
                        val targetItem = currentItems.find { item ->
                            val middle = draggedItem.offset + draggingOffset + draggedItem.size / 2
                            middle.toInt() in item.offset..(item.offset + item.size) && 
                            item.index != draggedIndex && 
                            item.key is Int
                        }

                        if (targetItem != null) {
                            todoManager.moveTodo(draggedItem.key as Int, targetItem.key as Int)
                            draggedItemIndex = targetItem.index
                            draggingOffset = 0f
                        }
                    },
                    onDragEnd = {
                        draggedItemIndex = null
                        draggingOffset = 0f
                    },
                    onDragCancel = {
                        draggedItemIndex = null
                        draggingOffset = 0f
                    }
                )
            }
    ) {
        Priority.values().forEach { priority ->
            stickyHeader(key = "header_${priority.name}") {
                PriorityHeader(
                    priority = priority,
                    onAddClick = { todoManager.addEmptyTodo(priority) }
                )
            }
            
            val itemsInPriority = todos.filter { it.priority == priority }
            itemsIndexed(itemsInPriority, key = { _, todo -> todo.id }) { _, todo ->
                val isDragging = draggedItemIndex != null && 
                                listState.layoutInfo.visibleItemsInfo.find { it.key == todo.id }?.index == draggedItemIndex
                
                val shadowElevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "shadow")
                
                SwipeableTodoRow(
                    modifier = Modifier
                        .graphicsLayer {
                            if (isDragging) {
                                translationY = draggingOffset
                                scaleX = 1.05f
                                scaleY = 1.05f
                                alpha = 0.9f
                            }
                        }
                        .zIndex(if (isDragging) 1f else 0f)
                        .shadow(shadowElevation),
                    todo = todo,
                    onTitleChange = { newTitle -> todoManager.updateTitle(todo.id, newTitle) },
                    onToggle = { todoManager.toggleTodo(todo.id) },
                    onToggleRecurring = { todoManager.toggleRecurring(todo.id) },
                    onDelete = { todoManager.deleteTodo(todo.id) },
                    onDefer = { todoManager.deferTodo(todo.id) }
                )
            }
        }
    }
}

@Composable
fun PriorityHeader(priority: Priority, onAddClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = priority.name,
            style = MaterialTheme.typography.labelLarge,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onAddClick) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Task",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTodoRow(
    todo: TodoItem,
    onTitleChange: (String) -> Unit,
    onToggle: () -> Unit,
    onToggleRecurring: () -> Unit,
    onDelete: () -> Unit,
    onDefer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onDefer()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Color.Blue.copy(alpha = 0.5f)
                    SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.5f)
                    else -> Color.Transparent
                }, label = "dismissColor"
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
            ) {
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Icon(Icons.Default.PlayArrow, contentDescription = "Defer")
                    SwipeToDismissBoxValue.EndToStart -> Icon(Icons.Default.Delete, contentDescription = "Delete")
                    else -> {}
                }
            }
        },
        content = {
            TodoRow(
                todo = todo,
                onTitleChange = onTitleChange,
                onToggle = onToggle,
                onToggleRecurring = onToggleRecurring
            )
        }
    )
}

@Composable
fun TodoRow(
    todo: TodoItem,
    onTitleChange: (String) -> Unit,
    onToggle: () -> Unit,
    onToggleRecurring: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = todo.isDone,
            onCheckedChange = { onToggle() }
        )
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            androidx.compose.foundation.text.BasicTextField(
                value = todo.title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = if (todo.isDone) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
            )
            if (todo.deferCount > 0) {
                Text(
                    text = "Deferred ${todo.deferCount} times",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        IconButton(onClick = onToggleRecurring) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Recurring",
                tint = if (todo.isRecurring) MaterialTheme.colorScheme.primary else Color.LightGray
            )
        }
    }
}
