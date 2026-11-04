package com.yeonseo.mycalendar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.yeonseo.mycalendar.MainActivity
import com.yeonseo.mycalendar.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class WidgetKind { AUTO, TODAY, WEEK, MONTH, AGENDA, MONTH_AGENDA }

open class MyCalendarWidgetProvider : AppWidgetProvider() {
    protected open val widgetKind: WidgetKind = WidgetKind.MONTH

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { safeUpdateWidget(context, manager, it, widgetKind, javaClass) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        safeUpdateWidget(context, manager, appWidgetId, widgetKind, javaClass)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        when (intent.action) {
            ACTION_PREVIOUS -> prefs.edit().putInt(offsetKey(id), prefs.getInt(offsetKey(id), 0) - 1).apply()
            ACTION_NEXT -> prefs.edit().putInt(offsetKey(id), prefs.getInt(offsetKey(id), 0) + 1).apply()
            ACTION_PREVIOUS_WEEK -> prefs.edit().putInt(weekOffsetKey(id), prefs.getInt(weekOffsetKey(id), 0) - 1).apply()
            ACTION_NEXT_WEEK -> prefs.edit().putInt(weekOffsetKey(id), prefs.getInt(weekOffsetKey(id), 0) + 1).apply()
            ACTION_TODAY -> prefs.edit().putInt(offsetKey(id), 0).putInt(weekOffsetKey(id), 0).apply()
            else -> return
        }
        safeUpdateWidget(context, AppWidgetManager.getInstance(context), id, widgetKind, javaClass)
    }

