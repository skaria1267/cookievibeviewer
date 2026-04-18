package com.cookievibe.viewer.web

object FingerprintScript {
    fun build(platform: String = "Win32", languages: List<String> = listOf("en-US", "en")): String {
        val langJs = languages.joinToString(",") { "\"$it\"" }
        return """
            (function() {
              try {
                Object.defineProperty(navigator, 'webdriver', { get: () => false, configurable: true });
                Object.defineProperty(navigator, 'platform', { get: () => '$platform', configurable: true });
                Object.defineProperty(navigator, 'languages', { get: () => [$langJs], configurable: true });
                Object.defineProperty(navigator, 'language', { get: () => navigator.languages[0] || 'en-US', configurable: true });
                Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 8, configurable: true });
                Object.defineProperty(navigator, 'deviceMemory', { get: () => 8, configurable: true });
                if (!window.chrome) { window.chrome = { runtime: {} }; }
                const originalQuery = navigator.permissions && navigator.permissions.query;
                if (originalQuery) {
                  navigator.permissions.query = (p) =>
                    p && p.name === 'notifications'
                      ? Promise.resolve({ state: Notification.permission, onchange: null })
                      : originalQuery.call(navigator.permissions, p);
                }
              } catch (e) {}
            })();
        """.trimIndent()
    }

    fun platformFor(uaKey: String): String = when (uaKey) {
        "chrome_desktop_win" -> "Win32"
        "chrome_desktop_mac", "firefox_desktop" -> "MacIntel"
        "chrome_android" -> "Linux armv8l"
        "safari_ios" -> "iPhone"
        else -> "Linux armv8l"
    }
}
