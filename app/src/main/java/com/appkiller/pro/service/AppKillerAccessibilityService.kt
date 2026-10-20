package com.appkiller.pro.service

import android.accessibilityservice.AccessibilityService
import android.provider.Settings
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AppKillerAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppKillerA11y"
        private const val SETTINGS_PACKAGE = "com.android.settings"
        private const val FORCE_STOP_TEXT = "Force Stop"
        private const val FORCE_STOP_TEXT_ZH = "强制停止"
        private const val OK_TEXT = "OK"
        private const val OK_TEXT_ZH = "确定"

        private var instance: AppKillerAccessibilityService? = null

        fun getInstance(): AppKillerAccessibilityService? = instance

        fun isServiceRunning(): Boolean = instance != null

        fun killPackage(packageName: String) {
            instance?.performKill(packageName)
        }

        fun killPackages(packages: List<String>): Int {
            val svc = instance ?: return 0
            var killed = 0
            for (pkg in packages) {
                if (svc.performKill(pkg)) killed++
            }
            return killed
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var pendingKillTarget: String? = null
    private var killInProgress = false
    private var killRetryCount = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (killInProgress && event?.packageName == SETTINGS_PACKAGE) {
            confirmForceStopDialog()
        }
    }

    override fun onInterrupt() {
        killInProgress = false
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    fun performKill(packageName: String): Boolean {
        if (killInProgress) return false
        pendingKillTarget = packageName
        killInProgress = true
        killRetryCount = 0

        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.parse("package:$packageName")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            return true
        } catch (e: Exception) {
            killInProgress = false
            return false
        }
    }

    private fun confirmForceStopDialog() {
        val source = rootInActiveWindow ?: return

        // Try clicking "Force Stop"
        var forceStopBtn = findClickableNodeByText(source, FORCE_STOP_TEXT)
        if (forceStopBtn == null) {
            forceStopBtn = findClickableNodeByText(source, FORCE_STOP_TEXT_ZH)
        }

        if (forceStopBtn != null) {
            forceStopBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)

            // Wait for confirmation dialog, then click OK
            handler.postDelayed({
                val root = rootInActiveWindow ?: return@postDelayed
                var okButton = findClickableNodeByText(root, OK_TEXT)
                if (okButton == null) {
                    okButton = findClickableNodeByText(root, OK_TEXT_ZH)
                }
                if (okButton != null) {
                    okButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    onKillComplete(pendingKillTarget, true)
                } else {
                    if (killRetryCount < 3) {
                        killRetryCount++
                        handler.postDelayed({ confirmForceStopDialog() }, 500)
                    } else {
                        onKillComplete(pendingKillTarget, false)
                    }
                }
            }, 500)
        } else {
            // Try scrolling to find Force Stop
            val scrollable = findScrollableNode(source)
            if (scrollable != null && killRetryCount < 3) {
                killRetryCount++
                scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                handler.postDelayed({ confirmForceStopDialog() }, 500)
            } else {
                onKillComplete(pendingKillTarget, false)
            }
        }
    }

    private fun onKillComplete(packageName: String?, success: Boolean) {
        killInProgress = false
        pendingKillTarget = null
        killRetryCount = 0
        if (!success) {
            // Fallback: kill background processes
            packageName?.let { pkg ->
                try {
                    val am = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
                    am.killBackgroundProcesses(pkg)
                } catch (e: Exception) {}
            }
        }
    }

    private fun findClickableNodeByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val nodes = root.findAccessibilityNodeInfosByText(text)
        for (node in nodes) {
            if (node.isClickable) return node
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) return parent
                parent = parent.parent
            }
        }
        return null
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isScrollable) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findScrollableNode(child)
            if (result != null) return result
        }
        return null
    }
}
