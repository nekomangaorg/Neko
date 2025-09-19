package eu.kanade.tachiyomi.ui.library.display

import android.content.Context
import android.util.AttributeSet
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.widget.BaseLibraryDisplayView
import org.nekomanga.databinding.LibraryBadgesLayoutBinding

class LibraryBadgesView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseLibraryDisplayView<LibraryBadgesLayoutBinding>(context, attrs) {

    override fun inflateBinding() = LibraryBadgesLayoutBinding.bind(this)

    override fun initGeneralPreferences() {
        binding.unreadBadgeGroup.bindToPreference(libraryPreferences.unreadBadgeType()) {
            controller?.presenter?.requestUnreadBadgesUpdate()
        }
        binding.hideReading.bindToPreference(libraryPreferences.showStartReadingButton())
        binding.downloadBadge.bindToPreference(libraryPreferences.showDownloadBadge()) {
            controller?.presenter?.requestDownloadBadgesUpdate()
        }
        binding.showNumberOfItems.bindToPreference(libraryPreferences.showCategoriesHeaderCount())
    }
}
