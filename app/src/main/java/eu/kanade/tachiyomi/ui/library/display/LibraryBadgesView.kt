package eu.kanade.tachiyomi.ui.library.display

import android.content.Context
import android.util.AttributeSet
import eu.kanade.tachiyomi.databinding.LibraryBadgesLayoutBinding
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.widget.BaseLibraryDisplayView

class LibraryBadgesView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseLibraryDisplayView<LibraryBadgesLayoutBinding>(context, attrs) {

    override fun inflateBinding() = LibraryBadgesLayoutBinding.bind(this)
    override fun initGeneralPreferences() {
        binding.unreadBadgeGroup.bindToPreference(libraryPreferences.unreadBadgeType()) {
            controller?.presenter?.requestUnreadBadgesUpdate()
        }
        binding.hideReading.bindToPreference(libraryPreferences.hideStartReadingButton())
        binding.downloadBadge.bindToPreference(libraryPreferences.showDownloadBadge()) {
            controller?.presenter?.requestDownloadBadgesUpdate()
        }
        binding.showNumberOfItems.bindToPreference(libraryPreferences.showCategoriesHeaderCount())
    }
}