    companion object {
        private const val PREFS = "my_calendar_widget"
        private const val ACTION_PREVIOUS = "com.yeonseo.mycalendar.widget.PREVIOUS_MONTH"
        private const val ACTION_NEXT = "com.yeonseo.mycalendar.widget.NEXT_MONTH"
        private const val ACTION_PREVIOUS_WEEK = "com.yeonseo.mycalendar.widget.PREVIOUS_WEEK"
        private const val ACTION_NEXT_WEEK = "com.yeonseo.mycalendar.widget.NEXT_WEEK"
        private const val ACTION_TODAY = "com.yeonseo.mycalendar.widget.TODAY"
        private const val EVENT_STYLE_KEY = "event_style"
        private const val EVENT_STYLE_FILL = "fill"
        private const val EVENT_STYLE_MARKER = "marker"
        private const val EVENT_STYLE_TEXT = "text"
        private const val OPACITY_KEY = "background_opacity"
        private const val FONT_SCALE_KEY = "font_scale"
        private val monthFormatter = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)
        private val dateFormatter = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        val providerKinds = linkedMapOf<Class<out AppWidgetProvider>, WidgetKind>(
            MyCalendarWidgetProvider::class.java to WidgetKind.MONTH,
            TodayWidgetProvider::class.java to WidgetKind.TODAY,
            WeekWidgetProvider::class.java to WidgetKind.WEEK,
            AgendaWidgetProvider::class.java to WidgetKind.AGENDA,
            MonthAgendaWidgetProvider::class.java to WidgetKind.MONTH_AGENDA
        )

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            providerKinds.forEach { (providerClass, kind) ->
                val component = ComponentName(context, providerClass)
                manager.getAppWidgetIds(component).forEach { safeUpdateWidget(context, manager, it, kind, providerClass) }
            }
        }

        fun setEventStyle(context: Context, style: String) {
            val safeStyle = if (style in setOf(EVENT_STYLE_FILL, EVENT_STYLE_MARKER, EVENT_STYLE_TEXT)) style else EVENT_STYLE_MARKER
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(EVENT_STYLE_KEY, safeStyle).apply()
            updateAll(context)
        }

        fun eventStyle(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(EVENT_STYLE_KEY, EVENT_STYLE_MARKER) ?: EVENT_STYLE_MARKER

        fun backgroundOpacity(context: Context): Int = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(OPACITY_KEY, 75)

        fun setBackgroundOpacity(context: Context, value: Int) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(OPACITY_KEY, value.coerceIn(25, 100)).apply()
            updateAll(context)
        }

        fun fontScale(context: Context): Int = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(FONT_SCALE_KEY, 100)

        fun setFontScale(context: Context, value: Int) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(FONT_SCALE_KEY, value.coerceIn(70, 160)).apply()
            updateAll(context)
        }

        private fun safeUpdateWidget(
            context: Context,
            manager: AppWidgetManager,
            id: Int,
            kind: WidgetKind,
            providerClass: Class<out AppWidgetProvider>
        ) {
            val view = runCatching { buildWidget(context, manager, id, kind, providerClass) }
                .getOrElse {
                    RemoteViews(context.packageName, R.layout.widget_small).apply {
                        setTextViewText(R.id.widgetDate, "My Calendar")
                        setTextViewText(R.id.widgetTitle, "위젯을 새로고침해 주세요")
                        setTextViewText(R.id.widgetTime, "앱을 열면 자동으로 다시 불러옵니다")
                        setOnClickPendingIntent(R.id.widgetRoot, openCalendarIntent(context, id))
                    }
                }
            manager.updateAppWidget(id, view)
        }

        private fun buildWidget(
            context: Context,
            manager: AppWidgetManager,
            id: Int,
            kind: WidgetKind,
            providerClass: Class<out AppWidgetProvider>
        ): RemoteViews {
            val options = manager.getAppWidgetOptions(id)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 240)
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
            val repository = CalendarRepository(context)
            val offset = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(offsetKey(id), 0)
            val month = YearMonth.now().plusMonths(offset.toLong())
            return when (kind) {
                WidgetKind.TODAY -> buildSmall(context, id, repository)
                WidgetKind.WEEK -> buildWeek(context, id, repository, providerClass)
                WidgetKind.MONTH -> buildMonth(context, id, repository, month, false, providerClass)
                WidgetKind.AGENDA -> buildAgenda(context, id, repository)
                WidgetKind.MONTH_AGENDA -> buildMonth(context, id, repository, month, true, providerClass)
                WidgetKind.AUTO -> when {
                    width < 230 || height < 95 -> buildSmall(context, id, repository)
                    height < 190 -> buildWeek(context, id, repository, providerClass)
                    height < 285 -> buildMonth(context, id, repository, month, false, providerClass)
                    else -> buildMonth(context, id, repository, month, true, providerClass)
                }
            }
        }

        private fun buildSmall(context: Context, id: Int, repository: CalendarRepository): RemoteViews {
            val view = RemoteViews(context.packageName, R.layout.widget_small)
            val today = LocalDate.now()
            view.setTextViewText(R.id.widgetDate, today.format(dateFormatter))
            val events = repository.eventsBetween(startOfDay(today), startOfDay(today.plusDays(2))).filter { it.localDate() == today }
            if (!repository.hasPermission()) {
                view.setTextViewText(R.id.widgetTitle, "캘린더 권한이 필요합니다")
                view.setTextViewText(R.id.widgetTime, "앱을 열어 권한을 허용하세요")
                clearEventStyle(view, R.id.widgetEventCard, R.id.widgetAccent, R.id.widgetTitle)
            } else if (events.isEmpty()) {
                view.setTextViewText(R.id.widgetTitle, "오늘 일정이 없습니다")
                view.setTextViewText(R.id.widgetTime, "여유로운 하루예요")
                clearEventStyle(view, R.id.widgetEventCard, R.id.widgetAccent, R.id.widgetTitle)
            } else {
                val first = events.first()
                view.setTextViewText(R.id.widgetTitle, first.title)
                view.setTextViewText(R.id.widgetTime, eventTime(first))
                applyEventStyle(context, view, R.id.widgetEventCard, R.id.widgetAccent, R.id.widgetTitle, first.color)
            }
            view.setOnClickPendingIntent(R.id.widgetRoot, openCalendarIntent(context, id))
            applyWidgetAppearance(context, view, listOf(R.id.widgetDate to 12f, R.id.widgetTitle to 15f, R.id.widgetTime to 11f))
            return view
        }

        private fun buildWeek(context: Context, id: Int, repository: CalendarRepository, providerClass: Class<out AppWidgetProvider>): RemoteViews {
            val view = RemoteViews(context.packageName, R.layout.widget_week)
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val offset = prefs.getInt(weekOffsetKey(id), 0)
            val today = LocalDate.now()
            val currentWeekStart = today.minusDays((today.dayOfWeek.value % 7).toLong())
            val weekStart = currentWeekStart.plusWeeks(offset.toLong())
            val weekEnd = weekStart.plusDays(6)
            view.setTextViewText(R.id.widgetWeekTitle, weekTitle(weekStart, weekEnd))
            view.setOnClickPendingIntent(R.id.widgetPrevious, actionIntent(context, id, ACTION_PREVIOUS_WEEK, providerClass))
            view.setOnClickPendingIntent(R.id.widgetNext, actionIntent(context, id, ACTION_NEXT_WEEK, providerClass))
            view.setOnClickPendingIntent(R.id.widgetToday, actionIntent(context, id, ACTION_TODAY, providerClass))
            view.setOnClickPendingIntent(R.id.widgetSettings, settingsIntent(context, id))
            view.setOnClickPendingIntent(R.id.widgetRoot, openCalendarIntent(context, id))
            view.removeAllViews(R.id.widgetWeekDays)

            val allEvents = repository.eventsBetween(startOfDay(weekStart), startOfDay(weekEnd.plusDays(1)))
            val eventsByDate = allEvents.filterNot { isHolidayCalendar(it.calendarName) }.groupBy { it.localDate() }
            val holidays = allEvents.filter { isHolidayCalendar(it.calendarName) }.map { it.localDate() }.toSet()
            val weekdayNames = arrayOf("일", "월", "화", "수", "목", "금", "토")
            repeat(7) { index ->
                val date = weekStart.plusDays(index.toLong())
                val dayView = RemoteViews(context.packageName, if (date == today) R.layout.widget_week_day_today else R.layout.widget_week_day)
                val dayEvents = eventsByDate[date].orEmpty()
                val dayColor = when {
                    date in holidays || date.dayOfWeek == DayOfWeek.SUNDAY -> Color.rgb(224, 100, 115)
                    date.dayOfWeek == DayOfWeek.SATURDAY -> Color.rgb(120, 154, 224)
                    else -> Color.WHITE
                }
                dayView.setTextViewText(R.id.widgetWeekday, weekdayNames[index])
                dayView.setTextViewText(R.id.widgetWeekDate, date.dayOfMonth.toString())
                dayView.setTextColor(R.id.widgetWeekday, dayColor)
                dayView.setTextColor(R.id.widgetWeekDate, dayColor)
                dayView.setTextViewText(R.id.widgetWeekEvent, dayEvents.firstOrNull()?.title.orEmpty())
                dayView.setTextViewText(R.id.widgetWeekMore, if (dayEvents.size > 1) "+${dayEvents.size - 1}" else "")
                if (dayEvents.isNotEmpty()) {
                    applyEventStyle(context, dayView, R.id.widgetWeekEventWrap, R.id.widgetWeekAccent, R.id.widgetWeekEvent, dayEvents.first().color)
                } else {
                    clearEventStyle(dayView, R.id.widgetWeekEventWrap, R.id.widgetWeekAccent, R.id.widgetWeekEvent)
                }
                view.addView(R.id.widgetWeekDays, dayView)
            }
            applyWidgetAppearance(context, view, listOf(R.id.widgetWeekTitle to 13f))
            return view
        }

        private fun buildMonth(context: Context, id: Int, repository: CalendarRepository, month: YearMonth, showAgenda: Boolean, providerClass: Class<out AppWidgetProvider>): RemoteViews {
            val layout = if (showAgenda) R.layout.widget_large else R.layout.widget_medium
            val view = RemoteViews(context.packageName, layout)
            view.setTextViewText(R.id.widgetMonthTitle, month.format(monthFormatter))
            view.setOnClickPendingIntent(R.id.widgetPrevious, actionIntent(context, id, ACTION_PREVIOUS, providerClass))
            view.setOnClickPendingIntent(R.id.widgetNext, actionIntent(context, id, ACTION_NEXT, providerClass))
            view.setOnClickPendingIntent(R.id.widgetToday, actionIntent(context, id, ACTION_TODAY, providerClass))
            view.setOnClickPendingIntent(R.id.widgetSettings, settingsIntent(context, id))
            view.setOnClickPendingIntent(R.id.widgetRoot, openCalendarIntent(context, id))
            view.removeAllViews(R.id.widgetCalendarRows)

            val gridStart = month.atDay(1).minusDays(((month.atDay(1).dayOfWeek.value % 7)).toLong())
            val gridEnd = gridStart.plusDays(42)
            val allEvents = repository.eventsBetween(startOfDay(gridStart), startOfDay(gridEnd))
            val eventsByDate = allEvents.filterNot { isHolidayCalendar(it.calendarName) }.groupBy { it.localDate() }
            val holidays = allEvents.filter { isHolidayCalendar(it.calendarName) }.map { it.localDate() }.toSet()
            repeat(6) { rowIndex ->
                val row = RemoteViews(context.packageName, R.layout.widget_week_row)
                repeat(7) { columnIndex ->
                    val date = gridStart.plusDays((rowIndex * 7 + columnIndex).toLong())
                    val day = RemoteViews(context.packageName, if (date == LocalDate.now()) R.layout.widget_day_today else R.layout.widget_day)
                    day.setTextViewText(R.id.widgetDayNumber, date.dayOfMonth.toString())
                    val inMonth = date.month == month.month
                    val dayColor = when {
                        date in holidays || date.dayOfWeek == DayOfWeek.SUNDAY -> Color.rgb(224, 100, 115)
                        date.dayOfWeek == DayOfWeek.SATURDAY -> Color.rgb(120, 154, 224)
                        inMonth -> Color.WHITE
                        else -> Color.rgb(105, 110, 126)
                    }
                    day.setTextColor(R.id.widgetDayNumber, dayColor)
                    val dayEvents = eventsByDate[date].orEmpty()
                    day.setTextViewText(R.id.widgetEventTitle, dayEvents.firstOrNull()?.title.orEmpty())
                    day.setTextViewText(R.id.widgetEventMore, if (dayEvents.size > 1) "+${dayEvents.size - 1}" else "")
                    if (dayEvents.isNotEmpty()) {
                        applyEventStyle(context, day, R.id.widgetEventWrap, R.id.widgetEventAccent, R.id.widgetEventTitle, dayEvents.first().color)
                    } else {
                        clearEventStyle(day, R.id.widgetEventWrap, R.id.widgetEventAccent, R.id.widgetEventTitle)
                    }
                    row.addView(rowCellId(columnIndex), day)
                }
                view.addView(R.id.widgetCalendarRows, row)
            }

            if (showAgenda) {
                view.removeAllViews(R.id.widgetAgenda)
                val upcoming = repository.eventsBetween(System.currentTimeMillis(), startOfDay(LocalDate.now().plusDays(31)))
                    .filterNot { isHolidayCalendar(it.calendarName) }
                    .take(5)
                if (!repository.hasPermission() || upcoming.isEmpty()) {
                    val empty = RemoteViews(context.packageName, R.layout.widget_agenda_empty)
                    empty.setTextViewText(R.id.widgetAgendaEmpty, if (repository.hasPermission()) "예정된 일정이 없습니다" else "앱을 열어 캘린더 권한을 허용하세요")
                    view.addView(R.id.widgetAgenda, empty)
                } else upcoming.forEach { event ->
                    val item = RemoteViews(context.packageName, R.layout.widget_agenda_item)
                    item.setTextViewText(R.id.widgetAgendaTitle, event.title)
                    item.setTextViewText(R.id.widgetAgendaTime, "${event.localDate().monthValue}/${event.localDate().dayOfMonth} ${eventTime(event)}")
                    applyEventStyle(context, item, R.id.widgetAgendaWrap, R.id.widgetAgendaDot, R.id.widgetAgendaTitle, event.color)
                    view.addView(R.id.widgetAgenda, item)
                }
            }
            applyWidgetAppearance(context, view, listOf(R.id.widgetMonthTitle to 15f))
            return view
        }

        private fun buildAgenda(context: Context, id: Int, repository: CalendarRepository): RemoteViews {
            val view = RemoteViews(context.packageName, R.layout.widget_agenda)
            view.setOnClickPendingIntent(R.id.widgetRoot, openCalendarIntent(context, id))
            view.setOnClickPendingIntent(R.id.widgetSettings, settingsIntent(context, id))
            view.removeAllViews(R.id.widgetAgenda)
            val upcoming = repository.eventsBetween(System.currentTimeMillis(), startOfDay(LocalDate.now().plusDays(31)))
                .filterNot { isHolidayCalendar(it.calendarName) }
                .take(6)
            if (!repository.hasPermission() || upcoming.isEmpty()) {
                val empty = RemoteViews(context.packageName, R.layout.widget_agenda_empty)
                empty.setTextViewText(R.id.widgetAgendaEmpty, if (repository.hasPermission()) "예정된 일정이 없습니다" else "앱을 열어 캘린더 권한을 허용하세요")
                view.addView(R.id.widgetAgenda, empty)
            } else upcoming.forEach { event ->
                val item = RemoteViews(context.packageName, R.layout.widget_agenda_item)
                item.setTextViewText(R.id.widgetAgendaTitle, event.title)
                item.setTextViewText(R.id.widgetAgendaTime, "${event.localDate().monthValue}/${event.localDate().dayOfMonth} ${eventTime(event)}")
                applyEventStyle(context, item, R.id.widgetAgendaWrap, R.id.widgetAgendaDot, R.id.widgetAgendaTitle, event.color)
                view.addView(R.id.widgetAgenda, item)
            }
            applyWidgetAppearance(context, view, emptyList())
            return view
        }

        private fun applyWidgetAppearance(context: Context, view: RemoteViews, textViews: List<Pair<Int, Float>>) {
            val background = when {
                backgroundOpacity(context) <= 37 -> R.drawable.widget_background_25
                backgroundOpacity(context) <= 62 -> R.drawable.widget_background_50
                backgroundOpacity(context) <= 87 -> R.drawable.widget_background_75
                else -> R.drawable.widget_background_100
            }
            view.setInt(R.id.widgetRoot, "setBackgroundResource", background)
            val scale = fontScale(context) / 100f
            textViews.forEach { (id, size) ->
                view.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, size * scale)
            }
        }

        private fun applyEventStyle(context: Context, view: RemoteViews, wrapperId: Int, accentId: Int, titleId: Int, labelColor: Int) {
            when (eventStyle(context)) {
                EVENT_STYLE_FILL -> {
                    view.setViewVisibility(accentId, View.GONE)
                    view.setInt(wrapperId, "setBackgroundColor", Color.argb(38, Color.red(labelColor), Color.green(labelColor), Color.blue(labelColor)))
                    view.setTextColor(titleId, labelColor)
                }
                EVENT_STYLE_TEXT -> {
                    view.setViewVisibility(accentId, View.GONE)
                    view.setInt(wrapperId, "setBackgroundColor", Color.TRANSPARENT)
                    view.setTextColor(titleId, labelColor)
                }
                else -> {
                    view.setViewVisibility(accentId, View.VISIBLE)
                    view.setTextColor(accentId, labelColor)
                    view.setInt(wrapperId, "setBackgroundColor", Color.TRANSPARENT)
                    view.setTextColor(titleId, Color.WHITE)
                }
            }
        }

        private fun clearEventStyle(view: RemoteViews, wrapperId: Int, accentId: Int, titleId: Int) {
            view.setViewVisibility(accentId, View.GONE)
            view.setInt(wrapperId, "setBackgroundColor", Color.TRANSPARENT)
            view.setTextColor(titleId, Color.WHITE)
        }

        private fun eventTime(event: WidgetEvent): String = if (event.allDay) "종일" else Instant.ofEpochMilli(event.startMillis).atZone(ZoneId.systemDefault()).format(timeFormatter)
        private fun isHolidayCalendar(name: String): Boolean = name.contains("휴일") || name.contains("holiday", true)
        private fun weekTitle(start: LocalDate, end: LocalDate): String = if (start.month == end.month) {
            "${start.year}년 ${start.monthValue}월 ${start.dayOfMonth}일–${end.dayOfMonth}일"
        } else {
            "${start.year}년 ${start.monthValue}월 ${start.dayOfMonth}일–${end.monthValue}월 ${end.dayOfMonth}일"
        }
        private fun startOfDay(date: LocalDate): Long = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        private fun offsetKey(id: Int) = "month_offset_$id"
        private fun weekOffsetKey(id: Int) = "week_offset_$id"
        private fun rowCellId(column: Int) = when (column) {
            0 -> R.id.weekDay0; 1 -> R.id.weekDay1; 2 -> R.id.weekDay2; 3 -> R.id.weekDay3
            4 -> R.id.weekDay4; 5 -> R.id.weekDay5; else -> R.id.weekDay6
        }

        private fun actionIntent(context: Context, id: Int, action: String, providerClass: Class<out AppWidgetProvider>): PendingIntent {
            val intent = Intent(context, providerClass).apply {
                this.action = action
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                data = Uri.parse("mycalendar://widget/$id/$action")
            }
            return PendingIntent.getBroadcast(context, id * 10 + action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun openCalendarIntent(context: Context, id: Int): PendingIntent {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(MainActivity.SITE_URL))
            return PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        fun settingsIntent(context: Context, id: Int): PendingIntent {
            val intent = Intent(context, com.yeonseo.mycalendar.WidgetSettingsActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            }
            return PendingIntent.getActivity(context, 90000 + id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}

class TodayWidgetProvider : MyCalendarWidgetProvider() {
    override val widgetKind = WidgetKind.TODAY
}

class WeekWidgetProvider : MyCalendarWidgetProvider() {
    override val widgetKind = WidgetKind.WEEK
}

class AgendaWidgetProvider : MyCalendarWidgetProvider() {
    override val widgetKind = WidgetKind.AGENDA
}

class MonthAgendaWidgetProvider : MyCalendarWidgetProvider() {
    override val widgetKind = WidgetKind.MONTH_AGENDA
}
