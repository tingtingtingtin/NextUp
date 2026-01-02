package com.nextup.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TodoManager(
    highCount: Int = 1,
    mediumCount: Int = 3,
    lowCount: Int = 5
) {
    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos.asStateFlow()

    init {
        val initialList = mutableListOf<TodoItem>()
        var currentId = 0
        repeat(highCount) {
            initialList.add(TodoItem(id = currentId++, title = "", priority = Priority.HIGH))
        }
        repeat(mediumCount) {
            initialList.add(TodoItem(id = currentId++, title = "", priority = Priority.MEDIUM))
        }
        repeat(lowCount) {
            initialList.add(TodoItem(id = currentId++, title = "", priority = Priority.LOW))
        }
        _todos.value = initialList
    }

    fun updateTitle(id: Int, newTitle: String) {
        _todos.update { list ->
            list.map {
                if (it.id == id) it.copy(title = newTitle) else it
            }
        }
    }

    fun toggleTodo(id: Int) {
        _todos.update { list ->
            list.map {
                if (it.id == id) it.copy(isDone = !it.isDone) else it
            }
        }
    }
}
