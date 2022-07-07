package eu.kanade.tachiyomi.ui.manga

object MergeConstants {

    data class MergeManga(
        val thumbnail: String,
        val url: String,
        val title: String,
    )

    sealed class IsMergedManga {
        class Yes(val url: String) : IsMergedManga()
        object No : IsMergedManga()
    }

    sealed class MergeSearchResult {
        object Loading : MergeSearchResult()
        object NoResult : MergeSearchResult()
        class Success(val mergeMangaList: List<MergeManga>) : MergeSearchResult()
        class Error(val errorMessage: String) : MergeSearchResult()
    }
}
