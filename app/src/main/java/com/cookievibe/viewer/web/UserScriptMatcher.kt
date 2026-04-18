package com.cookievibe.viewer.web

object UserScriptMatcher {

    fun matches(script: UserScript, url: String): Boolean {
        if (url.isBlank()) return false
        if (script.excludes.any { matchPattern(it, url) }) return false
        val patterns = script.matches + script.includes
        if (patterns.isEmpty()) return false
        return patterns.any { matchPattern(it, url) }
    }

    fun matchPattern(pattern: String, url: String): Boolean {
        val p = pattern.trim()
        if (p.isEmpty()) return false
        if (p == "*" || p == "<all_urls>") return true
        if (p.length >= 2 && p.startsWith("/") && p.endsWith("/")) {
            return try {
                Regex(p.substring(1, p.length - 1)).containsMatchIn(url)
            } catch (_: Exception) {
                false
            }
        }
        val re = globToRegex(p) ?: return false
        return re.matches(url)
    }

    private fun globToRegex(pattern: String): Regex? {
        val sb = StringBuilder("^")
        for (c in pattern) {
            when (c) {
                '*' -> sb.append(".*")
                '?' -> sb.append('.')
                '.', '+', '(', ')', '[', ']', '{', '}', '|', '^', '$', '\\' ->
                    sb.append('\\').append(c)
                else -> sb.append(c)
            }
        }
        sb.append('$')
        return try { Regex(sb.toString()) } catch (_: Exception) { null }
    }
}
