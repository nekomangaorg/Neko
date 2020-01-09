package eu.kanade.tachiyomi.ui.manga.info

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.ColorUtils
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.util.WebViewClientCompat
import eu.kanade.tachiyomi.util.doOnApplyWindowInsets
import eu.kanade.tachiyomi.util.getResourceColor
import eu.kanade.tachiyomi.util.marginBottom
import eu.kanade.tachiyomi.util.updateLayoutParams
import eu.kanade.tachiyomi.util.updatePadding
import kotlinx.android.synthetic.main.webview_activity.*
import uy.kohesive.injekt.injectLazy

class WebViewActivity : BaseActivity() {

    private val sourceManager by injectLazy<SourceManager>()
    private var bundle:Bundle? = null
    val preferences: PreferencesHelper by injectLazy()

    companion object {
        const val SOURCE_KEY = "source_key"
        const val URL_KEY = "url_key"
        const val TITLE_KEY = "title_key"

        fun newIntent(context: Context, sourceId: Long, url: String, title:String?): Intent {
            val intent = Intent(context, WebViewActivity::class.java)
            intent.putExtra(SOURCE_KEY, sourceId)
            intent.putExtra(URL_KEY, url)
            intent.putExtra(TITLE_KEY, title)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return intent
        }
    }

    override fun getTheme(): Resources.Theme {
        val theme = super.getTheme()
        theme.applyStyle(when (preferences.theme()) {
            3, 6 -> R.style.Theme_Tachiyomi_Amoled
            4, 7 -> R.style.Theme_Tachiyomi_DarkBlue
            else -> R.style.Theme_Tachiyomi
        }, true)
        return theme
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.webview_activity)
        title = intent.extras?.getString(TITLE_KEY)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            super.onBackPressed()
        }

        val container:ViewGroup = findViewById(R.id.web_view_layout)
        val content: LinearLayout = findViewById(R.id.web_linear_layout)
        container.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        content.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        container.setOnApplyWindowInsetsListener { v, insets ->
            val contextView = window?.decorView?.findViewById<View>(R.id.action_mode_bar)
            contextView?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.systemWindowInsetLeft
                rightMargin = insets.systemWindowInsetRight
            }
            // Consume any horizontal insets and pad all content in. There's not much we can do
            // with horizontal insets
            v.updatePadding(
                left = insets.systemWindowInsetLeft,
                right = insets.systemWindowInsetRight
            )
            insets.replaceSystemWindowInsets(
                0, insets.systemWindowInsetTop,
                0, insets.systemWindowInsetBottom
            )
        }

        content.setOnApplyWindowInsetsListener { v, insets ->
            window.statusBarColor = getResourceColor(R.attr.colorPrimary)
            window.navigationBarColor =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    v.context.getResourceColor(android.R.attr.colorPrimary)
                }
                // if the android q+ device has gesture nav, transparent nav bar
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    && (v.rootWindowInsets.systemWindowInsetBottom != v.rootWindowInsets
                        .tappableElementInsets.bottom)) {
                    getColor(android.R.color.transparent)
                } else {
                    v.context.getResourceColor(android.R.attr.colorBackground)
                }
            v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, 0)
            val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (Build.VERSION.SDK_INT >= 26 && currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
                content.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            insets
        }

        if (bundle == null) {
            val source = sourceManager.get(intent.extras!!.getLong(SOURCE_KEY)) as? HttpSource ?: return
            val url = intent.extras!!.getString(URL_KEY) ?: return
            val headers = source.headers.toMultimap().mapValues { it.value.getOrNull(0) ?: "" }
            webview.webViewClient = object : WebViewClientCompat() {
                override fun shouldOverrideUrlCompat(view: WebView, url: String): Boolean {
                    view.loadUrl(url)
                    return true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    invalidateOptionsMenu()
                    title = view?.title
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    invalidateOptionsMenu()
                }
            }
            val marginB = webview.marginBottom
            webview.doOnApplyWindowInsets { v, insets, _ ->
                val bottomInset =
                    if (Build.VERSION.SDK_INT >= 29) insets.tappableElementInsets.bottom
                    else insets.systemWindowInsetBottom
                v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = marginB + bottomInset
                }
            }
            webview.settings.javaScriptEnabled = true
            webview.settings.userAgentString = source.headers["User-Agent"]
            webview.loadUrl(url, headers)
        }
        else {
            webview.restoreState(bundle)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        window.statusBarColor = getResourceColor(R.attr.colorPrimary)
        toolbar.setBackgroundColor(getResourceColor(R.attr.colorPrimary))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            window.navigationBarColor = getResourceColor(android.R.attr.colorPrimary)
        else if (window.navigationBarColor != getColor(android.R.color.transparent))
            window.navigationBarColor = getResourceColor(android.R.attr.colorBackground)

        val currentNightMode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (Build.VERSION.SDK_INT >= 26) {
            if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
                web_linear_layout.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                web_linear_layout.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            }
        }
    }

    /**
     * Called when the options menu of the toolbar is being created. It adds our custom menu.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.webview, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val backItem = toolbar.menu.findItem(R.id.action_web_back)
        val forwardItem = toolbar.menu.findItem(R.id.action_web_forward)
        backItem?.isEnabled = webview.canGoBack()
        forwardItem?.isEnabled = webview.canGoForward()
        val hasHistory = webview.canGoBack() || webview.canGoForward()
        backItem?.isVisible = hasHistory
        forwardItem?.isVisible = hasHistory
        val translucentWhite = ColorUtils.setAlphaComponent(Color.WHITE, 127)
        backItem.icon?.setTint(if (webview.canGoBack()) Color.WHITE else translucentWhite)
        forwardItem?.icon?.setTint(if (webview.canGoForward()) Color.WHITE else translucentWhite)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onBackPressed() {
        if (webview.canGoBack()) webview.goBack()
        else super.onBackPressed()
    }

    /**
     * Called when an item of the options menu was clicked. Used to handle clicks on our menu
     * entries.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_web_back -> webview.goBack()
            R.id.action_web_forward ->  webview.goForward()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
