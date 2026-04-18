package com.cookievibe.viewer.db

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Entry(val title: String, val url: String, val time: Long)

class Store(context: Context, private val key: String, private val maxSize: Int = 0) {
    private val prefs = context.applicationContext.getSharedPreferences("cvv_store", Context.MODE_PRIVATE)

    fun list(): List<Entry> {
        val raw = prefs.getString(key, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<Entry>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(Entry(o.optString("title"), o.optString("url"), o.optLong("time")))
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(entry: Entry) {
        val cur = list().toMutableList()
        cur.removeAll { it.url == entry.url }
        cur.add(0, entry)
        if (maxSize > 0 && cur.size > maxSize) {
            while (cur.size > maxSize) cur.removeAt(cur.size - 1)
        }
        save(cur)
    }

    fun remove(url: String) {
        val cur = list().toMutableList()
        cur.removeAll { it.url == url }
        save(cur)
    }

    fun clear() {
        prefs.edit().remove(key).apply()
    }

    private fun save(list: List<Entry>) {
        val arr = JSONArray()
        for (e in list) {
            val o = JSONObject()
            o.put("title", e.title)
            o.put("url", e.url)
            o.put("time", e.time)
            arr.put(o)
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    companion object {
        fun bookmarks(c: Context) = Store(c, "bookmarks", 0)
        fun history(c: Context) = Store(c, "history", 500)
    }
}
