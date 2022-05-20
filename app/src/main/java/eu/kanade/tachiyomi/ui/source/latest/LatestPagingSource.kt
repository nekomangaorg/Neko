package eu.kanade.tachiyomi.ui.source.latest

import androidx.paging.PagingSource
import androidx.paging.PagingState
import eu.kanade.tachiyomi.data.models.DisplayManga

class LatestPagingSource(private val latestSourceRepo: LatestRepository) :
    PagingSource<Int, DisplayManga>() {
    override fun getRefreshKey(state: PagingState<Int, DisplayManga>): Int? {
        return state.anchorPosition
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DisplayManga> {
        return try {
            val nextPage = params.key ?: 1

            val repoResult = latestSourceRepo.getPage(nextPage)
            LoadResult.Page(
                data = repoResult.second,
                prevKey = if (nextPage == 1) null else nextPage - 1,
                nextKey = if (repoResult.first) nextPage + 1 else null,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
