package com.nextup.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar

class TodoManager(
    private var highCount: Int = 1,
    private var mediumCount: Int = 3,
    private var lowCount: Int = 5
) {
    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos.asStateFlow()

    init {
        generateMissingSlots()
    }

    private fun generateMissingSlots() {
        _todos.update { currentList ->
            val newList = currentList.toMutableList()
            var nextId = (newList.maxOfOrNull { it.id } ?: -1) + 1

            val currentHigh = newList.count { it.priority == Priority.HIGH }
            val currentMedium = newList.count { it.priority == Priority.MEDIUM }
            val currentLow = newList.count { it.priority == Priority.LOW }

            repeat((highCount - currentHigh).coerceAtLeast(0)) {
                newList.add(TodoItem(id = nextId++, title = "", priority = Priority.HIGH))
            }
            repeat((mediumCount - currentMedium).coerceAtLeast(0)) {
                newList.add(TodoItem(id = nextId++, title = "", priority = Priority.MEDIUM))
            }
            repeat((lowCount - currentLow).coerceAtLeast(0)) {
                newList.add(TodoItem(id = nextId++, title = "", priority = Priority.LOW))
            }
            return@update newList.sortedBy { it.priority }
        }
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

    fun toggleRecurring(id: Int) {
        _todos.update { list ->
            list.map {
                if (it.id == id) it.copy(isRecurring = !it.isRecurring) else it
            }
        }
    }

    fun deleteTodo(id: Int) {
        _todos.update { list ->
            list.filterNot { it.id == id }
        }
    }

    fun nextDayMigration() {
        _todos.update { list ->
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val tomorrow = java.time.LocalDate.now().plusDays(1)

            list.mapNotNull { item ->
                when {
                    item.isDone && !item.isRecurring -> null // Delete
                    item.isRecurring -> item.copy(isDone = false, dueDate = tomorrow) // Refresh
                    !item.isDone && item.title.isNotBlank() -> item.copy(
                        deferCount = item.deferCount + 1,
                        dueDate = tomorrow
                    ) // Defer
                    else -> item
                }
            }
        }
        generateMissingSlots()
    }
    
    fun updateDefaultCounts(high: Int, medium: Int, low: Int) {
        highCount = high
        mediumCount = medium
        lowCount = low
        generateMissingSlots()
    }
}
