package com.example

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.util.Calendar

enum class UsageTimeFrame(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week")
}

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeInForegroundMillis: Long,
    val lastTimeUsedMillis: Long,
    val category: String,
    val icon: Drawable? = null,
    val isSystemApp: Boolean = false,
    val dailyLimitMinutes: Int = 0
) {
    fun getFormattedDuration(): String {
        val totalSeconds = totalTimeInForegroundMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            totalSeconds > 0 -> "${totalSeconds}s"
            else -> "0m"
        }
    }
}

data class UsageCategorySummary(
    val category: String,
    val totalTimeMillis: Long,
    val appCount: Int,
    val percentage: Float
) {
    fun getFormattedDuration(): String {
        val totalSeconds = totalTimeMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            totalSeconds > 0 -> "${totalSeconds}s"
            else -> "0m"
        }
    }
}

data class AppUsageSummary(
    val totalScreenTimeMillis: Long,
    val topApps: List<AppUsageInfo>,
    val categories: List<UsageCategorySummary>,
    val hasUsagePermission: Boolean,
    val timeFrame: UsageTimeFrame,
    val isSampleData: Boolean = false
) {
    fun getFormattedTotalScreenTime(): String {
        val totalSeconds = totalScreenTimeMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            totalSeconds > 0 -> "${totalSeconds}s"
            else -> "0m"
        }
    }
}

