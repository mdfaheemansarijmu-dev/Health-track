package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

class ChallengeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val challengeId = intent.getStringExtra("challenge_id") ?: return
        val habitName = intent.getStringExtra("habit_name") ?: "Habit Challenge"
        val attendanceTime = intent.getStringExtra("attendance_time") ?: "10:00 PM"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val channelId = "challenge_push_reminder_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nightly Challenge Attendance Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nightly check-in reminders for your active habit challenges"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_open_challenge_id", challengeId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            challengeId.hashCode() + 50000,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏰ Nightly Attendance Check-in!")
            .setContentText("It's $attendanceTime! Tap to record attendance for '$habitName'.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("It's $attendanceTime! Did you follow your rule today for '$habitName'? Tap here to mark your pass/fail attendance.")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_edit,
                "Submit Check-In Now",
                pendingIntent
            )
            .build()

        try {
            notificationManager.notify(challengeId.hashCode() + 50000, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Reschedule alarm for tomorrow at attendance time
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val jsonStr = sharedPref.getString("user_challenges_json", "") ?: ""
        val challenges = deserializeChallenges(jsonStr)
        scheduleChallengeReminders(context, challenges)
    }
}
