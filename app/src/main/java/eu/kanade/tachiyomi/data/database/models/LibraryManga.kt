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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LibraryManga) return false
        if (!super.equals(other)) return false

        if (unread != other.unread) return false
        if (read != other.read) return false
        if (category != other.category) return false
        if (bookmarkCount != other.bookmarkCount) return false
        if (unavailableCount != other.unavailableCount) return false
        if (isMerged != other.isMerged) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + unread
        result = 31 * result + read
        result = 31 * result + category
        result = 31 * result + bookmarkCount
        result = 31 * result + unavailableCount
        result = 31 * result + isMerged.hashCode()
        return result
    }

    companion object {
        fun createBlank(categoryId: Int): LibraryManga =
            LibraryManga().apply {
                title = ""
                id = Long.MIN_VALUE
                category = categoryId
            }
    }
}