object AppUsageTracker {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (ignored: Exception) {}
        }
    }

    fun getUsageSummary(context: Context, timeFrame: UsageTimeFrame = UsageTimeFrame.TODAY): AppUsageSummary {
        val hasPermission = hasUsageStatsPermission(context)
        if (!hasPermission) {
            return AppUsageSummary(
                totalScreenTimeMillis = 0L,
                topApps = emptyList(),
                categories = emptyList(),
                hasUsagePermission = false,
                timeFrame = timeFrame,
                isSampleData = false
            )
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return AppUsageSummary(
                totalScreenTimeMillis = 0L,
                topApps = emptyList(),
                categories = emptyList(),
                hasUsagePermission = true,
                timeFrame = timeFrame,
                isSampleData = false
            )

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        val startTime = when (timeFrame) {
            UsageTimeFrame.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            UsageTimeFrame.YESTERDAY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            UsageTimeFrame.THIS_WEEK -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.timeInMillis
            }
        }

        val aggregatedMap = mutableMapOf<String, Long>()
        val lastUsedMap = mutableMapOf<String, Long>()

        // 1. Query standard usage stats
        try {
            val stats: List<UsageStats>? = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
            if (stats != null) {
                for (usage in stats) {
                    if (usage.totalTimeInForeground > 0) {
                        val current = aggregatedMap[usage.packageName] ?: 0L
                        aggregatedMap[usage.packageName] = maxOf(current, usage.totalTimeInForeground)
                        val currentLast = lastUsedMap[usage.packageName] ?: 0L
                        if (usage.lastTimeUsed > currentLast) {
                            lastUsedMap[usage.packageName] = usage.lastTimeUsed
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore error
        }

        // 2. Query usage events for precise foreground session duration
        try {
            val events = usageStatsManager.queryEvents(startTime, endTime)
            if (events != null) {
                val event = UsageEvents.Event()
                val sessionStartTimes = mutableMapOf<String, Long>()
                val eventDurations = mutableMapOf<String, Long>()

                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    val pkg = event.packageName ?: continue
                    val timestamp = event.timeStamp

                    when (event.eventType) {
                        UsageEvents.Event.ACTIVITY_RESUMED,
                        UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                            sessionStartTimes[pkg] = timestamp
                            val currLast = lastUsedMap[pkg] ?: 0L
                            if (timestamp > currLast) {
                                lastUsedMap[pkg] = timestamp
                            }
                        }
                        UsageEvents.Event.ACTIVITY_PAUSED,
                        UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                            val start = sessionStartTimes[pkg]
                            if (start != null && timestamp > start) {
                                val dur = timestamp - start
                                eventDurations[pkg] = (eventDurations[pkg] ?: 0L) + dur
                                sessionStartTimes.remove(pkg)
                            }
                        }
                    }
                }

                for ((pkg, start) in sessionStartTimes) {
                    if (endTime > start) {
                        val dur = endTime - start
                        eventDurations[pkg] = (eventDurations[pkg] ?: 0L) + dur
                    }
                }

                for ((pkg, dur) in eventDurations) {
                    val currentStatsTime = aggregatedMap[pkg] ?: 0L
                    aggregatedMap[pkg] = maxOf(currentStatsTime, dur)
                }
            }
        } catch (e: Exception) {
            // Ignore error
        }

        val pm = context.packageManager
        val appList = mutableListOf<AppUsageInfo>()
        var totalScreenTime = 0L

        // Process tracked packages
        for ((pkg, timeMillis) in aggregatedMap) {
            if (timeMillis <= 0L) continue

            try {
                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(pkg, 0)
                }

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val label = pm.getApplicationLabel(appInfo).toString()
                val icon = try { pm.getApplicationIcon(appInfo) } catch (e: Exception) { null }
                val category = determineCategory(appInfo, pkg, label)

                if (pkg == "com.android.systemui" || (pkg.contains("launcher") && timeMillis < 10000L)) {
                    continue
                }

                totalScreenTime += timeMillis
                appList.add(
                    AppUsageInfo(
                        packageName = pkg,
                        appName = label,
                        totalTimeInForegroundMillis = timeMillis,
                        lastTimeUsedMillis = lastUsedMap[pkg] ?: 0L,
                        category = category,
                        icon = icon,
                        isSystemApp = isSystem
                    )
                )
            } catch (e: PackageManager.NameNotFoundException) {
                // Uninstalled package
            }
        }

        // If no active usage events yet (e.g. app freshly launched or 0 screen time recorded so far),
        // query real installed apps from the device so user can see actual installed apps on their device
        if (appList.isEmpty()) {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launchableApps = pm.queryIntentActivities(mainIntent, 0)
            for (resolveInfo in launchableApps.take(15)) {
                val pkg = resolveInfo.activityInfo.packageName
                val label = resolveInfo.loadLabel(pm).toString()
                val icon = try { resolveInfo.loadIcon(pm) } catch (e: Exception) { null }
                val appInfo = resolveInfo.activityInfo.applicationInfo
                val category = determineCategory(appInfo, pkg, label)
                
                appList.add(
                    AppUsageInfo(
                        packageName = pkg,
                        appName = label,
                        totalTimeInForegroundMillis = 0L,
                        lastTimeUsedMillis = System.currentTimeMillis(),
                        category = category,
                        icon = icon,
                        isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                )
            }
        }

        appList.sortByDescending { it.totalTimeInForegroundMillis }

        // Category breakdown
        val categoryMap = mutableMapOf<String, Long>()
        val categoryAppCount = mutableMapOf<String, Int>()

        for (app in appList) {
            categoryMap[app.category] = (categoryMap[app.category] ?: 0L) + app.totalTimeInForegroundMillis
            categoryAppCount[app.category] = (categoryAppCount[app.category] ?: 0) + 1
        }

        val categories = categoryMap.map { (cat, time) ->
            val pct = if (totalScreenTime > 0) (time.toFloat() / totalScreenTime.toFloat()) * 100f else 0f
            UsageCategorySummary(
                category = cat,
                totalTimeMillis = time,
                appCount = categoryAppCount[cat] ?: 1,
                percentage = pct
            )
        }.sortedByDescending { it.totalTimeMillis }

        return AppUsageSummary(
            totalScreenTimeMillis = totalScreenTime,
            topApps = appList,
            categories = categories,
            hasUsagePermission = true,
            timeFrame = timeFrame,
            isSampleData = false
        )
    }

    private fun determineCategory(appInfo: ApplicationInfo, packageName: String, label: String): String {
        val lowerPkg = packageName.lowercase()
        val lowerLabel = label.lowercase()

        if (lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || lowerPkg.contains("spotify") ||
            lowerPkg.contains("tiktok") || lowerPkg.contains("twitch") || lowerLabel.contains("video") || lowerLabel.contains("music")) {
            return "Entertainment"
        }
        if (lowerPkg.contains("instagram") || lowerPkg.contains("facebook") || lowerPkg.contains("twitter") ||
            lowerPkg.contains("x.corp") || lowerPkg.contains("whatsapp") || lowerPkg.contains("telegram") ||
            lowerPkg.contains("snapchat") || lowerPkg.contains("reddit") || lowerLabel.contains("social")) {
            return "Social Media"
        }
        if (lowerPkg.contains("chrome") || lowerPkg.contains("browser") || lowerPkg.contains("gmail") ||
            lowerPkg.contains("docs") || lowerPkg.contains("sheets") || lowerPkg.contains("notion") ||
            lowerPkg.contains("slack") || lowerPkg.contains("teams") || lowerPkg.contains("jagr") || lowerLabel.contains("habit")) {
            return "Productivity & Tools"
        }
        if (lowerPkg.contains("game") || lowerPkg.contains("pubg") || lowerPkg.contains("roblox") || lowerPkg.contains("clash")) {
            return "Games"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_GAME -> return "Games"
                ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO -> return "Entertainment"
                ApplicationInfo.CATEGORY_SOCIAL -> return "Social Media"
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return "Productivity & Tools"
                ApplicationInfo.CATEGORY_NEWS -> return "News & Information"
                ApplicationInfo.CATEGORY_MAPS -> return "Navigation"
            }
        }

        return "Utilities"
    }
}
