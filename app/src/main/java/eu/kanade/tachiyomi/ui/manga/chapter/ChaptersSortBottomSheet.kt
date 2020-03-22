package eu.kanade.tachiyomi.ui.manga.chapter

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.setBottomEdge
import eu.kanade.tachiyomi.util.view.setEdgeToEdge
import eu.kanade.tachiyomi.util.view.visibleIf
import kotlinx.android.synthetic.main.chapter_sort_bottom_sheet.*

class ChaptersSortBottomSheet(controller: MangaDetailsController) : BottomSheetDialog
    (controller.activity!!, R.style.BottomSheetDialogTheme) {

    val activity = controller.activity!!

    private var sheetBehavior: BottomSheetBehavior<*>

    private val presenter = controller.presenter

    init {
        // Use activity theme for this layout
        val view = activity.layoutInflater.inflate(R.layout.chapter_sort_bottom_sheet, null)
        setContentView(view)

        sheetBehavior = BottomSheetBehavior.from(view.parent as ViewGroup)
        setEdgeToEdge(activity, bottom_sheet, view, false)
        val height = activity.window.decorView.rootWindowInsets.systemWindowInsetBottom
        sheetBehavior.peekHeight = 380.dpToPx + height

        sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, progress: Float) { }

            override fun onStateChanged(p0: View, state: Int) {
                if (state == BottomSheetBehavior.STATE_EXPANDED) {
                    sheetBehavior.skipCollapsed = true
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        sheetBehavior.skipCollapsed = true
    }

    /**
     * Called when the sheet is created. It initializes the listeners and values of the preferences.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initGeneralPreferences()
        setBottomEdge(hide_titles, activity)
        close_button.setOnClickListener { dismiss() }
        settings_scroll_view.viewTreeObserver.addOnGlobalLayoutListener {
            val isScrollable =
                settings_scroll_view!!.height < bottom_sheet.height +
                    settings_scroll_view.paddingTop + settings_scroll_view.paddingBottom
            close_button.visibleIf(isScrollable)
        }

        setOnDismissListener {
            presenter.setFilters(
                show_read.isChecked,
                show_unread.isChecked,
                show_download.isChecked,
                show_bookmark.isChecked
            )
        }
    }

    private fun initGeneralPreferences() {
        show_read.isChecked = presenter.onlyRead()
        show_unread.isChecked = presenter.onlyUnread()
        show_download.isChecked = presenter.onlyDownloaded()
        show_bookmark.isChecked = presenter.onlyBookmarked()

        show_all.isChecked = !(show_read.isChecked || show_unread.isChecked ||
            show_download.isChecked || show_bookmark.isChecked)

        sort_group.check(if (presenter.manga.sortDescending()) R.id.sort_newest else
            R.id.sort_oldest)

        hide_titles.isChecked = presenter.manga.displayMode != Manga.DISPLAY_NAME
        sort_method_group.check(if (presenter.manga.sorting == Manga.SORTING_SOURCE) R.id.sort_by_source else
            R.id.sort_by_number)

        sort_group.setOnCheckedChangeListener { _, checkedId ->
            presenter.setSortOrder(checkedId == R.id.sort_oldest)
            dismiss()
        }

        sort_method_group.setOnCheckedChangeListener { _, checkedId ->
            presenter.setSortMethod(checkedId == R.id.sort_by_source)
        }

        hide_titles.setOnCheckedChangeListener { _, isChecked ->
            presenter.hideTitle(isChecked)
        }

        show_all.setOnCheckedChangeListener(::checkedFilter)
        show_read.setOnCheckedChangeListener(::checkedFilter)
        show_unread.setOnCheckedChangeListener(::checkedFilter)
        show_download.setOnCheckedChangeListener(::checkedFilter)
        show_bookmark.setOnCheckedChangeListener(::checkedFilter)
    }

    private fun checkedFilter(checkBox: CompoundButton, isChecked: Boolean) {
        if (isChecked) {
            if (show_all == checkBox) {
                show_read.isChecked = false
                show_unread.isChecked = false
                show_download.isChecked = false
                show_bookmark.isChecked = false
            } else {
                show_all.isChecked = false
                if (show_read == checkBox) show_unread.isChecked = false
                else if (show_unread == checkBox) show_read.isChecked = false
            }
        } else if (!show_read.isChecked && !show_unread.isChecked &&
            !show_download.isChecked && !show_bookmark.isChecked) {
            show_all.isChecked = true
        }
    }
}
