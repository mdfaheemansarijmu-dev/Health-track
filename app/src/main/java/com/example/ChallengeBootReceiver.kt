package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ChallengeBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val jsonStr = sharedPref.getString("user_challenges_json", "") ?: ""
            val challenges = deserializeChallenges(jsonStr)
            updateAllChallengeNotifications(context, challenges)
        }
    }
}
