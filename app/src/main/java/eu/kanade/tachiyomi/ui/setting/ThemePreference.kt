package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.DynamicColors
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.ISelectionListener
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter.select.SelectExtension
import com.mikepenz.fastadapter.select.getSelectExtension
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.ThemeItemBinding
import eu.kanade.tachiyomi.databinding.ThemesPreferenceBinding
import eu.kanade.tachiyomi.util.system.ThemeUtil
import eu.kanade.tachiyomi.util.system.Themes
import eu.kanade.tachiyomi.util.system.appDelegateNightMode
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.isInNightMode
import kotlin.math.max
import uy.kohesive.injekt.injectLazy

class ThemePreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    Preference(context, attrs) {

    var fastAdapterLight: FastAdapter<ThemeItem>
    var fastAdapterDark: FastAdapter<ThemeItem>
    private val itemAdapterLight = ItemAdapter<ThemeItem>()
    private val itemAdapterDark = ItemAdapter<ThemeItem>()
    private var selectExtensionLight: SelectExtension<ThemeItem>
    private var selectExtensionDark: SelectExtension<ThemeItem>
    private val preferences: PreferencesHelper by injectLazy()
    var activity: Activity? = null
    var lastScrollPostionLight: Int? = null
    var lastScrollPostionDark: Int? = null
    lateinit var binding: ThemesPreferenceBinding
    private val managerLight = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    private val managerDark = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

    init {
        layoutResource = R.layout.themes_preference
        fastAdapterLight = FastAdapter.with(itemAdapterLight)
        fastAdapterDark = FastAdapter.with(itemAdapterDark)
        fastAdapterLight.setHasStableIds(true)
        fastAdapterDark.setHasStableIds(true)
        selectExtensionLight = fastAdapterLight.getSelectExtension().setThemeListener(false)
        selectExtensionDark = fastAdapterDark.getSelectExtension().setThemeListener(true)
        val enumConstants = Themes.values()
        val supportsDynamic = DynamicColors.isDynamicColorAvailable()
        itemAdapterLight.set(
            enumConstants
                .filter { (!it.isDarkTheme || it.followsSystem) && (it.styleRes != R.style.Theme_Tachiyomi_Monet || supportsDynamic) }
                .map { ThemeItem(it, false) },
        )
        itemAdapterDark.set(
            enumConstants
                .filter { (it.isDarkTheme || it.followsSystem) && (it.styleRes != R.style.Theme_Tachiyomi_Monet || supportsDynamic) }
                .map { ThemeItem(it, true) },
        )
        isSelectable = false
    }

