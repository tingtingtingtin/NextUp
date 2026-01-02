package com.nextup.core

enum class Priority {
    HIGH, MEDIUM, LOW
}

data class TodoItem(
    val id: Int,
    val title: String,
    val isDone: Boolean = false,
    val priority: Priority
)
