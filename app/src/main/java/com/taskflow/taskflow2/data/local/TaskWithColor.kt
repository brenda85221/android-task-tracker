package com.taskflow.taskflow2.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class TaskWithColor(

    @Embedded
    val task: TaskEntity,

    @Relation(
        parentColumn = "colorId",
        entityColumn = "id"
    )
    val color: TaskColor?
)