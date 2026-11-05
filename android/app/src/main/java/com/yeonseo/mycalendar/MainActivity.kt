package com.yeonseo.mycalendar

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ScrollView
import android.graphics.drawable.GradientDrawable
import android.app.Dialog
import android.widget.ImageView
import android.widget.FrameLayout
import android.widget.Switch
import com.yeonseo.mycalendar.widget.MyCalendarWidgetProvider
import com.yeonseo.mycalendar.widget.TodayWidgetProvider
import com.yeonseo.mycalendar.widget.WeekWidgetProvider
import com.yeonseo.mycalendar.widget.AgendaWidgetProvider
import com.yeonseo.mycalendar.widget.MonthAgendaWidgetProvider

class MainActivity : Activity() {
    private lateinit var permissionStatus: TextView
    private lateinit var widgetStyleStatus: TextView
    private lateinit var permissionButton: Button
    private lateinit var widgetAddButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        applyWidgetSettingsIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyWidgetSettingsIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        MyCalendarWidgetProvider.updateAll(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updatePermissionStatus()
        MyCalendarWidgetProvider.updateAll(this)
    }

    private fun buildContent(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(48), dp(28), dp(28))
            setBackgroundColor(Color.rgb(22, 24, 31))
        }
        root.addView(TextView(this).apply {
            text = "My Calendar"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }, matchWrap(top = 0, bottom = 12))
        root.addView(TextView(this).apply {
            text = "Google·네이버 캘린더처럼 홈 화면에서 월간 일정과 다음 일정을 확인합니다."
            textSize = 15f
            setTextColor(Color.rgb(190, 196, 212))
            gravity = Gravity.CENTER
        }, matchWrap(bottom = 26))
        permissionStatus = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
        }
        root.addView(permissionStatus, matchWrap(bottom = 18))
        widgetStyleStatus = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(205, 197, 255))
        }
        root.addView(widgetStyleStatus, matchWrap(bottom = 14))
        permissionButton = actionButton("캘린더 권한 허용") {
            if (checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), REQUEST_CALENDAR)
            } else {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
        }
        root.addView(permissionButton, matchWrap(bottom = 10))
        widgetAddButton = actionButton("홈 화면에 위젯 추가") {
            chooseWidgetToAdd()
        }
        root.addView(widgetAddButton, matchWrap(bottom = 10))
        root.addView(actionButton("My Calendar 열기") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SITE_URL)))
        }, matchWrap(bottom = 10))
        root.addView(actionButton("위젯 설정") {
            startActivity(Intent(this, WidgetSettingsActivity::class.java))
        }, matchWrap(bottom = 10))
        root.addView(actionButton("웹 설정 가져오기") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WIDGET_SETTINGS_URL)))
        }, matchWrap(bottom = 10))
        root.addView(actionButton("위젯 새로고침") {
            MyCalendarWidgetProvider.updateAll(this)
        }, matchWrap())
        return root
    }

    private fun updatePermissionStatus() {
        val granted = checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        permissionStatus.text = if (granted) "캘린더 권한 허용됨 · 위젯 사용 가능" else "위젯에 일정을 표시하려면 캘린더 권한이 필요합니다."
        permissionStatus.setTextColor(if (granted) Color.rgb(102, 221, 164) else Color.rgb(255, 174, 96))
        if (::permissionButton.isInitialized) {
            permissionButton.isEnabled = !granted
            permissionButton.alpha = if (granted) 0.42f else 1f
            permissionButton.text = if (granted) "캘린더 권한 허용 완료" else "캘린더 권한 허용"
        }
        updateWidgetAddStatus(granted)
        updateWidgetStyleStatus()
    }

    private fun applyWidgetSettingsIntent(sourceIntent: Intent?) {
        val uri = sourceIntent?.data ?: return
        if (uri.scheme != "mycalendar" || uri.host != "widget-settings") return
        val style = uri.getQueryParameter("eventStyle") ?: return
        MyCalendarWidgetProvider.setEventStyle(this, style)
        updateWidgetStyleStatus()
        val manager = AppWidgetManager.getInstance(this)
        val hasWidget = MyCalendarWidgetProvider.providerKinds.keys.any {
            manager.getAppWidgetIds(ComponentName(this, it)).isNotEmpty()
        }
        if (hasWidget) MyCalendarWidgetProvider.updateAll(this) else chooseWidgetToAdd()
    }

    private fun chooseWidgetToAdd() {
        val choices = arrayOf(
            "오늘 일정  ·  2×1",
            "주간 일정  ·  4×2",
            "월간 달력  ·  4×3",
            "일정 목록  ·  4×2",
            "월간 달력 + 일정  ·  4×5"
        )
        val providers = arrayOf(
            TodayWidgetProvider::class.java,
            WeekWidgetProvider::class.java,
            MyCalendarWidgetProvider::class.java,
            AgendaWidgetProvider::class.java,
            MonthAgendaWidgetProvider::class.java
        )
        val previews = intArrayOf(R.drawable.chooser_today, R.drawable.chooser_week,
            R.drawable.chooser_month, R.drawable.chooser_agenda, R.drawable.chooser_month_agenda)
        val dialog = Dialog(this, android.R.style.Theme_Material_NoActionBar)
        var selected = 2
        val cards = mutableListOf<FrameLayout>()
        val checks = mutableListOf<TextView>()
        fun decorate() {
            cards.forEachIndexed { index, card ->
                card.background = GradientDrawable().apply {
                    colors = intArrayOf(Color.rgb(49, 49, 60), Color.rgb(40, 40, 49))
                    orientation = GradientDrawable.Orientation.TL_BR
                    cornerRadius = dp(16).toFloat()
                    setStroke(dp(if (selected == index) 2 else 1), if (selected == index) Color.rgb(187, 125, 239) else Color.rgb(70, 70, 83))
                }
                card.contentDescription = choices[index] + if (selected == index) " 선택됨" else ""
                checks[index].visibility = if (selected == index) android.view.View.VISIBLE else android.view.View.INVISIBLE
            }
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        for (rowIndex in 0..2) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (index in rowIndex * 2..minOf(rowIndex * 2 + 1, 4)) {
                val card = FrameLayout(this).apply {
                    isClickable = true
                    isFocusable = true
                }
                val body = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                }
                // Draw the whole preview proportionally; never reflow miniature text into narrow columns.
                body.addView(ImageView(this).apply {
                    setImageResource(previews[index])
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(if (rowIndex == 0) 86 else 130)))
                body.addView(TextView(this).apply {
                    text = choices[index].substringBefore("  ·  ")
                    textSize = 17f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    setPadding(0, dp(8), 0, 0)
                })
                body.addView(TextView(this).apply {
                    text = choices[index].substringAfter("  ·  ")
                    textSize = 14f
                    setTextColor(Color.rgb(188, 187, 200))
                    setPadding(0, dp(2), 0, 0)
                })
                card.addView(body)
                val check = TextView(this).apply {
                    text = "✓"
                    textSize = 19f
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(182, 109, 232)) }
                }
                card.addView(check, FrameLayout.LayoutParams(dp(28), dp(28), Gravity.TOP or Gravity.END).apply { setMargins(0, dp(7), dp(7), 0) })
                checks.add(check)
                card.setOnClickListener { selected = index; decorate() }
                cards.add(card)
                row.addView(card, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(3), dp(4), dp(3), dp(4))
                })
            }
            content.addView(row)
        }
        decorate()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(25, 25, 33))
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(22), dp(16), dp(16), 0) }
        header.addView(TextView(this).apply { text = "▣  My Calendar"; textSize = 17f; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(0, dp(36), 1f))
        header.addView(TextView(this).apply { text = "×"; textSize = 30f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); contentDescription = "닫기"; setOnClickListener { dialog.dismiss() } }, LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(header)
        root.addView(TextView(this).apply { text = "추가할 위젯 선택"; textSize = 27f; setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(Color.WHITE); setPadding(dp(22), dp(4), dp(22), dp(8)) })
        val automatic = Switch(this).apply {
            text = "크기에 맞춰 자동 전환"
            textSize = 16f
            setTextColor(Color.WHITE)
            isChecked = MyCalendarWidgetProvider.autoResize(this@MainActivity, AppWidgetManager.INVALID_APPWIDGET_ID)
            buttonTintList = android.content.res.ColorStateList.valueOf(Color.rgb(187, 125, 239))
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        root.addView(automatic, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(20), 0, dp(20), 0) })
        val modeHelp = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.rgb(188, 187, 200))
            setPadding(dp(22), dp(2), dp(22), dp(6))
        }
        val initialSizeHeading = TextView(this).apply {
            text = "처음 추가할 크기"
            textSize = 17f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.rgb(196, 147, 239))
            setPadding(dp(22), dp(8), dp(22), dp(2))
        }
        val initialSizeHelp = TextView(this).apply {
            text = "선택한 크기로 추가한 뒤, 크기에 맞춰 자동 전환됩니다."
            textSize = 12f
            setTextColor(Color.rgb(188, 187, 200))
            setPadding(dp(22), 0, dp(22), dp(6))
        }
        fun updateModeHelp() {
            modeHelp.text = if (automatic.isChecked) "추가 후 크기를 조절하면 날짜·오늘·주간·월간으로 자동 전환됩니다."
                else "선택한 위젯 형식을 유지합니다. 크기에 맞춰 내부 배치를 조절합니다."
            val visibility = if (automatic.isChecked) android.view.View.VISIBLE else android.view.View.GONE
            initialSizeHeading.visibility = visibility
            initialSizeHelp.visibility = visibility
        }
        automatic.setOnCheckedChangeListener { _, _ -> updateModeHelp() }
        updateModeHelp()
        root.addView(modeHelp)
        root.addView(initialSizeHeading)
        root.addView(initialSizeHelp)
        root.addView(ScrollView(this).apply { isFillViewport = false; addView(content) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(Button(this).apply {
            text = "위젯 추가"; textSize = 22f; isAllCaps = false
            setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(Color.WHITE)
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.rgb(161, 94, 237), Color.rgb(185, 106, 230))).apply { cornerRadius = dp(18).toFloat() }
            setOnClickListener {
                MyCalendarWidgetProvider.setNewWidgetAutoResize(this@MainActivity, automatic.isChecked)
                dialog.dismiss()
                requestWidgetPin(providers[selected])
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)).apply { setMargins(dp(20), dp(8), dp(20), dp(16)) })
        dialog.setContentView(root)
        dialog.window?.let { applySafeInsets(it, root) }
        dialog.window?.statusBarColor = Color.rgb(25, 25, 33)
        dialog.window?.navigationBarColor = Color.rgb(25, 25, 33)
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    private fun requestWidgetPin(providerClass: Class<out android.appwidget.AppWidgetProvider>) {
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, providerClass)
        val requested = manager.isRequestPinAppWidgetSupported &&
            manager.requestPinAppWidget(provider, null, null)
        if (requested) {
            Toast.makeText(this, "홈 화면에 추가할 위젯을 확인해 주세요.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(
                this,
                "홈 화면을 길게 누른 뒤 위젯 → My Calendar를 선택해 주세요.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updateWidgetAddStatus(
        calendarPermissionGranted: Boolean = checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    ) {
        if (!::widgetAddButton.isInitialized) return
        val manager = AppWidgetManager.getInstance(this)
        val hasWidget = MyCalendarWidgetProvider.providerKinds.keys.any {
            manager.getAppWidgetIds(ComponentName(this, it)).isNotEmpty()
        }
        widgetAddButton.isEnabled = calendarPermissionGranted
        widgetAddButton.alpha = if (widgetAddButton.isEnabled) 1f else 0.42f
        widgetAddButton.text = when {
            !calendarPermissionGranted -> "캘린더 권한 허용 후 위젯 추가"
            hasWidget -> "다른 위젯 선택 및 추가"
            else -> "홈 화면에 위젯 추가"
        }
    }

    private fun updateWidgetStyleStatus() {
        if (!::widgetStyleStatus.isInitialized) return
        val label = when (MyCalendarWidgetProvider.eventStyle(this)) {
            "fill" -> "배경색 채우기"
            "text" -> "글씨에만 색상"
            else -> "왼쪽 색상선"
        }
        widgetStyleStatus.text = "현재 위젯 일정 표시 · $label"
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 16f
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(104, 91, 220))
        setOnClickListener { action() }
    }

    private fun matchWrap(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { setMargins(0, dp(top), 0, dp(bottom)) }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_CALENDAR = 2001
        const val SITE_URL = "https://yeonseo1605.github.io/my-calendar/"
        const val WIDGET_SETTINGS_URL = "${SITE_URL}widget-settings.html"
    }
}
