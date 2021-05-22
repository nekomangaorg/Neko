package eu.kanade.tachiyomi.ui.recents.options

import android.view.View
import android.view.View.inflate
import androidx.annotation.IntRange
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.recents.RecentsController
import eu.kanade.tachiyomi.widget.TabbedBottomSheetDialog

class TabbedRecentsOptionsSheet(val controller: RecentsController, @IntRange(from = 0, to = 2) startingTab: Int) :
    TabbedBottomSheetDialog(controller.activity!!) {

    private val generalView = inflate(controller.activity!!, R.layout.recents_general_view, null) as RecentsGeneralView
    private val historyView = inflate(controller.activity!!, R.layout.recents_history_view, null) as RecentsHistoryView
    private val updatesView = inflate(controller.activity!!, R.layout.recents_updates_view, null) as RecentsUpdatesView

    init {
        binding.menu.isVisible = false
        generalView.controller = controller
        historyView.controller = controller
        updatesView.controller = controller

        binding.tabs.getTabAt(startingTab)?.select()
    }

    override fun dismiss() {
        super.dismiss()
        controller.displaySheet = null
    }

    override fun getTabViews(): List<View> = listOf(
        generalView,
        historyView,
        updatesView
    )

    override fun getTabTitles(): List<Int> = listOf(
        R.string.general,
        R.string.history,
        R.string.updates
    )
}
