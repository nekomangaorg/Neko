package eu.kanade.tachiyomi.ui.library.display

import android.view.View
import android.view.View.inflate
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.ui.setting.SettingsLibraryController
import eu.kanade.tachiyomi.util.view.visible
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import eu.kanade.tachiyomi.widget.TabbedBottomSheetDialog
import kotlinx.android.synthetic.main.tabbed_bottom_sheet.*

open class TabbedLibraryDisplaySheet(controller: LibraryController):
    TabbedBottomSheetDialog(controller) {

    private val displayView: LibraryDisplayView = inflate(controller.activity!!, R.layout.library_display_layout, null) as LibraryDisplayView
    private val badgesView: LibraryBadgesView = inflate(controller.activity!!, R.layout.library_badges_layout, null) as LibraryBadgesView
    private val categoryView: LibraryCategoryView = inflate(controller.activity!!, R.layout.library_category_layout, null) as LibraryCategoryView

    init {
        displayView.controller = controller
        badgesView.controller = controller
        categoryView.controller = controller
        menu.visible()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            menu.tooltipText = context.getString(R.string.more_library_settings)
        }
        menu.setImageDrawable(ContextCompat.getDrawable(
            context,
            R.drawable.ic_settings_24dp
        ))
        menu.setOnClickListener {
            controller.router.pushController(SettingsLibraryController().withFadeTransaction())
            dismiss()
        }
    }

    override fun getTabViews(): List<View> = listOf(
        displayView,
        badgesView,
        categoryView
    )

    override fun getTabTitles(): List<Int> = listOf(
        R.string.display,
        R.string.badges,
        R.string.categories
    )
}