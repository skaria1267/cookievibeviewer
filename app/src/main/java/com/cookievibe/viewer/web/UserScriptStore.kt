package com.cookievibe.viewer.web

import android.content.Context
import org.json.JSONArray
import java.util.UUID

class UserScriptStore private constructor(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("cvv_userscripts", Context.MODE_PRIVATE)

    fun list(): List<UserScript> {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<UserScript>(arr.length())
            for (i in 0 until arr.length()) out.add(UserScript.fromJson(arr.getJSONObject(i)))
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun enabledOnly(): List<UserScript> = list().filter { it.enabled }

    fun get(id: String): UserScript? = list().firstOrNull { it.id == id }

    fun upsert(script: UserScript): UserScript {
        val now = System.currentTimeMillis()
        val withId = if (script.id.isBlank())
            script.copy(id = UUID.randomUUID().toString(), createdAt = now, updatedAt = now)
        else
            script.copy(updatedAt = now)
        val cur = list().toMutableList()
        val idx = cur.indexOfFirst { it.id == withId.id }
        if (idx >= 0) cur[idx] = withId else cur.add(0, withId)
        save(cur)
        return withId
    }

    fun remove(id: String) {
        val cur = list().toMutableList()
        if (cur.removeAll { it.id == id }) save(cur)
    }

    fun setEnabled(id: String, enabled: Boolean) {
        val cur = list().toMutableList()
        val idx = cur.indexOfFirst { it.id == id }
        if (idx < 0) return
        cur[idx] = cur[idx].copy(enabled = enabled, updatedAt = System.currentTimeMillis())
        save(cur)
    }

    private fun save(list: List<UserScript>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val KEY = "scripts"
        fun get(c: Context) = UserScriptStore(c)
    }
}
