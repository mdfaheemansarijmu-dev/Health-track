package com.example

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat

class BrowserProtectionService : AccessibilityService() {

    companion object {
        const val CHANNEL_ID = "jagr_browser_guard_channel"
        const val NOTIFICATION_ID = 90210

        val SUPPORTED_BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.sec.android.app.sbrowser",
            "com.opera.browser",
            "com.brave.browser",
            "com.duckduckgo.mobile.android",
            "com.google.android.googlequicksearchbox",
            "com.android.browser",
            "com.kiwibrowser.browser",
            "com.vivaldi.browser"
        )
    }

    private var lastCheckedUrl: String = ""
    private var lastCheckedTimestamp: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        createNotificationChannel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        if (!SUPPORTED_BROWSER_PACKAGES.contains(packageName)) return

        val rootNode = rootInActiveWindow ?: return

        try {
            val urlOrQuery = findUrlOrQueryInNodeTree(rootNode, packageName)
            if (!urlOrQuery.isNullOrBlank()) {
                val now = System.currentTimeMillis()
                if (urlOrQuery != lastCheckedUrl || (now - lastCheckedTimestamp) > 3000L) {
                    lastCheckedUrl = urlOrQuery
                    lastCheckedTimestamp = now
                    evaluateUrl(packageName, urlOrQuery)
                }
            }
        } catch (e: Exception) {
            // Ignore node traversal exceptions
        }
    }

    private fun findUrlOrQueryInNodeTree(node: AccessibilityNodeInfo?, packageName: String): String? {
        if (node == null) return null

        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val text = node.text?.toString()?.trim()

        // Common known URL bar IDs across popular Android browsers
        val isUrlBarNode = viewId.contains("url_bar") ||
                viewId.contains("search_box") ||
                viewId.contains("search_src_text") ||
                viewId.contains("location_bar") ||
                viewId.contains("address_bar") ||
                viewId.contains("toolbar_url") ||
                viewId.contains("urlbar") ||
                viewId.contains("search_edit_frame")

        if (isUrlBarNode && !text.isNullOrBlank()) {
            return text
        }

        // If editable text view and contains dot/slash or search
        if (node.isEditable && !text.isNullOrBlank() && (text.contains(".") || text.contains("/") || text.length > 3)) {
            return text
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val found = findUrlOrQueryInNodeTree(child, packageName)
            if (found != null) {
                return found
            }
        }
        return null
    }

    private fun evaluateUrl(packageName: String, urlOrQuery: String) {
        val matchResult = AdultContentGuardManager.checkUrlOrQueryForAdultContent(this, urlOrQuery)
        if (matchResult != null && matchResult.first) {
            val matchedPattern = matchResult.second
            val pm = packageManager
            val browserName = try {
                val info = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(info).toString()
            } catch (e: Exception) {
                packageName
            }

            // Record incident in store
            AdultContentGuardManager.recordIncident(
                context = this,
                browserPackage = packageName,
                browserName = browserName,
                detectedUrlOrTerm = urlOrQuery,
                matchedPattern = matchedPattern
            )

            // Issue shield notification
            showAdultAlertNotification(browserName, urlOrQuery, matchedPattern)
        }
    }

    private fun showAdultAlertNotification(browserName: String, url: String, pattern: String) {
        try {
            val notifyManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val openAppIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🛡️ Adult Content Alert ($browserName)")
                .setContentText("Restricted content detected: $pattern. Stay focused on your goals!")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Restricted content detected in $browserName:\n\"$url\"\n\nJagr has logged this attempt to help maintain your habit streak and focus.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notifyManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Notification failed
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Jagr Safe Browsing & Guard Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for detected adult websites and digital wellbeing focus alerts"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }
}
