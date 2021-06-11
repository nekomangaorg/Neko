package eu.kanade.tachiyomi.ui.library.display

import android.view.View
import android.view.View.inflate
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.ui.setting.SettingsLibraryController
import eu.kanade.tachiyomi.util.view.compatToolTipText
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import eu.kanade.tachiyomi.widget.TabbedBottomSheetDialog

open class TabbedLibraryDisplaySheet(val controller: Controller) :
    TabbedBottomSheetDialog(controller.activity!!) {

    private val displayView: LibraryDisplayView = inflate(controller.activity!!, R.layout.library_display_layout, null) as LibraryDisplayView
    private val badgesView: LibraryBadgesView = inflate(controller.activity!!, R.layout.library_badges_layout, null) as LibraryBadgesView
    private val categoryView: LibraryCategoryView = inflate(controller.activity!!, R.layout.library_category_layout, null) as LibraryCategoryView

    init {
        (controller as? LibraryController)?.let { libraryController ->
            displayView.controller = libraryController
            badgesView.controller = libraryController
            categoryView.controller = libraryController
        }
        displayView.mainView = controller.view
        binding.menu.isVisible = controller !is SettingsLibraryController
        binding.menu.compatToolTipText = context.getString(R.string.more_library_settings)
        binding.menu.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_outline_settings_24dp
            )
        )
        binding.menu.setOnClickListener {
            controller.router.pushController(SettingsLibraryController().withFadeTransaction())
            dismiss()
        }
        categoryView.binding.addCategoriesButton.isVisible = controller is LibraryController
    }

    override fun dismiss() {
        super.dismiss()
        (controller as? LibraryController)?.displaySheet = null
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
