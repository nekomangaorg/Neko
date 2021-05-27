package eu.kanade.tachiyomi.ui.reader.settings

import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R

enum class ReaderBottomButton(val value: String, @StringRes val stringRes: Int) {
    ViewChapters("vc", R.string.view_chapters),
    Comment("com", R.string.view_comments),
    ReadingMode("rm", R.string.reading_mode),
    Rotation("rot", R.string.rotation),
    CropBordersPaged("cbp", R.string.crop_borders_paged),
    CropBordersWebtoon("cbw", R.string.crop_borders_webtoon),
    PageLayout("pl", R.string.page_layout),
    ShiftDoublePage("sdp", R.string.shift_double_pages)
    ;

    fun isIn(buttons: Collection<String>) = value in buttons

    companion object {
        val BUTTONS_DEFAULTS = setOf(
            ViewChapters,
            Comment,
            PageLayout,
            CropBordersWebtoon
        ).map { it.value }.toSet()
    }
}
