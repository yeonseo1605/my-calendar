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
import com.yeonseo.mycalendar.widget.MyCalendarWidgetProvider

class MainActivity : Activity() {
    private lateinit var permissionStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
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
        root.addView(actionButton("캘린더 권한 허용") {
            if (checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), REQUEST_CALENDAR)
            } else {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
        }, matchWrap(bottom = 10))
        root.addView(actionButton("My Calendar 열기") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SITE_URL)))
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
    }
}
