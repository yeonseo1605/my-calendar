package com.yeonseo.mycalendar.widget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class WidgetEvent(
    val id: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val color: Int,
    val calendarName: String
) {
    fun localDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalDate()
}

class CalendarRepository(private val context: Context) {
    fun hasPermission(): Boolean = context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun eventsBetween(startMillis: Long, endMillis: Long): List<WidgetEvent> {
        if (!hasPermission()) return emptyList()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_COLOR,
            CalendarContract.Instances.CALENDAR_COLOR,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME
        )
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(startMillis.toString())
            .appendPath(endMillis.toString())
            .build()
        return runCatching {
            context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
                val events = mutableListOf<WidgetEvent>()
                while (cursor.moveToNext()) {
                    val eventColor = cursor.getInt(5)
                    val calendarColor = cursor.getInt(6)
                    events += WidgetEvent(
                        id = cursor.getLong(0),
                        title = cursor.getString(1)?.takeIf { it.isNotBlank() } ?: "(제목 없음)",
                        startMillis = cursor.getLong(2),
                        endMillis = cursor.getLong(3),
                        allDay = cursor.getInt(4) == 1,
                        color = if (eventColor != 0) eventColor else if (calendarColor != 0) calendarColor else 0xFF7D73E6.toInt(),
                        calendarName = cursor.getString(7).orEmpty()
                    )
                }
                events.distinctBy { "${it.id}:${it.startMillis}" }
            } ?: emptyList()
        }.getOrElse { emptyList() }
    }
}
