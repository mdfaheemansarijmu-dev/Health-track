package com.example

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AdultIncident(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val browserPackage: String,
    val browserName: String,
    val detectedUrlOrTerm: String,
    val matchedPattern: String,
    val actionTaken: String = "Logged & Alerted"
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

object AdultContentGuardManager {
    private const val PREFS_NAME = "jagr_adult_guard_prefs"
    private const val KEY_PROTECTION_ENABLED = "protection_enabled"
    private const val KEY_INCIDENTS_JSON = "incidents_json"
    private const val KEY_CUSTOM_DOMAINS = "custom_domains"
    private const val KEY_SENSITIVITY_HIGH = "sensitivity_high"

    // Core default adult domains list
    private val DEFAULT_ADULT_DOMAINS = setOf(
        "pornhub.com", "xvideos.com", "xnxx.com", "redtube.com", "xhamster.com",
        "youporn.com", "chaturbate.com", "onlyfans.com", "stripchat.com", "cam4.com",
        "livejasmin.com", "spankbang.com", "tube8.com", "eporner.com", "beeg.com",
        "brazzers.com", "bangbros.com", "naughtyamerica.com", "porn.com", "xcafe.com",
        "tnaflix.com", "motherless.com", "daftsex.com", "fuq.com", "txxx.com",
        "pornmd.com", "heavy-r.com", "playboy.com", "penthouse.com", "hustler.com",
        "rule34.xxx", "gelbooru.com", "danbooru.donmai.us", "e-hentai.org", "nhentai.net",
        "luscious.net", "xvideos2.com", "xnxx2.com", "hqporner.com", "fapello.com"
    )

    // Common adult keywords in search queries or URL paths
    private val ADULT_KEYWORDS = listOf(
        "porn", "xxx", "xvideos", "xnxx", "xhamster", "pornhub", "redtube",
        "youporn", "erotic", "nsfw", "hentai", "nude", "naked", "sex video",
        "camgirl", "chaturbate", "onlyfans leaks", "stripper", "hardcore sex",
        "milf", "blowjob", "fuck", "boobs", "pussy video", "dildo", "webcam sex"
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isProtectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PROTECTION_ENABLED, true)
    }

    fun setProtectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PROTECTION_ENABLED, enabled).apply()
    }

    fun isHighSensitivity(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SENSITIVITY_HIGH, true)
    }

    fun setHighSensitivity(context: Context, high: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SENSITIVITY_HIGH, high).apply()
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val expectedServiceName = "${context.packageName}/${BrowserProtectionService::class.java.canonicalName}"
        val expectedShortName = "${context.packageName}/${BrowserProtectionService::class.java.name}"
        
        for (service in enabledServices) {
            val id = service.id
            if (id.contains(context.packageName) && (id.contains("BrowserProtectionService") || id == expectedServiceName || id == expectedShortName)) {
                return true
            }
        }
        return false
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallback)
            } catch (ignored: Exception) {}
        }
    }

    fun checkUrlOrQueryForAdultContent(context: Context, input: String): Pair<Boolean, String>? {
        if (!isProtectionEnabled(context)) return null
        val cleanInput = input.trim().lowercase(Locale.ROOT)
        if (cleanInput.isBlank()) return null

        val customDomains = getCustomBlockedDomains(context)
        val allDomains = DEFAULT_ADULT_DOMAINS + customDomains

        // 1. Check exact domain or subdomain matching
        val extractedDomain = extractDomain(cleanInput)
        if (extractedDomain != null) {
            for (domain in allDomains) {
                if (extractedDomain == domain || extractedDomain.endsWith(".$domain") || extractedDomain.contains(domain)) {
                    return Pair(true, "Blocked Domain: $domain")
                }
            }
        }

        // 2. Check if cleanInput contains domain substring
        for (domain in allDomains) {
            val cleanDomain = domain.removePrefix("www.").lowercase()
            if (cleanInput.contains(cleanDomain)) {
                return Pair(true, "Matched Domain Pattern: $cleanDomain")
            }
        }

        // 3. High sensitivity keyword detection in URL or search query
        if (isHighSensitivity(context)) {
            for (kw in ADULT_KEYWORDS) {
                // Word boundary or explicit match
                val regex = Regex("\\b${Regex.escape(kw)}\\b", RegexOption.IGNORE_CASE)
                if (regex.containsMatchIn(cleanInput) || cleanInput.contains("/$kw") || cleanInput.contains("=$kw")) {
                    return Pair(true, "Adult Keyword Detected: $kw")
                }
            }
        }

        return null
    }

    private fun extractDomain(urlStr: String): String? {
        val withProtocol = if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
            "https://$urlStr"
        } else {
            urlStr
        }
        return try {
            val uri = URI(withProtocol)
            uri.host?.lowercase(Locale.ROOT)?.removePrefix("www.")
        } catch (e: Exception) {
            null
        }
    }

    // Incident logging
    fun recordIncident(
        context: Context,
        browserPackage: String,
        browserName: String,
        detectedUrlOrTerm: String,
        matchedPattern: String
    ): AdultIncident {
        val incident = AdultIncident(
            browserPackage = browserPackage,
            browserName = browserName,
            detectedUrlOrTerm = detectedUrlOrTerm,
            matchedPattern = matchedPattern
        )
        val incidents = getIncidents(context).toMutableList()
        
        // Prevent duplicate spam log within 5 seconds for same URL
        val recentDuplicate = incidents.firstOrNull {
            it.detectedUrlOrTerm == detectedUrlOrTerm && (incident.timestamp - it.timestamp) < 5000L
        }
        if (recentDuplicate == null) {
            incidents.add(0, incident)
            // Keep at most 100 entries
            val trimmed = incidents.take(100)
            saveIncidents(context, trimmed)
        }
        return incident
    }

    fun getIncidents(context: Context): List<AdultIncident> {
        val jsonStr = getPrefs(context).getString(KEY_INCIDENTS_JSON, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<AdultIncident>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AdultIncident(
                        id = obj.optString("id"),
                        timestamp = obj.optLong("timestamp"),
                        browserPackage = obj.optString("browserPackage"),
                        browserName = obj.optString("browserName"),
                        detectedUrlOrTerm = obj.optString("detectedUrlOrTerm"),
                        matchedPattern = obj.optString("matchedPattern"),
                        actionTaken = obj.optString("actionTaken", "Logged & Alerted")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveIncidents(context: Context, incidents: List<AdultIncident>) {
        try {
            val array = JSONArray()
            for (item in incidents) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("timestamp", item.timestamp)
                    put("browserPackage", item.browserPackage)
                    put("browserName", item.browserName)
                    put("detectedUrlOrTerm", item.detectedUrlOrTerm)
                    put("matchedPattern", item.matchedPattern)
                    put("actionTaken", item.actionTaken)
                }
                array.put(obj)
            }
            getPrefs(context).edit().putString(KEY_INCIDENTS_JSON, array.toString()).apply()
        } catch (e: Exception) {
            // Ignore write error
        }
    }

    fun clearIncidents(context: Context) {
        getPrefs(context).edit().remove(KEY_INCIDENTS_JSON).apply()
    }

    fun getCustomBlockedDomains(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_CUSTOM_DOMAINS, emptySet()) ?: emptySet()
    }

    fun addCustomBlockedDomain(context: Context, domain: String) {
        val clean = domain.trim().lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.")
        if (clean.isNotBlank()) {
            val current = getCustomBlockedDomains(context).toMutableSet()
            current.add(clean)
            getPrefs(context).edit().putStringSet(KEY_CUSTOM_DOMAINS, current).apply()
        }
    }

    fun removeCustomBlockedDomain(context: Context, domain: String) {
        val current = getCustomBlockedDomains(context).toMutableSet()
        current.remove(domain)
        getPrefs(context).edit().putStringSet(KEY_CUSTOM_DOMAINS, current).apply()
    }
}
