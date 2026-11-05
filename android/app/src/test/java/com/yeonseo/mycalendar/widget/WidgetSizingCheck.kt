package com.yeonseo.mycalendar.widget

/** Standalone JVM checks: same sizing code used by the Android renderer. */
fun main() {
    check(WidgetSizing.kind(60f, 65f) == WidgetKind.DATE)
    check(WidgetSizing.kind(180f, 90f) == WidgetKind.TODAY)
    check(WidgetSizing.kind(300f, 150f) == WidgetKind.WEEK)
    check(WidgetSizing.kind(300f, 250f) == WidgetKind.MONTH)
    check(WidgetSizing.kind(400f, 650f) == WidgetKind.MONTH_AGENDA)
    check(!WidgetSizing.wide(400f, 650f))
    check(WidgetSizing.wide(650f, 600f))
    check(WidgetSizing.agendaCount(700f, 1f, true, false) > WidgetSizing.agendaCount(350f, 1f, true, false))
    check(WidgetSizing.agendaCount(500f, 1.6f, true, false) < WidgetSizing.agendaCount(500f, .7f, true, false))
    println("Widget sizing: 9 checks passed")
}
