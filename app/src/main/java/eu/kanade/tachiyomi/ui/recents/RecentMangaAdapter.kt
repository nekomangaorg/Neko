package eu.kanade.tachiyomi.ui.recents

import androidx.recyclerview.widget.ItemTouchHelper
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.manga.chapter.BaseChapterAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uy.kohesive.injekt.injectLazy
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class RecentMangaAdapter(val delegate: RecentsInterface) :
    BaseChapterAdapter<IFlexible<*>>(delegate) {

    private val preferences: PreferencesHelper by injectLazy()

    var showDownloads = ShowRecentsDLs.All
    var showRemoveHistory = true
    var showTitleFirst = false

    fun updateItems(items: List<IFlexible<*>>?) {
        updateDataSet(items)
    }

    val decimalFormat = DecimalFormat(
        "#.###",
        DecimalFormatSymbols()
            .apply { decimalSeparator = '.' }
    )

    init {
        setDisplayHeadersAtStartUp(true)
        preferences.showRecentsDownloads()
            .asFlow()
            .onEach {
                showDownloads = it
                notifyDataSetChanged()
            }
            .launchIn(delegate.scope())
        preferences.showRecentsRemHistory()
            .asFlow()
            .onEach {
                showRemoveHistory = it
                notifyDataSetChanged()
            }
            .launchIn(delegate.scope())
        preferences.showTitleFirstInRecents()
            .asFlow()
            .onEach {
                showTitleFirst = it
                notifyDataSetChanged()
            }
            .launchIn(delegate.scope())
    }

    interface RecentsInterface : RecentMangaInterface, DownloadInterface

    interface RecentMangaInterface {
        fun onCoverClick(position: Int)
        fun onRemoveHistoryClicked(position: Int)
        fun markAsRead(position: Int)
        fun isSearching(): Boolean
        fun setViewType(viewType: Int)
        fun getViewType(): Int
        fun scope(): CoroutineScope
    }

    override fun onItemSwiped(position: Int, direction: Int) {
        super.onItemSwiped(position, direction)
        when (direction) {
            ItemTouchHelper.LEFT -> delegate.markAsRead(position)
        }
    }

    enum class ShowRecentsDLs {
        None,
        OnlyUnread,
        All,
    }
}
