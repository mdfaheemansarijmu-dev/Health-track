package com.example.update

data class UpdateInfo(
    val latestVersion: String,
    val minimumVersion: String,
    val releaseNotes: String,
    val apkUrl: String,
    val forceUpdate: Boolean
)
