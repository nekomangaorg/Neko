package eu.kanade.tachiyomi.data.database.models

class LibraryManga : MangaImpl() {

    var unread: Int = 0
    var read: Int = 0

    var category: Int = 0

    var bookmarkCount: Int = 0

    var unavailableCount: Int = 0

    var isMerged: Boolean = false

    val availableCount
        get() = totalChapters - unavailableCount

    val totalChapters
        get() = read + unread

    val hasStarted
        get() = read > 0

    companion object {
        fun createBlank(categoryId: Int): LibraryManga =
            LibraryManga().apply {
                title = ""
                id = Long.MIN_VALUE
                category = categoryId
            }
    }
}
