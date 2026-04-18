package com.cookievibe.viewer

import com.cookievibe.viewer.db.Store

class HistoryActivity : ListBaseActivity() {
    override fun store() = Store.history(this)
    override fun titleRes() = R.string.title_history
}
