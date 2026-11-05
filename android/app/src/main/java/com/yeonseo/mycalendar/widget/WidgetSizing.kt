package com.yeonseo.mycalendar.widget

/** Sizes are launcher-supplied dp, never assumed cell counts. */
object WidgetSizing {
    fun kind(width: Float, height: Float): WidgetKind = when {
        width < 125 && height < 125 -> WidgetKind.DATE
        width < 230 || height < 100 -> WidgetKind.TODAY
        height < 190 -> WidgetKind.WEEK
        height < 330 -> WidgetKind.MONTH
        else -> WidgetKind.MONTH_AGENDA
    }
    fun wide(width: Float, height: Float) = width >= 540 && width / height >= .85f
    fun agendaCount(height: Float, scale: Float, combined: Boolean, wide: Boolean): Int {
        val available = if (combined && !wide) height * .32f else height - 75
        return (available / (30 * scale)).toInt().coerceIn(1, 16)
    }
}
