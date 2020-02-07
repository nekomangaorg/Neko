package eu.kanade.tachiyomi.data.database.models

import android.content.Context
import eu.kanade.tachiyomi.R
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

    companion object {
        const val ALPHA_ASC = 'a'
        const val ALPHA_DSC = 'b'
        const val UPDATED_ASC = 'c'
        const val UPDATED_DSC = 'd'
        const val UNREAD_ASC = 'e'
        const val UNREAD_DSC = 'f'
        const val LAST_READ_ASC = 'g'
        const val LAST_READ_DSC = 'h'


        fun create(name: String): Category = CategoryImpl().apply {
            this.name = name
        }

        fun createDefault(context: Context): Category = create(context.getString(R.string.default_columns))
            .apply {
            id =
            0 }
    }

}