package com.example.nextup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.nextup.ui.theme.NextUpTheme
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TodoList(todoManager: TodoManager, modifier: Modifier = Modifier) {
    val todos by todoManager.todos.collectAsState()
    val grouped = todos.groupBy { it.priority }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        grouped.forEach { (priority, items) ->
            stickyHeader {
                Text(
                    text = priority.name,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray
                )
            }
            items(items, key = { it.id }) { todo ->
                TodoRow(
                    todo = todo,
                    onTitleChange = { newTitle -> todoManager.updateTitle(todo.id, newTitle) },
                    onToggle = { todoManager.toggleTodo(todo.id) },
                    onToggleRecurring = { todoManager.toggleRecurring(todo.id) }
                )
            }
        }
    }
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
