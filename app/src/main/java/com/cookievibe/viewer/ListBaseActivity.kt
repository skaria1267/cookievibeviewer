package com.cookievibe.viewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cookievibe.viewer.db.Entry
import com.cookievibe.viewer.db.Store
import com.google.android.material.appbar.MaterialToolbar

abstract class ListBaseActivity : AppCompatActivity() {

    protected abstract fun store(): Store
    protected abstract fun titleRes(): Int

    private lateinit var adapter: Adapter
    private val items = mutableListOf<Entry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setTitle(titleRes())
        toolbar.setNavigationOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rv)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = Adapter()
        rv.adapter = adapter
        reload()
    }

    private fun reload() {
        items.clear()
        items.addAll(store().list())
        adapter.notifyDataSetChanged()
        findViewById<TextView>(R.id.tvEmpty).visibility =
            if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun open(url: String) {
        val i = Intent(this, MainActivity::class.java).apply {
            data = Uri.parse(url)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(i)
        finish()
    }

    private inner class Adapter : RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_entry, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val e = items[position]
            holder.title.text = if (e.title.isBlank()) e.url else e.title
            holder.url.text = e.url
            holder.itemView.setOnClickListener { open(e.url) }
            holder.itemView.setOnLongClickListener {
                AlertDialog.Builder(this@ListBaseActivity)
                    .setItems(arrayOf(getString(R.string.action_open), getString(R.string.action_delete))) { _, which ->
                        when (which) {
                            0 -> open(e.url)
                            1 -> { store().remove(e.url); reload() }
                        }
                    }.show()
                true
            }
        }

        override fun getItemCount() = items.size
    }

    private class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvTitle)
        val url: TextView = v.findViewById(R.id.tvUrl)
    }
}
