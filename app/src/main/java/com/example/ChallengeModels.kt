package com.example

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ChallengeDayRecord(
    val dayNumber: Int,
    val status: String = "PENDING", // "PENDING", "SUCCESS", "FAILED"
    val notes: String = "",
    val timestamp: Long = 0L
)

data class HabitChallenge(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val habitName: String,
    val totalDays: Int,
    val startTimestamp: Long = System.currentTimeMillis(),
    val attendanceTime: String = "10:00 PM",
    val showLockscreenNotification: Boolean = true,
    val isCompleted: Boolean = false,
    val isActive: Boolean = true,
    val dayRecords: List<ChallengeDayRecord> = emptyList()
) {
    fun getCurrentDayNumber(): Int {
        val now = System.currentTimeMillis()
        val diffMillis = now - startTimestamp
        if (diffMillis < 0) return 1
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt() + 1
        return diffDays.coerceIn(1, totalDays)
    }

    fun calculateProgress(): Float {
        if (totalDays <= 0) return 0f
        return (getSuccessCount().toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
    }

    fun getSuccessCount(): Int = dayRecords.count { it.status == "SUCCESS" }
    fun getFailedCount(): Int = dayRecords.count { it.status == "FAILED" }
    fun getPendingCount(): Int = dayRecords.count { it.status == "PENDING" }

    fun getConsecutiveStreak(): Int {
        var streak = 0
        val currentDay = getCurrentDayNumber()
        for (d in currentDay downTo 1) {
            val rec = dayRecords.firstOrNull { it.dayNumber == d }
            if (rec?.status == "SUCCESS") {
                streak++
            } else if (rec?.status == "FAILED") {
                break
            } else if (d != currentDay) {
                break
            }
        }
        return streak
    }
}

fun serializeChallenges(challenges: List<HabitChallenge>): String {
    val jsonArray = JSONArray()
    for (c in challenges) {
        val obj = JSONObject().apply {
            put("id", c.id)
            put("title", c.title)
            put("habitName", c.habitName)
            put("totalDays", c.totalDays)
            put("startTimestamp", c.startTimestamp)
            put("attendanceTime", c.attendanceTime)
            put("showLockscreenNotification", c.showLockscreenNotification)
            put("isCompleted", c.isCompleted)
            put("isActive", c.isActive)

            val daysArray = JSONArray()
            c.dayRecords.forEach { d ->
                val dayObj = JSONObject().apply {
                    put("dayNumber", d.dayNumber)
                    put("status", d.status)
                    put("notes", d.notes)
                    put("timestamp", d.timestamp)
                }
                daysArray.put(dayObj)
            }
            put("dayRecords", daysArray)
        }
        jsonArray.put(obj)
    }
    return jsonArray.toString()
}

fun deserializeChallenges(jsonStr: String): List<HabitChallenge> {
    if (jsonStr.isBlank()) return emptyList()
    val list = mutableListOf<HabitChallenge>()
    try {
        val jsonArray = JSONArray(jsonStr)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val dayRecordsList = mutableListOf<ChallengeDayRecord>()
            val daysArray = obj.optJSONArray("dayRecords")
            if (daysArray != null) {
                for (j in 0 until daysArray.length()) {
                    val dayObj = daysArray.getJSONObject(j)
                    dayRecordsList.add(
                        ChallengeDayRecord(
                            dayNumber = dayObj.optInt("dayNumber", j + 1),
                            status = dayObj.optString("status", "PENDING"),
                            notes = dayObj.optString("notes", ""),
                            timestamp = dayObj.optLong("timestamp", 0L)
                        )
                    )
                }
            }
            list.add(
                HabitChallenge(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    title = obj.optString("title", "Custom Challenge"),
                    habitName = obj.optString("habitName", "Habit"),
                    totalDays = obj.optInt("totalDays", 3),
                    startTimestamp = obj.optLong("startTimestamp", System.currentTimeMillis()),
                    attendanceTime = obj.optString("attendanceTime", "10:00 PM"),
                    showLockscreenNotification = obj.optBoolean("showLockscreenNotification", true),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    isActive = obj.optBoolean("isActive", true),
                    dayRecords = dayRecordsList
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

fun updateChallengeNotification(context: Context, challenge: HabitChallenge?) {
    if (challenge == null) {
        updateAllChallengeNotifications(context, emptyList())
    } else {
        updateAllChallengeNotifications(context, listOf(challenge))
    }
}

fun parseAttendanceTime(timeStr: String): Pair<Int, Int> {
    return try {
        val upper = timeStr.trim().uppercase()
        val isPm = upper.contains("PM")
        val isAm = upper.contains("AM")
        val clean = upper.replace("AM", "").replace("PM", "").trim()
        val parts = clean.split(":")
        var hour = parts[0].trim().toInt()
        val min = if (parts.size > 1) parts[1].trim().toInt() else 0
        if (isPm && hour < 12) hour += 12
        if (isAm && hour == 12) hour = 0
        Pair(hour, min)
    } catch (e: Exception) {
        Pair(22, 0)
    }
}

fun scheduleChallengeReminders(context: Context, challenges: List<HabitChallenge>) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
    val activeList = challenges.filter { it.isActive && !it.isCompleted }

    for (challenge in activeList) {
        val (hour24, min) = parseAttendanceTime(challenge.attendanceTime)
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(java.util.Calendar.HOUR_OF_DAY, hour24)
            set(java.util.Calendar.MINUTE, min)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = android.content.Intent(context, ChallengeAlarmReceiver::class.java).apply {
            putExtra("challenge_id", challenge.id)
            putExtra("habit_name", challenge.habitName)
            putExtra("attendance_time", challenge.attendanceTime)
        }

        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            challenge.id.hashCode() + 50000,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val showIntent = android.app.PendingIntent.getActivity(
                    context,
                    challenge.id.hashCode() + 60000,
                    android.content.Intent(context, MainActivity::class.java),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                val alarmClockInfo = android.app.AlarmManager.AlarmClockInfo(calendar.timeInMillis, showIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback if exact alarm permissions are restricted on Android 12/13+
            try {
                alarmManager.setInexactRepeating(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    android.app.AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}

fun updateAllChallengeNotifications(context: Context, challenges: List<HabitChallenge>) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        ?: return

    val channelId = "active_challenges_lockscreen_channel"
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val channel = android.app.NotificationChannel(
            channelId,
            "Active Habit Challenges (Lock Screen & Status Bar)",
            android.app.NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent status bar & lock screen notifications for ongoing habit challenges"
            setShowBadge(true)
            lockscreenVisibility = androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    val activeList = challenges.filter { it.isActive && !it.isCompleted }

    // Cancel notifications for inactive, completed, or disabled challenges
    for (challenge in challenges) {
        if (!challenge.isActive || challenge.isCompleted || !challenge.showLockscreenNotification) {
            notificationManager.cancel(challenge.id.hashCode())
        }
    }

    // Post/update real notifications for active challenges
    for (challenge in activeList) {
        if (!challenge.showLockscreenNotification) continue

        val currentDay = challenge.getCurrentDayNumber()
        val successDays = challenge.getSuccessCount()

        val intent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            challenge.id.hashCode(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notificationText = "Day $currentDay of ${challenge.totalDays} • Check-in: ${challenge.attendanceTime} • Passed: $successDays days"

        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🏆 Active Challenge: ${challenge.habitName}")
            .setContentText(notificationText)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(notificationText))
            .setOngoing(true)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)

        try {
            notificationManager.notify(challenge.id.hashCode(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Schedule real push alarm reminders
    scheduleChallengeReminders(context, challenges)
}
