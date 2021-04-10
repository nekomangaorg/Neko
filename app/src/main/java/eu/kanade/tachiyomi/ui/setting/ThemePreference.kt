package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.visInvisIf
import uy.kohesive.injekt.injectLazy

class ThemePreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    Preference(context, attrs) {

    private lateinit var fastAdapter: FastAdapter<ThemeItem>
    private val itemAdapter = ItemAdapter<ThemeItem>()
    private lateinit var selectExtension: SelectExtension<ThemeItem>
    private val preferences: PreferencesHelper by injectLazy()
    var activity: Activity? = null
    var lastScrollPostion: Int? = null
    lateinit var binding: ThemesPreferenceBinding
    val manager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    init {
        layoutResource = R.layout.themes_preference
        fastAdapter = FastAdapter.with(itemAdapter)
        fastAdapter.setHasStableIds(true)
        val enumConstants = ThemeUtil.Themes::class.java.enumConstants
        val currentTheme = preferences.theme().get()
        selectExtension = fastAdapter.getSelectExtension().apply {
            isSelectable = true
            multiSelect = false
            selectionListener = object : ISelectionListener<ThemeItem> {
                override fun onSelectionChanged(item: ThemeItem, selected: Boolean) {
                    preferences.theme().set(item.theme)
                    activity?.recreate()
                }
            }
        }

        itemAdapter.set(enumConstants?.map(::ThemeItem).orEmpty())
        itemAdapter.adapterItems.forEach { item ->
            item.isSelected = currentTheme == item.theme
        }
        isSelectable = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        binding = ThemesPreferenceBinding.bind(holder.itemView)

        binding.themePrefTitle.text = title
        binding.themeRecycler.setHasFixedSize(true)
        binding.themeRecycler.layoutManager = manager

        binding.themeRecycler.adapter = fastAdapter

        binding.themeRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                lastScrollPostion =
                    recyclerView.computeHorizontalScrollOffset() // (lastScrollPostion ?: 0) + dx
            }
        })

        val enumConstants = ThemeUtil.Themes::class.java.enumConstants
        val currentTheme = preferences.theme().get()
        if (lastScrollPostion != null) {
            val lX = lastScrollPostion!!
            (binding.themeRecycler.layoutManager as LinearLayoutManager).apply {
                scrollToPositionWithOffset(
                    lX / 110.dpToPx,
                    -lX % 110.dpToPx + binding.themeRecycler.paddingStart
                )
            }
            lastScrollPostion = binding.themeRecycler.computeHorizontalScrollOffset()
        } else {
            binding.themeRecycler.scrollToPosition(
                enumConstants?.indexOf(currentTheme) ?: 0
            )
        }
    }

    inner class ThemeItem(val theme: ThemeUtil.Themes) : AbstractItem<FastAdapter.ViewHolder<ThemeItem>>() {

        /** defines the type defining this item. must be unique. preferably an id */
        override val type: Int = R.id.theme_card_view

        /** defines the layout which will be used for this item in the list */
        override val layoutRes: Int = R.layout.theme_item

        override var identifier = theme.hashCode().toLong()

        override fun getViewHolder(v: View): FastAdapter.ViewHolder<ThemeItem> {
            return ViewHolder(v)
        }

        val colors = theme.getColors()
        val darkColors = if (theme.nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            theme.getColors(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            null
        }

        inner class ViewHolder(view: View) : FastAdapter.ViewHolder<ThemeItem>(view) {

            val binding = ThemeItemBinding.bind(view)
            override fun bindView(item: ThemeItem, payloads: List<Any>) {
                binding.themeNameText.setText(item.theme.nameRes)

                binding.checkbox.isVisible = item.isSelected
                binding.themeSelected.visInvisIf(item.isSelected)
                binding.themeToolbar.setBackgroundColor(item.colors.appBar)
                binding.themeAppBarText.imageTintList = ColorStateList.valueOf(item.colors.appBarText)
                binding.themeHeroImage.imageTintList = ColorStateList.valueOf(item.colors.primaryText)
                binding.themePrimaryText.imageTintList = ColorStateList.valueOf(item.colors.primaryText)
                binding.themeAccentedButton.imageTintList = ColorStateList.valueOf(item.colors.colorAccent)
                binding.themeSecondaryText.imageTintList = ColorStateList.valueOf(item.colors.secondaryText)
                binding.themeSecondaryText2.imageTintList = ColorStateList.valueOf(item.colors.secondaryText)

                binding.themeBottomBar.setBackgroundColor(item.colors.bottomBar)
                binding.themeItem1.imageTintList = ColorStateList.valueOf(item.colors.inactiveTab)
                binding.themeItem2.imageTintList = ColorStateList.valueOf(item.colors.activeTab)
                binding.themeItem3.imageTintList = ColorStateList.valueOf(item.colors.inactiveTab)

                binding.themeLayout.setBackgroundColor(item.colors.colorBackground)
                binding.darkThemeLayout.isVisible = item.darkColors != null

                if (binding.darkThemeLayout.isVisible && item.darkColors != null) {
                    binding.darkThemeToolbar.setBackgroundColor(item.darkColors.appBar)
                    binding.darkThemeAppBarText.imageTintList = ColorStateList.valueOf(item.darkColors.appBarText)
                    binding.darkThemeLayout.setBackgroundColor(item.darkColors.colorBackground)
                    binding.darkThemePrimaryText.imageTintList = ColorStateList.valueOf(item.darkColors.primaryText)
                    binding.darkThemeHeroImage.imageTintList = ColorStateList.valueOf(item.darkColors.primaryText)
                    binding.darkThemeAccentedButton.imageTintList = ColorStateList.valueOf(item.darkColors.colorAccent)
                    binding.darkThemeSecondaryText.imageTintList = ColorStateList.valueOf(item.darkColors.secondaryText)

                    binding.darkThemeBottomBar.setBackgroundColor(item.darkColors.bottomBar)
                    binding.darkThemeItem2.imageTintList = ColorStateList.valueOf(item.darkColors.activeTab)
                    binding.darkThemeItem3.imageTintList = ColorStateList.valueOf(item.darkColors.inactiveTab)
                }
            }

            override fun unbindView(item: ThemeItem) {
            }
        }
    }
}
