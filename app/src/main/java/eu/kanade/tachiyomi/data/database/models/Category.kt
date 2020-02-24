package eu.kanade.tachiyomi.data.database.models

import android.content.Context
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.library.LibrarySort
import java.io.Serializable

interface Category : Serializable {

    var id: Int?

    var name: String

    var order: Int

    var flags: Int

    var mangaOrder:List<Long>

    var mangaSort:Char?

    var isFirst:Boolean?

    val nameLower: String
        get() = name.toLowerCase()

    fun isAscending(): Boolean {
       return ((mangaSort?.minus('a') ?: 0) % 2) != 1
    }

    fun sortingMode(): Int? = when (mangaSort) {
        ALPHA_ASC, ALPHA_DSC -> LibrarySort.ALPHA
        UPDATED_ASC, UPDATED_DSC -> LibrarySort.LAST_UPDATED
        UNREAD_ASC, UNREAD_DSC -> LibrarySort.UNREAD
        LAST_READ_ASC, LAST_READ_DSC -> LibrarySort.LAST_READ
        TOTAL_ASC, TOTAL_DSC -> LibrarySort.TOTAL
        DRAG_AND_DROP -> LibrarySort.DRAG_AND_DROP
        else -> null
    }

    fun sortRes(): Int = when (mangaSort) {
        ALPHA_ASC, ALPHA_DSC -> R.string.title
        UPDATED_ASC, UPDATED_DSC -> R.string.action_sort_last_updated
        UNREAD_ASC, UNREAD_DSC ->  R.string.action_filter_unread
        LAST_READ_ASC, LAST_READ_DSC ->  R.string.action_sort_last_read
        TOTAL_ASC, TOTAL_DSC ->  R.string.action_sort_total
        else -> R.string.action_sort_drag_and_drop
    }

    fun catSortingMode(): Int? = when (mangaSort) {
        ALPHA_ASC, ALPHA_DSC -> 0
        UPDATED_ASC, UPDATED_DSC -> 1
        UNREAD_ASC, UNREAD_DSC -> 2
        LAST_READ_ASC, LAST_READ_DSC -> 3
        TOTAL_ASC, TOTAL_DSC -> 4
        else -> null
    }

    companion object {
        private const val DRAG_AND_DROP = 'D'
        private const val ALPHA_ASC = 'a'
        private const val ALPHA_DSC = 'b'
        private const val UPDATED_ASC = 'c'
        private const val UPDATED_DSC = 'd'
        private const val UNREAD_ASC = 'e'
        private const val UNREAD_DSC = 'f'
        private const val LAST_READ_ASC = 'g'
        private const val LAST_READ_DSC = 'h'
        private const val TOTAL_ASC = 'i'
        private const val TOTAL_DSC = 'j'

        fun create(name: String): Category = CategoryImpl().apply {
            this.name = name
        }

        fun createDefault(context: Context): Category =
            create(context.getString(R.string.default_columns)).apply {
                id = 0
            }

        fun createAll(context: Context, libSort: Int, ascending: Boolean): Category =
            create(context.getString(R.string.all)).apply {
                id = -1
                mangaSort = when (libSort) {
                    LibrarySort.ALPHA -> ALPHA_ASC
                    LibrarySort.LAST_UPDATED -> UPDATED_ASC
                    LibrarySort.UNREAD -> UNREAD_ASC
                    LibrarySort.LAST_READ -> LAST_READ_ASC
                    LibrarySort.TOTAL -> TOTAL_ASC
                    LibrarySort.DRAG_AND_DROP -> DRAG_AND_DROP
                    else -> DRAG_AND_DROP
                }
                if (mangaSort != DRAG_AND_DROP && !ascending) {
                    mangaSort?.plus(1)
                }
                order = -1
                isFirst = true
            }
    }

}