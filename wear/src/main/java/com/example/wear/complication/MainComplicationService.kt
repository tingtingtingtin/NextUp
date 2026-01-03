package com.example.wear.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.nextup.core.TodoManager
import kotlinx.coroutines.flow.first

class MainComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) {
            return null
        }
        return createComplicationData("2/9", "2 out of 9 tasks done")
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val todoManager = TodoManager()
        val todos = todoManager.todos.first()
        
        val total = todos.size
        val done = todos.count { it.isDone }
        
        return createComplicationData("$done/$total", "$done out of $total tasks done")
    }

    private fun createComplicationData(text: String, contentDescription: String) =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        ).build()
}
