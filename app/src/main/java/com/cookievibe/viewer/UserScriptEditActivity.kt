package com.cookievibe.viewer

import android.os.Bundle
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.cookievibe.viewer.web.UserScript
import com.cookievibe.viewer.web.UserScriptParser
import com.cookievibe.viewer.web.UserScriptStore
import com.google.android.material.appbar.MaterialToolbar

class UserScriptEditActivity : AppCompatActivity() {

    private lateinit var store: UserScriptStore
    private lateinit var etCode: EditText
    private lateinit var toolbar: MaterialToolbar
    private var current: UserScript? = null
    private var originalCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_userscript_edit)
        store = UserScriptStore.get(this)

        toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { confirmExit() }
        toolbar.inflateMenu(R.menu.userscript_edit_menu)
        toolbar.setOnMenuItemClickListener(::onMenu)

        etCode = findViewById(R.id.etCode)

        val id = intent.getStringExtra(EXTRA_ID)
        if (id != null) {
            val s = store.get(id)
            if (s != null) {
                current = s
                originalCode = s.code
                toolbar.title = s.name
                etCode.setText(s.code)
            } else {
                toolbar.setTitle(R.string.title_userscript_new)
            }
        } else {
            toolbar.setTitle(R.string.title_userscript_new)
            etCode.setHint(R.string.hint_paste_script)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        confirmExit()
    }

    private fun onMenu(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_save -> { save(); true }
        R.id.action_delete -> { confirmDelete(); true }
        else -> false
    }

    private fun save() {
        val code = etCode.text.toString().trim()
        if (code.isEmpty()) {
            Toast.makeText(this, R.string.msg_no_script_content, Toast.LENGTH_SHORT).show()
            return
        }
        if (!UserScriptParser.hasHeader(code)) {
            Toast.makeText(this, R.string.msg_script_parse_error, Toast.LENGTH_LONG).show()
            return
        }
        val meta = UserScriptParser.parse(code)
        if (meta.matches.isEmpty() && meta.includes.isEmpty()) {
            Toast.makeText(this, R.string.msg_no_match_directive, Toast.LENGTH_LONG).show()
            return
        }
        val cur = current
        val now = System.currentTimeMillis()
        val script = UserScript(
            id = cur?.id ?: "",
            name = meta.name,
            namespace = meta.namespace,
            version = meta.version,
            description = meta.description,
            author = meta.author,
            matches = meta.matches,
            excludes = meta.excludes,
            includes = meta.includes,
            runAt = meta.runAt,
            noFrames = meta.noFrames,
            code = code,
            enabled = cur?.enabled ?: true,
            createdAt = cur?.createdAt ?: now,
            updatedAt = now
        )
        val saved = store.upsert(script)
        current = saved
        originalCode = saved.code
        Toast.makeText(this, R.string.msg_script_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun confirmDelete() {
        val s = current ?: run { finish(); return }
        AlertDialog.Builder(this)
            .setTitle(R.string.action_delete)
            .setMessage(getString(R.string.msg_delete_script_confirm, s.name))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                store.remove(s.id)
                finish()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun confirmExit() {
        if (etCode.text.toString() == originalCode) {
            finish()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.title_userscript_edit)
            .setMessage(R.string.msg_discard_changes)
            .setPositiveButton(R.string.action_discard) { _, _ -> finish() }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    companion object {
        const val EXTRA_ID = "script_id"
    }
}
