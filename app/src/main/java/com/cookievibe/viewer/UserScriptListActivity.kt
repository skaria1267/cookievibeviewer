package com.cookievibe.viewer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cookievibe.viewer.web.UserScript
import com.cookievibe.viewer.web.UserScriptStore
import com.google.android.material.appbar.MaterialToolbar

class UserScriptListActivity : AppCompatActivity() {

    private lateinit var store: UserScriptStore
    private lateinit var adapter: Adapter
    private lateinit var tvEmpty: TextView
    private val items = mutableListOf<UserScript>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_userscripts)
        store = UserScriptStore.get(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setTitle(R.string.title_userscripts)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.inflateMenu(R.menu.userscript_list_menu)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_new_script) {
                startActivity(Intent(this, UserScriptEditActivity::class.java))
                true
            } else false
        }

        tvEmpty = findViewById(R.id.tvEmpty)
        val rv = findViewById<RecyclerView>(R.id.rv)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = Adapter()
        rv.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        items.clear()
        items.addAll(store.list())
        adapter.notifyDataSetChanged()
        tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openEdit(id: String?) {
        val i = Intent(this, UserScriptEditActivity::class.java)
        if (id != null) i.putExtra(UserScriptEditActivity.EXTRA_ID, id)
        startActivity(i)
    }

    private fun confirmDelete(s: UserScript) {
        AlertDialog.Builder(this)
            .setTitle(R.string.action_delete)
            .setMessage(getString(R.string.msg_delete_script_confirm, s.name))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                store.remove(s.id)
                reload()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private inner class Adapter : RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_userscript, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val s = items[position]
            holder.title.text = if (s.version.isBlank()) s.name else "${s.name}  v${s.version}"
            val patterns = (s.matches + s.includes).joinToString("  ")
            val sub = buildString {
                if (s.description.isNotBlank()) append(s.description).append('\n')
                if (patterns.isNotBlank()) append(patterns)
                else append(getString(R.string.msg_no_match))
            }
            holder.subtitle.text = sub
            holder.sw.setOnCheckedChangeListener(null)
            holder.sw.isChecked = s.enabled
            holder.sw.setOnCheckedChangeListener { _, checked ->
                store.setEnabled(s.id, checked)
                val idx = items.indexOfFirst { it.id == s.id }
                if (idx >= 0) items[idx] = items[idx].copy(enabled = checked)
            }
            holder.itemView.setOnClickListener { openEdit(s.id) }
            holder.itemView.setOnLongClickListener {
                AlertDialog.Builder(this@UserScriptListActivity)
                    .setTitle(s.name)
                    .setItems(
                        arrayOf(
                            getString(R.string.action_edit),
                            getString(R.string.action_delete)
                        )
                    ) { _, which ->
                        when (which) {
                            0 -> openEdit(s.id)
                            1 -> confirmDelete(s)
                        }
                    }
                    .show()
                true
            }
        }

        override fun getItemCount() = items.size
    }

    private class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvTitle)
        val subtitle: TextView = v.findViewById(R.id.tvSubtitle)
        val sw: SwitchCompat = v.findViewById(R.id.swEnabled)
    }
}
