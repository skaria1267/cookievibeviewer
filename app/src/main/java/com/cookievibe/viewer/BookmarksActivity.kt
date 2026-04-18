package com.cookievibe.viewer

import com.cookievibe.viewer.db.Store

class BookmarksActivity : ListBaseActivity() {
    override fun store() = Store.bookmarks(this)
    override fun titleRes() = R.string.title_bookmarks
}