    private fun SelectExtension<ThemeItem>.setThemeListener(isDarkMode: Boolean): SelectExtension<ThemeItem> {
        isSelectable = true
        multiSelect = true
        val nightMode = if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        selectionListener = object : ISelectionListener<ThemeItem> {
            override fun onSelectionChanged(item: ThemeItem, selected: Boolean) {
                if (isDarkMode) {
                    preferences.darkTheme().set(item.theme)
                } else {
                    preferences.lightTheme().set(item.theme)
                }
                if (!selected) {
                    preferences.nightMode().set(nightMode)
                } else if (preferences.nightMode()
                    .get() != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ) {
                    preferences.nightMode().set(nightMode)
                }
                if ((
                    preferences.nightMode().get() == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM &&
                        nightMode != context.appDelegateNightMode()
                    ) ||
                    (!selected && nightMode == context.appDelegateNightMode())
                ) {
                    fastAdapterLight.notifyDataSetChanged()
                    fastAdapterDark.notifyDataSetChanged()
                } else {
                    activity?.recreate()
                }
            }
        }
        return this
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        binding = ThemesPreferenceBinding.bind(holder.itemView)

        binding.themeRecycler.setHasFixedSize(true)
        binding.themeRecycler.layoutManager = managerLight

        binding.themeRecycler.adapter = fastAdapterLight

        binding.themeRecycler.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    lastScrollPostionLight =
                        recyclerView.computeHorizontalScrollOffset()
                }
            },
        )

        binding.themeRecyclerDark.setHasFixedSize(true)
        binding.themeRecyclerDark.layoutManager = managerDark

        binding.themeRecyclerDark.adapter = fastAdapterDark

        binding.themeRecyclerDark.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    lastScrollPostionDark =
                        recyclerView.computeHorizontalScrollOffset()
                }
            },
        )

        if (lastScrollPostionLight != null) {
            val lX = lastScrollPostionLight!!
            (binding.themeRecycler.layoutManager as LinearLayoutManager).apply {
                scrollToPositionWithOffset(
                    lX / 110.dpToPx,
                    -lX % 110.dpToPx,
                )
            }
            lastScrollPostionLight = binding.themeRecycler.computeHorizontalScrollOffset()
        } else {
            binding.themeRecycler.scrollToPosition(
                max((selectExtensionLight.selections.firstOrNull() ?: 0) - 1, 0),
            )
        }

        if (lastScrollPostionDark != null) {
            val lX = lastScrollPostionDark!!
            (binding.themeRecyclerDark.layoutManager as LinearLayoutManager).apply {
                scrollToPositionWithOffset(
                    lX / 110.dpToPx,
                    -lX % 110.dpToPx,
                )
            }
            lastScrollPostionDark = binding.themeRecyclerDark.computeHorizontalScrollOffset()
        } else {
            binding.themeRecyclerDark.scrollToPosition(
                max((selectExtensionDark.selections.firstOrNull() ?: 0) - 1, 0),
            )
        }
    }

    inner class ThemeItem(val theme: Themes, val isDarkTheme: Boolean) : AbstractItem<FastAdapter.ViewHolder<ThemeItem>>() {

        /** defines the type defining this item. must be unique. preferably an id */
        override val type: Int = R.id.theme_card_view

        /** defines the layout which will be used for this item in the list */
        override val layoutRes: Int = R.layout.theme_item

        override var identifier = theme.hashCode().toLong()

        override fun getViewHolder(v: View) = ViewHolder(v)

        @Suppress("UNUSED_PARAMETER")
        override var isSelected: Boolean
            get() {
                val darkTheme = try {
                    preferences.darkTheme().get()
                } catch (_: Exception) {
                    ThemeUtil.convertNewThemes(preferences.context)
                    preferences.darkTheme().get()
                }
                val lightTheme = try {
                    preferences.lightTheme().get()
                } catch (_: Exception) {
                    ThemeUtil.convertNewThemes(preferences.context)
                    preferences.lightTheme().get()
                }
                return when (preferences.nightMode().get()) {
                    AppCompatDelegate.MODE_NIGHT_YES -> darkTheme == theme && isDarkTheme
                    AppCompatDelegate.MODE_NIGHT_NO -> lightTheme == theme && !isDarkTheme
                    else -> (darkTheme == theme && isDarkTheme) ||
                        (lightTheme == theme && !isDarkTheme)
                }
            }
            set(value) {}

        inner class ViewHolder(view: View) : FastAdapter.ViewHolder<ThemeItem>(view) {

            val binding = ThemeItemBinding.bind(view)

            override fun bindView(item: ThemeItem, payloads: List<Any>) {
                binding.themeNameText.setText(
                    if (item.isDarkTheme) {
                        item.theme.darkNameRes
                    } else {
                        item.theme.nameRes
                    },
                )

                binding.checkbox.isVisible = item.isSelected
                binding.themeSelected.isInvisible = !item.isSelected

                if (binding.checkbox.isVisible) {
                    val themeMatchesApp = if (context.isInNightMode()) {
                        item.isDarkTheme
                    } else {
                        !item.isDarkTheme
                    }
                    binding.themeSelected.alpha = if (themeMatchesApp) 1f else 0.5f
                    binding.checkbox.alpha = if (themeMatchesApp) 1f else 0.5f
                }
                val configuration = Configuration(context.resources.configuration)
                configuration.uiMode = if (item.isDarkTheme) UI_MODE_NIGHT_YES else UI_MODE_NIGHT_NO
                val themeContext = context.createConfigurationContext(configuration)
                themeContext.setTheme(item.theme.styleRes)
                val primaryText = themeContext.getResourceColor(android.R.attr.textColorPrimary)
                val secondaryText = themeContext.getResourceColor(android.R.attr.textColorSecondary)
                val background = themeContext.getResourceColor(R.attr.background)
                val colorSecondary = themeContext.getResourceColor(R.attr.colorSecondary)
                val appBar = themeContext.getResourceColor(R.attr.colorSurface)
                val appBarText = themeContext.getResourceColor(R.attr.actionBarTintColor)
                val bottomBar = themeContext.getResourceColor(R.attr.colorPrimaryVariant)
                val inactiveTab = themeContext.getResourceColor(R.attr.tabBarIconInactive)
                val activeTab = themeContext.getResourceColor(R.attr.tabBarIconColor)
                binding.themeToolbar.setBackgroundColor(appBar)
                binding.themeAppBarText.imageTintList = ColorStateList.valueOf(appBarText)
                binding.themeHeroImage.imageTintList = ColorStateList.valueOf(primaryText)
                binding.themePrimaryText.imageTintList = ColorStateList.valueOf(primaryText)
                binding.themeAccentedButton.imageTintList = ColorStateList.valueOf(colorSecondary)
                binding.themeSecondaryText.imageTintList = ColorStateList.valueOf(secondaryText)
                binding.themeSecondaryText2.imageTintList = ColorStateList.valueOf(secondaryText)
                binding.themeBottomBar.setBackgroundColor(bottomBar)
                binding.themeItem1.imageTintList =
                    ColorStateList.valueOf(inactiveTab)
                binding.themeItem2.imageTintList = ColorStateList.valueOf(activeTab)
                binding.themeItem3.imageTintList =
                    ColorStateList.valueOf(inactiveTab)
                binding.themeLayout.setBackgroundColor(background)
            }

            override fun unbindView(item: ThemeItem) {
            }
        }
    }
}
