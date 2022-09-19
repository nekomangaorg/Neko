package eu.kanade.tachiyomi.ui.recents

import androidx.recyclerview.widget.ItemTouchHelper
import com.fredporciuncula.flow.preferences.Preference
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.asImmediateFlowIn
import eu.kanade.tachiyomi.ui.manga.chapter.BaseChapterAdapter
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uy.kohesive.injekt.injectLazy

class RecentMangaAdapter(val delegate: RecentsInterface) :
    BaseChapterAdapter<IFlexible<*>>(delegate) {

    val preferences: PreferencesHelper by injectLazy()

    var showDownloads = preferences.showRecentsDownloads().get()
    var showRemoveHistory = preferences.showRecentsRemHistory().get()
    var showTitleFirst = preferences.showTitleFirstInRecents().get()
    var showUpdatedTime = preferences.showUpdatedTime().get()
    var uniformCovers = preferences.uniformGrid().get()
    var showOutline = preferences.outlineOnCovers().get()
    var sortByFetched = preferences.sortFetchedTime().get()

    val viewType: Int
        get() = delegate.getViewType()

    fun updateItems(items: List<IFlexible<*>>?) {
        updateDataSet(items)
    }

    val decimalFormat = DecimalFormat(
        "#.###",
        DecimalFormatSymbols()
            .apply { decimalSeparator = '.' },
    )

    init {
        setDisplayHeadersAtStartUp(true)
    }

    fun setPreferenceFlows() {
        preferences.showRecentsDownloads().register { showDownloads = it }
        preferences.showRecentsRemHistory().register { showRemoveHistory = it }
        preferences.showTitleFirstInRecents().register { showTitleFirst = it }
        preferences.showUpdatedTime().register { showUpdatedTime = it }
        preferences.uniformGrid().register { uniformCovers = it }
        preferences.sortFetchedTime().asImmediateFlowIn(delegate.scope()) { sortByFetched = it }
        preferences.outlineOnCovers().register(false) {
            showOutline = it
            (0 until itemCount).forEach { i ->
                (recyclerView.findViewHolderForAdapterPosition(i) as? RecentMangaHolder)?.updateCards()
            }
        }
    }

    private fun <T> Preference<T>.register(notify: Boolean = true, onChanged: (T) -> Unit) {
        asFlow()
            .drop(1)
            .onEach {
                onChanged(it)
                if (notify) {
                    notifyDataSetChanged()
                }
            }
            .launchIn(delegate.scope())
    }

    interface RecentsInterface : RecentMangaInterface, DownloadInterface

    interface RecentMangaInterface {
        fun onCoverClick(position: Int)
        fun onRemoveHistoryClicked(position: Int)
        fun markAsRead(position: Int)
        fun isSearching(): Boolean
        fun scope(): CoroutineScope
        fun getViewType(): Int
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
        OnlyDownloaded,
        UnreadOrDownloaded,
        All,
    }
}
