package com.cookievibe.viewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import org.json.JSONArray
import org.json.JSONObject

class CookieActivity : AppCompatActivity() {

    private val items = mutableListOf<Pair<String, String>>()
    private lateinit var url: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cookie)

        url = intent.getStringExtra(EXTRA_URL).orEmpty()

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        findViewById<TextView>(R.id.tvUrl).text = getString(R.string.label_url) + " " + url

        loadCookies()

        val rv = findViewById<RecyclerView>(R.id.rvCookies)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = Adapter()

        findViewById<TextView>(R.id.tvCount).text = getString(R.string.label_cookie_count, items.size)

        findViewById<Button>(R.id.btnCopyAll).setOnClickListener {
            copyAll()
        }
        findViewById<Button>(R.id.btnExportJson).setOnClickListener {
            shareText("cookies.json", toJson())
        }
        findViewById<Button>(R.id.btnExportHeader).setOnClickListener {
            shareText("cookie-header.txt", toHeader())
        }
    }

    private fun loadCookies() {
        items.clear()
        val raw = CookieManager.getInstance().getCookie(url).orEmpty()
        if (raw.isBlank()) return
        raw.split(";").forEach { chunk ->
            val s = chunk.trim()
            val idx = s.indexOf('=')
            if (idx > 0) {
                val name = s.substring(0, idx)
                val value = if (idx < s.length - 1) s.substring(idx + 1) else ""
                items.add(name to value)
            }
        }
    }

    private fun toHeader(): String = items.joinToString("; ") { "${it.first}=${it.second}" }

    private fun toJson(): String {
        val arr = JSONArray()
        items.forEach {
            val o = JSONObject()
            o.put("name", it.first)
            o.put("value", it.second)
            arr.put(o)
        }
        return arr.toString(2)
    }

    private fun copyAll() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("cookies", toHeader()))
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            Toast.makeText(this, R.string.msg_copied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareText(title: String, text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(title, text))
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
    }

    private inner class Adapter : RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_cookie, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val (name, value) = items[position]
            holder.name.text = name
            holder.value.text = value
            holder.itemView.setOnLongClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText(name, "$name=$value"))
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                    Toast.makeText(this@CookieActivity, R.string.msg_copied, Toast.LENGTH_SHORT).show()
                }
                true
            }
        }

        override fun getItemCount() = items.size
    }

    private class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvName)
        val value: TextView = v.findViewById(R.id.tvValue)
    }

    companion object {
        const val EXTRA_URL = "extra_url"
    }
}
