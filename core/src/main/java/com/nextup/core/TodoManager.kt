package com.nextup.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

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

            Priority.values().forEach { priority ->
                val currentCount = newList.count { it.priority == priority }
                val targetCount = when (priority) {
                    Priority.HIGH -> highCount
                    Priority.MEDIUM -> mediumCount
                    Priority.LOW -> lowCount
                }
                repeat((targetCount - currentCount).coerceAtLeast(0)) {
                    val insertIndex = newList.indexOfLast { it.priority == priority }.let {
                        if (it == -1) {
                            // Find the right boundary if group is empty
                            if (priority == Priority.HIGH) 0
                            else if (priority == Priority.MEDIUM) newList.indexOfFirst { it.priority == Priority.LOW }.let { if (it == -1) newList.size else it }
                            else newList.size
                        } else it + 1
                    }
                    newList.add(insertIndex, TodoItem(id = nextId++, title = "", priority = priority))
                }
            }
            newList
        }
    }

    fun addEmptyTodo(priority: Priority) {
        _todos.update { currentList ->
            val newList = currentList.toMutableList()
            val nextId = (newList.maxOfOrNull { it.id } ?: -1) + 1
            val newItem = TodoItem(id = nextId, title = "", priority = priority)
            
            val firstIndex = newList.indexOfFirst { it.priority == priority }
            if (firstIndex != -1) {
                newList.add(firstIndex, newItem)
            } else {
                val insertIndex = when (priority) {
                    Priority.HIGH -> 0
                    Priority.MEDIUM -> newList.indexOfFirst { it.priority == Priority.LOW }.let { if (it == -1) newList.size else it }
                    Priority.LOW -> newList.size
                }
                newList.add(insertIndex, newItem)
            }
            newList
        }
    }

    fun moveTodo(fromId: Int, toId: Int) {
        _todos.update { currentList ->
            val newList = currentList.toMutableList()
            val fromIndex = newList.indexOfFirst { it.id == fromId }
            val toIndex = newList.indexOfFirst { it.id == toId }
            
            if (fromIndex != -1 && toIndex != -1) {
                val item = newList.removeAt(fromIndex)
                newList.add(toIndex, item)
                
                // Update priority based on the items around it in the new position
                val newPriority = if (toIndex > 0 && toIndex < newList.size - 1) {
                    newList[toIndex - 1].priority
                } else if (toIndex == 0 && newList.size > 1) {
                    newList[1].priority
                } else if (toIndex == newList.size - 1 && newList.size > 1) {
                    newList[newList.size - 2].priority
                } else {
                    item.priority
                }
                
                newList[toIndex] = newList[toIndex].copy(priority = newPriority)
            }
            newList
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

    fun deferTodo(id: Int) {
        _todos.update { list ->
            val tomorrow = LocalDate.now().plusDays(1)
            list.map { item ->
                if (item.id == id) {
                    item.copy(
                        deferCount = item.deferCount + 1,
                        dueDate = tomorrow
                    )
                } else item
            }
        }
    }

    fun nextDayMigration() {
        _todos.update { list ->
            val tomorrow = LocalDate.now().plusDays(1)

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
