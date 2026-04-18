package com.cookievibe.viewer.web

import org.json.JSONArray
import org.json.JSONObject

enum class RunAt(val key: String) {
    START("document-start"),
    END("document-end"),
    IDLE("document-idle");

    companion object {
        fun from(s: String?): RunAt = values().firstOrNull { it.key == s } ?: IDLE
    }
}

data class UserScript(
    val id: String,
    val name: String,
    val namespace: String,
    val version: String,
    val description: String,
    val author: String,
    val matches: List<String>,
    val excludes: List<String>,
    val includes: List<String>,
    val runAt: RunAt,
    val noFrames: Boolean,
    val code: String,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("name", name)
        o.put("namespace", namespace)
        o.put("version", version)
        o.put("description", description)
        o.put("author", author)
        o.put("matches", JSONArray(matches))
        o.put("excludes", JSONArray(excludes))
        o.put("includes", JSONArray(includes))
        o.put("runAt", runAt.key)
        o.put("noFrames", noFrames)
        o.put("code", code)
        o.put("enabled", enabled)
        o.put("createdAt", createdAt)
        o.put("updatedAt", updatedAt)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): UserScript {
            fun arr(key: String): List<String> {
                val a = o.optJSONArray(key) ?: return emptyList()
                val list = ArrayList<String>(a.length())
                for (i in 0 until a.length()) list.add(a.optString(i))
                return list
            }
            return UserScript(
                id = o.optString("id"),
                name = o.optString("name"),
                namespace = o.optString("namespace"),
                version = o.optString("version"),
                description = o.optString("description"),
                author = o.optString("author"),
                matches = arr("matches"),
                excludes = arr("excludes"),
                includes = arr("includes"),
                runAt = RunAt.from(o.optString("runAt")),
                noFrames = o.optBoolean("noFrames"),
                code = o.optString("code"),
                enabled = o.optBoolean("enabled", true),
                createdAt = o.optLong("createdAt"),
                updatedAt = o.optLong("updatedAt")
            )
        }
    }
}

object UserScriptParser {
    private val HEADER = Regex(
        """//\s*==UserScript==\s*\r?\n(.*?)\r?\n\s*//\s*==/UserScript==""",
        RegexOption.DOT_MATCHES_ALL
    )

    data class Meta(
        val name: String,
        val namespace: String,
        val version: String,
        val description: String,
        val author: String,
        val matches: List<String>,
        val excludes: List<String>,
        val includes: List<String>,
        val runAt: RunAt,
        val noFrames: Boolean
    )

    fun hasHeader(code: String): Boolean = HEADER.containsMatchIn(code)

    fun parse(code: String): Meta {
        val m = HEADER.find(code) ?: return default()
        val block = m.groupValues[1]
        val dirs = LinkedHashMap<String, MutableList<String>>()
        for (raw in block.lines()) {
            val line = raw.trim()
            if (!line.startsWith("//")) continue
            val rest = line.removePrefix("//").trim()
            if (!rest.startsWith("@")) continue
            val parts = rest.split(Regex("\\s+"), limit = 2)
            val key = parts[0].removePrefix("@").lowercase()
            val value = if (parts.size > 1) parts[1].trim() else ""
            dirs.getOrPut(key) { mutableListOf() }.add(value)
        }
        fun one(k: String) = dirs[k]?.firstOrNull().orEmpty()
        fun all(k: String) = dirs[k].orEmpty().filter { it.isNotBlank() }
        return Meta(
            name = one("name").ifBlank { "Untitled" },
            namespace = one("namespace"),
            version = one("version"),
            description = one("description"),
            author = one("author"),
            matches = all("match"),
            excludes = all("exclude"),
            includes = all("include"),
            runAt = RunAt.from(one("run-at")),
            noFrames = dirs.containsKey("noframes")
        )
    }

    private fun default() = Meta(
        "Untitled", "", "", "", "",
        emptyList(), emptyList(), emptyList(),
        RunAt.IDLE, false
    )
}
