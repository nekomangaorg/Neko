package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.database.models.SourceMergeManga

object MergeConstants {

    sealed class IsMergedManga {
        class Yes(val url: String, val title: String) : IsMergedManga()
        object No : IsMergedManga()
    }

    sealed class MergeSearchResult {
        object Loading : MergeSearchResult()
        object NoResult : MergeSearchResult()
        class Success(val mergeMangaList: List<SourceMergeManga>) : MergeSearchResult()
        class Error(val errorMessage: String) : MergeSearchResult()
    }
}
