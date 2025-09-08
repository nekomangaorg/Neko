package eu.kanade.tachiyomi.ui.library.filter

sealed class FilterDownloaded() {
    object Downloaded : FilterDownloaded()

    object NotDownloaded : FilterDownloaded()

    object Inactive : FilterDownloaded()

    fun toInt(): Int {
        return when (this) {
            Inactive -> 0
            Downloaded -> 1
            NotDownloaded -> 2
        }
    }

    companion object {

        fun fromInt(fromInt: Int): FilterDownloaded {
            return when (fromInt) {
                2 -> NotDownloaded
                1 -> Downloaded
                else -> Inactive
            }
        }
    }
}
