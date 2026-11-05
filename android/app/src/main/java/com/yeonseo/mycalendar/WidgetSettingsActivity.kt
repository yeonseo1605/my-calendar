package com.yeonseo.mycalendar

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Switch
import android.widget.ScrollView
import com.yeonseo.mycalendar.widget.WidgetKind
import com.yeonseo.mycalendar.widget.MyCalendarWidgetProvider

class WidgetSettingsActivity : Activity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) setResult(RESULT_CANCELED)
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(22, 24, 31)); addView(buildContent()) }
        setContentView(scroll)
        applySafeInsets(window, scroll)
    }

    private fun buildContent(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(32), dp(24), dp(24))
            setBackgroundColor(Color.rgb(22, 24, 31))
        }
        root.addView(title("위젯 설정", 26f), params(bottom = 6))
        root.addView(description("변경 내용은 홈 화면 위젯에 바로 반영됩니다."), params(bottom = 24))

        root.addView(title("위젯 크기 설정", 17f), params(bottom = 8))
        var fixedKind = MyCalendarWidgetProvider.fixedKind(this, appWidgetId)
        val formats = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        listOf(WidgetKind.DATE to "날짜", WidgetKind.TODAY to "오늘 일정", WidgetKind.WEEK to "주간 일정",
            WidgetKind.MONTH to "월간 달력", WidgetKind.AGENDA to "일정 목록", WidgetKind.MONTH_AGENDA to "월간 달력 + 일정").forEach { (kind, label) ->
            formats.addView(RadioButton(this).apply {
                id = View.generateViewId(); text = label; tag = kind; setTextColor(Color.WHITE)
                isChecked = fixedKind == kind
            })
        }
        val automatic = Switch(this).apply {
            text = "크기에 맞춰 자동 전환"; textSize = 16f; setTextColor(Color.WHITE)
            isChecked = MyCalendarWidgetProvider.autoResize(this@WidgetSettingsActivity, appWidgetId)
        }
        fun updateFormats() {
            formats.alpha = if (automatic.isChecked) .4f else 1f
            for (i in 0 until formats.childCount) formats.getChildAt(i).isEnabled = !automatic.isChecked
        }
        automatic.setOnCheckedChangeListener { _, enabled ->
            updateFormats(); MyCalendarWidgetProvider.setSizeMode(this, appWidgetId, enabled, fixedKind)
        }
        formats.setOnCheckedChangeListener { group, checkedId ->
            fixedKind = group.findViewById<RadioButton>(checkedId)?.tag as? WidgetKind ?: return@setOnCheckedChangeListener
            MyCalendarWidgetProvider.setSizeMode(this, appWidgetId, automatic.isChecked, fixedKind)
        }
        root.addView(automatic, params(bottom = 8))
        root.addView(description("좁으면 날짜·오늘 일정, 넓어지면 주간·월간으로 전환합니다. 큰 공간에서는 달력과 목록을 함께 표시합니다."), params(bottom = 8))
        root.addView(description("자동 전환을 끄면 아래 형식을 유지합니다. 글씨 크기는 기존 설정을 따릅니다."), params(bottom = 8))
        updateFormats()
        root.addView(formats, params(bottom = 22))

        root.addView(title("일정 표시", 17f), params(bottom = 8))
        val styles = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        listOf("fill" to "배경", "marker" to "왼쪽 선", "text" to "글씨").forEach { (value, label) ->
            styles.addView(RadioButton(this).apply {
                id = View.generateViewId()
                text = label
                tag = value
                setTextColor(Color.WHITE)
                isChecked = MyCalendarWidgetProvider.eventStyle(this@WidgetSettingsActivity) == value
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        styles.setOnCheckedChangeListener { group, checkedId ->
            val selected = group.findViewById<RadioButton>(checkedId)?.tag as? String ?: return@setOnCheckedChangeListener
            MyCalendarWidgetProvider.setEventStyle(this, selected)
        }
        root.addView(styles, params(bottom = 22))

        val opacityValue = valueText("배경 투명도", "${MyCalendarWidgetProvider.backgroundOpacity(this)}%")
        root.addView(opacityValue.first)
        root.addView(SeekBar(this).apply {
            max = 75
            progress = MyCalendarWidgetProvider.backgroundOpacity(this@WidgetSettingsActivity) - 25
            setOnSeekBarChangeListener(simpleSeek { progress ->
                val value = progress + 25
                opacityValue.second.text = "$value%"
                MyCalendarWidgetProvider.setBackgroundOpacity(this@WidgetSettingsActivity, value)
            })
        }, params(bottom = 20))

        val fontValue = valueText("일정 글씨 크기", "${MyCalendarWidgetProvider.fontScale(this)}%")
        root.addView(fontValue.first)
        root.addView(SeekBar(this).apply {
            max = 90
            progress = MyCalendarWidgetProvider.fontScale(this@WidgetSettingsActivity) - 70
            setOnSeekBarChangeListener(simpleSeek { progress ->
                val value = progress + 70
                fontValue.second.text = "$value%"
                MyCalendarWidgetProvider.setFontScale(this@WidgetSettingsActivity, value)
            })
        }, params(bottom = 28))

        root.addView(actionButton("My Calendar 열기") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MainActivity.SITE_URL)))
        }, params(bottom = 10))
        root.addView(actionButton("완료") { finishWithResult() })
        return root
    }

    private fun finishWithResult() {
        MyCalendarWidgetProvider.updateAll(this)
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        }
        finish()
    }

    override fun onBackPressed() = finishWithResult()

    private fun title(value: String, size: Float) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(Color.WHITE)
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun description(value: String) = TextView(this).apply {
        text = value
        textSize = 14f
        setTextColor(Color.rgb(182, 188, 204))
    }

    private fun valueText(label: String, value: String): Pair<LinearLayout, TextView> {
        val valueView = TextView(this).apply { text = value; setTextColor(Color.rgb(196, 180, 255)); textSize = 14f }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(title(label, 16f), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(valueView)
        }
        return row to valueView
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 16f
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(104, 91, 220))
        setOnClickListener { action() }
    }

    private fun simpleSeek(onChanged: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { if (fromUser) onChanged(progress) }
        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    }

    private fun params(bottom: Int = 0) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        setMargins(0, 0, 0, dp(bottom))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
