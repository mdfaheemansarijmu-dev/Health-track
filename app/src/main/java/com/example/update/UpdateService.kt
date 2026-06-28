package com.example.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateResult {
    data class NewUpdateAvailable(val updateInfo: UpdateInfo, val mustForceUpdate: Boolean) : UpdateResult()
    object UpToDate : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

interface UpdateService {
    suspend fun checkForUpdates(url: String, currentVersion: String): UpdateResult
}

class HttpUpdateService : UpdateService {
    override suspend fun checkForUpdates(url: String, currentVersion: String): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.useCaches = false
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                connection.disconnect()
                
                val json = JSONObject(response.toString())
                val latestVersion = json.getString("latestVersion")
                val minimumVersion = if (json.has("minimumVersion")) {
                    json.getString("minimumVersion")
                } else if (json.has("minimumSupportedVersion")) {
                    json.getString("minimumSupportedVersion")
                } else {
                    "1.0.0"
                }
                val releaseNotes = if (json.has("releaseNotes")) {
                    json.getString("releaseNotes")
                } else if (json.has("updateMessage")) {
                    json.getString("updateMessage")
                } else {
                    json.optString("updateTitle", "New features and bug fixes")
                }
                val apkUrl = if (json.has("apkUrl")) {
                    json.getString("apkUrl")
                } else if (json.has("downloadUrl")) {
                    json.getString("downloadUrl")
                } else {
                    ""
                }
                val forceUpdate = json.optBoolean("forceUpdate", false)
                
                val updateInfo = UpdateInfo(
                    latestVersion = latestVersion,
                    minimumVersion = minimumVersion,
                    releaseNotes = releaseNotes,
                    apkUrl = apkUrl,
                    forceUpdate = forceUpdate
                )
                
                val isNewer = compareVersions(currentVersion, latestVersion) < 0
                if (isNewer) {
                    val mustForce = forceUpdate || compareVersions(currentVersion, minimumVersion) < 0
                    UpdateResult.NewUpdateAvailable(updateInfo, mustForce)
                } else {
                    UpdateResult.UpToDate
                }
            } else {
                UpdateResult.Error("HTTP Error: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "Unknown network error")
        }
    }
}

fun compareVersions(v1: String, v2: String): Int {
    val cleanV1 = v1.replace("[^0-9.]".toRegex(), "")
    val cleanV2 = v2.replace("[^0-9.]".toRegex(), "")
    val parts1 = cleanV1.split(".").map { it.toIntOrNull() ?: 0 }
    val parts2 = cleanV2.split(".").map { it.toIntOrNull() ?: 0 }
    val length = maxOf(parts1.size, parts2.size)
    for (i in 0 until length) {
        val p1 = parts1.getOrElse(i) { 0 }
        val p2 = parts2.getOrElse(i) { 0 }
        if (p1 != p2) {
            return p1.compareTo(p2)
        }
    }
    return 0
}
