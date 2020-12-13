package eu.kanade.tachiyomi.ui.webview

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.core.graphics.ColorUtils
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.util.system.ThemeUtil
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.isInNightMode
import eu.kanade.tachiyomi.util.system.setDefaultSettings
import eu.kanade.tachiyomi.util.view.invisible
import eu.kanade.tachiyomi.util.view.marginBottom
import eu.kanade.tachiyomi.util.view.setStyle
import eu.kanade.tachiyomi.util.view.updateLayoutParams
import eu.kanade.tachiyomi.util.view.updatePadding
import eu.kanade.tachiyomi.util.view.visible
import kotlinx.android.synthetic.main.webview_activity.*

open class BaseWebViewActivity : BaseActivity() {

    private var bundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        delegate.localNightMode = ThemeUtil.nightMode(preferences.theme())
        setContentView(R.layout.webview_activity)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            super.onBackPressed()
        }
        toolbar.navigationIcon?.setTint(getResourceColor(R.attr.actionBarTintColor))

        val container: ViewGroup = findViewById(R.id.web_view_layout)
        val content: LinearLayout = findViewById(R.id.web_linear_layout)
        container.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        content.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

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
                0,
                insets.systemWindowInsetTop,
                0,
                insets.systemWindowInsetBottom
            )
        }
        swipe_refresh.setStyle()
        swipe_refresh.setOnRefreshListener {
            refreshPage()
        }

        window.statusBarColor = ColorUtils.setAlphaComponent(
            getResourceColor(
                R.attr
                    .colorSecondary
            ),
            255
        )

        content.setOnApplyWindowInsetsListener { v, insets ->
            // if pure white theme on a device that does not support dark status bar
            /*if (getResourceColor(android.R.attr.statusBarColor) != Color.TRANSPARENT)
                window.statusBarColor = Color.BLACK
            else window.statusBarColor = getResourceColor(R.attr.colorPrimary)*/
            window.navigationBarColor = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                val colorPrimary = getResourceColor(R.attr.colorPrimaryVariant)
                if (colorPrimary == Color.WHITE) Color.BLACK
                else getResourceColor(android.R.attr.colorPrimary)
            }
            // if the android q+ device has gesture nav, transparent nav bar
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                (v.rootWindowInsets.systemWindowInsetBottom != v.rootWindowInsets.tappableElementInsets.bottom)
            ) {
                getColor(android.R.color.transparent)
            } else {
                getResourceColor(android.R.attr.colorBackground)
            }
            v.setPadding(
                insets.systemWindowInsetLeft,
                insets.systemWindowInsetTop,
                insets.systemWindowInsetRight,
                0
            )
            if (Build.VERSION.SDK_INT >= 26 && !isInNightMode()) {
                content.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            insets
        }

        swipe_refresh.isEnabled = false

        if (bundle == null) {
            webview.setDefaultSettings()

            webview.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progressBar.visible()
                    progressBar.progress = newProgress
                    if (newProgress == 100)
                        progressBar.invisible()
                    super.onProgressChanged(view, newProgress)
                }
            }
            val marginB = webview.marginBottom
            webview.setOnApplyWindowInsetsListener { v, insets ->
                val bottomInset =
                    if (Build.VERSION.SDK_INT >= 29) insets.tappableElementInsets.bottom
                    else insets.systemWindowInsetBottom
                v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = marginB + bottomInset
                }
                insets
            }
        } else {
            webview.restoreState(bundle)
        }
    }

    private fun refreshPage() {
        swipe_refresh.isRefreshing = true
        webview.reload()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val lightMode = !isInNightMode()
        window.statusBarColor = ColorUtils.setAlphaComponent(
            getResourceColor(
                R.attr
                    .colorSecondary
            ),
            255
        )
        toolbar.setBackgroundColor(getResourceColor(R.attr.colorSecondary))
        toolbar.popupTheme = if (lightMode) R.style.ThemeOverlay_MaterialComponents else R
            .style.ThemeOverlay_MaterialComponents_Dark
        val tintColor = getResourceColor(R.attr.actionBarTintColor)
        toolbar.navigationIcon?.setTint(tintColor)
        toolbar.overflowIcon?.mutate()
        toolbar.setTitleTextColor(tintColor)
        toolbar.overflowIcon?.setTint(tintColor)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            window.navigationBarColor = getResourceColor(R.attr.colorPrimaryVariant)
        else if (window.navigationBarColor != getColor(android.R.color.transparent))
            window.navigationBarColor = getResourceColor(android.R.attr.colorBackground)

        web_linear_layout.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && lightMode) {
            web_linear_layout.systemUiVisibility = web_linear_layout.systemUiVisibility.or(
                View
                    .SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            )
        }
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.windowLightStatusBar, typedValue, true)

        if (typedValue.data == -1)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility
                .or(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        else
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility
                .rem(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
    }
    override fun onBackPressed() {
        if (webview.canGoBack()) webview.goBack()
        else super.onBackPressed()
    }
}
