package com.nextup.core
import java.time.LocalDate

enum class Priority {
    HIGH, MEDIUM, LOW
}

data class TodoItem(
    val id: Int,
    val title: String,
    val priority: Priority,
    val isDone: Boolean = false,
    val isRecurring: Boolean = false,
    val deferCount: Int = 0,
    val dueDate: LocalDate = LocalDate.now()
)
