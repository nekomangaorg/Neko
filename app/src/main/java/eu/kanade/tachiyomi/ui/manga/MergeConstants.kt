package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.models.SourceMergeManga

object MergeConstants {

    sealed class IsMergedManga {
        data class Yes(val url: String, val title: String, val mergeType: MergeType) :
            IsMergedManga()

        object No : IsMergedManga()
    }

    sealed class MergeSearchResult {
        object Loading : MergeSearchResult()

        object NoResult : MergeSearchResult()

        data class Success(val mergeMangaList: List<SourceMergeManga>) : MergeSearchResult()

        data class Error(val errorMessage: String) : MergeSearchResult()
    }
}
