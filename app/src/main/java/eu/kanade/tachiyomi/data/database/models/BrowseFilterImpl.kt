package eu.kanade.tachiyomi.data.database.models

// TODO rename to Browse Filter
data class BrowseFilterImpl(
    val id: Long? = null,
    val name: String,
    val default: Boolean = false,
    val dexFilters: String,
)
