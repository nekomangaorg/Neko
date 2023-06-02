package eu.kanade.tachiyomi.data.database.models

data class CustomList(
    val name: String,
    val uuid: String,
    val visibility: String,
) {
    fun toCustomListImpl(): CustomListImpl {
        return CustomListImpl(
            name = this.name,
            uuid = this.uuid,
        )
    }
}

data class CustomListImpl(
    val id: Long? = null,
    val name: String,
    val uuid: String,
)
