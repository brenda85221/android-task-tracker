package com.taskflow.taskflow2.util

import java.text.SimpleDateFormat
import java.util.*

private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

fun Long.toFormattedDate(): String {
    return sdf.format(Date(this))
}
