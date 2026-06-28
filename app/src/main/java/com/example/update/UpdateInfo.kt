package com.example.update

data class UpdateInfo(
    val latestVersion: String,
    val minimumSupportedVersion: String,
    val updateTitle: String,
    val updateMessage: String,
    val downloadUrl: String,
    val forceUpdate: Boolean
)
