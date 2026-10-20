package com.appkiller.pro.ui

import android.app.PictureInPictureParams
import android.content.Intent
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.appkiller.pro.R
import com.appkiller.pro.data.AppRepository
import com.appkiller.pro.model.AppInfo
import com.appkiller.pro.service.AppKillerAccessibilityService

class PipActivity : AppCompatActivity() {

    private lateinit var appNameText: TextView
    private lateinit var statusText: TextView
    private lateinit var killBtn: Button
    private lateinit var nextBtn: Button
    private var runningApps: List<AppInfo> = emptyList()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pip)

        appNameText = findViewById(R.id.pipAppName)
        statusText = findViewById(R.id.pipStatus)
        killBtn = findViewById(R.id.pipKillBtn)
        nextBtn = findViewById(R.id.pipNextBtn)

        loadRunningApps()

        killBtn.setOnClickListener {
            killCurrentApp()
        }
        nextBtn.setOnClickListener {
            nextApp()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPipMode()
    }

    private fun enterPipMode() {
        try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        } catch (e: Exception) {
            // PiP not supported
        }
    }

    private fun loadRunningApps() {
        runningApps = AppRepository.getRunningApps()
        currentIndex = 0
        updateCurrentApp()
    }

    private fun updateCurrentApp() {
        if (runningApps.isEmpty()) {
            appNameText.text = "No running apps"
            statusText.text = ""
            return
        }
        if (currentIndex >= runningApps.size) currentIndex = 0
        val app = runningApps[currentIndex]
        appNameText.text = app.appName
        statusText.text = "Tap to kill"
    }

    private fun killCurrentApp() {
        if (runningApps.isEmpty()) return
        val app = runningApps[currentIndex]
        AppKillerAccessibilityService.killPackage(app.packageName)
        Toast.makeText(this, "${app.appName} killed", Toast.LENGTH_SHORT).show()
        loadRunningApps()
    }

    private fun nextApp() {
        if (runningApps.isEmpty()) return
        currentIndex++
        updateCurrentApp()
    }
}
