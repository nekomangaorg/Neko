package eu.kanade.tachiyomi.data.database.models

import android.content.Context
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.library.LibrarySort
import java.io.Serializable

interface Category : Serializable {

    var id: Int?

    var name: String

    var order: Int

    var flags: Int

    var mangaOrder: List<Long>

    var mangaSort: Char?

    var isAlone: Boolean

    val nameLower: String
        get() = name.toLowerCase()

    var isHidden: Boolean

    var isDynamic: Boolean

    var sourceId: Long?

    fun isAscending(): Boolean {
        return ((mangaSort?.minus('a') ?: 0) % 2) != 1
    }

    fun sortingMode(nullAsDND: Boolean = false): LibrarySort? = LibrarySort.valueOf(mangaSort)
        ?: if (nullAsDND && !isDynamic) LibrarySort.DragAndDrop else null

    val isDragAndDrop
        get() = (
            mangaSort == null ||
                mangaSort == LibrarySort.DragAndDrop.categoryValue
            ) && !isDynamic

    @StringRes
    fun sortRes(): Int =
        (LibrarySort.valueOf(mangaSort) ?: LibrarySort.DragAndDrop).stringRes(isDynamic)

    fun changeSortTo(sort: Int) {
        mangaSort = (LibrarySort.valueOf(sort) ?: LibrarySort.Title).categoryValue
    }

    companion object {
        fun create(name: String): Category = CategoryImpl().apply {
            this.name = name
        }

        fun createDefault(context: Context): Category =
            create(context.getString(R.string.default_value)).apply {
                id = 0
            }

        fun createCustom(name: String, libSort: Int, ascending: Boolean): Category =
            create(name).apply {
                val librarySort = LibrarySort.valueOf(libSort) ?: LibrarySort.DragAndDrop
                changeSortTo(librarySort.mainValue)
                if (mangaSort != LibrarySort.DragAndDrop.categoryValue && !ascending) {
                    mangaSort = mangaSort?.plus(1)
                }
                isDynamic = true
            }

        fun createAll(context: Context, libSort: Int, ascending: Boolean): Category =
            createCustom(context.getString(R.string.all), libSort, ascending).apply {
                id = -1
                order = -1
                isAlone = true
            }
    }
}
