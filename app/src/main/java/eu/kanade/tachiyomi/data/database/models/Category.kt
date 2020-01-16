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

    companion object {

        fun create(name: String): Category = CategoryImpl().apply {
            this.name = name
        }

        fun createDefault(context: Context): Category = create(context.getString(R.string.default_columns))
            .apply {
            id =
            0 }
    }

}