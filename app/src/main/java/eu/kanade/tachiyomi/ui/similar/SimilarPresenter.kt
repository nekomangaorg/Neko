package eu.kanade.tachiyomi.ui.manga.similar

import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.similar.SimilarController
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourcePresenter
import eu.kanade.tachiyomi.ui.source.browse.Pager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Presenter of [SimilarController]. Inherit BrowseSourcePresenter.
 */
class SimilarPresenter(
    val mangaId: Long,
    private val controller: SimilarController,
    val preferences: PreferencesHelper = Injekt.get(),
) : BrowseSourcePresenter() {

    var manga: Manga? = null
    var isRefreshing: Boolean = false
    var scope = CoroutineScope(Job() + Dispatchers.Default)

    override fun createPager(query: String, filters: FilterList): Pager {
        this.manga = db.getManga(mangaId).executeAsBlocking()
        return SimilarPager(this.manga!!, source)
    }

    fun refreshSimilarManga() {
        scope.launch {
            withContext(Dispatchers.IO) {
                isRefreshing = true
                try {
                    val manga = db.getManga(mangaId).executeAsBlocking()
                    source.fetchSimilarManga(manga!!, true)
                    isRefreshing = false
                    withContext(Dispatchers.Main) {
                        controller.showUserMessage("Updated Similar Manga")
                    }
                } catch (e: java.lang.Exception) {
                    isRefreshing = false
                    withContext(Dispatchers.Main) {
                        controller.showUserMessage(trimException(e))
                    }
                }
            }
            restartPager()
        }
    }

    private fun trimException(e: java.lang.Exception): String {
        return (
            if (e.message?.contains(": ") == true) e.message?.split(": ")?.drop(1)
                ?.joinToString(": ")
            else e.message
            ) ?: preferences.context.getString(R.string.unknown_error)
    }
}
