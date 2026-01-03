package com.example.wear.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.nextup.core.TodoManager
import com.nextup.core.Priority
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import kotlinx.coroutines.flow.first

private const val RESOURCES_VERSION = "1"

@OptIn(ExperimentalHorologistApi::class)
class MainTileService : SuspendingTileService() {

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ) = ResourceBuilders.Resources.Builder()
        .setVersion(RESOURCES_VERSION)
        .build()

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ): TileBuilders.Tile {
        val todoManager = TodoManager() 
        val todos = todoManager.todos.first()
        
        val highTask = todos.firstOrNull { it.priority == Priority.HIGH }
        val otherTasks = todos.filter { it != highTask && !it.isDone }.take(3)

        val column = LayoutElementBuilders.Column.Builder()
            .addContent(
                Text.Builder(this, highTask?.title?.ifBlank { "High Task" } ?: "No High Task")
                    .setTypography(Typography.TYPOGRAPHY_TITLE2)
                    .setColor(argb(0xFFFF4444.toInt())) 
                    .build()
            )

        otherTasks.forEach { task ->
            column.addContent(
                Text.Builder(this, task.title.ifBlank { "Task" })
                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                    .build()
            )
        }

        val root = PrimaryLayout.Builder(requestParams.deviceConfiguration)
            .setResponsiveContentInsetEnabled(true)
            .setContent(column.build())
            .build()

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(LayoutElementBuilders.Layout.Builder().setRoot(root).build())
                            .build()
                    ).build()
            ).build()
    }
}
