package com.cookievibe.viewer.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object Prefs {
    fun get(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    fun homepage(c: Context): String =
        get(c).getString("homepage", "https://duckduckgo.com/") ?: "https://duckduckgo.com/"

    fun searchEngine(c: Context): String =
        get(c).getString("search_engine", "https://duckduckgo.com/?q=") ?: "https://duckduckgo.com/?q="

    fun uaPreset(c: Context): String = get(c).getString("ua_preset", "default") ?: "default"

    fun uaCustom(c: Context): String = get(c).getString("ua_custom", "") ?: ""

    fun acceptLanguage(c: Context): String =
        get(c).getString("accept_language", "en-US,en;q=0.9") ?: "en-US,en;q=0.9"

    fun jsEnabled(c: Context): Boolean = get(c).getBoolean("js_enabled", true)
    fun loadImages(c: Context): Boolean = get(c).getBoolean("load_images", true)
    fun dnt(c: Context): Boolean = get(c).getBoolean("dnt", true)
    fun thirdPartyCookies(c: Context): Boolean = get(c).getBoolean("third_party_cookies", true)
    fun fingerprintSpoof(c: Context): Boolean = get(c).getBoolean("fingerprint_spoof", false)
    fun mixedContent(c: Context): Boolean = get(c).getBoolean("mixed_content", false)
    fun sslOverride(c: Context): Boolean = get(c).getBoolean("ssl_override", false)
}
