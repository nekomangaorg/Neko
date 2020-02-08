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
        else -> null
    }

    fun catSortingMode(): Int? = when (mangaSort) {
        ALPHA_ASC, ALPHA_DSC -> 0
        UPDATED_ASC, UPDATED_DSC -> 1
        UNREAD_ASC, UNREAD_DSC -> 2
        LAST_READ_ASC, LAST_READ_DSC -> 3
        else -> null
    }

    companion object {
        private const val ALPHA_ASC = 'a'
        private const val ALPHA_DSC = 'b'
        private const val UPDATED_ASC = 'c'
        private const val UPDATED_DSC = 'd'
        private const val UNREAD_ASC = 'e'
        private const val UNREAD_DSC = 'f'
        private const val LAST_READ_ASC = 'g'
        private const val LAST_READ_DSC = 'h'

        fun create(name: String): Category = CategoryImpl().apply {
            this.name = name
        }

        fun createDefault(context: Context): Category = create(context.getString(R.string.default_columns))
            .apply {
            id =
            0 }
    }

}