package com.cookievibe.viewer.web

object UserAgents {
    const val CHROME_DESKTOP_WIN =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    const val CHROME_DESKTOP_MAC =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    const val CHROME_ANDROID =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
    const val SAFARI_IOS =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"
    const val FIREFOX_DESKTOP =
        "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0"

    fun resolve(key: String, custom: String, default: String): String = when (key) {
        "chrome_desktop_win" -> CHROME_DESKTOP_WIN
        "chrome_desktop_mac" -> CHROME_DESKTOP_MAC
        "chrome_android" -> CHROME_ANDROID
        "safari_ios" -> SAFARI_IOS
        "firefox_desktop" -> FIREFOX_DESKTOP
        "custom" -> if (custom.isNotBlank()) custom else default
        else -> default
    }

    fun isDesktop(key: String): Boolean = when (key) {
        "chrome_desktop_win", "chrome_desktop_mac", "firefox_desktop" -> true
        else -> false
    }
}
