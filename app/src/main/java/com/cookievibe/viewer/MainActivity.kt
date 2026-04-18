package com.cookievibe.viewer

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.cookievibe.viewer.db.Entry
import com.cookievibe.viewer.db.Store
import com.cookievibe.viewer.prefs.Prefs
import com.cookievibe.viewer.web.FingerprintScript
import com.cookievibe.viewer.web.UserAgents

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var etUrl: EditText
    private lateinit var progress: ProgressBar
    private lateinit var webContainer: FrameLayout
    private lateinit var fullscreenContainer: FrameLayout

    private var incognito = false
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private val prefChange = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        applyWebSettings()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        etUrl = findViewById(R.id.etUrl)
        progress = findViewById(R.id.progressBar)
        webContainer = findViewById(R.id.webContainer)
        fullscreenContainer = findViewById(R.id.fullscreenContainer)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
        }
        findViewById<ImageButton>(R.id.btnForward).setOnClickListener {
            if (webView.canGoForward()) webView.goForward()
        }
        findViewById<ImageButton>(R.id.btnReload).setOnClickListener { webView.reload() }
        findViewById<ImageButton>(R.id.btnMenu).setOnClickListener(::showMenu)

        etUrl.setOnEditorActionListener { _, actionId, event ->
            val done = actionId == EditorInfo.IME_ACTION_GO ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            if (done) {
                loadInput(etUrl.text.toString())
                etUrl.clearFocus()
                true
            } else false
        }

        setupWebView()
        applyWebSettings()

        Prefs.get(this).registerOnSharedPreferenceChangeListener(prefChange)

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            val data = intent?.data
            if (data != null) webView.loadUrl(data.toString())
            else webView.loadUrl(Prefs.homepage(this))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { webView.loadUrl(it.toString()) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onDestroy() {
        Prefs.get(this).unregisterOnSharedPreferenceChangeListener(prefChange)
        webContainer.removeView(webView)
        webView.stopLoading()
        webView.settings.javaScriptEnabled = false
        webView.clearHistory()
        webView.removeAllViews()
        webView.destroy()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (customView != null) { onHideCustomViewInternal(); return true }
            if (webView.canGoBack()) { webView.goBack(); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.setDownloadListener(DownloadListener { url, _, contentDisposition, mimetype, _ ->
            try {
                val req = DownloadManager.Request(Uri.parse(url))
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                val name = URLUtil.guessFileName(url, contentDisposition, mimetype)
                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
                req.setMimeType(mimetype)
                CookieManager.getInstance().getCookie(url)?.let { req.addRequestHeader("cookie", it) }
                req.addRequestHeader("User-Agent", webView.settings.userAgentString)
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(req)
                Toast.makeText(this, getString(R.string.msg_downloading, name), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.msg_download_fail, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        })

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                progress.visibility = View.VISIBLE
                if (!etUrl.hasFocus()) etUrl.setText(url)
                if (Prefs.fingerprintSpoof(this@MainActivity)) {
                    val key = Prefs.uaPreset(this@MainActivity)
                    val langs = Prefs.acceptLanguage(this@MainActivity)
                        .split(",").map { it.substringBefore(";").trim() }.filter { it.isNotEmpty() }
                    view.evaluateJavascript(
                        FingerprintScript.build(FingerprintScript.platformFor(key), langs), null
                    )
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                progress.visibility = View.GONE
                if (!incognito) {
                    val title = view.title ?: url
                    Store.history(this@MainActivity).add(Entry(title, url, System.currentTimeMillis()))
                }
                CookieManager.getInstance().flush()
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return handleUrlOverride(request.url.toString())
            }

            @Deprecated("legacy")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handleUrlOverride(url)
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return super.shouldInterceptRequest(view, request)
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: android.net.http.SslError) {
                if (Prefs.sslOverride(this@MainActivity)) {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.msg_ssl_title)
                        .setMessage(getString(R.string.msg_ssl_warning) + "\n\n" + error.toString())
                        .setPositiveButton(R.string.action_proceed) { _, _ -> handler.proceed() }
                        .setNegativeButton(R.string.action_cancel) { _, _ -> handler.cancel() }
                        .setCancelable(false)
                        .show()
                } else {
                    handler.cancel()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }

            override fun onReceivedTitle(view: WebView, title: String?) { /* could update tab title */ }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) { callback.onCustomViewHidden(); return }
                customView = view
                customViewCallback = callback
                fullscreenContainer.addView(view)
                fullscreenContainer.visibility = View.VISIBLE
                webView.visibility = View.GONE
            }

            override fun onHideCustomView() { onHideCustomViewInternal() }

            override fun onPermissionRequest(request: android.webkit.PermissionRequest) {
                runOnUiThread {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.msg_permission_title)
                        .setMessage(request.resources.joinToString(", "))
                        .setPositiveButton(R.string.action_grant) { _, _ -> request.grant(request.resources) }
                        .setNegativeButton(R.string.action_deny) { _, _ -> request.deny() }
                        .show()
                }
            }
        }
    }

    private fun onHideCustomViewInternal() {
        customView?.let {
            fullscreenContainer.removeView(it)
            fullscreenContainer.visibility = View.GONE
            webView.visibility = View.VISIBLE
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null
        }
    }

    private fun handleUrlOverride(url: String): Boolean {
        val scheme = Uri.parse(url).scheme?.lowercase() ?: return false
        return when (scheme) {
            "http", "https", "file", "about", "data", "blob", "javascript" -> false
            else -> {
                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } catch (_: Exception) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (_: Exception) {
                        Toast.makeText(this, R.string.msg_cannot_open, Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun applyWebSettings() {
        val s = webView.settings
        s.javaScriptEnabled = Prefs.jsEnabled(this)
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.setSupportZoom(true)
        s.builtInZoomControls = true
        s.displayZoomControls = false
        s.loadWithOverviewMode = true
        s.useWideViewPort = true
        s.loadsImagesAutomatically = Prefs.loadImages(this)
        s.blockNetworkImage = !Prefs.loadImages(this)
        s.allowContentAccess = true
        s.allowFileAccess = false
        s.mediaPlaybackRequiresUserGesture = true
        s.mixedContentMode = if (Prefs.mixedContent(this))
            WebSettings.MIXED_CONTENT_ALWAYS_ALLOW else WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        val uaKey = Prefs.uaPreset(this)
        val defaultUa = WebSettings.getDefaultUserAgent(this)
        s.userAgentString = UserAgents.resolve(uaKey, Prefs.uaCustom(this), defaultUa)

        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(!incognito)
        cm.setAcceptThirdPartyCookies(webView, Prefs.thirdPartyCookies(this) && !incognito)

        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(s, true)
        }
    }

    private fun loadInput(raw: String) {
        val input = raw.trim()
        if (input.isEmpty()) return
        val url = when {
            input.startsWith("http://") || input.startsWith("https://") ||
                    input.startsWith("about:") || input.startsWith("javascript:") -> input
            input.contains(" ") || !input.contains(".") ->
                Prefs.searchEngine(this) + Uri.encode(input)
            else -> "https://$input"
        }
        webView.loadUrl(url)
    }

    private fun showMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_new_tab -> { webView.loadUrl(Prefs.homepage(this)); true }
                R.id.action_bookmarks -> { startActivity(Intent(this, BookmarksActivity::class.java)); true }
                R.id.action_add_bookmark -> {
                    val url = webView.url ?: return@setOnMenuItemClickListener true
                    Store.bookmarks(this).add(Entry(webView.title ?: url, url, System.currentTimeMillis()))
                    Toast.makeText(this, R.string.msg_bookmark_added, Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_history -> { startActivity(Intent(this, HistoryActivity::class.java)); true }
                R.id.action_cookies -> {
                    val url = webView.url
                    if (url.isNullOrEmpty()) {
                        Toast.makeText(this, R.string.msg_no_cookies, Toast.LENGTH_SHORT).show()
                    } else {
                        val i = Intent(this, CookieActivity::class.java)
                        i.putExtra(CookieActivity.EXTRA_URL, url)
                        startActivity(i)
                    }
                    true
                }
                R.id.action_import_cookies -> { showImportCookiesDialog(); true }
                R.id.action_incognito -> { toggleIncognito(); true }
                R.id.action_desktop -> { toggleDesktop(); true }
                R.id.action_find -> { showFindInPage(); true }
                R.id.action_share -> {
                    val url = webView.url ?: return@setOnMenuItemClickListener true
                    val i = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                    }
                    startActivity(Intent.createChooser(i, getString(R.string.action_share)))
                    true
                }
                R.id.action_copy_url -> {
                    val url = webView.url ?: ""
                    copyToClipboard("URL", url)
                    true
                }
                R.id.action_clear -> { clearData(); true }
                R.id.action_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun toggleIncognito() {
        incognito = !incognito
        Toast.makeText(
            this,
            if (incognito) R.string.msg_incognito_on else R.string.msg_incognito_off,
            Toast.LENGTH_SHORT
        ).show()
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(!incognito)
        cm.setAcceptThirdPartyCookies(webView, Prefs.thirdPartyCookies(this) && !incognito)
        if (incognito) {
            cm.removeSessionCookies(null)
            webView.clearFormData()
            webView.clearHistory()
        }
    }

    private fun toggleDesktop() {
        val current = webView.settings.userAgentString
        val desktop = UserAgents.CHROME_DESKTOP_WIN
        webView.settings.userAgentString = if (current == desktop) {
            UserAgents.resolve(
                Prefs.uaPreset(this), Prefs.uaCustom(this),
                WebSettings.getDefaultUserAgent(this)
            )
        } else desktop
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.reload()
    }

    private fun showFindInPage() {
        val input = EditText(this).apply { hint = getString(R.string.msg_find_hint) }
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_find_in_page)
            .setView(input)
            .setPositiveButton(R.string.action_find) { _, _ -> webView.findAllAsync(input.text.toString()) }
            .setNegativeButton(R.string.action_clear) { _, _ -> webView.clearMatches() }
            .show()
    }

    private fun showImportCookiesDialog() {
        val url = webView.url
        if (url.isNullOrEmpty()) {
            Toast.makeText(this, R.string.msg_load_first, Toast.LENGTH_SHORT).show()
            return
        }
        val input = EditText(this).apply {
            hint = getString(R.string.msg_import_hint)
            minLines = 4
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_import_cookies)
            .setMessage(getString(R.string.msg_import_domain, Uri.parse(url).host ?: ""))
            .setView(input)
            .setPositiveButton(R.string.action_import) { _, _ ->
                val ok = importCookies(url, input.text.toString())
                Toast.makeText(
                    this,
                    if (ok) R.string.msg_import_success else R.string.msg_import_fail,
                    Toast.LENGTH_SHORT
                ).show()
                if (ok) webView.reload()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun importCookies(url: String, raw: String): Boolean {
        val cm = CookieManager.getInstance()
        val text = raw.trim()
        if (text.isEmpty()) return false
        var count = 0
        try {
            if (text.startsWith("[")) {
                val arr = org.json.JSONArray(text)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val name = o.optString("name")
                    val value = o.optString("value")
                    if (name.isEmpty()) continue
                    val cookie = buildString {
                        append(name).append('=').append(value)
                        o.optString("domain").takeIf { it.isNotEmpty() }?.let { append("; Domain=$it") }
                        o.optString("path").takeIf { it.isNotEmpty() }?.let { append("; Path=$it") }
                        if (o.optBoolean("secure", false)) append("; Secure")
                        if (o.optBoolean("httpOnly", false)) append("; HttpOnly")
                    }
                    cm.setCookie(url, cookie); count++
                }
            } else {
                text.split(";").forEach { piece ->
                    val p = piece.trim()
                    if (p.contains("=")) { cm.setCookie(url, p); count++ }
                }
            }
            cm.flush()
        } catch (_: Exception) { return false }
        return count > 0
    }

    private fun clearData() {
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_clear_data)
            .setMessage(R.string.msg_clear_confirm)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                val cm = CookieManager.getInstance()
                cm.removeAllCookies(null)
                cm.flush()
                webView.clearCache(true)
                webView.clearFormData()
                webView.clearHistory()
                Store.history(this).clear()
                Toast.makeText(this, R.string.msg_data_cleared, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun copyToClipboard(label: String, text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            Toast.makeText(this, R.string.msg_copied, Toast.LENGTH_SHORT).show()
        }
    }
}
