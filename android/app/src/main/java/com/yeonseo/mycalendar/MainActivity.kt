package com.yeonseo.mycalendar

import android.Manifest
import android.app.Activity
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
import com.yeonseo.mycalendar.widget.MyCalendarWidgetProvider

class MainActivity : Activity() {
    private lateinit var permissionStatus: TextView
    private lateinit var widgetStyleStatus: TextView
    private lateinit var permissionButton: Button

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
        root.addView(actionButton("My Calendar 열기") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SITE_URL)))
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
        updateWidgetStyleStatus()
    }

    private fun applyWidgetSettingsIntent(sourceIntent: Intent?) {
        val uri = sourceIntent?.data ?: return
        if (uri.scheme != "mycalendar" || uri.host != "widget-settings") return
        val style = uri.getQueryParameter("eventStyle") ?: return
        MyCalendarWidgetProvider.setEventStyle(this, style)
        updateWidgetStyleStatus()
        applyToExistingWidgetOrRequestAdd()
    }

    private fun applyToExistingWidgetOrRequestAdd() {
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, MyCalendarWidgetProvider::class.java)
        val widgetIds = manager.getAppWidgetIds(provider)

        if (widgetIds.isNotEmpty()) {
            MyCalendarWidgetProvider.updateAll(this)
            Toast.makeText(this, "기존 위젯에 표시 설정을 적용했습니다.", Toast.LENGTH_SHORT).show()
            return
        }

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
