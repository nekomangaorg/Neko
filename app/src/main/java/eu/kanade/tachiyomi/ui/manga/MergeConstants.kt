package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.SourceMergeManga

object MergeConstants {

    sealed class IsMergedManga {
        data class Yes(val mergedMangaList: List<MergeMangaImpl>) : IsMergedManga()

        object No : IsMergedManga()
    }

    sealed class MergeSearchResult {
        object Loading : MergeSearchResult()

        object NoResult : MergeSearchResult()

        data class Success(val mergeMangaList: List<SourceMergeManga>) : MergeSearchResult()

        data class Error(val errorMessage: String) : MergeSearchResult()
    }
}
